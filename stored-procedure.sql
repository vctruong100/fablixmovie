CREATE PROCEDURE add_movie(
    in movieTitle varchar (100),
    in movieYear int,
    in movieDirector varchar (100),
    in starName varchar (100),
    in genreName varchar (50),
    out statusMessage varchar (255)
)
