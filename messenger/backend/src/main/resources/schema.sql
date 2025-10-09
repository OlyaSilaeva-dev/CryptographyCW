CREATE TABLE IF NOT EXISTS users
(
    id SERIAL PRIMARY KEY,
    username VARCHAR (255) UNIQUE NOT NULL,
    password VARCHAR (255) NOT NULL
);

CREATE TABLE IF NOT EXISTS chats
(
    id SERIAL PRIMARY KEY,
    first_user_id BIGINT NOT NULL,
    second_user_id BIGINT NOT NULL,

    symmetric_cipher VARCHAR(50) NOT NULL,
    encryption_mode VARCHAR(50) NOT NULL,
    padding_mode VARCHAR(50) NOT NULL,

    CONSTRAINT fk_first_user FOREIGN KEY (first_user_id) REFERENCES users (id),
    CONSTRAINT fk_second_user FOREIGN KEY (second_user_id) REFERENCES users (id)
);
