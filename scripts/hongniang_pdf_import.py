#!/usr/bin/env python3
from __future__ import annotations

import argparse
import datetime as dt
import io
import json
import mimetypes
import os
import re
import sys
import uuid
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Any

import boto3
import fitz
import pdfplumber
import pymysql


DEFAULT_DB_HOST = os.getenv("YUELAO_MASTER_DB_HOST", "localhost")
DEFAULT_DB_PORT = int(os.getenv("YUELAO_MASTER_DB_PORT", "3306"))
DEFAULT_DB_NAME = os.getenv("YUELAO_MASTER_DB", os.getenv("YUELAO_APP_DB", "bang_yi"))
DEFAULT_DB_USER = os.getenv("YUELAO_MASTER_DB_USERNAME", os.getenv("YUELAO_APP_DB_USERNAME", "root"))
DEFAULT_DB_PASSWORD = os.getenv("YUELAO_MASTER_DB_PASSWORD", os.getenv("YUELAO_APP_DB_PASSWORD", "123456"))


FIELD_ALIASES = {
    "性别": "gender",
    "学历": "education",
    "籍贯": "native_place",
    "居住地": "residence",
    "出生年月": "birth_text",
    "身高": "height",
    "体重": "weight",
    "工作单位": "job",
    "月薪": "salary",
    "婚史": "marital_status",
    "婚史简述": "marital_status",
    "房车情况": "house_car_status",
    "家庭情况": "family_info",
    "本人健康状况": "health_status",
    "择偶要求": "mate_requirement",
    "联系方式": "contact_text",
}


@dataclass
class OssConfig:
    provider_code: str
    endpoint: str
    domain: str
    prefix: str
    access_key: str
    secret_key: str
    bucket_name: str
    region: str
    is_https: str

    @property
    def scheme(self) -> str:
        return "https://" if str(self.is_https or "Y").upper() == "Y" else "http://"

    @property
    def endpoint_without_scheme(self) -> str:
        value = (self.endpoint or "").strip()
        return re.sub(r"^https?://", "", value)

    @property
    def base_url(self) -> str:
        endpoint = self.endpoint_without_scheme
        domain = re.sub(r"^https?://", "", (self.domain or "").strip())
        bucket = (self.bucket_name or "").strip()
        cloud_like = any(token in endpoint for token in ("aliyuncs.com", "myqcloud.com", "qiniucs.com", "amazonaws.com"))
        if cloud_like:
            if domain:
                if endpoint.startswith(f"{bucket}.") and "qiniucs.com" in endpoint:
                    return f"{self.scheme}{domain.lstrip('/')}/{bucket}"
                return f"{self.scheme}{domain.lstrip('/')}"
            if endpoint.startswith(f"{bucket}."):
                return f"{self.scheme}{endpoint}"
            return f"{self.scheme}{bucket}.{endpoint}"
        if domain:
            return f"{self.scheme}{domain.lstrip('/')}/{bucket}"
        if endpoint.startswith(f"{bucket}."):
            return f"{self.scheme}{endpoint}"
        return f"{self.scheme}{endpoint}/{bucket}"


@dataclass
class ParsedProfile:
    page_number: int
    hongniang_id: int
    hongniang_user_no: int | None = None
    member_number_text: str = ""
    gender: int = 0
    education: str = ""
    native_place: str = ""
    residence: str = ""
    birth_text: str = ""
    age: int | None = None
    height: str = ""
    weight: str = ""
    job: str = ""
    salary: str = ""
    marital_status: str = ""
    house_car_status: str = ""
    family_info: str = ""
    health_status: str = ""
    mate_requirement: str = ""
    recommender_name: str = ""
    recommender_phone: str = ""
    contact_text: str = ""
    mobile: str = ""
    avatar_url: str = ""
    username: str = ""
    raw_text: str = ""


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Import hongniang PDF profiles into AiLeMe database.")
    parser.add_argument("--pdf", required=True, help="Path to source PDF.")
    parser.add_argument("--hongniang-id", type=int, required=True, help="Hongniang ID, e.g. 14.")
    parser.add_argument("--limit", type=int, default=3, help="Only import the first N parsed profiles.")
    parser.add_argument("--dry-run", action="store_true", help="Parse and upload nothing, write preview JSON only.")
    parser.add_argument("--machine-json", action="store_true", help="Print a single machine-readable JSON summary to stdout.")
    parser.add_argument("--preview-json", default="tmp/hongniang-import-preview.json", help="Where to write preview JSON.")
    parser.add_argument("--db-host", default=DEFAULT_DB_HOST)
    parser.add_argument("--db-port", type=int, default=DEFAULT_DB_PORT)
    parser.add_argument("--db-name", default=DEFAULT_DB_NAME)
    parser.add_argument("--db-user", default=DEFAULT_DB_USER)
    parser.add_argument("--db-password", default=DEFAULT_DB_PASSWORD)
    return parser.parse_args()


