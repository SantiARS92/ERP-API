-- =============================================================
-- Init script para PostgreSQL en Docker
-- Se ejecuta automaticamente al iniciar el contenedor por
-- docker-compose, en el orden indicado.
-- Todos los scripts son idempotentes (IF NOT EXISTS / IF EXISTS).
-- =============================================================

-- 1. Secuencia para clients.identificador
CREATE SEQUENCE IF NOT EXISTS clients_identificador_seq
    START WITH 1
    INCREMENT BY 1;

-- 2. Limpieza de la columna sale_time en sales
DROP INDEX IF EXISTS idx_sales_date_time_id;

ALTER TABLE sales
    DROP COLUMN IF EXISTS sale_time;

CREATE INDEX IF NOT EXISTS idx_sales_date_id
    ON sales(sale_date, id);

-- 3. Productos con variantes (tablas, columnas, indices)
ALTER TABLE inventory_products
    ADD COLUMN IF NOT EXISTS has_variants boolean NOT NULL DEFAULT false;

CREATE TABLE IF NOT EXISTS inventory_product_variants (
    id bigserial PRIMARY KEY,
    inventory_product_id bigint NOT NULL,
    variant_label varchar(150) NOT NULL,
    sku varchar(100),
    stock integer NOT NULL DEFAULT 0 CHECK (stock >= 0),
    minimum_stock integer NOT NULL DEFAULT 0 CHECK (minimum_stock >= 0),
    purchase_price numeric(12, 2),
    active boolean NOT NULL DEFAULT true,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT fk_inventory_product_variants_product
        FOREIGN KEY (inventory_product_id) REFERENCES inventory_products(id)
);

CREATE TABLE IF NOT EXISTS inventory_product_variant_attributes (
    variant_id bigint NOT NULL,
    attribute_name varchar(100) NOT NULL,
    attribute_value varchar(150) NOT NULL,
    PRIMARY KEY (variant_id, attribute_name),
    CONSTRAINT fk_inventory_product_variant_attributes_variant
        FOREIGN KEY (variant_id) REFERENCES inventory_product_variants(id) ON DELETE CASCADE
);

ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS inventory_product_variant_id bigint;

ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS inventory_product_variant_id bigint;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_expenses_inventory_product_variant'
    ) THEN
        ALTER TABLE expenses
            ADD CONSTRAINT fk_expenses_inventory_product_variant
            FOREIGN KEY (inventory_product_variant_id)
            REFERENCES inventory_product_variants(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_sales_inventory_product_variant'
    ) THEN
        ALTER TABLE sales
            ADD CONSTRAINT fk_sales_inventory_product_variant
            FOREIGN KEY (inventory_product_variant_id)
            REFERENCES inventory_product_variants(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_inventory_product_variants_product_active
    ON inventory_product_variants(inventory_product_id, active);

CREATE INDEX IF NOT EXISTS idx_inventory_product_variants_sku
    ON inventory_product_variants(sku);

CREATE INDEX IF NOT EXISTS idx_expenses_inventory_product_variant
    ON expenses(inventory_product_variant_id);

CREATE INDEX IF NOT EXISTS idx_sales_inventory_product_variant_date
    ON sales(inventory_product_variant_id, sale_date);

CREATE UNIQUE INDEX IF NOT EXISTS uq_inventory_variant_product_label_active
    ON inventory_product_variants(inventory_product_id, LOWER(variant_label))
    WHERE active = true;

CREATE UNIQUE INDEX IF NOT EXISTS uq_inventory_variant_sku_active
    ON inventory_product_variants(LOWER(sku))
    WHERE active = true AND sku IS NOT NULL;

-- 4. Correccion de iconos de categorias
UPDATE categories SET icon = 'content_cut'   WHERE name = 'Corte y Peinado';
UPDATE categories SET icon = 'auto_awesome'  WHERE name = 'Unas';
UPDATE categories SET icon = 'local_florist' WHERE name = 'Estetica Facial';
UPDATE categories SET icon = 'wb_sunny'      WHERE name = 'Depilacion';
UPDATE categories SET icon = 'spa'           WHERE name = 'Masajes y Bienestar';
UPDATE categories SET icon = 'water_drop'    WHERE name = 'Tratamientos Capilares';
UPDATE categories SET icon = 'shopping_bag'  WHERE name = 'Retail Profesional';
