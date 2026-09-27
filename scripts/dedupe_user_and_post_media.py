#!/usr/bin/env python3
import argparse
import json
from collections import Counter

import pymysql


DB_CONFIG = {
    "host": "127.0.0.1",
    "user": "root",
    "password": "123456",
    "database": "bang_yi",
    "charset": "utf8mb4",
    "autocommit": False,
}


def build_parser():
    parser = argparse.ArgumentParser(description="Deduplicate repeated user/profile/post media in place")
    parser.add_argument("--execute", action="store_true", help="apply updates")
    parser.add_argument("--limit-users", type=int, default=0, help="only inspect first N users")
    parser.add_argument("--limit-posts", type=int, default=0, help="only inspect first N posts")
    parser.add_argument(
        "--scope",
        choices=("single-record", "cross-record"),
        default="single-record",
        help="single-record: only remove duplicates inside one row; cross-record: keep first usage globally",
    )
    return parser


def normalize_text(value):
    return str(value or "").strip()


def parse_media_field(raw):
    text = normalize_text(raw)
    if not text:
        return [], "empty"
    if text.startswith("[") and text.endswith("]"):
        try:
            parsed = json.loads(text)
            if isinstance(parsed, list):
                return [normalize_text(item) for item in parsed if normalize_text(item)], "json"
        except Exception:
            pass
    return [normalize_text(item) for item in text.split(",") if normalize_text(item)], "csv"


def render_media_field(items, field_type):
    cleaned = [normalize_text(item) for item in items if normalize_text(item)]
    if field_type == "json":
        return json.dumps(cleaned, ensure_ascii=False)
    if field_type == "empty":
        return ""
    return ",".join(cleaned)


def dedupe_preserve_order(items):
    result = []
    seen = set()
    for item in items:
        key = normalize_text(item)
        if not key or key in seen:
            continue
        seen.add(key)
        result.append(key)
    return result


def dedupe_user_record(row):
    avatar = normalize_text(row["avatar"])
    figures, figure_type = parse_media_field(row["figur"])
    original_figures = list(figures)

    # 头像字段保留为主图；形象照列表中去掉与头像重复的项，再做去重。
    normalized_figures = []
    seen = {avatar} if avatar else set()
    removed_avatar_duplicates = 0
    removed_internal_duplicates = 0

    for item in figures:
        if avatar and item == avatar:
            removed_avatar_duplicates += 1
            continue
        if item in seen:
            removed_internal_duplicates += 1
            continue
        seen.add(item)
        normalized_figures.append(item)

    next_figur = render_media_field(normalized_figures, figure_type)
    changed = (removed_avatar_duplicates + removed_internal_duplicates) > 0
    return {
        "uid": row["uid"],
        "username": row["username"],
        "avatar": avatar,
        "old_figur_items": original_figures,
        "new_figur_items": normalized_figures,
        "new_figur": next_figur,
        "changed": changed,
        "removed_avatar_duplicates": removed_avatar_duplicates,
        "removed_internal_duplicates": removed_internal_duplicates,
    }


def dedupe_post_record(row):
    medias, media_type = parse_media_field(row["media"])
    deduped = dedupe_preserve_order(medias)
    next_media = render_media_field(deduped, media_type)
    removed = max(0, len(medias) - len(deduped))
    return {
        "id": row["id"],
        "uid": row["uid"],
        "type": row["type"],
        "old_media_items": medias,
        "new_media_items": deduped,
        "new_media": next_media,
        "changed": removed > 0,
        "removed_duplicates": removed,
    }


def dedupe_user_record_cross(row, seen_avatar_urls, seen_figur_urls):
    avatar = normalize_text(row["avatar"])
    figures, figure_type = parse_media_field(row["figur"])
    original_figures = list(figures)

    avatar_removed = 0
    if avatar:
        if avatar in seen_avatar_urls:
            avatar = ""
            avatar_removed = 1
        else:
            seen_avatar_urls.add(avatar)

    normalized_figures = []
    removed_avatar_duplicates = 0
    removed_internal_duplicates = 0
    removed_cross_duplicates = 0
    local_seen = {avatar} if avatar else set()

    for item in figures:
        if avatar and item == avatar:
            removed_avatar_duplicates += 1
            continue
        if item in local_seen:
            removed_internal_duplicates += 1
            continue
        if item in seen_figur_urls:
            removed_cross_duplicates += 1
            continue
        local_seen.add(item)
        seen_figur_urls.add(item)
        normalized_figures.append(item)

    next_figur = render_media_field(normalized_figures, figure_type)
    changed = (avatar_removed + removed_avatar_duplicates + removed_internal_duplicates + removed_cross_duplicates) > 0
    return {
        "uid": row["uid"],
        "username": row["username"],
        "old_avatar": normalize_text(row["avatar"]),
        "new_avatar": avatar,
        "old_figur_items": original_figures,
        "new_figur_items": normalized_figures,
        "new_figur": next_figur,
        "changed": changed,
        "avatar_removed": avatar_removed,
        "removed_avatar_duplicates": removed_avatar_duplicates,
        "removed_internal_duplicates": removed_internal_duplicates,
        "removed_cross_duplicates": removed_cross_duplicates,
    }


