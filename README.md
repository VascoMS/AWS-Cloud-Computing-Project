## Games@Cloud

The following modules can be found in `src/`:

1. `capturetheflag` - the Capture the Flag workload
2. `fifteenpuzzle` - the 15-Puzzle Solver workload
3. `gameoflife` - the Conway's Game of Life workload
4. `webserver` - the web server exposing the functionality of the workloads
5. `javassist` - javassist tool to gather metrics
6. `storage` - storage of the gathered metrics using Amazon DynamoDB

Refer to the `README.md` files of the first three modules to get more details about each workload.

### How to run the webserver locally with instrumentation of the workloads

1. `cd src`
2. `mvn clean package`
3. `java -cp webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar -Xbootclasspath/a:javassist/target/JavassistWrapper-1.0-jar-with-dependencies.jar -javaagent:webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar=ICount:pt.ulisboa.tecnico.cnv.capturetheflag,pt.ulisboa.tecnico.cnv.fifteenpuzzle,pt.ulisboa.tecnico.cnv.gameoflife:output pt.ulisboa.tecnico.cnv.webserver.WebServer`

### How to run the webserver in AWS

1. `cd aws-scripts`
2. Create a `config.sh` with your AWS credentials following [this structure](https://gitlab.rnl.tecnico.ulisboa.pt/cnv/cnv25/-/blob/master/labs/lab-aws/scripts/config.sh?ref_type=heads)
3. `chmod +x *.sh` (This is to give the necessary permissions for the shell scripts to run)
4. `./create-image.sh` - This will create an AMI with the webserver code present in `src/`
5. `./launch-deployment.sh` - This will create the load balancer and autoscaler

When we want to terminate the deployment and leave no resources being used in AWS:

1. `./terminate-deployment.sh`
2. `./deregister-delete-image.sh`

## Description of the architecture

1. The webserver now includes a new endpoint `test/` so that the `test-vm.sh` script can verify that the webserver is autostarting as it should
2. We edited the code from the `ICount` tool in the `javassist` module to count method invocations, data accesses (reads and writes), number of basic blocks and instructions executed. The code necessary to collect and store some of the previously mentioned metrics is commented out. This is because the analysis we made after and explained in the report made us only care about the number of method invocations. 
3. Each thread stores these metrics in their own object of the class `Statistics`
4. After sending the game solution to the client, each of the games' handlers will fetch the `Statistics` object and call the `storeStatistics` method from the class `StorageUtil`.
5. The method `storeStatistics` creates a table in Amazon DynamoDB if it doesn't exist already and adds a new entry with the parameters and metrics of a request.
6. After storing the metrics, the object `Statistics` associated with the current thread is deleted by calling the method `ICount.clearThreadStatistics()` 

## Selections of the AWS system configurations

### Load Balancer

1. Configured to listen on port 80 and forward to port 8000 on the instances
2. The default timeout is increased to 120 seconds so it doesn't give up on heavier requests
3. A health check is configured to run every 30 seconds on the `test/` endpoint

### Auto Scaler

1. We create an auto scaling group named CNV-AutoScalingGroup
2. Instances are launched using the CNV-LaunchTemplate
3. We associate this ASG with the previous load balancer
4. Health checks are performed by the load balancer
5. New instances have 60 seconds after launch before health checks begin
6. The ASG never scales below 1 instance or above 5 instances and the desired capacity is 1 instance
7. We added a scaling policy to the ASG making it launch new instances when the average CPU utilization goes above 90% or terminate instances when it goes below this target.
