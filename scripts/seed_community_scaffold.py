#!/usr/bin/env python3
"""Idempotent seed for hongniang_info + topic scaffold."""

from __future__ import annotations

import os
import random
import re
from dataclasses import dataclass
from datetime import datetime, timedelta

import pymysql


DB_HOST = os.getenv("YUELAO_DB_HOST", "127.0.0.1")
DB_PORT = int(os.getenv("YUELAO_DB_PORT", "3306"))
DB_USER = os.getenv("YUELAO_DB_USER", "root")
DB_PASSWORD = os.getenv("YUELAO_DB_PASSWORD", "123456")
DB_NAME = os.getenv("YUELAO_DB_NAME", "bang_yi")

TARGET_HONGNIANG_TOTAL = int(os.getenv("TARGET_HONGNIANG_TOTAL", "12"))
TARGET_USER_TOPIC_TOTAL = int(os.getenv("TARGET_USER_TOPIC_TOTAL", "18"))

RNG = random.Random(20260325)

TOPIC_SUFFIXES = [
    "认真相处局",
    "下班后慢聊局",
    "同城见面局",
    "生活分享局",
    "周末松弛局",
    "真诚脱单局",
]

HONGNIANG_COMPANIES = [
    "煊光同频社",
    "城市见面研究所",
    "认真恋爱计划",
    "同城慢聊俱乐部",
    "关系推进工作室",
    "线下社交事务局",
]

TOPIC_DESCRIPTIONS = {
    True: "这是一个偏认真相处和线下见面的主理人圈子，适合真实聊天、稳定认识和高质量互动。",
    False: "这是一个偏真实聊天和同城见面的用户圈子，适合分享生活、认识同频的人、自然推进关系。",
}


@dataclass
class User:
    uid: int
    username: str
    gender: int | None
    city: str | None
    job: str | None
    mobile: str | None
    avatar: str | None
    interest: str | None
    self_introduction: str | None
    intro: str | None
    vip: int | None
    hongniang_id: int | None


def clean_name(name: str | None) -> str:
    value = (name or "").strip()
    if not value:
        return "同频主理人"
    value = re.sub(r"在[\u4e00-\u9fa5A-Za-z]+$", "", value)
    value = re.sub(r"\d+$", "", value)
    value = value.strip()
    return value or "同频主理人"


def split_interest(raw: str | None) -> list[str]:
    if not raw:
        return []
    items = [
        item.strip()
        for item in re.split(r"[、,，/|]", raw)
        if item and item.strip()
    ]
    return items


def first_interest(user: User) -> str:
    interests = split_interest(user.interest)
    if interests:
        return interests[0]
    if user.job:
        return user.job
    return "同频"


def resolve_cate_id(user: User) -> int:
    interest = first_interest(user)
    if any(token in interest for token in ("健身", "撸铁", "跑步", "羽毛球", "瑜伽", "游泳", "飞盘")):
        return 5
    if any(token in interest for token in ("旅行", "徒步", "露营", "爬山", "自驾", "滑雪")):
        return 6
    if any(token in interest for token in ("音乐", "吉他", "Livehouse", "民谣")):
        return 2
    if any(token in interest for token in ("二次元", "动漫")):
        return 8
    if any(token in interest for token in ("摄影", "拍照", "看展", "Citywalk", "咖啡")):
        return 4
    return 9


def build_topic_name(user: User, hongniang_topic: bool, offset: int) -> str:
    city = (user.city or "同城").strip() or "同城"
    focus = first_interest(user)
    suffix = TOPIC_SUFFIXES[(user.uid + offset) % len(TOPIC_SUFFIXES)]
    if hongniang_topic:
        name = f"{city}{focus}主理人{suffix}"
    else:
        name = f"{city}{focus}{suffix}"
    return name[:18]


def build_topic_desc(user: User, hongniang_topic: bool) -> str:
    base = (user.self_introduction or user.intro or "").strip()
    desc = TOPIC_DESCRIPTIONS[hongniang_topic]
    if base:
        desc = f"{desc}{base}"
    return desc[:180]


def connect():
    return pymysql.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        password=DB_PASSWORD,
        database=DB_NAME,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
        autocommit=False,
    )


def fetch_users(cur) -> list[User]:
    cur.execute(
        """
        select uid, username, gender, city, job, mobile, avatar, interest,
               self_introduction, intro, vip, hongniang_id
        from user
        where status = 0
          and avatar is not null
          and avatar <> ''
        order by case when gender = 2 then 0 else 1 end, vip desc, uid desc
        """
    )
    rows = cur.fetchall()
    return [User(**row) for row in rows]


