CREATE TABLE `user` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`    VARCHAR(64)   NOT NULL UNIQUE COMMENT '用户名',
  `password`    VARCHAR(255)  NOT NULL COMMENT '加密密码',
  `salt`        VARCHAR(64)   COMMENT '加密盐（视加密方案保留）',
  `avatar`      LONGTEXT  COMMENT '头像URL',
  `email`       VARCHAR(128)  UNIQUE COMMENT '邮箱',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) COMMENT='用户主表';


CREATE TABLE `user_llm_config` (
  `user_id`     BIGINT        NOT NULL COMMENT '关联用户ID（PK & FK）',
  `provider`    VARCHAR(32)   COMMENT '模型提供商 doubao/kimi/deepseek/openai/gemini/grok',
  `api_key`     VARCHAR(512)  COMMENT '用户 API Key（建议加密存储）',
  `base_url`    VARCHAR(512)  COMMENT '自定义接口地址',
  `model_name`  VARCHAR(128)  COMMENT '模型名称 如 gpt-4o / deepseek-chat',
  `temperature` DOUBLE        NOT NULL DEFAULT 0.5 COMMENT '温度参数',
  `max_tokens`  INT           NOT NULL DEFAULT 4096 COMMENT '最大Token数',
  PRIMARY KEY (`user_id`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) COMMENT='用户大模型配置（一对一）';


CREATE TABLE `project` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT,
  `project_code`   VARCHAR(64)   NOT NULL UNIQUE COMMENT '项目唯一编码',
  `project_name`   VARCHAR(128)  NOT NULL COMMENT '项目名称',
  `description`    VARCHAR(1024) COMMENT '项目描述',
  `gen_prompt`     TEXT          COMMENT '用户原始提示词',
  `code_dsl_json`  MEDIUMTEXT    COMMENT '完整 CodeDSL JSON（含pages/components树）',
  `project_type`   VARCHAR(64)   COMMENT 'CRM/OA/管理后台/H5 等',
  `preview_image`  LONGTEXT  COMMENT '首页预览图URL',
  `view_count`     BIGINT        NOT NULL DEFAULT 0,
  `like_count`     BIGINT        NOT NULL DEFAULT 0,
  `tech_stack`     VARCHAR(64)   COMMENT 'react_ant / vue_element 等',
  `user_id`        BIGINT        NOT NULL COMMENT '创建人',
  `is_public`      TINYINT       NOT NULL DEFAULT 0 COMMENT '0私有 1公开',
  `status`         VARCHAR(32)   COMMENT 'DRAFT/GENERATING/DONE/FAILED',
  `version`        INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) COMMENT='项目主表';


CREATE TABLE `project_generate_task` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT,
  `task_code`        VARCHAR(64)   NOT NULL UNIQUE COMMENT 'TASK_20260415123456789',
  `project_id`       BIGINT        COMMENT '生成成功后回填',
  `user_id`          BIGINT        NOT NULL,
  `gen_prompt`       TEXT          COMMENT '原始提示词',
  `status`           VARCHAR(32)   NOT NULL COMMENT 'PENDING/RUNNING/SUCCESS/FAILED',
  `progress`         INT           NOT NULL DEFAULT 0 COMMENT '0-100',
  `ai_project_name`  VARCHAR(128)  COMMENT 'AI推断的项目名',
  `ai_description`   VARCHAR(1024) COMMENT 'AI推断的项目描述',
  `ai_project_type`  VARCHAR(64)   COMMENT 'AI推断的项目类型',
  `ai_tech_stack`    VARCHAR(64)   COMMENT 'AI推断的技术栈',
  `error_msg`        TEXT          COMMENT '失败时的错误信息',
  `generate_time`    DATETIME      COMMENT '开始生成时间',
  `complete_time`    DATETIME      COMMENT '完成时间',
  `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_project_id` (`project_id`)
) COMMENT='项目AI生成任务表';


CREATE TABLE `page_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `page_dsl_id` BIGINT NOT NULL COMMENT '关联主Page的ID',
    `version_num` INT NOT NULL COMMENT '版本号，自动递增（1,2,3...）',
    `version_remark` VARCHAR(255) DEFAULT NULL COMMENT '版本备注，如v1.0、20260418-修改登录页',
    `dsl_json` LONGTEXT NOT NULL COMMENT '完整PageDSL的JSON快照',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '本次修改说明',
    `operator` VARCHAR(100) DEFAULT NULL COMMENT '操作人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态：DRAFT(草稿)/PUBLISHED(已发布)/ARCHIVED(已归档)',
    `page_generated_code` LONGTEXT COMMENT 'AI生成的原始代码',
    `user_edited_page_code` LONGTEXT COMMENT '用户修改后的最终代码',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_page_version` (`page_dsl_id`, `version_num`),
    KEY `idx_page_dsl_id` (`page_dsl_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='页面版本历史表';


CREATE TABLE `sensitive_word` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `word`        VARCHAR(128) NOT NULL UNIQUE COMMENT '敏感词',
  `category`    VARCHAR(32)  COMMENT '政治/色情/暴力等',
  `level`       TINYINT      NOT NULL COMMENT '1=替换 2=拦截',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_word` (`word`)
) COMMENT='敏感词库';
