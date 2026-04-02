CREATE TABLE worker_heartbeats
(
    worker_id         VARCHAR(255) NOT NULL,
    worker_name       VARCHAR(120) NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    last_heartbeat_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_worker_heartbeats PRIMARY KEY (worker_id)
);