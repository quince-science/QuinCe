-- Database migration folder --
Database migration scripts should be added to this folder:

* src/migrations/db_migrations

Migration files are simply sql files, named using the following convention:

```
V[number]__[any_name].sql, eg V2__github_issue_740.sql
```
...referencing github issue #740.

Look in the folder `src/migrations/db_migrations` to see which version is
current, and increase the number by one for your new migration. The first file
added should be named V2__[something].sql, as the first migration is the
baseline and has no migration file.

To run a migration, use the upgrade-script:

```
$ ./scripts/upgrade.sh
```
