#!/usr/bin/env python3
"""Prepare realistic regression data for user profiles, hongniang, topics, and chat."""

from __future__ import annotations

import argparse
import json
import os
import random
import re
from dataclasses import dataclass
from datetime import date, datetime, timedelta

import pymysql


DB_CONFIG = {
    "host": os.getenv("YUELAO_DB_HOST", "127.0.0.1"),
    "port": int(os.getenv("YUELAO_DB_PORT", "3306")),
    "user": os.getenv("YUELAO_DB_USER", "root"),
    "password": os.getenv("YUELAO_DB_PASSWORD", "123456"),
    "database": os.getenv("YUELAO_DB_NAME", "bang_yi"),
    "charset": "utf8mb4",
    "autocommit": False,
    "cursorclass": pymysql.cursors.DictCursor,
}

RNG = random.Random(20260403)

ACTIVE_USER_SQL = """
select uid, mobile, username, avatar, gender, city, province, birthday, age, height, job,
       education, school, income, marry_status, intro, self_introduction, love_declaration,
       interest, figur, info, audit_status, status, type, vip, vip_expire_time, group_id,
       level, integral, sign_num, money, home_city, abode_city, location_city, tag_str,
       hongniang_id
from user
where status = 0
order by uid asc
"""

MALE_BAIDU_AVATARS = [
    "https://img0.baidu.com/it/u=3727516819,1861485078&fm=253&fmt=auto&app=138&f=JPEG?w=870&h=800",
    "https://img2.baidu.com/it/u=4240068872,3712614126&fm=253&app=138&f=JPEG?w=646&h=599",
    "https://img0.baidu.com/it/u=382352698,1399454437&fm=253&app=138&f=JPEG?w=800&h=882",
    "https://img2.baidu.com/it/u=278878039,253574776&fm=253&app=138&f=JPEG?w=869&h=800",
    "https://img0.baidu.com/it/u=2154652553,2383431160&fm=253&fmt=auto&app=138&f=JPEG?w=507&h=500",
    "https://img0.baidu.com/it/u=3240677672,415517454&fm=253&app=138&f=JPEG?w=500&h=562",
    "https://img0.baidu.com/it/u=3818057155,704305936&fm=253&app=138&f=JPEG?w=512&h=500",
    "https://img2.baidu.com/it/u=1423922963,558452063&fm=253&app=138&f=JPEG?w=521&h=500",
    "https://img1.baidu.com/it/u=829354742,2433209838&fm=253&fmt=auto&app=138&f=JPEG?w=504&h=500",
    "https://img0.baidu.com/it/u=925931293,1503343837&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500",
]

FEMALE_BAIDU_AVATARS = [
    "https://img1.baidu.com/it/u=2616858463,3646651699&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=584",
    "https://img1.baidu.com/it/u=145904989,1771414628&fm=253&app=138&f=JPEG?w=500&h=500",
    "https://img2.baidu.com/it/u=2530111115,2139169352&fm=253&app=138&f=JPEG?w=509&h=500",
    "https://img0.baidu.com/it/u=2388615347,3739939155&fm=253&app=138&f=JPEG?w=500&h=500",
    "https://img1.baidu.com/it/u=3074644945,854122769&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=524",
    "https://img0.baidu.com/it/u=3516060725,1694884512&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500",
    "https://img1.baidu.com/it/u=905240344,1104663529&fm=253&app=138&f=JPEG?w=500&h=500",
    "https://img2.baidu.com/it/u=2019204321,2317660844&fm=253&app=138&f=JPEG?w=500&h=500",
    "https://img0.baidu.com/it/u=1842705353,3171208175&fm=253&app=138&f=JPEG?w=500&h=500",
    "https://img1.baidu.com/it/u=1683389318,236818286&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500",
]

SURNAMES = [
    "林", "周", "沈", "苏", "顾", "程", "许", "江", "陆", "宋",
    "裴", "季", "邵", "叶", "袁", "贺", "梁", "韩", "谢", "陈",
    "温", "吴", "何", "罗", "唐", "高", "孟", "赵", "方", "戴",
]

