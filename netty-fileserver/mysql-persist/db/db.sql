create database netty_file;

use netty_file;

CREATE TABLE `upload_file_meta` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `upload_user_id` bigint(20) NOT NULL COMMENT '文件上传用户id',
  `biz_line`  varchar(255) NOT NULL COMMENT '产品线',
  `original_file_name` varchar(255) NOT NULL COMMENT '原始文件名称',
  `target_url` varchar(512) NOT NULL COMMENT '文件存储url',
  `file_length` bigint NOT NULL,
  `url_hash`    bigint NOT NULL COMMENT 'target_url hash',
  `store_name` varchar(64) NOT NULL DEFAULT 'DEFAULT' COMMENT '存储名称,比如oss',
  `status`     int NOT NULL DEFAULT 0 COMMENT '0 初始状态、1 上传完成',
  `created_at` DATETIME NOT NULL,
  `finished_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4 COMMENT 'file meta';