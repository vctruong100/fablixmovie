## Demo
https://www.youtube.com/watch?v=gUnYy8DNaRM

## Contributions
### Vinh
- Implemented JS and HTML for Movie List (index), Single Star, and Single Movie
- Implemented and integrated styles.css
### Jason
- Did create_table.sql and set up other SQL scripts
- Implemented SingleStarServlet, SingleMovieServlet, MovieListServlet

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

