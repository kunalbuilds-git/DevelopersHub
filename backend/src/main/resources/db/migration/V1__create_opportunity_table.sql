CREATE TABLE opportunity (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    organization VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    description TEXT,
    url VARCHAR(500),
    deadline TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
