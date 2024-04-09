# ifs-modeller-svc Project

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding by,

Open a cmd and run command 
```aidl
```
A batch file with script has been included

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.
>



###EXECUTE BELOW ONLY IF YOU NEED THE CASSANDRA AND KAFKA IN LOCAL ENV, NOT REQUIRED IN CURRENT DEV SCENARIO

## Running Kafka service on Docker for App

- In the docker-compose folder run <b>docker compose up</b> which will launch the kafka and zookeeper server
- Please make sure you don't have any other kafka servers occupying the port <b>9092</b>
- If so, please edit the <b>docker-compose.yml</b> file and change the port value


## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/ifs-modeller-svc-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

# Dockerizing Ifs-Modeller-Svc
 
 step 1: Build the docker image.
   ---
      docker build -f src/main/docker/Dockerfile.New.jvm -t quarkus/ifs-modeller-svc-jvm --build-arg PROFILE=colo .
   ---
   step 2: Run the docker image.
   ----
     docker run -i --rm -p 31501:31501 quarkus/ifs-modeller-svc-jvm
     ---
     The above command starts the modelller service image inside the container and exposes port 51501 inside container to port 51501 outside the container.
     ----

   step 3: Check the image created 
   ---
    command used: docker images
   ---
 step 4:Access the route on server using http://localhost:51501

