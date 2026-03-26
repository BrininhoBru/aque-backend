INSERT INTO users (id, username, password)
VALUES (
    uuid_generate_v4(),
    'admin',
    '$2a$12$z9J4aAWXVDcy535NMABrSeVr.wmf.62TgHTA9e9rROKNm2bebcm2C'
)ON CONFLICT (username) DO NOTHING;