MALE_GIVEN = [
    "景川", "砚舟", "奕辰", "知远", "怀川", "予安", "承泽", "竞阳", "嘉树", "云骁",
    "明赫", "廷宇", "盛谦", "叙白", "曜廷", "清和", "柏霖", "屿川", "知行", "聿成",
]

FEMALE_GIVEN = [
    "知意", "听雨", "若宁", "晚星", "语棠", "清妍", "沐橙", "予诺", "星遥", "舒禾",
    "嘉宁", "安冉", "今禾", "温宁", "书妍", "景姝", "初夏", "一诺", "念安", "可颂",
]

CITIES = [
    ("浙江省", "杭州"),
    ("上海市", "上海"),
    ("江苏省", "苏州"),
    ("江苏省", "南京"),
    ("浙江省", "宁波"),
    ("广东省", "深圳"),
    ("广东省", "广州"),
    ("四川省", "成都"),
    ("湖北省", "武汉"),
    ("福建省", "厦门"),
    ("湖南省", "长沙"),
    ("山东省", "青岛"),
    ("天津市", "天津"),
    ("陕西省", "西安"),
    ("河南省", "郑州"),
    ("安徽省", "合肥"),
    ("重庆市", "重庆"),
    ("福建省", "福州"),
    ("广东省", "珠海"),
    ("江苏省", "无锡"),
]

FEMALE_JOBS = [
    "产品经理", "品牌策划", "小学老师", "运营经理", "插画师", "新媒体编辑", "室内设计师", "咖啡店主理人",
    "护士", "财务主管", "健身教练", "民宿主理人", "心理咨询师", "婚礼策划", "摄影师",
    "服装买手", "舞蹈老师", "珠宝顾问", "宠物美容师", "皮肤管理师", "空乘", "UI设计师",
]

MALE_JOBS = [
    "后端工程师", "建筑设计师", "短视频导演", "新能源项目经理", "律师", "宠物医生", "广告创意总监", "外贸经理",
    "金融分析师", "飞盘教练", "纪录片摄影师", "咖啡烘焙师", "工业设计师", "数据产品经理", "民谣乐手",
    "航空机械师", "室内灯光设计师", "投行顾问", "健身工作室主理人", "汽车评测博主", "口腔医生", "软件工程师",
]

FEMALE_INTERESTS = [
    "健身", "普拉提", "瑜伽", "爬山", "徒步", "美食探店", "手冲咖啡", "拍照", "旅行", "Citywalk",
    "烘焙", "看展", "骑行", "跑步", "游泳", "露营", "舞蹈", "羽毛球", "逛书店", "花艺",
]

MALE_INTERESTS = [
    "撸铁", "健身", "跑步", "篮球", "羽毛球", "超跑", "自驾", "咖啡", "摄影", "露营",
    "徒步", "爬山", "游泳", "骑行", "做饭", "桌游", "看展", "电影", "滑雪", "Citywalk",
]

FEMALE_TONES = [
    "慢热但不冷淡，熟起来很会照顾人",
    "有自己的节奏，也愿意认真回应关系",
    "外表清爽，熟人面前很会接梗",
    "喜欢把生活过得有点仪式感",
]

MALE_TONES = [
    "不太会花言巧语，但答应的事会做到",
    "工作认真，生活里保留一点少年感",
    "更喜欢真实直接的相处方式",
    "属于越聊越有意思的类型",
]

SCHOOLS_BY_EDU = {
    1: ["杭州职业技术学院", "深圳信息职业技术学院", "厦门城市职业学院", "宁波职业技术学院"],
    2: ["浙江工商大学", "苏州大学", "华南师范大学", "南京信息工程大学", "深圳大学", "西南交通大学"],
    3: ["复旦大学", "浙江大学", "同济大学", "武汉大学", "中山大学", "东南大学"],
    4: ["浙江大学", "上海交通大学", "中国人民大学", "华中科技大学", "中山大学", "南开大学"],
    5: ["清华大学", "北京大学", "浙江大学", "复旦大学"],
}

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