def dedupe_post_record_cross(row, seen_post_media_urls):
    medias, media_type = parse_media_field(row["media"])
    original_medias = list(medias)
    normalized = []
    local_seen = set()
    removed_internal_duplicates = 0
    removed_cross_duplicates = 0

    for item in medias:
        if item in local_seen:
            removed_internal_duplicates += 1
            continue
        local_seen.add(item)
        if item in seen_post_media_urls:
            removed_cross_duplicates += 1
            continue
        seen_post_media_urls.add(item)
        normalized.append(item)

    next_media = render_media_field(normalized, media_type)
    removed = removed_internal_duplicates + removed_cross_duplicates
    return {
        "id": row["id"],
        "uid": row["uid"],
        "type": row["type"],
        "old_media_items": original_medias,
        "new_media_items": normalized,
        "new_media": next_media,
        "changed": removed > 0,
        "removed_duplicates": removed,
        "removed_internal_duplicates": removed_internal_duplicates,
        "removed_cross_duplicates": removed_cross_duplicates,
    }


def fetch_rows(cur, sql, limit):
    final_sql = sql
    if limit and limit > 0:
        final_sql = f"{sql} limit {int(limit)}"
    cur.execute(final_sql)
    return cur.fetchall()


def main():
    args = build_parser().parse_args()
    conn = pymysql.connect(**DB_CONFIG)
    try:
        with conn.cursor(pymysql.cursors.DictCursor) as cur:
            user_rows = fetch_rows(
                cur,
                """
                select uid, username, avatar, figur
                from user
                where (figur is not null and figur <> '')
                   or (avatar is not null and avatar <> '')
                order by uid asc
                """,
                args.limit_users,
            )
            post_rows = fetch_rows(
                cur,
                """
                select id, uid, type, media
                from post
                where media is not null and media <> ''
                order by id asc
                """,
                args.limit_posts,
            )

            if args.scope == "cross-record":
                seen_avatar_urls = set()
                seen_figur_urls = set()
                user_updates = [
                    dedupe_user_record_cross(row, seen_avatar_urls, seen_figur_urls)
                    for row in user_rows
                ]
            else:
                user_updates = [dedupe_user_record(row) for row in user_rows]
            user_updates = [item for item in user_updates if item["changed"]]

            if args.scope == "cross-record":
                seen_post_media_urls = set()
                post_updates = [
                    dedupe_post_record_cross(row, seen_post_media_urls)
                    for row in post_rows
                ]
            else:
                post_updates = [dedupe_post_record(row) for row in post_rows]
            post_updates = [item for item in post_updates if item["changed"]]

            user_removed_avatar_duplicates = sum(item.get("removed_avatar_duplicates", 0) for item in user_updates)
            user_removed_internal_duplicates = sum(item["removed_internal_duplicates"] for item in user_updates)
            user_removed_cross_duplicates = sum(item.get("removed_cross_duplicates", 0) for item in user_updates)
            user_removed_avatar_cross = sum(item.get("avatar_removed", 0) for item in user_updates)
            post_removed_duplicates = sum(item["removed_duplicates"] for item in post_updates)
            post_removed_cross_duplicates = sum(item.get("removed_cross_duplicates", 0) for item in post_updates)
            post_type_counter = Counter(item["type"] for item in post_updates)

            print(f"scope={args.scope}")
            print(f"user_updates={len(user_updates)}")
            print(f"user_removed_cross_avatar_fields={user_removed_avatar_cross}")
            print(f"user_removed_avatar_duplicates={user_removed_avatar_duplicates}")
            print(f"user_removed_internal_duplicates={user_removed_internal_duplicates}")
            print(f"user_removed_cross_duplicates={user_removed_cross_duplicates}")
            for item in user_updates[:20]:
                print(
                    f"user uid={item['uid']} username={item['username']} "
                    f"avatar={'drop' if item.get('avatar_removed') else 'keep'} "
                    f"figur {len(item['old_figur_items'])}->{len(item['new_figur_items'])}"
                )

            print(f"post_updates={len(post_updates)}")
            print(f"post_removed_duplicates={post_removed_duplicates}")
            print(f"post_removed_cross_duplicates={post_removed_cross_duplicates}")
            print(f"post_type_breakdown={dict(sorted(post_type_counter.items()))}")
            for item in post_updates[:20]:
                print(
                    f"post id={item['id']} uid={item['uid']} type={item['type']} "
                    f"media {len(item['old_media_items'])}->{len(item['new_media_items'])}"
                )

            if not args.execute:
                conn.rollback()
                print("dry_run_only=true")
                return

            if user_updates:
                cur.executemany(
                    """
                    update user
                    set avatar=%s,
                        figur=%s
                    where uid=%s
                    """,
                    [(item.get("new_avatar", item.get("avatar", "")), item["new_figur"], item["uid"]) for item in user_updates],
                )

            if post_updates:
                cur.executemany(
                    """
                    update post
                    set media=%s
                    where id=%s
                    """,
                    [(item["new_media"], item["id"]) for item in post_updates],
                )

        conn.commit()
        print(f"updated_users={len(user_updates)}")
        print(f"updated_posts={len(post_updates)}")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
