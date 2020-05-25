# How to deploy java-bos after modularization

After modularization, java-bos is launched via shell script instead of typing command: `java -jar FullNode.jar`.

*`java -jar FullNode.jar` still works, but will be deprecated in future*.

## Download

```
git clone git@github.com:bosprotocol/java-bos.git
```

## Compile

Change to project directory and run:
```
./gradlew build
```
java-bos-1.0.0.zip will be generated in java-bos/build/distributions after compilation.

## Unzip

Unzip java-bos-1.0.0.zip
```
cd java-bos/build/distributions
unzip -o java-bos-1.0.0.zip
```
After unzip, two directories will be generated in java-bos: `bin` and `lib`, shell scripts are located in `bin`, jars are located in `lib`.

## Startup

Use the corresponding script to start java-bos according to the OS type, use `*.bat` on Windows, Linux demo is as below:
```
# default
java-bos-1.0.0/bin/FullNode

# using config file, there are some demo configs in java-bos/framework/build/resources
java-bos-1.0.0/bin/FullNode -c config.conf

# when startup with SR mode，add parameter: -w
java-bos-1.0.0/bin/FullNode -c config.conf -w
```

## JVM configuration

JVM options can also be specified, located in `bin/java-bos.vmoptions`:
```
# demo
-XX:+UseConcMarkSweepGC
-XX:+PrintGCDetails
-Xloggc:./gc.log
-XX:+PrintGCDateStamps
-XX:+CMSParallelRemarkEnabled
-XX:ReservedCodeCacheSize=256m
-XX:+CMSScavengeBeforeRemark
```