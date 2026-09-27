SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `pay_order_detail`;
DROP TABLE IF EXISTS `account_bill`;
DROP TABLE IF EXISTS `pay_order`;
DROP TABLE IF EXISTS `pay_product`;
DROP TABLE IF EXISTS `account`;

CREATE TABLE `account` (
  `id` int NOT NULL AUTO_INCREMENT,
  `uid` int NOT NULL DEFAULT 0 COMMENT '用户ID',
  `asset_type` varchar(32) NOT NULL DEFAULT 'coin' COMMENT '账户类型',
  `balance` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '可用余额',
  `frozen_balance` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '冻结余额',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态:0正常 1禁用',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_uid_asset_type` (`uid`,`asset_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户账户表';

CREATE TABLE `pay_product` (
  `id` int NOT NULL AUTO_INCREMENT,
  `product_type` varchar(32) NOT NULL DEFAULT 'coin' COMMENT '商品类型:coin/vip',
  `name` varchar(128) NOT NULL DEFAULT '' COMMENT '商品名称',
  `price` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '售价',
  `give_price` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '赠送金额',
  `coin_amount` int NOT NULL DEFAULT 0 COMMENT '到账爱情币',
  `valid_days` int NOT NULL DEFAULT 0 COMMENT 'VIP有效天数',
  `remark` varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态:0正常 1禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pay_product_type_status` (`product_type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付商品表';

CREATE TABLE `pay_order` (
  `id` int NOT NULL AUTO_INCREMENT,
  `uid` int NOT NULL DEFAULT 0 COMMENT '用户ID',
  `nickname` varchar(64) NOT NULL DEFAULT '' COMMENT '用户昵称快照',
  `order_id` varchar(64) NOT NULL COMMENT '平台订单号',
  `title` varchar(128) NOT NULL DEFAULT '' COMMENT '订单标题',
  `biz_id` varchar(64) NOT NULL DEFAULT '' COMMENT '商品或业务关联ID',
  `price` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
  `give_price` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '赠送金额',
  `coin_amount` int NOT NULL DEFAULT 0 COMMENT '到账爱情币数量',
  `recharge_type` varchar(32) NOT NULL DEFAULT 'h5' COMMENT '支付端类型',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '订单状态:0待支付 1已支付 2已退款 3已关闭',
  `refund_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计退款金额',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `add_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `transaction_id` varchar(128) NOT NULL DEFAULT '' COMMENT '三方流水号',
  `out_trade_no` varchar(128) NOT NULL DEFAULT '' COMMENT '商户单号',
  `type` tinyint NOT NULL DEFAULT 2 COMMENT '订单类型:0旧余额充值 1会员 2爱情币 3活动报名',
  `channel` varchar(32) NOT NULL DEFAULT 'wechat' COMMENT '支付渠道',
  `remark` varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pay_order_order_id` (`order_id`),
  KEY `idx_pay_order_uid` (`uid`),
  KEY `idx_pay_order_type_status` (`type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付订单表';

CREATE TABLE `pay_order_detail` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_record_id` int NOT NULL DEFAULT 0 COMMENT '订单主键ID',
  `order_id` varchar(64) NOT NULL DEFAULT '' COMMENT '平台订单号',
  `uid` int NOT NULL DEFAULT 0 COMMENT '用户ID',
  `order_type` tinyint NOT NULL DEFAULT 0 COMMENT '订单类型',
  `event_type` varchar(64) NOT NULL DEFAULT '' COMMENT '事件类型',
  `title` varchar(128) NOT NULL DEFAULT '' COMMENT '事件标题',
  `amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '事件金额',
  `coin_amount` int NOT NULL DEFAULT 0 COMMENT '涉及爱情币数量',
  `biz_id` varchar(64) NOT NULL DEFAULT '' COMMENT '业务ID',
  `transaction_id` varchar(128) NOT NULL DEFAULT '' COMMENT '三方流水号',
  `request_json` text COMMENT '请求报文',
  `response_json` text COMMENT '返回报文',
  `remark` varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `add_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_pay_order_detail_order_id` (`order_id`),
  KEY `idx_pay_order_detail_uid` (`uid`),
  KEY `idx_pay_order_detail_event_type` (`event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单流水表';

CREATE TABLE `account_bill` (
  `id` int NOT NULL AUTO_INCREMENT,
  `uid` int NOT NULL DEFAULT 0 COMMENT '账户所属用户ID',
  `tip_user_id` int NOT NULL DEFAULT 0 COMMENT '关联用户ID',
  `link_id` varchar(64) NOT NULL DEFAULT '' COMMENT '业务关联ID',
  `order_id` varchar(64) NOT NULL DEFAULT '' COMMENT '关联订单号',
  `pm` tinyint NOT NULL DEFAULT 1 COMMENT '收支方向:0支出 1收入',
  `title` varchar(128) NOT NULL DEFAULT '' COMMENT '流水标题',
  `category` varchar(32) NOT NULL DEFAULT 'integral' COMMENT '资产分类',
  `type` varchar(64) NOT NULL DEFAULT '' COMMENT '业务类型',
  `number` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '变动金额',
  `balance` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '变动后余额',
  `mark` varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `add_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_account_bill_uid_category` (`uid`,`category`),
  KEY `idx_account_bill_uid_pm` (`uid`,`pm`),
  KEY `idx_account_bill_type` (`type`),
  KEY `idx_account_bill_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账户流水表';

INSERT INTO `account` (`uid`, `asset_type`, `balance`, `frozen_balance`, `status`, `version`, `create_time`, `update_time`)
SELECT `uid`,
       'coin',
       CAST(IFNULL(`integral`, 0) AS DECIMAL(12,2)),
       0.00,
       0,
       0,
       NOW(),
       NOW()
FROM `user`;

INSERT INTO `pay_product` (`id`, `product_type`, `name`, `price`, `give_price`, `coin_amount`, `valid_days`, `remark`, `sort`, `status`, `create_time`, `update_time`)
SELECT `id`,
       'coin',
       CONCAT('爱情币套餐', `id`),
       IFNULL(`price`, 0.00),
       IFNULL(`give_price`, 0.00),
       0,
       0,
       '',
       IFNULL(`sort`, 0),
       IFNULL(`status`, 0),
       NOW(),
       NOW()
FROM `recharge`;

INSERT INTO `pay_product` (`id`, `product_type`, `name`, `price`, `give_price`, `coin_amount`, `valid_days`, `remark`, `sort`, `status`, `create_time`, `update_time`)
SELECT 10000 + `id`,
       'vip',
       IFNULL(`name`, CONCAT('VIP套餐', `id`)),
       IFNULL(`price`, 0.00),
       0.00,
       0,
       IFNULL(`valid_days`, 0),
       IFNULL(`remark`, ''),
       IFNULL(`sort`, 0),
       0,
       NOW(),
       NOW()
FROM `vip_option`;

INSERT INTO `pay_order` (`id`, `uid`, `nickname`, `order_id`, `title`, `biz_id`, `price`, `give_price`, `coin_amount`, `recharge_type`, `status`, `refund_amount`, `pay_time`, `add_time`, `update_time`, `transaction_id`, `out_trade_no`, `type`, `channel`, `remark`)
SELECT `id`,
       `uid`,
       IFNULL(`nickname`, ''),
       `order_id`,
       IFNULL(`title`, ''),
       CASE
         WHEN `type` = 1 AND `biz_id` REGEXP '^[0-9]+$' THEN CAST(10000 + CAST(`biz_id` AS UNSIGNED) AS CHAR)
         ELSE IFNULL(`biz_id`, '')
       END,
       IFNULL(`price`, 0.00),
       IFNULL(`give_price`, 0.00),
       IFNULL(`coin_amount`, 0),
       IFNULL(`recharge_type`, 'h5'),
       IFNULL(`status`, 0),
       IFNULL(`refund_amount`, 0.00),
       `pay_time`,
       IFNULL(`add_time`, NOW()),
       IFNULL(`update_time`, NOW()),
       IFNULL(`transaction_id`, ''),
       IFNULL(`out_trade_no`, ''),
       IFNULL(`type`, 0),
       IFNULL(`channel`, 'wechat'),
       IFNULL(`remark`, '')
FROM `user_recharge`;

INSERT INTO `pay_order_detail` (`order_record_id`, `order_id`, `uid`, `order_type`, `event_type`, `title`, `amount`, `coin_amount`, `biz_id`, `transaction_id`, `remark`, `status`, `add_time`)
SELECT po.`id`,
       po.`order_id`,
       po.`uid`,
       po.`type`,
       ab.`type`,
       ab.`title`,
       IFNULL(ab.`number`, 0.00),
       0,
       IFNULL(po.`biz_id`, ''),
       '',
       IFNULL(ab.`mark`, ''),
       IFNULL(ab.`status`, 1),
       IFNULL(ab.`add_time`, NOW())
FROM `bill` ab
JOIN `pay_order` po ON po.`order_id` = ab.`order_id`
WHERE ab.`category` = 'order';

INSERT INTO `pay_order_detail` (`order_record_id`, `order_id`, `uid`, `order_type`, `event_type`, `title`, `amount`, `coin_amount`, `biz_id`, `transaction_id`, `remark`, `status`, `add_time`)
SELECT po.`id`,
       r.`order_id`,
       r.`uid`,
       r.`type`,
       'refund_record',
       '退款成功',
       IFNULL(r.`refund_amount`, 0.00),
       IFNULL(r.`coin_amount`, 0),
       IFNULL(po.`biz_id`, ''),
       IFNULL(r.`transaction_id`, ''),
       IFNULL(r.`reason`, ''),
       IFNULL(r.`refund_status`, 1),
       IFNULL(r.`refund_time`, IFNULL(r.`add_time`, NOW()))
FROM `user_recharge_refund` r
JOIN `pay_order` po ON po.`order_id` = r.`order_id`;

INSERT INTO `account_bill` (`id`, `uid`, `tip_user_id`, `link_id`, `order_id`, `pm`, `title`, `category`, `type`, `number`, `balance`, `mark`, `status`, `add_time`)
SELECT `id`,
       `uid`,
       IFNULL(`tip_user_id`, 0),
       IFNULL(`link_id`, ''),
       IFNULL(`order_id`, ''),
       IFNULL(`pm`, 1),
       IFNULL(`title`, ''),
       IFNULL(`category`, 'integral'),
       IFNULL(`type`, ''),
       IFNULL(`number`, 0.00),
       IFNULL(`balance`, 0.00),
       IFNULL(`mark`, ''),
       IFNULL(`status`, 1),
       IFNULL(`add_time`, NOW())
FROM `bill`
WHERE `category` <> 'order';

SET FOREIGN_KEY_CHECKS = 1;
