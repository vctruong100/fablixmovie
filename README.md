## Extra Credit: Fuzzy Search
- Implemented a fuzzy search feature using the Levenshtein distance algorithm.

## Demo
Project 1: https://www.youtube.com/watch?v=gUnYy8DNaRM

Project 2: https://www.youtube.com/watch?v=kSFtYlGfwkM

Project 3: https://www.youtube.com/watch?v=IttZBBCscT4

Project 4:

# General
## Team5 Contributions
### Vinh
- Created AutocompleteServlet to support autocomplete search
- Integrated autocomplete functionality into HTML, JS, and CSS.
- Created Master/Slave routing for read/write operations on AWS
- Scale project with Tomcat and MySQL cluster and load balancing on AWS
- Created demo video for Project 4.

### Jason
- Refined MovieListQuery to improve query structure and clarity.
- Implemented initial full-text search
- Developed edth.sh to install the edth toolkit for Levenshtein distance-based fuzzy search
- Integrated fuzzy search in the backend

## Instruction of Deployment

# Connection Pooling
- ## Filename/Path of All Code/Configuration Files using JDBC Connection Pooling:
  - context.xml: /WebContent/META-INF/context.xml

- ## How Connection Pooling is Utilized in the Code:
  - Connection pooling is configured in context.xml using the org.apache.tomcat.jdbc.pool.DataSourceFactory 
    with a maximum of 100 connections, 30 idle connections and a 10-second wait time
  - Servlets use DataSource to fetch connections:
  ```
  dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
  try (Connection conn = dataSource.getConnection()) {
  PreparedStatement ps = conn.prepareStatement(query);
  ResultSet rs = ps.executeQuery();
  }
  ```
- ## Connection Pooling with 2 Backend SQL:
  - In a master-slave setup:
    - Write queries (e.g., insert, update) are routed to the master database.
    - Read queries (e.g., select) are routed to slave databases.
  - MySQL handles read/write routing internally when configured with master-replica replication. 
    The application sends queries to the master, and replicas sync automatically for reads.

# Master/Slave
- ## Filename/Path of All Code/Configuration Files Routing Queries to Master/Slave SQL:
  - context.xml: /WebContent/META-INF/context.xml
  - AutocompleteServlet.java: /src/AutocompleteServlet.java
  - DashboardServlet.java: /src/DashboardServlet.java
  - LoginServlet.java: /src/LoginServlet.java
  - MainPageServlet.java: /src/MainPageServlet.java
  - MovieListServlet.java: /src/MovieListServlet.java
  - PaymentServlet.java: /src/PaymentServlet.java
  - ShoppingCartServlet.java: /src/ShoppingCartServlet.java
  - SingleMovieServlet.java: /src/SingleMovieServlet.java
  - SingleStarServlet.java: /src/SingleStarServlet.java

- ## How Read/Write Requests were Routed to Master/Slave SQL:
  - MySQL master-replica replication automatically routes:
    - Write operations to the master database.
      - Example: DashboardServlet performs a write operation to add a new movie.
  ```
  CallableStatement cs = conn.prepareCall("{CALL add_movie(?, ?, ?, ?, ?, ?)}");
  cs.setString(1, title);
  cs.setInt(2, year);
  cs.setString(3, director);
  cs.setString(4, star);
  cs.setString(5, genre);
  cs.registerOutParameter(6, Types.VARCHAR);
  cs.execute();
  ```
    - Read operations to replicas (if the query originates from replicas via appropriate load balancing).
      - Example: MainPageServlet reads data (genres) from the db.
  ```
  String genresQuery = "SELECT * FROM genres ORDER BY name ASC";
  PreparedStatement ps = conn.prepareStatement(genresQuery);
  ResultSet rs = ps.executeQuery();
  ``` 

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

