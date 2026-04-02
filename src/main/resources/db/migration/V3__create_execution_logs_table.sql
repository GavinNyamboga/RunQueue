CREATE TABLE execution_logs
(
    id        UUID        NOT NULL,
    run_id    UUID        NOT NULL,
    level     VARCHAR(20) NOT NULL,
    message   TEXT        NOT NULL,
    logged_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_execution_logs PRIMARY KEY (id)
);