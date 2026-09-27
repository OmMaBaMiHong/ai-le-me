ALTER TABLE `gift_task`
  ADD COLUMN `gift_status` tinyint NOT NULL DEFAULT 0 COMMENT '礼物状态:0待接受 1已接受 2已拒绝 3已过期' AFTER `generated_media`;

UPDATE `gift_task`
SET `gift_status` = 1;