TOPIC_APPLY_ANSWERS = [
    "平时会认真分享生活，也希望在圈子里多认识几位同频朋友。",
    "我比较喜欢自然慢聊，想先从共同兴趣开始认识。",
    "会积极参与活动和话题，也愿意认真回复别人。",
    "希望能在圈子里认识真实、稳定、愿意推进关系的人。",
]

CHAT_CONVERSATIONS = [
    [
        "你好呀，我看你资料里也喜欢Citywalk，最近有在上海逛到不错的路线吗？",
        "有的，我最近去了愚园路一带，节奏很舒服，你平时更喜欢咖啡店还是散步路线？",
        "我更偏散步加顺路喝杯咖啡，先轻松认识一下会更自然。",
    ],
    [
        "看到你也喜欢羽毛球和看展，感觉会挺聊得来。",
        "哈哈被你发现了，我最近周末不是去球馆就是去新展。",
        "那我们可以先从最近想看的展开始聊，熟一点再约线下。",
    ],
    [
        "我觉得你自我介绍里那句“认真回应关系”很加分。",
        "谢谢，你的资料也很真诚，不是那种很套路的感觉。",
        "我也是想慢一点但认真一点，所以想先来打个招呼。",
    ],
]


@dataclass
class User:
    uid: int
    mobile: str | None
    username: str | None
    avatar: str | None
    gender: int | None
    city: str | None
    province: str | None
    birthday: str | None
    age: int | None
    height: str | None
    job: str | None
    education: int | None
    school: str | None
    income: int | None
    marry_status: int | None
    intro: str | None
    self_introduction: str | None
    love_declaration: str | None
    interest: str | None
    figur: str | None
    info: str | None
    audit_status: int | None
    status: int | None
    type: int | None
    vip: int | None
    vip_expire_time: datetime | None
    group_id: int | None
    level: int | None
    integral: int | None
    sign_num: int | None
    money: float | None
    home_city: str | None
    abode_city: str | None
    location_city: str | None
    tag_str: str | None
    hongniang_id: int | None


def build_parser():
    parser = argparse.ArgumentParser(description="Prepare AiLeMe social regression fixtures")
    parser.add_argument("--execute", action="store_true", help="apply database updates")
    parser.add_argument("--hongniang-count", type=int, default=6, help="number of hongniang rows to recreate")
    parser.add_argument("--topic-owner-count", type=int, default=4, help="number of normal topic owners to seed")
    parser.add_argument("--topic-apply-count", type=int, default=6, help="number of topic applications to create")
    parser.add_argument("--chat-session-count", type=int, default=3, help="number of chat sessions to seed")
    return parser


def connect():
    return pymysql.connect(**DB_CONFIG)


def normalize_gender(row) -> int:
    if row["gender"] in (1, 2):
        return int(row["gender"])
    username = row["username"] or ""
    if re.search(r"(姐|妹|宁|妍|雨|橙|夏|棠|妍|冉|诺)", username):
        return 2
    if re.search(r"(哥|川|舟|阳|泽|骁|赫|树|远)", username):
        return 1
    return 1 if row["uid"] % 2 else 2


def is_placeholder_name(name: str | None) -> bool:
    if not name:
        return True
    stripped = name.strip()
    if not stripped:
        return True
    if re.fullmatch(r"\d{6,}", stripped):
        return True
    if re.fullmatch(r"用户\d+", stripped):
        return True
    return False


def clean_name(name: str | None) -> str:
    value = (name or "").strip()
    if not value:
        return "同频主理人"
    value = re.sub(r"在[\u4e00-\u9fa5A-Za-z]+$", "", value)
    value = re.sub(r"\d+$", "", value)
    value = value.strip()
    return value or "同频主理人"


def build_name(uid: int, gender: int) -> str:
    surname = SURNAMES[uid % len(SURNAMES)]
    given_pool = FEMALE_GIVEN if gender == 2 else MALE_GIVEN
    return surname + given_pool[(uid * 3) % len(given_pool)]


