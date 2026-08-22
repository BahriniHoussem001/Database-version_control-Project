--liquibase formatted sql

--changeset admin:V008-add-builder-ui-test-column-to-product
ALTER TABLE PRODUCT ADD BUILDER_UI_TEST VARCHAR2(50) DEFAULT 'UI_OK' NOT NULL;

--rollback ALTER TABLE PRODUCT DROP COLUMN BUILDER_UI_TEST;