def fetch_existing(cur):
    cur.execute(
        """
        select id, user_id, hongniang_name, status
        from hongniang_info
        """
    )
    hongniang_rows = cur.fetchall()
    hongniang_by_user = {
        row["user_id"]: row for row in hongniang_rows if row.get("user_id")
    }

    cur.execute(
        """
        select id, uid, hongniang_id, topic_name, channel_type, status, is_privacy
        from topic
        where status = 0 and is_privacy = 0
        """
    )
    topic_rows = cur.fetchall()
    hongniang_topic_by_hn = {}
    user_topic_by_uid = {}
    for row in topic_rows:
        if row.get("hongniang_id"):
            hongniang_topic_by_hn.setdefault(row["hongniang_id"], row)
        if row.get("uid"):
            user_topic_by_uid.setdefault(row["uid"], row)

    cur.execute("select uid, topic_id from user_topic")
    memberships = {(row["uid"], row["topic_id"]) for row in cur.fetchall()}
    return hongniang_by_user, hongniang_topic_by_hn, user_topic_by_uid, memberships


def choose_hongniang_candidates(users: list[User], hongniang_by_user: dict[int, dict]) -> list[User]:
    candidates = []
    for user in users:
        if user.uid in hongniang_by_user:
            continue
        if not user.avatar:
            continue
        candidates.append(user)
    candidates.sort(key=lambda item: (0 if item.gender == 2 else 1, -(item.vip or 0), -item.uid))
    return candidates


def choose_user_topic_candidates(
    users: list[User],
    user_topic_by_uid: dict[int, dict],
    hongniang_user_ids: set[int],
) -> list[User]:
    candidates = []
    for user in users:
        if user.uid in user_topic_by_uid:
            continue
        if user.uid in hongniang_user_ids:
            continue
        candidates.append(user)
    candidates.sort(key=lambda item: (-item.uid, 0 if item.gender == 2 else 1))
    return candidates


def insert_hongniang(cur, user: User, index: int) -> int:
    now = datetime.now()
    name = clean_name(user.username)
    service_area = (user.city or "杭州").strip() or "杭州"
    company = f"{service_area}{HONGNIANG_COMPANIES[index % len(HONGNIANG_COMPANIES)]}"
    intro = (user.self_introduction or user.intro or f"{name}在{service_area}做认真社交主理服务。").strip()[:180]
    phone = user.mobile or f"13{RNG.randint(100000000, 999999999)}"
    wechat = f"hn_{user.uid}"
    tenant_id = f"seed-hn-{user.uid}"
    cur.execute(
        """
        insert into hongniang_info (
            certification_img, certification_status, company_name, create_time,
            hongniang_name, avatar, intro, level, phone, service_area, status,
            success_count, total_activities, total_users, update_time, user_id,
            wechat, del_flag, tenant_id
        ) values (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """,
        (
            user.avatar,
            1,
            company,
            now,
            name,
            user.avatar,
            intro,
            1 + (index % 3),
            phone,
            service_area,
            1,
            0,
            0,
            0,
            now,
            user.uid,
            wechat,
            0,
            tenant_id,
        ),
    )
    return cur.lastrowid


def insert_topic(cur, user: User, hongniang_id: int | None, hongniang_topic: bool, index: int) -> int:
    now = datetime.now() - timedelta(days=RNG.randint(1, 20), hours=RNG.randint(0, 23))
    cur.execute(
        """
        insert into topic (
            topic_name, description, cover_image, bg_image, cate_id, channel_type,
            hongniang_id, create_time, index_recommend, is_privacy, question, rest,
            status, top_type, uid, user_num
        ) values (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """,
        (
            build_topic_name(user, hongniang_topic, index),
            build_topic_desc(user, hongniang_topic),
            user.avatar,
            user.avatar,
            resolve_cate_id(user),
            2 if hongniang_topic else 3,
            hongniang_id,
            now,
            1,
            0,
            "",
            0,
            0,
            1 if hongniang_topic else 0,
            user.uid,
            0,
        ),
    )
    return cur.lastrowid


def ensure_membership(cur, memberships: set[tuple[int, int]], uid: int, topic_id: int):
    key = (uid, topic_id)
    if key in memberships:
        return
    cur.execute(
        """
        insert into user_topic (uid, topic_id, create_time)
        values (%s, %s, %s)
        """,
        (uid, topic_id, datetime.now()),
    )
    memberships.add(key)


def seed_topic_members(cur, memberships: set[tuple[int, int]], users: list[User], owner: User, topic_id: int, max_members: int):
    ensure_membership(cur, memberships, owner.uid, topic_id)
    pool = [user for user in users if user.uid != owner.uid]
    pool.sort(key=lambda item: abs(item.uid - owner.uid))
    selected = pool[: max_members * 2]
    RNG.shuffle(selected)
    for member in selected[:max_members]:
        ensure_membership(cur, memberships, member.uid, topic_id)