def connect_db(args: argparse.Namespace):
    return pymysql.connect(
        host=args.db_host,
        port=args.db_port,
        user=args.db_user,
        password=args.db_password,
        database=args.db_name,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
        autocommit=False,
    )


def fetch_current_oss_config(conn) -> OssConfig:
    with conn.cursor() as cursor:
        cursor.execute(
            """
            SELECT provider_code, config_json, is_current, is_enabled
            FROM sys_third_party_provider
            WHERE service_type = 'oss' AND is_enabled = 1
            ORDER BY is_current DESC, display_order ASC, provider_id ASC
            LIMIT 1
            """
        )
        row = cursor.fetchone()
    if not row:
        raise RuntimeError("No enabled OSS provider found in sys_third_party_provider")
    config = json.loads(row["config_json"] or "{}")
    return OssConfig(
        provider_code=row["provider_code"],
        endpoint=first_non_blank(config, "endpoint"),
        domain=first_non_blank(config, "domain"),
        prefix=first_non_blank(config, "prefix"),
        access_key=first_non_blank(config, "accessKey", "access_key", "secret_id", "access_key_id"),
        secret_key=first_non_blank(config, "secretKey", "secret_key", "secret", "access_key_secret"),
        bucket_name=first_non_blank(config, "bucketName", "bucket", "bucket_name"),
        region=first_non_blank(config, "region") or "us-east-1",
        is_https=first_non_blank(config, "isHttps", "is_https") or "Y",
    )


def first_non_blank(mapping: dict[str, Any], *keys: str) -> str:
    for key in keys:
        value = mapping.get(key)
        if value is not None and str(value).strip():
            return str(value).strip()
    return ""


def extract_profiles(pdf_path: Path, hongniang_id: int, limit: int) -> list[ParsedProfile]:
    with pdfplumber.open(str(pdf_path)) as plumber_doc, fitz.open(str(pdf_path)) as fitz_doc:
        profiles: list[ParsedProfile] = []
        max_pages = min(len(plumber_doc.pages), limit * 4 if limit > 0 else len(plumber_doc.pages))
        for index in range(max_pages):
            text = (plumber_doc.pages[index].extract_text() or "").strip()
            if not text:
                continue
            profile = parse_profile_block(text, page_number=index + 1, hongniang_id=hongniang_id)
            if not profile or profile.hongniang_user_no is None:
                continue
            image_payload = extract_best_page_image(fitz_doc[index])
            if image_payload:
                profile.avatar_url = image_payload  # temporary bytes marker replaced later
            profiles.append(profile)
            if limit > 0 and len(profiles) >= limit:
                break
    return profiles


def parse_profile_block(text: str, page_number: int, hongniang_id: int) -> ParsedProfile | None:
    normalized = re.sub(r"\u3000", " ", text).strip()
    if "联系方式" not in normalized or "编号" not in normalized:
        return None
    profile = ParsedProfile(page_number=page_number, hongniang_id=hongniang_id, raw_text=normalized)
    lines = [cleanup_line(line) for line in normalized.splitlines() if cleanup_line(line)]
    if not lines:
        return None
    header = next((line for line in lines if line.startswith("编号")), "")
    profile.member_number_text = extract_value(header) if header else ""
    no_match = re.search(r"(\d+)\s*号", header)
    if no_match:
        profile.hongniang_user_no = int(no_match.group(1))

    for line in lines:
        matched = False
        for label, attr in FIELD_ALIASES.items():
            if line.startswith(label):
                value = extract_value(line)
                setattr(profile, attr, value)
                matched = True
                break
        if matched:
            continue

    profile.gender = normalize_gender(profile.gender if isinstance(profile.gender, int) else 0, profile.raw_text)
    profile.age = extract_age(profile.birth_text)
    profile.height = extract_number_text(profile.height, "cm")
    profile.weight = extract_number_text(profile.weight, "斤")
    profile.recommender_name = extract_recommender_name(profile.contact_text)
    profile.recommender_phone = extract_phone(profile.contact_text)
    profile.mobile = extract_user_mobile(profile.contact_text)
    profile.username = build_initial_username(profile)
    return profile


