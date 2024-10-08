#!/bin/bash
#
# generates movie-data.sql with a curl request to the
# URL from the project 1 page

URL="https://inst-fs-iad-prod.inscloudgate.net/files/7ef2482f-273d-4915-8c5b-5bd1fab440f4/movie-data.sql?token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpYXQiOjE3MjgzNDY3MTQsInVzZXJfaWQiOiI0NDA3MDAwMDAwMDM0NzA2NiIsInJlc291cmNlIjoiL2ZpbGVzLzdlZjI0ODJmLTI3M2QtNDkxNS04YzViLTViZDFmYWI0NDBmNC9tb3ZpZS1kYXRhLnNxbCIsImp0aSI6IjY2ZTQ4MGFmLTU3YjUtNGRiMy1hYWM4LTEzYTcxMDlhOTIzYSIsImhvc3QiOiJjYW52YXMuZWVlLnVjaS5lZHUiLCJvcmlnaW5hbF91cmwiOiJodHRwczovL2E0NDA3LTI2MTI1NTc3LmNsdXN0ZXIzNy5jYW52YXMtdXNlci1jb250ZW50LmNvbS9jb3Vyc2VzLzQ0MDd-NjM1MDEvZmlsZXMvNDQwN34yNjEyNTU3Ny9jb3Vyc2UlMjBmaWxlcy9Qcm9qZWN0MS9tb3ZpZS1kYXRhLnNxbD9jb250ZXh0X2lkPTQ0MDd-NjM1MDFcdTAwMjZjb250ZXh0X3R5cGU9Q291cnNlXHUwMDI2ZG93bmxvYWQ9MVx1MDAyNmlkPTQ0MDcwMDAwMDI2MTI1NTc3XHUwMDI2aW5saW5lPTFcdTAwMjZub19jYWNoZT10cnVlXHUwMDI2cmVkaXJlY3Q9dHJ1ZSIsImV4cCI6MTcyODQzMzExNH0.-51qPjz6K1Bn0WjdrXSuPrhd4IKsJDYzx9_mYNDL6yniaLc4TXQ8jvaIto9SUhBdJU6LN7UV6KlwUsAD5ApRsQ"

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