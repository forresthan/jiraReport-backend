-- JIRA 报告子系统表结构（数据库：jira_tools）
-- 执行：mysql -uroot jira_tools < src/main/resources/schema/summary_report_schema.sql

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS summary_report (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_id        VARCHAR(64)     NOT NULL COMMENT 'JIRA user key，与 JWT claim userId 一致',
    project_key     VARCHAR(50)              DEFAULT NULL,
    project_name    VARCHAR(255)             DEFAULT NULL,
    title           VARCHAR(500)    NOT NULL DEFAULT '',
    content         MEDIUMTEXT               DEFAULT NULL,
    status          VARCHAR(32)     NOT NULL DEFAULT 'draft' COMMENT 'draft|submitted|reviewed',
    template_id     BIGINT UNSIGNED          DEFAULT NULL COMMENT '关联 report_text_template.id',
    slides_json     MEDIUMTEXT               DEFAULT NULL COMMENT 'PPT 幻灯片结构化 JSON',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    KEY idx_owner (owner_id),
    KEY idx_owner_project (owner_id, project_key),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='总结报告';

CREATE TABLE IF NOT EXISTS report_text_template (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_id        VARCHAR(64)     NOT NULL,
    project_key     VARCHAR(50)              DEFAULT NULL,
    name            VARCHAR(255)    NOT NULL,
    enabled         TINYINT         NOT NULL DEFAULT 1,
    sections_json   JSON                     DEFAULT NULL COMMENT '章节数组 [{id,title,description},...]',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    KEY idx_owner (owner_id),
    KEY idx_owner_project (owner_id, project_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报告文字模板';

CREATE TABLE IF NOT EXISTS ppt_theme (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    theme_key       VARCHAR(64)     NOT NULL COMMENT '唯一键，如 business-blue',
    name            VARCHAR(128)    NOT NULL,
    style           VARCHAR(32)              DEFAULT NULL COMMENT 'business|tech|creative|minimal',
    preview_meta    JSON                     DEFAULT NULL COMMENT '预览用元数据',
    sort_order      INT             NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_theme_key (theme_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='PPT 皮肤模板';

CREATE TABLE IF NOT EXISTS ppt_export_job (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_id        VARCHAR(64)     NOT NULL,
    report_id       BIGINT UNSIGNED NOT NULL,
    theme_key       VARCHAR(64)     NOT NULL,
    status          VARCHAR(32)     NOT NULL DEFAULT 'pending' COMMENT 'pending|running|done|failed',
    progress        INT             NOT NULL DEFAULT 0,
    result_url      VARCHAR(1024)            DEFAULT NULL COMMENT '下载地址或服务器文件路径',
    error_message   VARCHAR(2000)          DEFAULT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    KEY idx_report (report_id),
    KEY idx_owner (owner_id),
    CONSTRAINT fk_ppt_job_report FOREIGN KEY (report_id) REFERENCES summary_report (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='PPT 导出任务';

-- 与前端 style 页默认模板一致，可按需增删
INSERT INTO ppt_theme (theme_key, name, style, preview_meta, sort_order, deleted)
VALUES
    ('business-blue', '商务蓝', 'business', JSON_OBJECT('previewColor', 'from-blue-600 to-blue-800'), 10, 0),
    ('tech-dark', '科技黑', 'tech', JSON_OBJECT('previewColor', 'from-slate-800 to-slate-950'), 20, 0),
    ('creative-gradient', '渐变彩', 'creative', JSON_OBJECT('previewColor', 'from-purple-600 via-pink-500 to-orange-400'), 30, 0),
    ('minimal-white', '极简白', 'minimal', JSON_OBJECT('previewColor', 'from-gray-50 to-gray-100'), 40, 0),
    ('finance-green', '金融绿', 'business', JSON_OBJECT('previewColor', 'from-emerald-600 to-teal-700'), 50, 0),
    ('corporate-red', '企业红', 'business', JSON_OBJECT('previewColor', 'from-red-600 to-red-800'), 60, 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    style = VALUES(style),
    preview_meta = VALUES(preview_meta),
    sort_order = VALUES(sort_order);
