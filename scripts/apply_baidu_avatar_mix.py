#!/usr/bin/env python3
import argparse
import json
import math
import re
import urllib.request

import pymysql


DB_CONFIG = {
    "host": "127.0.0.1",
    "user": "root",
    "password": "123456",
    "database": "bang_yi",
    "charset": "utf8mb4",
    "autocommit": False,
}

TARGET_SQL = """
select uid, username, gender, avatar, figur, info, type
from user
where uid < 106 or type = 2
order by uid asc
"""

BAIDU_DISCOVERED_PAGES = {
    2: [
        "https://www.itouxiang.com/touxiang/57739066.html",
        "https://www.itouxiang.com/touxiang/77861119.html",
        "https://www.keaitupian.cn/touxiang/76273_4.html",
        "https://www.keaitupian.cn/touxiang/77207_2.html",
    ],
    1: [
        "https://www.tuxiangyan.com/touxiang/42033.html",
        "https://www.itouxiang.com/touxiang/64686752.html",
        "https://www.itouxiang.com/touxiang/97563730.html",
        "https://www.tuxiangyan.com/touxiang/33975.html",
    ],
}


def build_parser():
    parser = argparse.ArgumentParser(description="Use Baidu-discovered Chinese portrait pages as primary avatar source")
    parser.add_argument("--execute", action="store_true", help="apply updates")
    return parser


def fetch_html(url):
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=30) as resp:
        return resp.read().decode("utf-8", "ignore")


def scrape_page_images(url):
    html = fetch_html(url)
    raw = re.findall(r'<img[^>]+src=["\']([^"\']+)["\']', html, re.I)
    items = []
    for img in raw:
        if not img.startswith("http"):
            continue
        if any(token in img for token in [
            "/images/logo", "/style/images/", "/fx_", "/static.itouxiang.com/images/logo",
        ]):
            continue
        if not any(domain in img for domain in [
            "img.itouxiang.com",
            "img.tuxiangyan.com",
            "img.keaitupian.cn",
            "www.keaitupian.cn/cjpic/frombd/",
        ]):
            continue
        items.append(img)
    dedup = []
    seen = set()
    for item in items:
        if item not in seen:
            dedup.append(item)
            seen.add(item)
    return dedup


def build_baidu_pool():
    pools = {1: [], 2: []}
    for gender, pages in BAIDU_DISCOVERED_PAGES.items():
        seen = set()
        for url in pages:
            for item in scrape_page_images(url):
                if item not in seen:
                    pools[gender].append(item)
                    seen.add(item)
    return pools


def normalize_gender(row):
    if row["gender"] in (1, 2):
        return int(row["gender"])
    username = row["username"] or ""
    if re.search(r"(姐|妹|宁|妍|雨|橙|静姝|嘉宁|南栀|亦宁)", username):
        return 2
    return 1


def avatar_source(avatar):
    value = (avatar or "").strip()
    if not value:
        return "empty"
    if value.startswith("https://randomuser.me/"):
        return "randomuser"
    if value.startswith("https://i.pravatar.cc/"):
        return "pravatar"
    return "baidu_like"


def replace_in_figur(figur, old_avatar, new_avatar):
    raw = (figur or "").strip()
    if not raw:
        return new_avatar
    parts = [item.strip() for item in raw.split(",") if item.strip()]
    if not parts:
        return new_avatar
    replaced = []
    for item in parts:
        if item == old_avatar or avatar_source(item) in ("randomuser", "pravatar"):
            candidate = new_avatar
        else:
            candidate = item
        replaced.append(candidate)
    if new_avatar not in replaced:
        replaced.insert(0, new_avatar)
    dedup = []
    seen = set()
    for item in replaced:
        if item not in seen:
            dedup.append(item)
            seen.add(item)
    return ",".join(dedup[:9])


def replace_in_info(info, new_avatar):
    if not info:
        return json.dumps({"mohuAvatar": new_avatar}, ensure_ascii=False)
    try:
        data = json.loads(info)
        if not isinstance(data, dict):
            data = {}
    except Exception:
        data = {}
    data["mohuAvatar"] = new_avatar
    return json.dumps(data, ensure_ascii=False)


def pick_targets(rows):
    grouped = {1: [], 2: []}
    for row in rows:
        row["safe_gender"] = normalize_gender(row)
        grouped[row["safe_gender"]].append(row)

    chosen = []
    for gender, items in grouped.items():
        total = len(items)
        target_baidu = math.ceil(total * 2 / 3)
        current_baidu = sum(1 for row in items if avatar_source(row["avatar"]) == "baidu_like")
        need = max(0, target_baidu - current_baidu)
        candidates = sorted(
            [row for row in items if avatar_source(row["avatar"]) in ("pravatar", "randomuser")],
            key=lambda row: (0 if avatar_source(row["avatar"]) == "pravatar" else 1, row["uid"])
        )
        chosen.extend(candidates[:need])
    return chosen


def main():
    args = build_parser().parse_args()
    baidu_pools = build_baidu_pool()

    conn = pymysql.connect(**DB_CONFIG)
    try:
        with conn.cursor(pymysql.cursors.DictCursor) as cur:
            cur.execute(TARGET_SQL)
            rows = cur.fetchall()

            cur.execute("select avatar from user where avatar is not null and avatar <> ''")
            used_avatars = {row["avatar"].strip() for row in cur.fetchall() if row["avatar"]}

            targets = pick_targets(rows)
            updates = []
            pool_index = {1: 0, 2: 0}
            for row in targets:
                gender = row["safe_gender"]
                pool = baidu_pools[gender]
                while pool_index[gender] < len(pool) and pool[pool_index[gender]] in used_avatars:
                    pool_index[gender] += 1
                if pool_index[gender] >= len(pool):
                    raise RuntimeError(f'性别 {gender} 的百度头像池不够用了')
                new_avatar = pool[pool_index[gender]]
                pool_index[gender] += 1
                used_avatars.add(new_avatar)
                updates.append({
                    "uid": row["uid"],
                    "username": row["username"],
                    "gender": gender,
                    "old_avatar": row["avatar"],
                    "new_avatar": new_avatar,
                    "figur": replace_in_figur(row["figur"], row["avatar"], new_avatar),
                    "info": replace_in_info(row["info"], new_avatar),
                })

            print(f"scraped_male_pool={len(baidu_pools[1])}")
            print(f"scraped_female_pool={len(baidu_pools[2])}")
            print(f"target_updates={len(updates)}")
            for item in updates[:20]:
                print(item["uid"], item["username"], item["gender"], "=>", item["new_avatar"])

            if not args.execute:
                conn.rollback()
                print("dry_run_only=true")
                return

            sql = """
                update user
                set avatar=%s,
                    figur=%s,
                    info=%s
                where uid=%s
            """
            payload = [
                (item["new_avatar"], item["figur"], item["info"], item["uid"])
                for item in updates
            ]
            cur.executemany(sql, payload)

        conn.commit()
        print("updated_users=", len(payload))
    finally:
        conn.close()


if __name__ == "__main__":
    main()
