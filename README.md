## Demo
Project 1: https://www.youtube.com/watch?v=gUnYy8DNaRM

Project 2: https://www.youtube.com/watch?v=kSFtYlGfwkM

Project 3: https://www.youtube.com/watch?v=IttZBBCscT4

Project 4: https://www.youtube.com/watch?v=EvFg85EX3F8

Project 5: https://www.youtube.com/watch?v=O6n0-ZlaY3M

# General
## Team5 Contributions
### Vinh
- Added Docker file, built Docker Image, and pushed image to Docker Hub
- Set up Kubernetes cluster config with master/slave database on AWS
- Deployed Fabflix to Kubernetes cluster
- Created demo video for Project 5

### Jason
- Setup YAML files for Kubernetes deployment
- Developed the JMX test plan file for JMeter
- Helped run the JMeter tests
- Assisted in resolving issues during Kubernetes deployment

## Kubernetes Cluster JMeter Test Results (1 minute)
### 1 Control Plane + 3 Worker nodes + 1 master MySQL pod + 1 slave MySQL pod + 2 Fabflix pods
- Samples: 10495
- Throughput: 10274.61/min
### 1 Control Plane + 4 Worker nodes + 1 master MySQL pod + 1 slave MySQL pod + 3 Fabflix pods
- Samples: 9887
- Throughput: 9514.46/min

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

