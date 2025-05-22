#!/bin/bash

source config.sh

# Create load balancer and configure health check.
aws elb create-load-balancer \
	--load-balancer-name CNV-LoadBalancer \
	--listeners "Protocol=HTTP,LoadBalancerPort=80,InstanceProtocol=HTTP,InstancePort=8000" \
	--availability-zones us-east-1a \
	--security-groups $AWS_SECURITY_GROUP

aws elb configure-health-check \
	--load-balancer-name CNV-LoadBalancer \
	--health-check Target=HTTP:8000/test,Interval=30,UnhealthyThreshold=2,HealthyThreshold=10,Timeout=5

# Create launch configuration.
# aws autoscaling create-launch-configuration \
#	--launch-configuration-name CNV-LaunchConfiguration \
#	--image-id $(cat image.id) \
#	--instance-type t2.micro \
#	--security-groups $AWS_SECURITY_GROUP \
#	--key-name $AWS_KEYPAIR_NAME \
#	--instance-monitoring Enabled=true

# Create a launch template.
aws ec2 create-launch-template \
  --launch-template-name CNV-LaunchTemplate \
  --version-description "v1" \
  --launch-template-data "{
    \"ImageId\": \"$(cat image.id)\",
    \"InstanceType\": \"t2.micro\",
    \"SecurityGroupIds\": [\"$AWS_SECURITY_GROUP\"],
    \"KeyName\": \"$AWS_KEYPAIR_NAME\",
    \"Monitoring\": {\"Enabled\": true}
  }"



# Create auto scaling group.
aws autoscaling create-auto-scaling-group \
	--auto-scaling-group-name CNV-AutoScalingGroup \
	--launch-template "LaunchTemplateName=CNV-LaunchTemplate,Version=1" \
	--load-balancer-names CNV-LoadBalancer \
	--availability-zones us-east-1a \
	--health-check-type ELB \
	--health-check-grace-period 60 \
	--min-size 1 \
	--max-size 1 \
	--desired-capacity 1
