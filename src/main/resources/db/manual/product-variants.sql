alter table inventory_products
    add column if not exists has_variants boolean not null default false;

create table if not exists inventory_product_variants (
    id bigserial primary key,
    inventory_product_id bigint not null,
    variant_label varchar(150) not null,
    sku varchar(100),
    stock integer not null default 0 check (stock >= 0),
    minimum_stock integer not null default 0 check (minimum_stock >= 0),
    purchase_price numeric(12, 2),
    active boolean not null default true,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint fk_inventory_product_variants_product
        foreign key (inventory_product_id) references inventory_products(id)
);

create table if not exists inventory_product_variant_attributes (
    variant_id bigint not null,
    attribute_name varchar(100) not null,
    attribute_value varchar(150) not null,
    primary key (variant_id, attribute_name),
    constraint fk_inventory_product_variant_attributes_variant
        foreign key (variant_id) references inventory_product_variants(id) on delete cascade
);

alter table expenses
    add column if not exists inventory_product_variant_id bigint;

alter table sales
    add column if not exists inventory_product_variant_id bigint;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'fk_expenses_inventory_product_variant'
    ) then
        alter table expenses
            add constraint fk_expenses_inventory_product_variant
            foreign key (inventory_product_variant_id)
            references inventory_product_variants(id);
    end if;
end $$;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'fk_sales_inventory_product_variant'
    ) then
        alter table sales
            add constraint fk_sales_inventory_product_variant
            foreign key (inventory_product_variant_id)
            references inventory_product_variants(id);
    end if;
end $$;

create index if not exists idx_inventory_product_variants_product_active
    on inventory_product_variants(inventory_product_id, active);

create index if not exists idx_inventory_product_variants_sku
    on inventory_product_variants(sku);

create index if not exists idx_expenses_inventory_product_variant
    on expenses(inventory_product_variant_id);

create index if not exists idx_sales_inventory_product_variant_date
    on sales(inventory_product_variant_id, sale_date);

create unique index if not exists uq_inventory_variant_product_label_active
    on inventory_product_variants(inventory_product_id, lower(variant_label))
    where active = true;

create unique index if not exists uq_inventory_variant_sku_active
    on inventory_product_variants(lower(sku))
    where active = true and sku is not null;