def pick_city(uid: int):
    return CITIES[uid % len(CITIES)]


def pick_job(uid: int, gender: int) -> str:
    jobs = FEMALE_JOBS if gender == 2 else MALE_JOBS
    return jobs[(uid * 5) % len(jobs)]


def pick_education(uid: int) -> int:
    return [1, 2, 2, 3, 3, 4][uid % 6]


def pick_school(uid: int, education: int) -> str:
    pool = SCHOOLS_BY_EDU.get(education) or SCHOOLS_BY_EDU[2]
    return pool[(uid * 7) % len(pool)]


def split_interest(raw: str | None) -> list[str]:
    if not raw:
        return []
    return [item.strip() for item in re.split(r"[、,，/|]", raw) if item and item.strip()]


def pick_interests(uid: int, gender: int) -> list[str]:
    existing_pool = FEMALE_INTERESTS if gender == 2 else MALE_INTERESTS
    start = uid % len(existing_pool)
    picked = []
    idx = start
    while len(picked) < 3:
        item = existing_pool[idx % len(existing_pool)]
        if item not in picked:
            picked.append(item)
        idx += 3
    return picked


def build_intro(job: str, city: str, tone: str) -> str:
    return f"{job}，常住{city}，{tone}。"


def build_self_intro(job: str, interests: list[str], city: str) -> str:
    return f"平时做{job}，下班后喜欢{interests[0]}、{interests[1]}和{interests[2]}，更喜欢在{city}把日子过得松弛一点。"


def build_love_declaration(gender: int, interests: list[str]) -> str:
    if gender == 2:
        return f"想认识情绪稳定、愿意认真推进关系的人，最好也喜欢{interests[0]}或者{interests[1]}。"
    return f"更想遇到一个相处舒服、能一起过好普通日子的人，周末可以一起{interests[0]}或者{interests[1]}。"


def build_birthday(uid: int, age: int) -> str:
    month = uid % 12 + 1
    day = uid % 27 + 1
    year = date.today().year - age
    return f"{year:04d}-{month:02d}-{day:02d}"


def build_height(uid: int, gender: int) -> str:
    return str((158 + (uid % 14)) if gender == 2 else (170 + (uid % 18)))


def pick_avatar(uid: int, gender: int) -> str:
    pool = FEMALE_BAIDU_AVATARS if gender == 2 else MALE_BAIDU_AVATARS
    return pool[uid % len(pool)]


def first_interest(raw: str | None, fallback_job: str | None = None) -> str:
    interests = split_interest(raw)
    if interests:
        return interests[0]
    if fallback_job:
        return fallback_job
    return "同频"


def resolve_cate_id(interest: str) -> int:
    if any(token in interest for token in ("健身", "撸铁", "跑步", "羽毛球", "瑜伽", "游泳", "飞盘")):
        return 5
    if any(token in interest for token in ("旅行", "徒步", "露营", "爬山", "自驾", "滑雪")):
        return 6
    if any(token in interest for token in ("音乐", "吉他", "Livehouse", "民谣")):
        return 2
    if any(token in interest for token in ("摄影", "拍照", "看展", "Citywalk", "咖啡")):
        return 4
    return 9


def build_topic_name(user: User, hongniang_topic: bool, offset: int) -> str:
    city = (user.city or "同城").strip() or "同城"
    focus = first_interest(user.interest, user.job)
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


def fetch_active_users(cur) -> list[User]:
    cur.execute(ACTIVE_USER_SQL)
    return [User(**row) for row in cur.fetchall()]


