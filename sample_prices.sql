SET @min_price = 1.00;
SET @max_price = 100.00;
SET @price_range = @max_price - @min_price;

-- autocommit off for faster transactions
SET GLOBAL autocommit = 0;

-- assign random prices between min_price and max_price
-- for each movie in the database
DELETE FROM prices;
INSERT INTO prices (movieId,price)
SELECT m.id, TRUNCATE(RAND() * @price_range + @min_price, 2) price FROM movies m;

-- commit transactions and re-enable autocommit
COMMIT;
SET GLOBAL autocommit = 1;