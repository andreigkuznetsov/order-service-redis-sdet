create table orders (
    id uuid primary key,
    product varchar(255) not null,
    quantity integer not null,
    price numeric(19, 2) not null,
    status varchar(50) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);