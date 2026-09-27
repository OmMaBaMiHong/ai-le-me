#!/usr/bin/env python3
import argparse
import json
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
select uid, username, gender, avatar, figur, info
from user
where (
    (avatar is not null
      and avatar <> ''
      and (
        avatar like '%xg-1306483003.cos.ap-beijing.myqcloud.com/%'
        or avatar like '%picsum.photos%'
        or avatar like '%pic.linfeng.tech/test/%'
      ))
    or (figur is not null and (
        figur like '%xg-1306483003.cos.ap-beijing.myqcloud.com/%'
        or figur like '%picsum.photos%'
        or figur like '%pic.linfeng.tech/test/%'
      ))
  )
order by uid asc
"""


def build_parser():
    parser = argparse.ArgumentParser(description="Replace dirty avatar sources globally")
    parser.add_argument("--execute", action="store_true", help="apply updates")
    return parser


def build_avatar_pool(used_avatars):
    male = []
    female = []
    for idx in range(100):
        mu = f"https://randomuser.me/api/portraits/men/{idx}.jpg"
        fu = f"https://randomuser.me/api/portraits/women/{idx}.jpg"
        if mu not in used_avatars:
            male.append(mu)
        if fu not in used_avatars:
            female.append(fu)
    for idx in range(1, 71):
        pu = f"https://i.pravatar.cc/400?img={idx}"
        if pu in used_avatars:
            continue
        if idx % 2 == 0:
            female.append(pu)
        else:
            male.append(pu)
    return male, female


def next_avatar(gender, male_pool, female_pool):
    pool = female_pool if int(gender or 0) == 2 else male_pool
    if not pool:
        raise RuntimeError("没有足够的可用头像 URL 继续替换脏头像")
    return pool.pop(0)


def replace_in_figur(figur, old_avatar, new_avatar):
    raw = (figur or "").strip()
    if not raw:
        return new_avatar
    parts = [item.strip() for item in raw.split(",") if item.strip()]
    if not parts:
        return new_avatar
    replaced = []
    for item in parts:
        if (
            item == old_avatar
            or "xg-1306483003.cos.ap-beijing.myqcloud.com" in item
            or "picsum.photos" in item
            or "pic.linfeng.tech/test/" in item
        ):
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


def main():
    args = build_parser().parse_args()
    conn = pymysql.connect(**DB_CONFIG)
    try:
        with conn.cursor(pymysql.cursors.DictCursor) as cur:
            cur.execute("select avatar from user where avatar is not null and avatar <> ''")
            used_avatars = {row["avatar"].strip() for row in cur.fetchall() if row["avatar"]}

            cur.execute(TARGET_SQL)
            targets = cur.fetchall()
            male_pool, female_pool = build_avatar_pool(used_avatars)

            updates = []
            for row in targets:
                old_avatar = row["avatar"].strip()
                new_avatar = next_avatar(row["gender"], male_pool, female_pool)
                used_avatars.add(new_avatar)
                updates.append({
                    "uid": row["uid"],
                    "username": row["username"],
                    "old_avatar": old_avatar,
                    "new_avatar": new_avatar,
                    "figur": replace_in_figur(row["figur"], old_avatar, new_avatar),
                    "info": replace_in_info(row["info"], new_avatar),
                })

            print(f"target_users={len(updates)}")
            for item in updates[:20]:
                print(item["uid"], item["username"], "=>", item["new_avatar"])

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
                (
                    item["new_avatar"],
                    item["figur"],
                    item["info"],
                    item["uid"],
                )
                for item in updates
            ]
            cur.executemany(sql, payload)

        conn.commit()
        print("updated_users=", len(payload))
    finally:
        conn.close()


if __name__ == "__main__":
    main()
