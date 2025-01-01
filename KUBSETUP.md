## Step 1: Preparation
1. Create an Ubuntu AWS EC2 (t2.micro) instance called “k8s-admin”. We will set up our k8s cluster from this instance. Note that the “k8s-admin” instance itself is not part of the K8s cluster, and it does not run any pods.
2. SSH into the “k8s-admin” instance from your laptop.
3. Install the AWS Command Line Interface. (CLI) on the instance.
```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
```

5. Install unzip and use it to unzip this zip file.
```bash
sudo apt install unzip
unzip awscliv2.zip
sudo ./aws/install
```

6. Install Kubernetes command line tool. (kubectl)
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

7. To test whether it’s installed correctly, run 
kubectl version --client 
It should output a valid kubectl version.

8. Install the kOps. toolkit for managing k8s clusters on AWS
```bash
curl -Lo kops https://github.com/kubernetes/kops/releases/download/$(curl -s https://api.github.com/repos/kubernetes/kops/releases/latest | grep tag_name | cut -d '"' -f 4)/kops-linux-amd64

chmod +x kops

sudo mv kops /usr/local/bin/kops
```

9. Follow the instructions on this page. to create an access key on your AWS account. Write down the newly created “Access Key” and “Secret Access key”. On the EC2 instance, run:
aws configure 

10. It will ask for both keys. Use us-west-2 as the “Default region name”. Use json as the “Default output format”.
To allow kOps to create and manage resources in our AWS account, use the following steps to create an AWS IAM user with all necessary access privileges from the command line:
```bash
aws iam create-group --group-name k8s-admin-group

aws iam attach-group-policy --policy-arn arn:aws:iam::aws:policy/AmazonEC2FullAccess --group-name k8s-admin-group

aws iam attach-group-policy --policy-arn arn:aws:iam::aws:policy/AmazonRoute53FullAccess --group-name k8s-admin-group

aws iam attach-group-policy --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess --group-name k8s-admin-group

aws iam attach-group-policy --policy-arn arn:aws:iam::aws:policy/IAMFullAccess --group-name k8s-admin-group

aws iam attach-group-policy --policy-arn arn:aws:iam::aws:policy/AmazonVPCFullAccess --group-name k8s-admin-group

aws iam attach-group-policy --policy-arn arn:aws:iam::aws:policy/AmazonSQSFullAccess --group-name k8s-admin-group

aws iam attach-group-policy --policy-arn arn:aws:iam::aws:policy/AmazonEventBridgeFullAccess --group-name k8s-admin-group

aws iam create-user --user-name k8s-admin-user

aws iam add-user-to-group --user-name k8s-admin-user --group-name k8s-admin-group
```

11. After creating the IAM user, we should generate an access key for the IAM user and copy and paste the SecretAccessKey and AccessKeyID in the returned JSON output into a scratch notepad:
```bash 
aws iam create-access-key --user-name k8s-admin-user
```
12. Run the following commands to log in as the IAM user you just created. 
```bash
aws configure
```
Use your new access and secret key (created for the k8s-admin-user) here. Again, make sure to use us-west-2 for the “Default region name”.
Run the following command to check whether you are able to see the IAM user “k8s-admin-user”.
```bash
aws iam list-users
```
13. kOps uses two global variables AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY  when managing your AWS resources, so we must set their values:
```bash
export AWS_ACCESS_KEY_ID=$(aws configure get aws_access_key_id)

export AWS_SECRET_ACCESS_KEY=$(aws configure get aws_secret_access_key)
```
In order to store the state of your cluster, we need to create a dedicated S3 bucket for kOps to use. In this tutorial we’ll call this bucket <YOUR_BUCKET_NAME>-state-store, and you should change <YOUR_BUCKET_NAME> to a unique name. Follow AWS S3 Bucket Naming rules. to decide a name. Make sure it’s long and random. See a related story.
aws s3api create-bucket --bucket <YOUR_BUCKET_NAME>-state-store --region us-west-2 --create-bucket-configuration LocationConstraint=us-west-2

15. Create a bucket for hosting OIDC documents.
```bash
aws s3api create-bucket --bucket <YOUR_BUCKET_NAME>-oidc-store --region us-west-2 --object-ownership BucketOwnerPreferred --create-bucket-configuration LocationConstraint=us-west-2
aws s3api put-public-access-block --bucket <YOUR_BUCKET_NAME>-oidc-store --public-access-block-configuration BlockPublicAcls=false,IgnorePublicAcls=false,BlockPublicPolicy=false,RestrictPublicBuckets=false

aws s3api put-bucket-acl --bucket <YOUR_BUCKET_NAME>-oidc-store --acl public-read
```