def recalc_topic_user_num(cur):
    cur.execute(
        """
        update topic t
        left join (
            select topic_id, count(*) as cnt
            from user_topic
            group by topic_id
        ) ut on ut.topic_id = t.id
        set t.user_num = ifnull(ut.cnt, 0)
        """
    )


def recalc_hongniang_totals(cur):
    cur.execute(
        """
        update hongniang_info h
        left join (
            select hongniang_id, count(*) as topic_cnt, sum(user_num) as total_users
            from topic
            where hongniang_id is not null and status = 0 and is_privacy = 0
            group by hongniang_id
        ) t on t.hongniang_id = h.id
        set h.total_activities = ifnull(t.topic_cnt, 0),
            h.total_users = ifnull(t.total_users, 0),
            h.update_time = now()
        """
    )


def sync_user_hongniang(cur):
    cur.execute(
        """
        update user u
        join hongniang_info h on h.user_id = u.uid
        set u.hongniang_id = h.id
        """
    )


def fetch_summary(cur):
    cur.execute("select count(*) as cnt from hongniang_info")
    hongniang_total = cur.fetchone()["cnt"]
    cur.execute("select count(*) as cnt from topic")
    topic_total = cur.fetchone()["cnt"]
    cur.execute("select count(*) as cnt from user_topic")
    membership_total = cur.fetchone()["cnt"]
    cur.execute(
        """
        select h.id, h.hongniang_name, h.user_id, h.service_area, h.total_users
        from hongniang_info h
        order by h.id desc
        limit 6
        """
    )
    hongniang_sample = cur.fetchall()
    cur.execute(
        """
        select id, topic_name, channel_type, hongniang_id, uid, user_num
        from topic
        order by id desc
        limit 8
        """
    )
    topic_sample = cur.fetchall()
    return {
        "hongniang_total": hongniang_total,
        "topic_total": topic_total,
        "membership_total": membership_total,
        "hongniang_sample": hongniang_sample,
        "topic_sample": topic_sample,
    }


def main():
    conn = connect()
    stats = {
        "created_hongniang": 0,
        "created_hongniang_topics": 0,
        "created_user_topics": 0,
        "touched_memberships": 0,
    }
    try:
        with conn.cursor() as cur:
            users = fetch_users(cur)
            hongniang_by_user, hongniang_topic_by_hn, user_topic_by_uid, memberships = fetch_existing(cur)

            hongniang_candidates = choose_hongniang_candidates(users, hongniang_by_user)
            need_hongniang = max(0, TARGET_HONGNIANG_TOTAL - len(hongniang_by_user))
            created_hongniang_ids = {}
            for index, user in enumerate(hongniang_candidates[:need_hongniang]):
                hn_id = insert_hongniang(cur, user, index)
                created_hongniang_ids[user.uid] = hn_id
                hongniang_by_user[user.uid] = {"id": hn_id, "user_id": user.uid}
                stats["created_hongniang"] += 1

            hongniang_user_ids = set(hongniang_by_user.keys())
            user_by_uid = {user.uid: user for user in users}

            ordered_hongniang_users = []
            for uid in hongniang_user_ids:
                user = user_by_uid.get(uid)
                if user:
                    ordered_hongniang_users.append(user)
            ordered_hongniang_users.sort(key=lambda item: (0 if item.gender == 2 else 1, -item.uid))

            for index, user in enumerate(ordered_hongniang_users):
                hn_id = hongniang_by_user[user.uid]["id"]
                if hn_id in hongniang_topic_by_hn:
                    continue
                topic_id = insert_topic(cur, user, hn_id, True, index)
                hongniang_topic_by_hn[hn_id] = {"id": topic_id}
                seed_topic_members(cur, memberships, users, user, topic_id, 8)
                stats["created_hongniang_topics"] += 1

            current_user_topic_total = len(
                [row for row in user_topic_by_uid.values() if row.get("channel_type") == 3]
            )
            need_user_topics = max(0, TARGET_USER_TOPIC_TOTAL - current_user_topic_total)
            user_topic_candidates = choose_user_topic_candidates(users, user_topic_by_uid, hongniang_user_ids)
            for index, user in enumerate(user_topic_candidates[:need_user_topics]):
                topic_id = insert_topic(cur, user, None, False, index)
                user_topic_by_uid[user.uid] = {"id": topic_id, "channel_type": 3}
                seed_topic_members(cur, memberships, users, user, topic_id, 5)
                stats["created_user_topics"] += 1

            sync_user_hongniang(cur)
            recalc_topic_user_num(cur)
            recalc_hongniang_totals(cur)

            summary = fetch_summary(cur)
            conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()

    stats.update(summary)
    print(stats)


if __name__ == "__main__":
    main()
