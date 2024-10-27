## Demo
Project 1: https://www.youtube.com/watch?v=gUnYy8DNaRM

Project 2: https://www.youtube.com/watch?v=kSFtYlGfwkM
Note: I initially typed "tern" instead of "term" in the search bar, resulting in an incorrect movie item. 
I redid the entire search to demonstrate that it functions correctly.

## Contributions
### Vin
- Implemented MainPageServlet, ShoppingCartServlet, and PaymentServlet
- Developed JavaScript (JS) and HTML for payment, shopping cart, and confirmation pages
- Helped extend the Movie List feature on the front end
- Created styles for each HTML page
- Recorded the Demo video

### Jason
- Implemented LoginServlet, SearchServlet, BrowseServlet
- Designed the SQL queries for search / browsing
- Extended Movie List, Single Star, Single Movie with proper stars & genres ordering according to spec
- Jointly worked on supporting jumps from single pages to movie list
- Modified DB schema to support prices and multiple movies per sale
- Migrated existing DB with scripts to generate random prices for movies and associate existing sales with new sales records

## Substring Pattern Design
### Title / Director / Star (Search)
- Searches movie by either title / director / star
- Pattern matching implemented in `src/query/MovieListQuery.java`
- Case insensitivity implemented by matching the lowercase column with the lowercase pattern
- We use `LIKE` to match the column with `%query%`, where `query` is replaced with the user's query
- This matches columns where the substring exists
- Examples:
  - `inter` matches `Divine Intervention` or `Winter Tale`
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

