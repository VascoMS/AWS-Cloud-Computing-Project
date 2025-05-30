## Games@Cloud

The following packages can be found in `src/`:

1. `capturetheflag` - the Capture the Flag workload
2. `fifteenpuzzle` - the 15-Puzzle Solver workload
3. `gameoflife` - the Conway's Game of Life workload
4. `webserver` - the web server exposing the functionality of the workloads
5. `javassist` - javassist tool to gather metrics
6. `storage` - storage of the gathered metrics using Amazon DynamoDB

Refer to the `README.md` files of the first three packages to get more details about each workload.

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