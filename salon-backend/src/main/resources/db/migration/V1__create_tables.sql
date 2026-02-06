/*
  =======================
   Core tables
  =======================
 */
CREATE TABLE businesses (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    name VARCHAR(100) not null UNIQUE,
    description TEXT,
    image TEXT NOT NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp
);

CREATE TABLE users
(
    id BIGSERIAL NOT NULL primary key,
    first_name varchar(100) NOT NULL,
    last_name varchar(100) NOT NULL,
    role varchar(50)  NOT NULL,
    business_id BIGINT,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp
);

CREATE TABLE staff (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    user_id bigint not null unique,
    business_id bigint not null,
    title varchar(50) NOT NULL,
    is_active boolean NOT NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp
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
    business_id bigint,
    user_id bigint,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp
);

CREATE TABLE contacts (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    type varchar(20),
    value varchar(255) unique,
    business_id bigint,
    user_id bigint,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp
);

/*
  =======================
   Services
  =======================
 */
 CREATE TABLE services (
     id BIGSERIAL NOT NULL PRIMARY KEY,
     name varchar(100) NOT NULL,
     description TEXT,
     duration_minutes INTEGER NOT NULL,
     price DECIMAL(10,2) NOT NULL,
     is_active BOOLEAN NOT NULL DEFAULT true,
     created_at TIMESTAMP NOT NULL DEFAULT now(),
     updated_at TIMESTAMP
 );

/*
  =======================
   Services and business (many-to-many)
  =======================
 */
CREATE TABLE business_service (
      business_id bigint NOT NULL,
      service_id bigint NOT NULL,

      PRIMARY KEY (business_id, service_id)
);
