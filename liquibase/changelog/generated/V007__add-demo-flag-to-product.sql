--liquibase formatted sql

--changeset houssem:V007-add-demo-flag-to-product
ALTER TABLE PRODUCT ADD DEMO_FLAG VARCHAR2(10) DEFAULT 'NO' NOT NULL;

--rollback ALTER TABLE PRODUCT DROP COLUMN DEMO_FLAG;
