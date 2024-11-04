-- create moviedb database if it doesn't exist already
CREATE DATABASE IF NOT EXISTS moviedb;
USE moviedb;

-- create movie table
CREATE TABLE IF NOT EXISTS movies(
	id varchar(10) primary key,
	title varchar(100) not null,
	year integer not null,
	director varchar(100) not null
);

-- create stars table
CREATE TABLE IF NOT EXISTS stars(
	id varchar(10) primary key,
	name varchar(100) not null,
	birthYear integer
);

-- create stars_in_movies table
CREATE TABLE IF NOT EXISTS stars_in_movies(
	starId varchar(10) not null,
	movieId varchar(10) not null,
	foreign key(starId) references stars(id),
	foreign key(movieId) references movies(id)
);

-- create genres table
CREATE TABLE IF NOT EXISTS genres(
	id integer primary key AUTO_INCREMENT,
	name varchar(32) not null
);

-- create genres_in_movies table
CREATE TABLE IF NOT EXISTS genres_in_movies(
	genreId integer not null,
	movieId varchar(10) not null,
	foreign key(genreId) references genres(id),
	foreign key(movieId) references movies(id)
);

-- create creditcards table
CREATE TABLE IF NOT EXISTS creditcards(
	id varchar(20) primary key,
	firstName varchar(50) not null,
	lastName varchar(50) not null,
	expiration date not null
);

-- create customers table
CREATE TABLE IF NOT EXISTS customers(
	id integer primary key AUTO_INCREMENT,
	firstName varchar(50) not null,
	lastName varchar(50) not null,
	ccId varchar(20) not null,
	address varchar(200) not null,
	email varchar(50) not null,
	password varchar(20) not null,
	foreign key(ccId) references creditcards(id)
);

-- create sales table
CREATE TABLE IF NOT EXISTS sales(
	id integer primary key AUTO_INCREMENT,
	customerId integer not null,
	movieId varchar(10) not null,
	saleDate date not null,
	foreign key(customerId) references customers(id),
	foreign key(movieId) references movies(id)
);

-- create ratings table
CREATE TABLE IF NOT EXISTS ratings(
	movieId varchar(10) not null,
	rating float not null,
	numVotes integer not null,
	foreign key(movieId) references movies(id)
);

-- create prices table
CREATE TABLE IF NOT EXISTS prices(
	movieId varchar(10) not null,
	price decimal(5,2) not null,
	foreign key(movieId) references movies(id)
);

-- create sales records table
CREATE TABLE IF NOT EXISTS sales_records(
    saleId integer not null,
    movieId varchar(10) not null,
    salePrice decimal(5,2) not null,
    quantity integer not null,
    foreign key(saleId) references sales(id),
    foreign key(movieId) references movies(id)
);

CREATE TABLE IF NOT EXISTS employees(
    email varchar(50) primary key,
    password varchar(128) not null, -- 128 for encrypted password?
    fullname varchar(100)
);

-- Sample employee
INSERT INTO employees (email, password, fullname)
VALUES ('classta@email.edu', 'classta', 'TA CS122B');