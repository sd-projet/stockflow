
CREATE TABLE purchase_order_counters (
    year INTEGER PRIMARY KEY,
    last_number BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_purchase_order_counter_year
        CHECK (year BETWEEN 2000 AND 9999),
    CONSTRAINT chk_purchase_order_counter_number
        CHECK (last_number >= 0)
);
