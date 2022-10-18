# QuinCe Initial Setup
This document will guide you through a basic setup of QuinCe. It is a work
in progress.
If you get this to work successfully in a test environment then you should
know enough to deploy it in production.

## Requirements
Before you start with the QuinCe side of things, you will need the following:

- JDK 17. NB Newer versions will not work.
- Apache Tomcat 9.
- MySQL 8 with an empty database set up.
- Details of an SMTP server for sending emails to users. Continue reading if
you don't want to set this up immediately (e.g. for testing).

## Setup
1. Clone the repo from Github using SSH (not HTTP). At this stage download the
source release will not work correctly.
2. In the repo root folder, copy `quince.setup.default` to `quince.setup`.
3. Edit `quince.setup` as required. Comments in the file should help you out.
4. From the repo root folder, run `./scripts/setup_replace_strings.sh`.
This will inject the configuration into the files ready for the application WAR
to be built.
5. Run `./scripts/prodserver/deploy_new_version.sh` and follow the instructions.
Treat Yes/No questions as Continue/Abort.
6. Start Tomcat if it's not already running.
7. Access the QuinCe app using the URL you put in `quince.setup`'s `app_url`.

## Creating the first user
You won't be able to do anything in QuinCe until you've created your first user.
Click the `Sign Up` link and follow the instructions.

If you have set up the email server details, you should receive an email to
activate your account. If you have not, you can activate it manually in the
MySQL database, using the following statement:

```sql
UPDATE user SET email_code = NULL, email_code_time = NULL WHERE id = x;
```
### Administrator permissions
Users with administrator permissions can view all instruments (not just their
own) and access other features not available to 'normal' users. Again, this
must be set directly in the database as follows:

```sql
UPDATE user SET permissions = 5 WHERE id = x;
```

