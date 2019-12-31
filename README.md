# RxStacktrace
## Status: Experimental

Weaves minimal async stack traces into RxJava2 chains using javassist transformers.
Be aware: Only the source location of the call-site is tracked, not a complete stracktrace is stored.
This has a lower overhead compared to approaches throwing exceptions along the chain.

Currently only considers calls with return type Flowable, Maybe, Observable, Single, Completable.

Add RxStacktrace dependency and configure transformer profile.
Replace YOUR_COMMA_SEPARATED_PACKAGE_PREFIXES with your packages.

```xml
<repositories>
    ...
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/trho/RxStacktrace</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
      <groupId>io.github.trho.rxstacktrace</groupId>
      <artifactId>RxStacktrace</artifactId>
      <version>1.0.4</version>
    </dependency>
</dependencies>
    
<profiles>    
 <profile>
    <id>asynctrace</id>
    <activation>
        <activeByDefault>true</activeByDefault>
    </activation>
    <build>
        <finalName>${project.artifactId}</finalName>
        <plugins>
            <plugin>
                <groupId>de.icongmbh.oss.maven.plugins</groupId>
                <artifactId>javassist-maven-plugin</artifactId>
                <version>2.0.0</version>
                <configuration>
                    <includeTestClasses>false</includeTestClasses>
                    <transformerClasses>
                        <transformerClass>
                            <className>io.github.trho.rxstacktrace.RxAsyncStacktraceTransformer</className>
                            <properties>
                                <property>
                                    <name>prefixes.included.packages</name>
                                    <value>YOUR_COMMA_SEPARATED_PACKAGE_PREFIXES</value>
                                </property>
                            </properties>
                        </transformerClass>
                    </transformerClasses>
                </configuration>
                <executions>
                    <execution>
                        <phase>process-classes</phase>
                        <goals>
                            <goal>javassist</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</profile>
```
