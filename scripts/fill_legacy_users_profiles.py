#!/usr/bin/env python3
import argparse
import json
import random
import re
from datetime import date, datetime, timedelta

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
select uid, mobile, username, avatar, gender, city, province, birthday, age, height, job,
       education, school, income, marry_status, intro, self_introduction, love_declaration,
       interest, figur, info, audit_status, status, type, vip, vip_expire_time, group_id,
       level, integral, sign_num, money, home_city, abode_city, location_city
from user
where uid < 106
order by uid asc
"""


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


def build_parser():
    parser = argparse.ArgumentParser(description="Fill legacy users with complete test profile data")
    parser.add_argument("--execute", action="store_true", help="apply updates, otherwise dry-run")
    return parser


def normalize_gender(row):
    username = row["username"] or ""
    if row["gender"] in (1, 2):
        return int(row["gender"])
    if re.search(r"(姐|妹|宁|妍|雨|橙|MM_)", username):
        return 2
    if re.search(r"(哥|川|舟|阳|伟)", username):
        return 1
    return 1 if row["uid"] % 2 else 2


def is_placeholder_name(name):
    if not name:
        return True
    stripped = name.strip()
    if re.fullmatch(r"\d{6,}", stripped):
        return True
    if re.fullmatch(r"用户\d+", stripped):
        return True
    if stripped.startswith("MM_"):
        return True
    return False


def build_name(uid, gender):
    surname = SURNAMES[uid % len(SURNAMES)]
    given_pool = FEMALE_GIVEN if gender == 2 else MALE_GIVEN
    return surname + given_pool[(uid * 3) % len(given_pool)]


def pick_city(uid):
    return CITIES[uid % len(CITIES)]


def pick_job(uid, gender):
    jobs = FEMALE_JOBS if gender == 2 else MALE_JOBS
    return jobs[(uid * 5) % len(jobs)]


def pick_education(uid):
    return [1, 2, 2, 3, 3, 4][uid % 6]


def pick_school(uid, education):
    pool = SCHOOLS_BY_EDU.get(education) or SCHOOLS_BY_EDU[2]
    return pool[(uid * 7) % len(pool)]


def pick_interests(uid, gender):
    pool = FEMALE_INTERESTS if gender == 2 else MALE_INTERESTS
    start = uid % len(pool)
    picked = []
    idx = start
    while len(picked) < 3:
        item = pool[idx % len(pool)]
        if item not in picked:
            picked.append(item)
        idx += 3
    return picked


def build_intro(job, city, tone):
    return f"{job}，常住{city}，{tone}。"


def build_self_intro(job, interests, city):
    return f"平时做{job}，下班后喜欢{interests[0]}、{interests[1]}和{interests[2]}，更喜欢在{city}把日子过得松弛一点。"


def build_love_declaration(gender, interests):
    if gender == 2:
        return f"想认识情绪稳定、愿意认真推进关系的人，最好也喜欢{interests[0]}或者{interests[1]}。"
    return f"更想遇到一个相处舒服、能一起过好普通日子的人，周末可以一起{interests[0]}或者{interests[1]}。"


def build_birthday(uid, age):
    month = uid % 12 + 1
    day = uid % 27 + 1
    year = date.today().year - age
    return f"{year:04d}-{month:02d}-{day:02d}"


def build_height(uid, gender):
    if gender == 2:
        return str(158 + (uid % 14))
    return str(170 + (uid % 18))


def build_info_json(avatar):
    return json.dumps({"mohuAvatar": avatar}, ensure_ascii=False)


def should_replace_avatar(row, duplicate_avatar_set):
    avatar = (row["avatar"] or "").strip()
    if not avatar:
        return True
    if "picsum.photos" in avatar:
        return True
    if "pic.linfeng.tech/test/20220825/794e4d232bef4dcdac96497f9f487b48.jpeg" in avatar:
        return True
    if avatar in duplicate_avatar_set and "randomuser.me" not in avatar:
        return True
    return False


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
    pool = female_pool if gender == 2 else male_pool
    if not pool:
        raise RuntimeError("真人头像池不够用了，当前随机头像 URL 已耗尽")
    return pool.pop(0)


def enrich_rows(rows, used_avatars, duplicate_avatar_set):
    male_pool, female_pool = build_avatar_pool(used_avatars)
    enriched = []
    today = datetime.now()
    for row in rows:
        uid = row["uid"]
        gender = normalize_gender(row)
        province, city = pick_city(uid)
        education = int(row["education"]) if row["education"] not in (None, "") else pick_education(uid)
        age = int(row["age"]) if row["age"] not in (None, "") else (23 + uid % 11 if gender == 2 else 24 + uid % 12)
        interests = pick_interests(uid, gender)
        job = row["job"].strip() if row["job"] and row["job"].strip() and row["job"] != "未知" else pick_job(uid, gender)
        tone_pool = FEMALE_TONES if gender == 2 else MALE_TONES
        tone = tone_pool[uid % len(tone_pool)]
        replace_avatar = should_replace_avatar(row, duplicate_avatar_set)
        avatar = next_avatar(gender, male_pool, female_pool) if replace_avatar else row["avatar"].strip()

        if replace_avatar:
            used_avatars.add(avatar)

        username = row["username"].strip() if row["username"] else ""
        if is_placeholder_name(username):
            username = build_name(uid, gender)

        intro = build_intro(job, city, tone)
        self_intro = build_self_intro(job, interests, city)
        love_declaration = build_love_declaration(gender, interests)
        school = row["school"].strip() if row["school"] and row["school"].strip() else pick_school(uid, education)
        income = int(row["income"]) if row["income"] not in (None, "", 0) else 1 + (uid % 5)
        level = int(row["level"]) if row["level"] not in (None, "") else uid % 5
        integral = int(row["integral"]) if row["integral"] not in (None, "") else 20 + uid * 3
        sign_num = int(row["sign_num"]) if row["sign_num"] not in (None, "") else uid % 18
        vip = int(row["vip"]) if row["vip"] not in (None, "") else (1 if uid % 7 == 0 else 0)
        vip_expire_time = row["vip_expire_time"]
        if vip == 1 and not vip_expire_time:
            vip_expire_time = today + timedelta(days=60 + uid % 120)
        if vip == 0:
            vip_expire_time = None

        existing_info = {}
        if row["info"]:
            try:
                existing_info = json.loads(row["info"])
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
            "birthday": row["birthday"].strip() if row["birthday"] and row["birthday"].strip() else build_birthday(uid, age),
            "age": age,
            "height": row["height"].strip() if row["height"] and row["height"].strip() else build_height(uid, gender),
            "job": job,
            "education": education,
            "school": school,
            "income": income,
            "marry_status": 0 if row["marry_status"] in (None, "") else int(row["marry_status"]),
            "intro": intro,
            "self_introduction": self_intro,
            "love_declaration": love_declaration,
            "interest": "、".join(interests),
            "figur": avatar,
            "info": json.dumps(existing_info, ensure_ascii=False),
            "audit_status": 0 if row["audit_status"] in (None, "") else int(row["audit_status"]),
            "status": 0 if row["status"] in (None, "") else int(row["status"]),
            "type": 0 if row["type"] in (None, "") else int(row["type"]),
            "vip": vip,
            "vip_expire_time": vip_expire_time,
            "group_id": 1 if row["group_id"] in (None, "") else int(row["group_id"]),
            "level": level,
            "integral": integral,
            "sign_num": sign_num,
            "money": row["money"] if row["money"] is not None else 0,
            "home_city": city,
            "abode_city": city,
            "location_city": city,
            "location_update_time": today,
            "tag_str": json.dumps([city, job, interests[0], "认真交友"], ensure_ascii=False),
        })
    return enriched


def main():
    args = build_parser().parse_args()
    conn = pymysql.connect(**DB_CONFIG)
    try:
        with conn.cursor(pymysql.cursors.DictCursor) as cur:
            cur.execute(TARGET_SQL)
            rows = cur.fetchall()

            cur.execute("""
                select avatar
                from user
                where avatar is not null and avatar <> ''
            """)
            used_avatars = {item["avatar"].strip() for item in cur.fetchall() if item["avatar"]}

            cur.execute("""
                select avatar
                from user
                where avatar is not null and avatar <> ''
                group by avatar
                having count(*) > 1
            """)
            duplicate_avatar_set = {item["avatar"].strip() for item in cur.fetchall() if item["avatar"]}

            enriched = enrich_rows(rows, used_avatars, duplicate_avatar_set)

            print(f"target_users={len(enriched)}")
            print("preview:")
            for item in enriched[:8]:
                print(item["uid"], item["username"], item["gender"], item["city"], item["job"], item["avatar"])

            if not args.execute:
                conn.rollback()
                print("dry_run_only=true")
                return

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
        conn.commit()
        print("updated_users=", len(payload))
    finally:
        conn.close()


if __name__ == "__main__":
    main()
