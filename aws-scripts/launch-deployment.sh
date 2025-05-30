#!/bin/bash

source config.sh

# Create load balancer and configure health check.
aws elb create-load-balancer \
	--load-balancer-name CNV-LoadBalancer \
	--listeners "Protocol=HTTP,LoadBalancerPort=80,InstanceProtocol=HTTP,InstancePort=8000" \
	--availability-zones us-east-1a \
	--security-groups $AWS_SECURITY_GROUP

# We set the timeout for 120 seconds so the load balancer doesn't give up too early on the requests
aws elb modify-load-balancer-attributes \
  --load-balancer-name CNV-LoadBalancer \
  --load-balancer-attributes '{"ConnectionSettings": {"IdleTimeout": 120}}'

aws elb configure-health-check \
	--load-balancer-name CNV-LoadBalancer \
	--health-check Target=HTTP:8000/test,Interval=30,UnhealthyThreshold=2,HealthyThreshold=10,Timeout=5

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
	--max-size 5 \
	--desired-capacity 1

# Create scaling policy. This policy scales out when CPU utilization exceeds 50%.
aws autoscaling put-scaling-policy \
  --policy-name scale-out-policy \
  --auto-scaling-group-name CNV-AutoScalingGroup \
  --policy-type TargetTrackingScaling \
  --target-tracking-configuration '{
      "PredefinedMetricSpecification": {
          "PredefinedMetricType": "ASGAverageCPUUtilization"
      },
      "TargetValue": 90.0
  }'