def enrich_user_rows(rows: list[User]) -> list[dict]:
    today = datetime.now()
    enriched = []
    for row in rows:
        uid = row.uid
        gender = normalize_gender(row.__dict__)
        province, city = pick_city(uid)
        if row.province and row.city:
            province = row.province.strip() or province
            city = row.city.strip() or city
        education = int(row.education) if row.education not in (None, "") else pick_education(uid)
        age = int(row.age) if row.age not in (None, "") else (23 + uid % 11 if gender == 2 else 24 + uid % 12)
        interests = split_interest(row.interest)
        if len(interests) < 3:
            interests = pick_interests(uid, gender)
        job = row.job.strip() if row.job and row.job.strip() and row.job != "未知" else pick_job(uid, gender)
        tone_pool = FEMALE_TONES if gender == 2 else MALE_TONES
        tone = tone_pool[uid % len(tone_pool)]
        avatar = pick_avatar(uid, gender)

        username = row.username.strip() if row.username else ""
        if is_placeholder_name(username):
            username = build_name(uid, gender)

        intro = row.intro.strip() if row.intro and row.intro.strip() else build_intro(job, city, tone)
        self_intro = row.self_introduction.strip() if row.self_introduction and row.self_introduction.strip() else build_self_intro(job, interests, city)
        love_declaration = (
            row.love_declaration.strip()
            if row.love_declaration and row.love_declaration.strip()
            else build_love_declaration(gender, interests)
        )
        school = row.school.strip() if row.school and row.school.strip() else pick_school(uid, education)
        income = int(row.income) if row.income not in (None, "", 0) else 1 + (uid % 5)
        level = int(row.level) if row.level not in (None, "") else uid % 5
        integral = int(row.integral) if row.integral not in (None, "") else 20 + uid * 3
        sign_num = int(row.sign_num) if row.sign_num not in (None, "") else uid % 18
        vip = int(row.vip) if row.vip not in (None, "") else (1 if uid % 7 == 0 else 0)
        vip_expire_time = row.vip_expire_time
        if vip == 1 and not vip_expire_time:
            vip_expire_time = today + timedelta(days=60 + uid % 120)
        if vip == 0:
            vip_expire_time = None

        existing_info = {}
        if row.info:
            try:
                existing_info = json.loads(row.info)
                if not isinstance(existing_info, dict):
                    existing_info = {}
            except Exception:
                existing_info = {}
        existing_info["mohuAvatar"] = avatar

        enriched.append({
            "uid": uid,
            "username": username,
            "avatar": avatar,
            "gender": gender,
            "province": province,
            "city": city,
            "birthday": row.birthday.strip() if isinstance(row.birthday, str) and row.birthday.strip() else build_birthday(uid, age),
            "age": age,
            "height": row.height.strip() if row.height and str(row.height).strip() else build_height(uid, gender),
            "job": job,
            "education": education,
            "school": school,
            "income": income,
            "marry_status": 0 if row.marry_status in (None, "") else int(row.marry_status),
            "intro": intro,
            "self_introduction": self_intro,
            "love_declaration": love_declaration,
            "interest": "、".join(interests[:3]),
            "figur": avatar,
            "info": json.dumps(existing_info, ensure_ascii=False),
            "audit_status": 0 if row.audit_status in (None, "") else int(row.audit_status),
            "status": 0 if row.status in (None, "") else int(row.status),
            "type": 0 if row.type in (None, "") else int(row.type),
            "vip": vip,
            "vip_expire_time": vip_expire_time,
            "group_id": 1 if row.group_id in (None, "") else int(row.group_id),
            "level": level,
            "integral": integral,
            "sign_num": sign_num,
            "money": row.money if row.money is not None else 0,
            "home_city": city,
            "abode_city": city,
            "location_city": city,
            "location_update_time": today,
            "tag_str": json.dumps([city, job, interests[0], "认真交友"], ensure_ascii=False),
        })
    return enriched


