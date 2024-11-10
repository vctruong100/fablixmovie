#!/bin/bash
#
# generates movie-data.sql with a curl request to the
# URL from the project 1 page

URL="https://canvas.eee.uci.edu/courses/63501/files/26125577/download"

echo "-- interpret as utf8 and disable autocommit for faster population of data
SET NAMES utf8mb4;
SET GLOBAL autocommit = 0;

-- begin movie-data.sql" > ./movie-data.sql

curl "$URL" -L >> ./movie-data.sql

echo "
-- end movie-data.sql

-- commit transactions and re-enable autocommit
COMMIT;
SET GLOBAL autocommit = 1;" >> ./movie-data.sql