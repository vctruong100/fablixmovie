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

## Parsing Time Optimization Strategies
### One: In-Memory Caching for Duplicate Checking:
- Implemented in-memory caching using hash maps and sets.
- Efficiently checked for duplicate entries (e.g., actors, films, and cast relationships) before database insertion.

### Two: 

### Performance Improvement 
- Naive Implementation: The initial, unoptimized implementation took 626 seconds (over 10 minutes) to complete.
- Optimized Implementation: After applying these two optimization strategies, the process now runs in under 30 seconds, resulting in a substantial performance boost.

## Inconsistent Data Reports
### actors.xml
- Missing or Empty Stage Name:
  - Some actors have no stage name or an empty stage name
- Invalid Date of Birth (DOB): 
  - DOBs that cannot be parsed as integers or are formatted incorrectly.

### casts.xml
- Missing Film ID (fid): 
  - Some cast entries are missing the film ID, leading to schema inconsistencies.
- Missing Actor Name: 
  - Some cast entries have no actor name or an empty actor name.
- Unresolved Film References: 
  - Film IDs that do not exist in the parsed films database are flagged as missing references.
- Duplicate Cast Entries: 
  - Identical cast relationships (same actor and film ID) are flagged as duplicates.
- Missing Actor in Database: 
  - Actors referenced in the cast data but not present in the actor database are added, with warnings for potential data integrity issues.
- Multiple Actor Entries Warning: 
  - Casts with the same actor name but multiple records for that actor are flagged = ambiguity.

### mains.xml
- Missing Film ID (fid): 
  - Films with no film ID or an empty film ID are marked as inconsistent.
- Empty Title: 
  - Film titles that are missing or empty are flagged.
- Invalid Year: 
  - Years that cannot be parsed as integers or are formatted incorrectly are reported.
- Duplicate Films: 
  - Films with the same title, year, and director are flagged as duplicates.
- Category Warnings: 
  - Empty or unrecognized categories are flagged, but these are treated as warnings rather than schema violations.
- Propagation of Categories: 
  - Duplicate film entries are updated with new categories if applicable.

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

