/*
  =======================
   Core tables
  =======================
 */
CREATE TABLE businesses (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    name VARCHAR(100) not null,
    description TEXT,
    created_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE users
(
    id BIGSERIAL NOT NULL primary key,
    first_name varchar(100) NOT NULL,
    last_name varchar(100) NOT NULL,
    role varchar(50)  NOT NULL,
    business_id BIGINT,
    created_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE staff (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    user_id bigint not null unique,
    business_id bigint not null,
    title varchar(50) NOT NULL,
    is_active boolean NOT NULL,
    created_at timestamp NOT NULL DEFAULT now()
);

/*
  =======================
   Customer relationship (many-to-many)
  =======================
 */
CREATE TABLE business_customers (
    business_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT now()
);

/*
  =======================
   Contact and address tables
  =======================
 */
CREATE TABLE addresses (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    street VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country varchar(100) NOT NULL,
    postal_code varchar(20),
    latitude varchar(255),
    longitude varchar(255),
    created_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE contacts (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    type varchar(20),
    value varchar(255) unique,
    created_at timestamp NOT NULL DEFAULT now()
);

/*
  =======================
   Services
  =======================
 */
 CREATE TABLE services (
     id BIGSERIAL NOT NULL PRIMARY KEY,
     name varchar(100),
     description TEXT,
     duration_minutes int,
     price decimal NOT NULL,
     is_active boolean
 );