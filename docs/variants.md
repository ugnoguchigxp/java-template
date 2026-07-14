# Variant and snapshot maintenance

This repository maintains four independently runnable variants:

| Variant | Git branch | Java | Database |
| --- | --- | --- | --- |
| Java 8 SQLite | `variant/java8-sqlite` | 8 | SQLite |
| Java 25 SQLite | `variant/java25-sqlite` | 25 | SQLite |
| Java 8 PostgreSQL | `variant/java-8-postgresql` | 8 | PostgreSQL |
| Java 25 PostgreSQL | `variant/java25-postgres` | 25 | PostgreSQL |

The branch name is the source of truth for the variant. Do not use runtime database auto-detection or a shared multi-database profile to blur the boundaries.

## Snapshot tags

Every releaseable variant is tagged with the form `<variant>-v<version>`, for example:

```sh
git clone https://github.com/ugnoguchigxp/java-template.git
git archive --format=tar.gz --output=java25-sqlite-v0.1.0.tar.gz java25-sqlite-v0.1.0
```

The corresponding GitHub archive is available at:

`https://github.com/ugnoguchigxp/java-template/archive/refs/tags/java25-sqlite-v0.1.0.tar.gz`

Create or move a tag only after the variant's own verification gate succeeds. Snapshot tags are immutable release points; publish a new version instead of force-moving an existing tag.

## Maintenance workflow

1. Check out exactly one variant branch.
2. Make only changes valid for that Java/database combination.
3. Run that branch's documented verification command.
4. Commit the change on the variant branch.
5. Create a new `<variant>-v<version>` tag and push the branch and tag.
6. Verify that the tag archive reproduces the tested commit.

The SQLite variants use `JAVA8_HOME` or `JAVA25_HOME` as documented by their branch. The PostgreSQL variants use `DATABASE_URL` and a disposable PostgreSQL database for integration and packaged-JAR tests.