def cleanup_line(value: str) -> str:
    return re.sub(r"\s+", " ", (value or "").strip())


def extract_value(line: str) -> str:
    if "：" in line:
        return line.split("：", 1)[1].strip()
    if ":" in line:
        return line.split(":", 1)[1].strip()
    return ""


def normalize_gender(current: int, raw_text: str) -> int:
    if current in (1, 2):
        return current
    if "性别：男" in raw_text or "性别:男" in raw_text:
        return 1
    if "性别：女" in raw_text or "性别:女" in raw_text:
        return 2
    return 0


def extract_age(value: str) -> int | None:
    match = re.search(r"(\d{1,2})\s*周岁", value or "")
    return int(match.group(1)) if match else None


def extract_number_text(value: str, unit: str) -> str:
    match = re.search(r"(\d{2,3})", value or "")
    return f"{match.group(1)}{unit}" if match else (value or "").strip()


def extract_recommender_name(value: str) -> str:
    match = re.search(r"推荐人\s*([^，,。 ]+)", value or "")
    return match.group(1).strip() if match else ""


def extract_phone(value: str) -> str:
    match = re.search(r"(1[3-9]\d{9})", value or "")
    return match.group(1) if match else ""


def extract_user_mobile(value: str) -> str:
    phones = re.findall(r"(1[3-9]\d{9})", value or "")
    if len(phones) >= 2:
        return phones[-1]
    return ""


def build_initial_username(profile: ParsedProfile) -> str:
    gender_tag = {1: "male", 2: "female"}.get(profile.gender, "user")
    return f"hn{profile.hongniang_id}_no{int(profile.hongniang_user_no or 0):03d}_{gender_tag}"


def extract_best_page_image(page: fitz.Page) -> bytes | None:
    candidates: list[tuple[int, bytes]] = []
    for image in page.get_images(full=True):
        xref = image[0]
        try:
            base = page.parent.extract_image(xref)
        except Exception:
            continue
        width = int(base.get("width") or 0)
        height = int(base.get("height") or 0)
        image_bytes = base.get("image")
        if not image_bytes or width * height < 160 * 160:
            continue
        candidates.append((width * height, image_bytes))
    if not candidates:
        return None
    candidates.sort(key=lambda item: item[0], reverse=True)
    return candidates[0][1]


def build_s3_client(config: OssConfig):
    endpoint = config.endpoint
    if endpoint and not endpoint.startswith("http://") and not endpoint.startswith("https://"):
        endpoint = f"{config.scheme}{endpoint}"
    session = boto3.session.Session()
    return session.client(
        "s3",
        aws_access_key_id=config.access_key,
        aws_secret_access_key=config.secret_key,
        region_name=config.region or "us-east-1",
        endpoint_url=endpoint,
    )


def build_oss_key(prefix: str, suffix: str) -> str:
    date_path = dt.datetime.now().strftime("%Y/%m/%d")
    key = f"{date_path}/{uuid.uuid4().hex}{suffix}"
    return f"{prefix.strip('/')}/{key}" if prefix else key


def upload_avatar_if_needed(client, config: OssConfig, profile: ParsedProfile) -> str:
    if not isinstance(profile.avatar_url, (bytes, bytearray)):
        return profile.avatar_url or ""
    content = bytes(profile.avatar_url)
    suffix = ".png"
    key = build_oss_key(config.prefix, suffix)
    content_type = mimetypes.guess_type(f"avatar{suffix}")[0] or "image/png"
    client.put_object(Bucket=config.bucket_name, Key=key, Body=content, ContentType=content_type)
    return f"{config.base_url}/{key}"


