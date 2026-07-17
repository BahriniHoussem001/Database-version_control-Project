# Development Notes

## Current Migration Strategy

During the first proof of concept, we created separate folders for migrations and rollback scripts.

However, the active Liquibase migration source is currently:

`liquibase/changelog/db.changelog-master.sql`

This file contains the executable Liquibase changesets.

Important rule:

One schema change should be written once as a Liquibase changeset.

The files inside `migrations/` and `rollback/` are not currently executed by Liquibase. They were created during the initial learning setup and should not be considered the active migration source.

## Current Executed Changesets

- V001-create-customer-table
- V002-add-phone-number-to-customer

## Current Database State

The `CUSTOMER` table currently contains:

- ID
- FULL_NAME
- EMAIL
- CREATED_AT
- PHONE_NUMBER