def apply_user_updates(cur, enriched: list[dict]):
    sql = """
        update user
        set username=%s,
            avatar=%s,
            gender=%s,
            province=%s,
            city=%s,
            birthday=%s,
            age=%s,
            height=%s,
            job=%s,
            education=%s,
            school=%s,
            income=%s,
            marry_status=%s,
            intro=%s,
            self_introduction=%s,
            love_declaration=%s,
            interest=%s,
            figur=%s,
            info=%s,
            audit_status=%s,
            status=%s,
            type=%s,
            vip=%s,
            vip_expire_time=%s,
            group_id=%s,
            level=%s,
            integral=%s,
            sign_num=%s,
            money=%s,
            home_city=%s,
            abode_city=%s,
            location_city=%s,
            location_update_time=%s,
            tag_str=%s
        where uid=%s
    """
    payload = [
        (
            item["username"],
            item["avatar"],
            item["gender"],
            item["province"],
            item["city"],
            item["birthday"],
            item["age"],
            item["height"],
            item["job"],
            item["education"],
            item["school"],
            item["income"],
            item["marry_status"],
            item["intro"],
            item["self_introduction"],
            item["love_declaration"],
            item["interest"],
            item["figur"],
            item["info"],
            item["audit_status"],
            item["status"],
            item["type"],
            item["vip"],
            item["vip_expire_time"],
            item["group_id"],
            item["level"],
            item["integral"],
            item["sign_num"],
            item["money"],
            item["home_city"],
            item["abode_city"],
            item["location_city"],
            item["location_update_time"],
            item["tag_str"],
            item["uid"],
        )
        for item in enriched
    ]
    cur.executemany(sql, payload)


def reset_hongniang_fixtures(cur) -> dict:
    cur.execute(
        """
        select id
        from topic
        where channel_type = 2 or hongniang_id is not null
        """
    )
    topic_ids = [row["id"] for row in cur.fetchall()]
    deleted_topic_rows = 0
    if topic_ids:
        placeholders = ",".join(["%s"] * len(topic_ids))
        cur.execute(f"delete from user_topic where topic_id in ({placeholders})", topic_ids)
        cur.execute(f"delete from topic_apply where topic_id in ({placeholders})", topic_ids)
        cur.execute(f"delete from topic where id in ({placeholders})", topic_ids)
        deleted_topic_rows = cur.rowcount
    cur.execute("update user set hongniang_id = null where hongniang_id is not null")
    cur.execute("select count(*) as cnt from hongniang_info")
    hongniang_total = cur.fetchone()["cnt"]
    cur.execute("delete from hongniang_info")
    return {
        "deleted_hongniang_rows": hongniang_total,
        "deleted_hongniang_topics": len(topic_ids),
    }


def fetch_user_topic_owner_uids(cur) -> set[int]:
    cur.execute("select distinct uid from topic where channel_type = 3 and status = 0")
    return {int(row["uid"]) for row in cur.fetchall() if row.get("uid")}


def fetch_memberships(cur) -> set[tuple[int, int]]:
    cur.execute("select uid, topic_id from user_topic")
    return {(row["uid"], row["topic_id"]) for row in cur.fetchall()}


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
    interest = first_interest(user.interest, user.job)
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
            resolve_cate_id(interest),
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


def ensure_vip(cur, user_ids: list[int]):
    if not user_ids:
        return
    placeholders = ",".join(["%s"] * len(user_ids))
    cur.execute(
        f"""
        update user
        set vip = 1,
            vip_expire_time = date_add(now(), interval 120 day)
        where uid in ({placeholders})
        """,
        user_ids,
    )


def choose_hongniang_users(users: list[User], count: int) -> list[User]:
    candidates = [user for user in users if user.avatar]
    candidates.sort(key=lambda item: (0 if item.gender == 2 else 1, -(item.vip or 0), -item.uid))
    return candidates[:count]


def choose_topic_owner_users(users: list[User], excluded_uids: set[int], existing_owner_uids: set[int], count: int) -> list[User]:
    candidates = [
        user for user in users
        if user.uid not in excluded_uids and user.uid not in existing_owner_uids
    ]
    candidates.sort(key=lambda item: (-(item.vip or 0), 0 if item.gender == 2 else 1, -item.uid))
    return candidates[:count]