def ensure_unique_username(cursor, candidate: str) -> str:
    username = candidate
    suffix = 0
    while True:
        cursor.execute("SELECT uid FROM `user` WHERE username = %s LIMIT 1", (username,))
        row = cursor.fetchone()
        if not row:
            return username
        suffix += 1
        username = f"{candidate}_{suffix}"


def build_info_text(profile: ParsedProfile) -> str:
    lines = []
    if profile.family_info:
        lines.append(f"家庭情况：{profile.family_info}")
    if profile.house_car_status:
        lines.append(f"房车情况：{profile.house_car_status}")
    if profile.health_status:
        lines.append(f"健康状况：{profile.health_status}")
    if profile.salary:
        lines.append(f"月薪：{profile.salary}")
    if profile.recommender_name or profile.recommender_phone:
        lines.append(f"推荐人：{profile.recommender_name} {profile.recommender_phone}".strip())
    return "\n".join(lines)


def upsert_profile(conn, profile: ParsedProfile, import_file_name: str):
    with conn.cursor() as cursor:
        cursor.execute(
            """
            SELECT id, user_id
            FROM hongniang_user_relation
            WHERE hongniang_id = %s AND hongniang_user_no = %s
            LIMIT 1
            """,
            (profile.hongniang_id, profile.hongniang_user_no),
        )
        relation_row = cursor.fetchone()
        user_id = relation_row["user_id"] if relation_row else None

        if user_id:
            cursor.execute("SELECT uid FROM `user` WHERE uid = %s LIMIT 1", (user_id,))
            user_row = cursor.fetchone()
            if not user_row:
                user_id = None

        if not user_id:
            username = ensure_unique_username(cursor, profile.username)
            cursor.execute(
                """
                INSERT INTO `user`
                (mobile, email, username, password, group_id, avatar, gender, province, city, status, intro, money,
                 integral, sign_num, last_login_ip, tag_str, vip, type, update_time, create_time, height, home_city,
                 abode_city, job, marry_status, education, info, adminre_herart, audit_status, age, birthday, level, hongniang_id)
                VALUES
                (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW(), %s, %s,
                 %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    profile.mobile or None,
                    None,
                    username,
                    None,
                    None,
                    profile.avatar_url or None,
                    profile.gender or 0,
                    None,
                    profile.residence or None,
                    0,
                    profile.member_number_text or None,
                    None,
                    None,
                    None,
                    None,
                    "红娘导入",
                    0,
                    0,
                    profile.height or None,
                    profile.native_place or None,
                    profile.residence or None,
                    profile.job or None,
                    map_marry_status(profile.marital_status),
                    map_education(profile.education),
                    build_info_text(profile) or None,
                    profile.mate_requirement or None,
                    0,
                    profile.age,
                    profile.birth_text or None,
                    None,
                    profile.hongniang_id,
                ),
            )
            user_id = cursor.lastrowid
        else:
            cursor.execute(
                """
                UPDATE `user`
                SET avatar = COALESCE(%s, avatar),
                    gender = CASE WHEN %s > 0 THEN %s ELSE gender END,
                    age = COALESCE(%s, age),
                    height = COALESCE(%s, height),
                    home_city = COALESCE(%s, home_city),
                    abode_city = COALESCE(%s, abode_city),
                    job = COALESCE(%s, job),
                    marry_status = COALESCE(%s, marry_status),
                    education = COALESCE(%s, education),
                    info = COALESCE(%s, info),
                    adminre_herart = COALESCE(%s, adminre_herart),
                    birthday = COALESCE(%s, birthday),
                    hongniang_id = COALESCE(%s, hongniang_id),
                    update_time = NOW()
                WHERE uid = %s
                """,
                (
                    profile.avatar_url or None,
                    profile.gender or 0,
                    profile.gender or 0,
                    profile.age,
                    profile.height or None,
                    profile.native_place or None,
                    profile.residence or None,
                    profile.job or None,
                    map_marry_status(profile.marital_status),
                    map_education(profile.education),
                    build_info_text(profile) or None,
                    profile.mate_requirement or None,
                    profile.birth_text or None,
                    profile.hongniang_id,
                    user_id,
                ),
            )

        if relation_row:
            cursor.execute(
                """
                UPDATE hongniang_user_relation
                SET user_id = %s,
                    source_type = 1,
                    import_file_name = %s,
                    remark = %s,
                    create_time = COALESCE(create_time, NOW())
                WHERE id = %s
                """,
                (user_id, import_file_name, build_relation_remark(profile), relation_row["id"]),
            )
        else:
            cursor.execute(
                """
                INSERT INTO hongniang_user_relation
                (hongniang_id, user_id, hongniang_user_no, source_type, import_file_name, remark, create_time)
                VALUES (%s, %s, %s, %s, %s, %s, NOW())
                """,
                (profile.hongniang_id, user_id, profile.hongniang_user_no, 1, import_file_name, build_relation_remark(profile)),
            )

        return user_id


def build_relation_remark(profile: ParsedProfile) -> str:
    values = [
        f"会员编号:{profile.member_number_text}" if profile.member_number_text else "",
        f"推荐人:{profile.recommender_name}" if profile.recommender_name else "",
        f"推荐电话:{profile.recommender_phone}" if profile.recommender_phone else "",
    ]
    return " | ".join([item for item in values if item])[:255]


def map_education(value: str) -> int | None:
    if not value:
        return None
    mapping = [
        ("博士", 8),
        ("硕士", 7),
        ("本科", 6),
        ("大专", 5),
        ("中专", 4),
        ("高中", 3),
        ("初中", 2),
        ("小学", 1),
    ]
    for token, code in mapping:
        if token in value:
            return code
    return None


def map_marry_status(value: str) -> int | None:
    if not value:
        return None
    if "离异" in value or "离婚" in value:
        return 1
    if "丧偶" in value:
        return 2
    if "未婚" in value:
        return 0
    return None


def write_preview(path: Path, profiles: list[ParsedProfile]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = [asdict(profile) | {"avatar_url": profile.avatar_url if isinstance(profile.avatar_url, str) else "<binary-image>"} for profile in profiles]
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def main() -> int:
    args = parse_args()
    pdf_path = Path(args.pdf).expanduser().resolve()
    if not pdf_path.exists():
        raise FileNotFoundError(f"PDF not found: {pdf_path}")

    profiles = extract_profiles(pdf_path, args.hongniang_id, args.limit)
    if not profiles:
        print("No profiles parsed from PDF", file=sys.stderr)
        return 1

    write_preview(Path(args.preview_json), profiles)
    preview_payload = {
        "count": len(profiles),
        "preview_json": str(args.preview_json),
        "rows": [
            {
                "page_number": profile.page_number,
                "hongniang_user_no": profile.hongniang_user_no,
                "username": profile.username,
            }
            for profile in profiles
        ],
    }
    if args.machine_json:
        if args.dry_run:
            print(json.dumps(preview_payload, ensure_ascii=False))
            return 0
    else:
        print(f"Parsed {len(profiles)} profiles, preview saved to {args.preview_json}")

    if args.dry_run:
        return 0

    conn = connect_db(args)
    try:
        oss_config = fetch_current_oss_config(conn)
        oss_client = build_s3_client(oss_config)
        imported_rows = []
        for profile in profiles:
            profile.avatar_url = upload_avatar_if_needed(oss_client, oss_config, profile)
            user_id = upsert_profile(conn, profile, pdf_path.name)
            imported_rows.append(
                {
                    "page_number": profile.page_number,
                    "hongniang_user_no": profile.hongniang_user_no,
                    "user_id": user_id,
                    "avatar_url": profile.avatar_url,
                }
            )
        conn.commit()
        summary_payload = {
            "count": len(imported_rows),
            "preview_json": str(args.preview_json),
            "rows": imported_rows,
        }
        if args.machine_json:
            print(json.dumps(summary_payload, ensure_ascii=False))
        else:
            print(json.dumps(imported_rows, ensure_ascii=False, indent=2))
        return 0
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    raise SystemExit(main())
