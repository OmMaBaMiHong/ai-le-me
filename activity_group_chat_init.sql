CREATE TABLE IF NOT EXISTS activity_chat_group (
    id INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    activity_id INT NOT NULL COMMENT '活动ID',
    title VARCHAR(120) NOT NULL DEFAULT '' COMMENT '群标题',
    cover_img VARCHAR(500) NOT NULL DEFAULT '' COMMENT '群封面',
    organizer_uid INT NOT NULL DEFAULT 0 COMMENT '组局者用户ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1正常 0关闭',
    last_message VARCHAR(255) NOT NULL DEFAULT '' COMMENT '最后一条消息预览',
    last_message_time DATETIME DEFAULT NULL COMMENT '最后消息时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_activity_chat_group_activity (activity_id),
    KEY idx_activity_chat_group_organizer (organizer_uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组局活动群聊';

CREATE TABLE IF NOT EXISTS activity_chat_member (
    id INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    group_id INT NOT NULL COMMENT '群ID',
    activity_id INT NOT NULL COMMENT '活动ID',
    user_id INT NOT NULL COMMENT '用户ID',
    enrollment_id INT DEFAULT NULL COMMENT '报名ID',
    role TINYINT NOT NULL DEFAULT 2 COMMENT '角色:1组局者 2成员',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1在群 0离群',
    join_time DATETIME DEFAULT NULL COMMENT '入群时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_activity_chat_member_group_user (group_id, user_id),
    KEY idx_activity_chat_member_activity (activity_id),
    KEY idx_activity_chat_member_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组局活动群成员';

CREATE TABLE IF NOT EXISTS activity_chat_message (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    group_id INT NOT NULL COMMENT '群ID',
    activity_id INT NOT NULL COMMENT '活动ID',
    sender_id INT NOT NULL DEFAULT 0 COMMENT '发送者用户ID,0为系统',
    message_type VARCHAR(20) NOT NULL DEFAULT 'text' COMMENT '消息类型',
    content TEXT NOT NULL COMMENT '消息内容',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_activity_chat_message_group_id (group_id, id),
    KEY idx_activity_chat_message_activity_id (activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组局活动群消息';