def seed_topic_applications(cur, topics: list[dict], users: list[User], count: int) -> int:
    cur.execute("delete from topic_apply")
    if not topics or count <= 0:
        return 0
    owner_uids = {item["owner_uid"] for item in topics}
    candidates = [user for user in users if user.uid not in owner_uids]
    candidates.sort(key=lambda item: (-item.uid, 0 if item.gender == 2 else 1))
    created = 0
    for index in range(count):
        applicant = candidates[index % len(candidates)]
        topic = topics[index % len(topics)]
        if applicant.uid == topic["owner_uid"]:
            continue
        answer = TOPIC_APPLY_ANSWERS[index % len(TOPIC_APPLY_ANSWERS)]
        now = datetime.now() - timedelta(hours=index * 6)
        cur.execute(
            """
            insert into topic_apply (topic_id, answer, question, uid, status, create_time, update_time)
            values (%s, %s, %s, %s, %s, %s, %s)
            """,
            (
                topic["topic_id"],
                answer,
                "想加入后怎么一起玩？",
                applicant.uid,
                0,
                now,
                now,
            ),
        )
        created += 1
    return created


def stable_session_id(uid1: int, uid2: int) -> str:
    low, high = sorted((int(uid1), int(uid2)))
    return str(900000000000000000 + low * 1000000 + high)


def ensure_friend_rows(cur, uid1: int, uid2: int, session_id: str, last_message: str, unread_target_uid: int):
    now = datetime.now()
    cur.execute("delete from friend where (my_id=%s and friend_id=%s) or (my_id=%s and friend_id=%s)", (uid1, uid2, uid2, uid1))
    rows = [
        (uid1, uid2, session_id, last_message, 1 if unread_target_uid == uid1 else 0, 0, now, now),
        (uid2, uid1, session_id, last_message, 1 if unread_target_uid == uid2 else 0, 0, now, now),
    ]
    cur.executemany(
        """
        insert into friend (my_id, friend_id, session_id, last_message, unread, is_hidden, create_time, update_time)
        values (%s, %s, %s, %s, %s, %s, %s, %s)
        """,
        rows,
    )


def seed_chat_messages(cur, hongniang_users: list[User], users: list[User], session_count: int) -> int:
    targets = [user for user in users if user.uid not in {item.uid for item in hongniang_users}]
    if not hongniang_users or not targets or session_count <= 0:
        return 0
    seeded = 0
    for index in range(min(session_count, len(hongniang_users), len(targets), len(CHAT_CONVERSATIONS))):
        sender = hongniang_users[index]
        receiver = targets[index]
        session_id = stable_session_id(sender.uid, receiver.uid)
        cur.execute("delete from chat_message where session_id=%s", (session_id,))
        conversation = CHAT_CONVERSATIONS[index]
        base_time = datetime.now() - timedelta(days=index + 1, hours=2)
        payload = []
        for message_index, content in enumerate(conversation):
            if message_index % 2 == 0:
                from_uid, to_uid = sender.uid, receiver.uid
            else:
                from_uid, to_uid = receiver.uid, sender.uid
            send_time = base_time + timedelta(minutes=message_index * 9)
            payload.append(
                (
                    session_id,
                    str(from_uid),
                    str(to_uid),
                    send_time.strftime("%Y-%m-%d %H:%M:%S"),
                    content,
                    "text",
                    0,
                    send_time,
                )
            )
        cur.executemany(
            """
            insert into chat_message (session_id, sender_id, receiver_id, send_time, content, message_type, is_withdrawn, update_time)
            values (%s, %s, %s, %s, %s, %s, %s, %s)
            """,
            payload,
        )
        ensure_friend_rows(cur, sender.uid, receiver.uid, session_id, conversation[-1], receiver.uid)
        seeded += len(payload)
    return seeded


