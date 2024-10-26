## Demo
https://www.youtube.com/watch?v=gUnYy8DNaRM

## Contributions
### Vinh
- Implemented JS and HTML for Movie List (index), Single Star, and Single Movie
- Implemented and integrated styles.css
### Jason
- Did create_table.sql and set up other SQL scripts
- Implemented SingleStarServlet, SingleMovieServlet, MovieListServlet

## Substring Pattern Design
### Title / Director / Star (Search)
- Searches movie by either title / director / star
- Pattern matching implemented in `src/query/MovieListQuery.java`
- Case insensitivity implemented by matching the lowercase column with the lowercase pattern
- We use `LIKE` to match the column with `query%` or `% query%`, where `query` is replaced with the user's query
- This matches columns that either start with the query or has a word that starts with the query
- We didn't do `%query%` because `inter` could match titles with `Winter`, and it's not immediately obvious how they're related
- Examples:
  - `inter` matches `Intermission` or `Divine Intervention` but not `Winter Tale`
  - `hell` matches `Comedy Hell` or `Hello, Goodbye!`

### Alpha (Browse by Title)
- Searches movie based on the first char of the movie title (alpha or non-alpha)
- Pattern matching implemented in `src/query/MovieListQuery.java`
- Case insensitivity implemented by matching the lowercase column with lowercase patterns
- If `alpha` is a valid alphanumeric char, we use `LIKE` to match title with `alpha%`, which gets titles that start with `alpha`
- Otherwise, wildcard `*` uses `REGEXP_LIKE` to match title with regex `^[^a-zA-Z0-9].+`, which gets titles that start with a non-alphanumeric char
- Examples:
  - `alpha=3` matches `3000` or `3b1b`
  - `alpha=A` matches `A Movie` or `Asterisk`
  - `alpha=*` matches `:)` or `.hack`

## Requirements
- Java 11.0.24
- Tomcat 10
- MySQL 8.0
- Maven

## Before running
- Setup MySQL by creating a user `mytestuser` with privileges:
```mysql
CREATE USER 'mytestuser'@'localhost' IDENTIFIED BY 'My6$Password';
GRANT ALL PRIVILEGES ON *.* TO 'mytestuser'@'localhost';
```

