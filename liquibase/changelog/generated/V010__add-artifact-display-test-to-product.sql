--liquibase formatted sql

--changeset admin:V010-add-artifact-display-test-to-product
ALTER TABLE PRODUCT ADD ARTIFACT_DISPLAY_TEST VARCHAR2(30) DEFAULT 'VISIBLE' NOT NULL;

--rollback ALTER TABLE PRODUCT DROP COLUMN ARTIFACT_DISPLAY_TEST;