def fetch_summary(cur) -> dict:
    summary = {}
    for table in ("user", "hongniang_info", "topic", "topic_apply", "user_topic", "friend", "chat_message"):
        cur.execute(f"select count(*) as cnt from `{table}`")
        summary[table] = cur.fetchone()["cnt"]
    cur.execute(
        """
        select uid, username, city, job, vip, hongniang_id
        from user
        where status = 0
        order by uid asc
        limit 8
        """
    )
    summary["sample_users"] = cur.fetchall()
    cur.execute(
        """
        select id, hongniang_name, user_id, service_area
        from hongniang_info
        order by id asc
        """
    )
    summary["hongniang_rows"] = cur.fetchall()
    cur.execute(
        """
        select id, topic_name, uid, hongniang_id, channel_type, user_num
        from topic
        order by id desc
        limit 10
        """
    )
    summary["topic_rows"] = cur.fetchall()
    cur.execute(
        """
        select id, session_id, sender_id, receiver_id, content
        from chat_message
        order by id desc
        limit 9
        """
    )
    summary["chat_rows"] = cur.fetchall()
    return summary


def main():
    args = build_parser().parse_args()
    conn = connect()
    stats = {
        "updated_users": 0,
        "created_hongniang": 0,
        "created_topic_owners": 0,
        "created_topic_apply": 0,
        "created_chat_messages": 0,
    }

    try:
        with conn.cursor() as cur:
            users = fetch_active_users(cur)
            enriched = enrich_user_rows(users)
            stats["updated_users"] = len(enriched)

            if not args.execute:
                preview = {
                    "updated_users": len(enriched),
                    "preview_users": [
                        {
                            "uid": item["uid"],
                            "username": item["username"],
                            "city": item["city"],
                            "job": item["job"],
                            "avatar": item["avatar"],
                        }
                        for item in enriched[:8]
                    ],
                }
                print(json.dumps(preview, ensure_ascii=False, indent=2, default=str))
                conn.rollback()
                return

            apply_user_updates(cur, enriched)

            refreshed_users = fetch_active_users(cur)
            reset_stats = reset_hongniang_fixtures(cur)
            memberships = fetch_memberships(cur)
            existing_owner_uids = fetch_user_topic_owner_uids(cur)

            hongniang_users = choose_hongniang_users(refreshed_users, args.hongniang_count)
            ensure_vip(cur, [user.uid for user in hongniang_users])
            refreshed_users = fetch_active_users(cur)
            user_by_uid = {user.uid: user for user in refreshed_users}
            hongniang_users = [user_by_uid[user.uid] for user in hongniang_users]

            for index, user in enumerate(hongniang_users):
                hongniang_id = insert_hongniang(cur, user, index)
                topic_id = insert_topic(cur, user, hongniang_id, True, index)
                seed_topic_members(cur, memberships, refreshed_users, user, topic_id, 8)
                stats["created_hongniang"] += 1

            excluded_uids = {user.uid for user in hongniang_users}
            topic_owner_users = choose_topic_owner_users(
                refreshed_users,
                excluded_uids,
                existing_owner_uids,
                args.topic_owner_count,
            )
            ensure_vip(cur, [user.uid for user in topic_owner_users])
            refreshed_users = fetch_active_users(cur)
            user_by_uid = {user.uid: user for user in refreshed_users}
            topic_owner_users = [user_by_uid[user.uid] for user in topic_owner_users]

            topic_owner_topics = []
            for index, user in enumerate(topic_owner_users):
                topic_id = insert_topic(cur, user, None, False, index)
                seed_topic_members(cur, memberships, refreshed_users, user, topic_id, 6)
                topic_owner_topics.append({
                    "topic_id": topic_id,
                    "owner_uid": user.uid,
                })
                stats["created_topic_owners"] += 1

            sync_user_hongniang(cur)
            recalc_topic_user_num(cur)
            recalc_hongniang_totals(cur)

            stats["created_topic_apply"] = seed_topic_applications(
                cur,
                topic_owner_topics,
                refreshed_users,
                args.topic_apply_count,
            )
            stats["created_chat_messages"] = seed_chat_messages(
                cur,
                hongniang_users,
                refreshed_users,
                args.chat_session_count,
            )

            summary = fetch_summary(cur)
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()

    output = {
        **stats,
        **reset_stats,
        **summary,
    }
    print(json.dumps(output, ensure_ascii=False, indent=2, default=str))


if __name__ == "__main__":
    main()
