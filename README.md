## Extra Credit Domain Registration: https://fablixmovies.tech/fab-project/login.html
- Credits to .Tech Domains for providing the free domain name for our project

## Demo
Project 1: https://www.youtube.com/watch?v=gUnYy8DNaRM

Project 2: https://www.youtube.com/watch?v=kSFtYlGfwkM

Project 3: 

## Contributions
### Vinh
- Integrated reCAPTCHA for login and added logout feature
- Updated the dashboard with proper GET/POST handling and secure password encryption
- Created a stored procedure add_movie for adding movies and stars to the database
- Worked on HTTPS configuration and URL redirection for secure connections
- Assisted in optimizing queries and updating the LoginFilter to handle customer and employee access
- Recorded the Demo video and updated the README file
- Registered a domain name on .tech and deployed the project on https://fablixmovies.tech/fab-project/login.html

### Jason
- Developed LoginFilter to handle dashboard and login URLs
- Modified the database schema to support employee logins, prices, and multiple movies per sale
- Migrated existing database records with scripts to include encrypted employee passwords and random movie prices
- Implemented data parsing for XML files (main.xml, actors.xml, and casts.xml) and reported inconsistencies
- Enhanced the security of the login process using prepared statements and proper password verification
- Worked on parsing time optimization strategies for XML data

## Filenames with Prepared Statements
- src/query/BaseQuery.java
- src/query/ConditionalQuery.java
- src/query/GroupingQuery.java
- src/query/MovieGenresQuery.java
- src/query/MovieListQuery.java
- src/query/MovieQuery.java
- src/query/MovieStarsQuery.java
- src/query/StarMoviesQuery.java
- src/query/StarQuery.java
- src/DashboardServlet.java
- src/LoginServlet.java
- src/PaymentServlet.java
- src/ShoppingCartServlet.java
- src/UpdateSecurePassword.java
- src/StanfordXmlParser.java
- Note: SingleStarServlet, SingleMovieServlet and MovieListServlet use src/query/* under the hood (listed above)

## Parsing Time Optimization Strategies
### 1: In-Memory Caching for Duplicate Checking:
- Implemented in-memory caching using hash maps and sets.
- Efficiently checked for duplicate entries (e.g., actors, films, and cast relationships) before database insertion.
- Significantly reduced redundant SQL queries.

### 2: Bulk Selection and Insertion of SQL Queries
- Consolidated multiple SQL queries into bulk operations.
- Reduced the number of database interactions by performing batch inserts and selections.
- Enhanced the efficiency of data loading and minimized transaction overhead.

### Performance Improvement 
- Naive Implementation: The initial, unoptimized implementation took `626` seconds (over 10 minutes) to complete.
- Optimized Implementation: After applying the bulk selection/insertion of SQL queries, the parser runs in under `30` seconds
- After in-memory caching for duplicate checking optimization, the parser now takes `8-16` seconds, a substantial performance boost compared to the naive implementation

## Inconsistent Data Reports
The following report also gives an idea of how the XML parser handles inconsistencies when such are marked as warnings.
### actors.xml
- Missing or Empty Stage Name:
  - If actors have no stage name or an empty stage name, these shall be reported as inconsistencies (none reported)
- Invalid Date of Birth (DOB): 
  - DOBs that cannot be parsed as integers or are formatted incorrectly.
  - These are reported as warnings and set to NULL, which still makes it consistent
  - Example: `ELEMENT actor 680: dob='n.a.' (WARNING);`
- Duplicates
  - Actors are marked as duplicates if they share the same stage name and dob, which is invalid
  - Example: `ELEMENT actor 727: stagename='Wilford Brimley' (DUPLICATE);dob='null' (DUPLICATE);`
### casts.xml
- Missing Film ID (fid): 
  - If cast entries do not have a film ID, they shall be marked as schema inconsistencies (none reported)
- Missing Actor Name: 
  - Some cast entries have no actor name or an empty actor name.
  - Example: `ELEMENT m 6749: a='';`
- Unresolved Film References: 
  - Film IDs that do not exist in the parsed films database are flagged as missing references.
  - Example: `ELEMENT m 72: f='AAd10' (MISS);`
- Duplicate Cast Entries: 
  - Identical cast relationships (same actor and film ID) are flagged as duplicates.
  - Example: `ELEMENT m 13416: f='GC52' (DUPLICATE);a='Elizabeth Taylor' (DUPLICATE);`
- Missing Actor in Database: 
  - Actors referenced in the cast data but not present in the actor database are added with `dob=null`, and marked as warnings for potential data integrity issues.
  - Example: `ELEMENT m 11302: a='Samuel Hinds' (WARNING) (MISS);`
- Multiple Actor Entries Warning: 
  - Casts with the same actor name but multiple records for that actor are flagged = ambiguity.
  - Example: `ELEMENT m 11901: a='Harvey Stephens' (WARNING) (MULTIPLE);`

### mains.xml
- Missing Film ID (fid): 
  - Films with no film ID or an empty film ID are marked as inconsistent.
  - Example: `ELEMENT film 2530: fid=null;`
- Empty Title: 
  - Film titles that are missing or empty are flagged.
  - Example: `ELEMENT film 9305: title='';`
- Invalid Year: 
  - Years that cannot be parsed as integers or are formatted incorrectly are reported. Unlike `actors.xml`, this is schema inconsistent.
  - Example: `ELEMENT film 11636: year='19yy';`
- Duplicate Films: 
  - Films with the same title, year, and director are flagged as duplicates. The film id it references is shown.
  - Example: `ELEMENT film 191: fid='Z0270';fid2='Z0260' (REFERENCE);title='Romeo and Juliet';year='1916';director='Unknown2';cats=['Romantic'] (PROPAGATED);`
- Category Warnings: 
  - Empty or unrecognized categories are flagged, but these are treated as warnings rather than schema violations.
- Propagation of Categories: 
  - Duplicate film entries are updated with new categories if applicable.
  - Example: `ELEMENT film 10934: fid='BS6';fid2='BS4' (REFERENCE);title='For Love or Money';year='1993';director='Sonnenfeld';cats=['Comedy', 'Romantic'] (PROPAGATED);`
  - Note in the example that `fid='BS4'` would have `Comedy` and `Romantic` applied to its categories if it didn't exist already.

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

