# Fabflix Deployment Guide

This guide walks through deploying Fabflix across different phases (p1–p5). Each phase corresponds to a progression in your infrastructure, from a simple single-instance AWS setup to a Kubernetes cluster. Refer to the relevant phase depending on your current branch or the setup you plan to implement.

## Table of Contents
1. [Phase 1 (p1–p3): Single AWS Setup](#phase-1-p1p3-single-aws-setup)
2. [Phase 2 (p3): reCAPTCHA & HTTPS](#phase-2-p3-recaptcha--https)
3. [Phase 3 (p4): Master-Slave Replication & Load Balancing](#phase-3-p4-master-slave-replication--load-balancing)
4. [Phase 4 (p5): Docker & Kubernetes](#phase-4-p5-docker--kubernetes)
5. [Additional Notes](#additional-notes)

---

## Phase 1 (p1–p3): Single AWS Setup

### 1. Launch a Free-Tier Ubuntu AWS EC2 Instance
- Sign in to your AWS Console and create a **free-tier t2.micro EC2** instance running **Ubuntu 20.04 or 22.04**.
- Download the SSH key pair (`.pem` file) to connect to your instance.
- Open inbound ports in the Security Group:
  - **22 (SSH)** to your IP address.
  - **8080 (Tomcat)** to the subnet range required for your course or testing environment.
  - **3306 (MySQL)** only to the internal IPs as needed (or keep it restricted if you’re testing locally).

### 2. Install Java 11 (or 17 on Ubuntu 22.04)
```bash
sudo apt update
sudo apt install default-jdk
```
Verify Java installation:
```bash
java -version
```

### 3. Install MySQL 8
```bash
sudo apt update
sudo apt install mysql-server
```
- Create a MySQL user (e.g., mytestuser) with the required privileges.
- Enable MySQL logging:
```bash
SET GLOBAL general_log = 'ON';
```
- Check log file paths with:
```bash
SHOW VARIABLES LIKE '%log%';
```

### 4. Install Tomcat 10
- Add the repository and install:
```bash
sudo add-apt-repository -S deb http://cz.archive.ubuntu.com/ubuntu lunar main universe
sudo apt update
sudo apt install tomcat10 tomcat10-admin
```
- Configure Tomcat users by editing /etc/tomcat10/tomcat-users.xml to add a manager-gui user:
```bash
<role rolename="manager-gui"/>
<user username="admin" password="mypassword" roles="manager-gui"/>
```
- Restart Tomcat
```bash
sudo systemctl restart tomcat10
```
- Ensure inbound rule for 8080 is open.

### 5. Test Tomcat & Deploy Fabflix
- Go to http://<your-ec2-public-ip>:8080/manager/html.
- Deploy your WAR file (if you’re generating one) or place your app in /var/lib/tomcat10/webapps/.

### 6. Set Up Maven on Your Local Machine
```bash
sudo apt-get install maven
```

## Phase 2 (p3): reCAPTCHA & HTTPS

### 1. Integrate reCAPTCHA
- Obtain keys from Google reCAPTCHA.
- Add your AWS instance’s public IP and localhost to the allowed domains.
- In your Fabflix login (or other) pages, include the reCAPTCHA logic both front-end and back-end to validate the token.
    

### 2. Enable HTTPS (Self-Signed Certificate)
    1. Generate a keystore on your AWS instance:
```bash 
sudo keytool -genkey -alias fabflix -keyalg RSA -keystore /var/lib/tomcat10/keystore
```
    2. In /etc/tomcat10/server.xml, uncomment and configure the HTTPS connector (port 8443)
```bash
<Connector port="8443" protocol="org.apache.coyote.http11.Http11NioProtocol" maxThreads="150" SSLEnabled="true">
    <UpgradeProtocol className="org.apache.coyote.http2.Http2Protocol" />
    <SSLHostConfig>
        <Certificate certificateKeystoreFile="/var/lib/tomcat10/keystore"
                    certificateKeystorePassword="changeit"
                    type="RSA" />
    </SSLHostConfig>
</Connector>
```
    3. Open port 8443 in your AWS Security Group.
    4. Restart Tomcat: sudo systemctl restart tomcat10
    5. Access Tomcat Manager at https://<your-public-ip>:8443/manager/html.
    You may bypass browser warnings about the self-signed cert.

    6.  (Optional) Force HTTPS only:
    - In your app’s web.xml, add:
    ```bash
    <security-constraint>
    <web-resource-collection>
        <web-resource-name>HTTPSOnly</web-resource-name>
        <url-pattern>/*</url-pattern>
    </web-resource-collection>
    <user-data-constraint>
        <transport-guarantee>CONFIDENTIAL</transport-guarantee>
    </user-data-constraint>
    </security-constraint>
    ```
    - This redirects any HTTP request to HTTPS.

## Phase 3 (p4): Master-Slave Replication & Load Balancing

### 1. Set Up Two New AWS Instances
- Create two additional AWS EC2 Ubuntu instances, each identical to your initial setup (MySQL, Tomcat, Java).
- Open port 3306 (MySQL) for internal AWS IP addresses to allow replication.

### 2. Configure MySQL Master
- Edit /etc/mysql/mysql.conf.d/mysqld.cnf:
- Set bind-address = 0.0.0.0.
- Uncomment server-id and log_bin lines.
- Restart MySQL.
- Create a replication user:
```sql
CREATE USER 'repl'@'%' IDENTIFIED WITH mysql_native_password BY 'slave66Pass$word';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
SHOW MASTER STATUS;  -- note File and Position
```

### 3. Configure MySQL Slave
- Edit /etc/mysql/mysql.conf.d/mysqld.cnf:
    - Set bind-address = 0.0.0.0.
    - Uncomment server-id (e.g., server-id=2), but do not enable log_bin.
- Restart MySQL, then:
```sql
CHANGE MASTER TO MASTER_HOST='172.2.2.2', MASTER_USER='repl', MASTER_PASSWORD='slave66Pass$word', MASTER_LOG_FILE='mysql-bin.000001', MASTER_LOG_POS=337;
START SLAVE;
SHOW SLAVE STATUS;
```

### 4. Deploy Tomcat on Both Master & Slave
- Each instance runs a Tomcat server pointed to the master or slave MySQL DB.
- For write queries, ensure your app uses the master DB. Reads can be performed on either server.

### 5. Load Balancer (Apache HTTPD) on a Separate Instance
1. Create a third AWS instance to act as the load balancer (LB).
2. Install Apache2:
```bash
sudo apt-get update
sudo apt-get install apache2
sudo a2enmod proxy proxy_balancer proxy_http rewrite headers lbmethod_byrequests
sudo service apache2 restart
```
3. In /etc/apache2/sites-enabled/000-default.conf, add a <Proxy> block referencing your two Tomcat backends:
```xml
<Proxy "balancer://TomcatPooling_balancer">
    BalancerMember "http://172.2.2.2:8080/cs122b-project5-TomcatPooling-example/"
    BalancerMember "http://172.3.3.3:8080/cs122b-project5-TomcatPooling-example/"
</Proxy>

<VirtualHost *:80>
    ProxyPass /cs122b-project5-TomcatPooling-example balancer://TomcatPooling_balancer
    ProxyPassReverse /cs122b-project5-TomcatPooling-example balancer://TomcatPooling_balancer
    ...
</VirtualHost>
```
4. Open port 80 in the LB instance Security Group and test by browsing to http://<LB-instance-ip>/cs122b-project5-TomcatPooling-example.

### 6. Sticky Sessions (Optional)
- In the same Apache config:
```xml
Header add Set-Cookie "ROUTEID=.%{BALANCER_WORKER_ROUTE}e; path=/" env=BALANCER_ROUTE_CHANGED

<Proxy "balancer://Session_balancer">
    BalancerMember "http://172.2.2.2:8080/cs122b-project2-session-example" route=1
    BalancerMember "http://172.3.3.3:8080/cs122b-project2-session-example" route=2
    ProxySet stickysession=ROUTEID
</Proxy>

<VirtualHost *:80>
    ProxyPass /cs122b-project2-session-example balancer://Session_balancer
    ProxyPassReverse /cs122b-project2-session-example balancer://Session_balancer
    ...
</VirtualHost>
```

## Phase 4 (p5): Docker & Kubernetes
1. Dockerize Fabflix
- On the AWS instance, edit the `/etc/mysql/mysql.conf.d/mysqld.cnf` file and set `bind-address` to `0.0.0.0`.
- Restart MySQL by running `sudo service mysql restart`.
- Install Docker on the AWS instance by following the instructions on https://docs.docker.com/engine/install/ubuntu/. Use the instructions under "Install using the apt repository".
- Register a Docker Hub account.
- Log into our Docker Hub account by running `sudo docker login` on the AWS instance;
- Go to the root folder of this application. Run `sudo docker build . --platform linux/amd64 -t <DockerHub-user-name>/fablixmovies:v1 `
- Check the created image by running `sudo docker images`
- Push the image to Docker Hub by running the following command: `sudo docker push <DockerHub-user-name>/fablixmovies:v1`
- Log in our Docker Hub web page. We should be able to see the newly pushed image.
- Use the image to start a Docker container on the AWS instance
    - In the security group of the AWS instance, open the "8080" port to our local machine.
    - On the AWS instance, use the image to start a container by running `sudo docker run --add-host host.docker.internal:host-gateway -p 8080:8080 <image ID>` to start a docker container to run your application.  In the command, `-p 8080:8080` means we bind the port 8080 (first parameter) of the host instance to the port 8080 (second parameter) of the container.  When the host machine (the AWS instance) receives a request to the port 8080, the request will be relayed to the container's port 8080.
    - Use our browser to access the website via `<AWS_PUBLIC_IP>:8080/fab-project/login.html`.
- The following commands are useful to manage containers on the AWS instance:
    - `sudo docker ps -a`: list all the containers
    - `sudo docker logs <container ID>` : see the logs of a container
    - `sudo docker stop <container ID>` : stop a running container
    - `sudo docker rm <container ID>` : delete a stopped container
- The following commands are useful to manage images on the AWS instance:
    - `sudo docker images` : list the images
    - `sudo docker rmi <image ID>` : delete an image

2. Kubernetes Cluster
- Please refer to KUBSETUP.md

## Additional Notes
-Branch Differences: p1–p3 rely on a single AWS instance, p4 introduces master-slave replication and an Apache load balancer, and p5 uses Docker containers and Kubernetes.
- Logging: Monitor Tomcat logs (/var/lib/tomcat10/logs/) and MySQL logs (/var/log/mysql/) for any issues.
- Security: Restrict inbound rules to IP ranges required for testing or your institution.
