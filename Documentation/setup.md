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

## Tomcat Configuration
**Note:** *At the time of writing these settings are experimental and under active revision. They may not be appropriate for all situations, and may turn out to be counter-productive. Any feedback on this issue is welcome.*

This section assumes that you are familiar with setting Java's command line options, both in general and for a Tomcat installation.

### Heap Size
QuinCe is able to run quite reasonably with a maximum heap size of ~6Gb (`-Xmx6g`) if there is not more than one concurrent user. (You could probably get away with a maximum size of 4Gb, but this has not been thoroughly tested.)

In an environment where multiple simultaneous users are more common, the current best estimate is to allocate 4Gb of heap space per concurrent user. This should be adequate for the size of most datasets used for the installations so far (equivalent to 2 months' worth of ship-based pCO<sub>2</sub> data).

### Garbage Collection
It may be desirable to adjust the JVM's garbage collection strategy to reduce delays in the application. This is most commonly seen when loading the QC page for a dataset, because it involves creating a large number of objects and is therefore likely to trigger a major garbage collection action.

The latest experiments involve using the [Z Garbage Collector](https://docs.oracle.com/en/java/javase/17/gctuning/z-garbage-collector.html). If the maximum heap size is less than 10Gb, then set the ZGC soft limit to 4Gb; otherwise set it to 8Gb. Example command line options are below.

`-Xmx10g -XX:+UseZGC -XX:SoftMaxHeapSize=4g`

`-Xmx32g -XX:+UseZGC -XX:SoftMaxHeapSize=8g`

Note that more modern JVM have a new Generational version of the ZGC which supports setting a target "soft" heap size, but this is not available in JDK17.


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

## Monitoring
The default QuinCe installation includes [Java Melody](https://github.com/javamelody/javamelody/wiki) to monitor RAM and processor usage, along with response times. In theory SQL database monitoring should be provided too, but this is frequently broken and there is no obvious fix at the time of writing.

Access to Java Melody is via `https://<quince_url>/monitoring` and requires a Tomcat user to be created with the `monitoring` role. Read the Tomcat documentation to learn how to create Tomcat users.