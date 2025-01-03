DELIMITER //

CREATE PROCEDURE add_movie(
    in movieTitle varchar (100),
    in movieYear int,
    in movieDirector varchar (100),
    in starName varchar (100),
    in genreName varchar (50),
    out statusMessage varchar (255)
)
BEGIN
    DECLARE movieExists INT;
    DECLARE genreId INT;
    DECLARE starId VARCHAR(10);
    DECLARE newMovieId VARCHAR(10);

    -- Check if the movie exists
    SELECT COUNT(*) INTO movieExists
    FROM movies
    WHERE title = movieTitle AND year = movieYear AND director = movieDirector;

    IF movieExists > 0 THEN
        SET statusMessage = 'Movie already exists.';
    ELSE
    -- Find or create genre
    SELECT id INTO genreId FROM genres WHERE name = genreName LIMIT 1;
    IF genreId IS NULL THEN
        INSERT INTO genres (name) VALUES (genreName);
        SET genreId = LAST_INSERT_ID();
    END IF;

    -- Find or create star
    SELECT id INTO starId FROM stars WHERE name = starName LIMIT 1;
    IF starId IS NULL THEN
        -- Generate a new starId if the star does not exist
        SELECT MAX(id) INTO starId FROM stars WHERE id LIKE 'nm%';
        IF starId IS NULL THEN
            SET starId = 'nm0000001';
        ELSE
            SET starId = CONCAT('nm', LPAD(SUBSTRING(starId, 3) + 1, 7, '0'));
        END IF;

        -- Insert the new star
        INSERT INTO stars (id, name) VALUES (starId, starName);
    END IF;

    -- Insert new movie with unique ID
    SELECT MAX(id) INTO newMovieId FROM movies WHERE id LIKE 'tt%';
    IF newMovieId IS NULL THEN
        SET newMovieId = 'tt0000001';
    ELSE
        SET newMovieId = CONCAT('tt', LPAD(SUBSTRING(newMovieId, 3) + 1, 7, '0'));
    END IF;

    -- Insert the new movie
    INSERT INTO movies (id, title, year, director) VALUES (newMovieId, movieTitle, movieYear, movieDirector);

    -- Link movie to star and genre
    INSERT INTO stars_in_movies (starId, movieId) VALUES (starId, newMovieId);
    INSERT INTO genres_in_movies (genreId, movieId) VALUES (genreId, newMovieId);

    -- Generate random price for this movie
    SET @min_price = 1.00;
    SET @max_price = 100.00;
    SET @price_range = @max_price - @min_price;
    INSERT INTO prices (movieId, price)
    VALUES (newMovieId, TRUNCATE(RAND() * @price_range + @min_price, 2));

    SET statusMessage = 'Movie added successfully.';
    END IF;
END //
DELIMITER ;
