drop index if exists idx_sales_date_time_id;

alter table sales
    drop column if exists sale_time;

create index if not exists idx_sales_date_id
    on sales(sale_date, id);