16. If your S3 bucket has a default encryption set up, kOps will use it:
```bash
aws s3api put-bucket-encryption --bucket <YOUR_BUCKET_NAME>-state-store --server-side-encryption-configuration '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'
```

## Step 2: Creating a cluster on AWS
1. Set up a few environment variables. To give your first kOps cluster a name, define a NAME global variable. Note that this is an environment variable, so you can rename it to something more meaningful like (CLUSTER_NAME).
For a gossip-based cluster, the NAME variable must end with the suffix .k8s.local
```bash
export NAME=myfirstcluster.k8s.local
```
2. Set up the following environment variable to tell your kOps cluster which bucket to store its state in.
```bash
export KOPS_STATE_STORE=s3://<YOUR_BUCKET_NAME>-state-store
```
3. Find which zones are available in your region. In this tutorial since we will deploy our cluster to the us-west-2 region, we run the following 
```bash
aws ec2 describe-availability-zones --region us-west-2
```
You will see a list of zones available to you. Choose any one availability zone. In the rest of the tutorial, we will use zoon us-west-2a.
3. Run the following command to generate a cluster configuration, without building the cluster. 
```bash
kops create cluster --name=${NAME} --cloud=aws --zones=us-west-2a --discovery-store=s3://<YOUR_BUCKET>-oidc-store/${NAME}/discovery
```
Now build the cluster.
```bash
kops update cluster --name ${NAME} --yes --admin
```
kOps will start allocating AWS resources for your kubernetes cluster, such as EC2 instances, instance groups, load balancers etc.
4. This step can take some time (~5 mins). Just wait.  Then run the following command to verify whether kOps has finished setting up the cluster: kops validate cluster

If it gives a ValidationError / Unauthorized error, it means that the cluster is not ready yet, and you need to continue waiting. If the command gives you a “Cluster is ready to use”, you have successfully set up the k8s cluster! You can also have a look at your EC2 dashboard (in us-west-2 region) and see automatically spawned AWS instances created by kOps.

## Scale up your cluster to more EC2 instances
1. Run 
```bash
kops get ig
``` 
to list the instance groups. By default kOps will spawn two t3.medium EC2 instances, one as the control-plane and one as a worker node. However, to deploy our cluster, we need more instances. We suggest you use 3 worker nodes and 1 control-plane node.
2. To add more worker nodes in the Kubernetes cluster, run the following command to edit a JSON file:
```bash
kops edit ig nodes-us-west-2a
```
3. Change the minNodes and maxNodes attributes to the following values :
maxSize: 3

minSize: 3

4. Run the following to apply these changes to your cluster:
```bash
kops update cluster --name=${NAME} --yes
```
If you see a message “Changes may require instances to restart: kops rolling-update cluster”. In that case do what it says, i.e., run
kops rolling-update cluster

## Step 3: Pause/stop the cluster
When you don’t need the cluster, make sure to stop the cluster to save costs.
1. To do so, run
kops get ig

to list the instance groups. You will see two instance groups – one for the control plane and one for the node. 

2. Run
```bash
kops edit ig nodes-us-west-2a
```
and change the minSize and maxSize values to 0.

3. Do the same for the control page group by running
```bash
kops edit ig control-plane-us-west-2a
```
4. To apply these changes to your cluster, run
```bash
kops update cluster --name=${NAME} --yes
```
If you get a message  “Changes may require instances to restart: kops rolling-update cluster”, just  run
kops rolling-update cluster 

## Step 4: Delete the cluster
1. To delete the cluster, run
kops delete cluster --name ${NAME}
It may take a few minutes.
2. Run
kops delete cluster --name ${NAME} --yes
to confirm.
3. Delete the S3 buckets you’ve created from AWS Console. Make sure you select the correct region.
4. Delete (if) any load balancers you see in your AWS Console. Make sure you select the correct region.
5. Delete any VPCs and EBS volumes as well!

## Step 5: Modify context.mxl:
- DataSources specified in the context.xml will no longer be individual MySQL servers but rather two Kubernetes services:
    - mysql-primary:3306 will redirect queries to the only Master pod. It can handle both Read and Write queries.
    - mysql-secondary:3306 will redirect queries to one of many Slave pods. It can handle Read queries ONLY.