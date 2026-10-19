#!/bin/sh
set -eu

mysql_cmd="mysql --default-character-set=utf8mb4 -uroot -p${MYSQL_ROOT_PASSWORD} --database=${MYSQL_DATABASE}"

$mysql_cmd < /docker-entrypoint-initdb.d/source/01-dop.sql
$mysql_cmd < /docker-entrypoint-initdb.d/source/02-quartz.sql
$mysql_cmd < /docker-entrypoint-initdb.d/source/03-digital-schema.sql
$mysql_cmd < /docker-entrypoint-initdb.d/source/04-digital-init.sql
$mysql_cmd < /docker-entrypoint-initdb.d/source/05-demo-data.sql
