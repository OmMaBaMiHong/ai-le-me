#!/usr/bin/env python3
import argparse

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
    parser = argparse.ArgumentParser(description="Sync topic cover_image from owner user avatar")
    parser.add_argument("--execute", action="store_true", help="apply updates")
    parser.add_argument(
        "--only-changed",
        action="store_true",
        help="only update topics whose cover_image differs from user.avatar",
    )
    return parser


def normalize_text(value):
    return str(value or "").strip()


def main():
    args = build_parser().parse_args()
    conn = pymysql.connect(**DB_CONFIG)
    try:
        with conn.cursor(pymysql.cursors.DictCursor) as cur:
            cur.execute(
                """
                select
                    t.id,
                    t.uid,
                    t.topic_name,
                    t.cover_image,
                    u.avatar as user_avatar
                from topic t
                inner join user u on u.uid = t.uid
                order by t.id asc
                """
            )
            rows = cur.fetchall()

            updates = []
            for row in rows:
                current_cover = normalize_text(row["cover_image"])
                user_avatar = normalize_text(row["user_avatar"])
                if args.only_changed and current_cover == user_avatar:
                    continue
                updates.append(
                    {
                        "id": row["id"],
                        "uid": row["uid"],
                        "topic_name": row["topic_name"],
                        "old_cover": current_cover,
                        "new_cover": user_avatar,
                    }
                )

            print(f"topic_updates={len(updates)}")
            for item in updates[:20]:
                print(
                    f"topic id={item['id']} uid={item['uid']} "
                    f"name={item['topic_name']} "
                    f"cover={'same' if item['old_cover'] == item['new_cover'] else 'change'}"
                )

            if not args.execute:
                conn.rollback()
                print("dry_run_only=true")
                return

            if updates:
                cur.executemany(
                    """
                    update topic
                    set cover_image=%s
                    where id=%s
                    """,
                    [(item["new_cover"], item["id"]) for item in updates],
                )

        conn.commit()
        print(f"updated_topics={len(updates)}")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
