## Tested Environment
```bash
$ mvn -version                 
Apache Maven 3.8.7
Maven home: /usr/share/maven
Java version: 17.0.16, vendor: Ubuntu, runtime: /usr/lib/jvm/java-17-openjdk-amd64
Default locale: en, platform encoding: UTF-8
...
```
```bash
$ ./bin/catalina.sh version             
Using CATALINA_BASE:   /opt/tomcat/apache-tomcat-11.0.11
Using CATALINA_HOME:   /opt/tomcat/apache-tomcat-11.0.11
Using CATALINA_TMPDIR: /opt/tomcat/apache-tomcat-11.0.11/temp
Using JRE_HOME:        /usr
Using CLASSPATH:       /opt/tomcat/apache-tomcat-11.0.11/bin/bootstrap.jar:/opt/tomcat/apache-tomcat-11.0.11/bin/tomcat-juli.jar
Using CATALINA_OPTS:   
Server version: Apache Tomcat/11.0.11
...
```

## How to Use
Prepare the attach helper
```bash
$ javac Attach.java
```

Package the agent.jar
```bash
$ mvn clean package -DskipTests
```

Attach agent.jar to tomcat 
```bash
$ java --add-modules jdk.attach Attach "$(jcmd | grep -E 'catalina' | awk '{print $1}')" ~/workspace/tomcat-memshell-agent/target/agent.jar
```

## Note
run `curl localhost:8080` once before attach the agent, otherwise the ApplicationFilterChain class may not be loaded

make sure there's no other tomcat running on your testing environment :)
