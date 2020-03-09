#!/bin/sh

set -x

datauser=$1
database=$2
shift 2
datasets=$@

echo "Initializing databases $database for user $datauser with datasets $datasets"

find . -name "*.sql" -maxdepth 1 | env LC_ALL=C sort -n | xargs -I {} psql -f {} -d $database -U $datauser | grep "NOTICE\|ERROR"
for dataset in $datasets
do
    psql -f datasets/${dataset}.sql -d $database -U $datauser | grep "NOTICE\|ERROR"
done

exit 0
