-- autocommit off for faster transactions
SET GLOBAL autocommit = 0;

-- for each entry in sales without any sales records,
-- generate a sales record with quantity 1 and salePrice
-- equivalent to the individual movie price at the time
-- the sales record was created

INSERT INTO sales_records (saleId, movieId, salePrice, quantity)
SELECT s.id, s.movieId, p.price, 1
FROM sales s
	LEFT JOIN movies m ON s.movieId = m.id
	LEFT JOIN prices p ON s.movieId = p.movieId
WHERE s.id NOT IN (SELECT saleId FROM sales_records sr);


-- commit transactions and re-enable autocommit
COMMIT;
SET GLOBAL autocommit = 1;
