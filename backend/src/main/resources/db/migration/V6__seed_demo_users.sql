-- V6__seed_demo_users.sql: Safe, idempotent seed for default application users
-- Admin: admin / admin123
-- Accountant: accountant / account123
-- Viewer: viewer / viewer123

INSERT INTO app_user (username, password_hash, full_name, role, is_active, created_by, created_on, updated_by, updated_on)
VALUES
    ('admin', '$2a$10$WhvZj/xX7wuSAGKhllpHK.CDz7a25Xqxp8DJj/mMb0jnjc9ROn4m2', 'System Administrator', 'ADMIN', 'Y', 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP),
    ('accountant', '$2a$10$P.AIJMQAN9as8h57ZJ6fiegLnLi9PVDAelpYhOg.WxsxCk/uLpl1.', 'Accountant User', 'ACCOUNTANT', 'Y', 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP),
    ('viewer', '$2a$10$CHMPZmmUmod2.xjnJSA9hOnmKkzk8Jtge4nhnKRxkfgbn3fTMqzgm', 'Read-Only Viewer', 'VIEWER', 'Y', 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;
