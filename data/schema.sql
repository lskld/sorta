-- Sorta schema
-- Run against the `sorta` database, after pgvector + pg_trgm extensions
-- are enabled (see README.md).

CREATE TABLE products (
    product_id   TEXT PRIMARY KEY,        -- StockCode values from source data, e.g. '85123A'
    name         TEXT NOT NULL,           -- canonical product description (see data/sample_and_clean.py)
    category     TEXT NOT NULL,           -- derived via data/categorize_products.py keyword rules
    embedding    vector(384)              -- populated in a later step; nullable until then
);

CREATE TABLE sales (
    sale_id      BIGSERIAL PRIMARY KEY,
    product_id   TEXT NOT NULL REFERENCES products(product_id),
    invoice_no   TEXT NOT NULL,
    quantity     INTEGER NOT NULL,
    unit_price   NUMERIC(10, 2) NOT NULL,
    sale_date    TIMESTAMP NOT NULL,
    customer_id  TEXT,                    -- nullable: some source rows have no CustomerID
    country      TEXT
);

-- No additional indexes beyond primary keys.
-- - No ivfflat/hnsw on products.embedding: flat scan is the deliberate
--   choice at this row count (~1070 products), per Phase 0.
-- - No pg_trgm GIN index on products.name: sequential word_similarity()
--   scans are fine at this row count. Revisit only if row-count target
--   changes materially.
