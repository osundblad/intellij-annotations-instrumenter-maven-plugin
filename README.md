# notnull-instrumenter-maven-plugin

This code is based on Vlad Rassokhin's intellij-annotations-instrumenter-maven-plugin. The following
significant additions/changes have been made:

* Added Java 8 to 17 support
* Added configuration: which NotNull/Nullable annotations to instrument (default is still `@org.jetbrains.annotations.NotNull` and `@org.jetbrains.annotations.Nullable`)
* Added basic unit and functional tests
* Isolated Maven plugin dependencies to allow usage without Maven
* Added implicit NotNull option
* Added flag to turn instrumentation of (e.g. for testing code coverage)

Note this project is *Teaware* so please click here [<img src="https://github.com/osundblad/intellij-annotations-instrumenter-maven-plugin/blob/master/src/docs/images/tea.png?raw=true" width="24">](https://www.buymeacoffee.com/osundblad)

## What does it do

This plugin in insert null checks on parameters and/or method return values in you byte code so that you don't have to 
do it manually.

For example:
```java
    public String append(String a, String b) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);

        String result = a + b;
        Objects.requireNonNull(result);
        return result;
        }
```
can be replaced with*:
```java
public String append(String a, String b) {
    return a + b;
}
```
*using implicit instrumentation (see below).


## Usage

Just update your `pom.xml with following: 
```xml
<dependencies>
    <dependency>
        <groupId>org.jetbrains</groupId>
        <artifactId>annotations</artifactId>
        <version>20.1.0</version>
    </dependency>
    ...
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>se.eris</groupId>
            <artifactId>notnull-instrumenter-maven-plugin</artifactId>
            <version>1.1.1</version>
            <executions>
                <execution>
                    <goals>
                        <goal>instrument</goal>
                        <goal>tests-instrument</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
        ...
    </plugins>
</build>
```

and start adding `@NotNull`/`@Nullable` annotations to your code.


## Use other and/or multiple annotations

By default, only the annotation org.jetbrains.annotations.NotNull is supported if you
want to one or more other annotations add them to configuration, for example:
```xml
<build>
    <plugins>
        <plugin>
            <groupId>se.eris</groupId>
            <artifactId>notnull-instrumenter-maven-plugin</artifactId>
            <version>1.1.1</version>
            <executions>
                <execution>
                    <id>instrument</id>
                    <goals>
                        <goal>instrument</goal>
                        <goal>tests-instrument</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <notNull>
                    <param>org.jetbrains.annotations.NotNull</param>
                    <param>javax.validation.constraints.NotNull</param>
                </notNull>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Will instrument both jetbrains and javax annotations.

Note that configuration will replace the default annotations, so org.jetbrains.annotations.NotNull will
no longer be included by default thus it must be added again if used (as in the above example).


## Implicit NotNull instrumentation

If you don't like to have `@NotNull` on 99.99% of your parameters and methods turn on the implicit instrumentation:
```xml
<build>
    <plugins>
        <plugin>
            <groupId>se.eris</groupId>
            <artifactId>notnull-instrumenter-maven-plugin</artifactId>
            <version>1.1.1</version>
            <executions>
                <execution>
                    <id>instrument</id>
                    <goals>
                        <goal>instrument</goal>
                        <goal>tests-instrument</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <implicit>true</implicit>
                <nullable>
                    <param>org.jetbrains.annotations.Nullable</param>
                </nullable>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Will instrument all parameters and return values with NotNull unless annotated with Nullable. I.e.
```java
public String implicit(String a, String b) {
    if (a.equals(b)) {
        return null;
    }
    return a + b;
}
```

will throw an IllegalArgumentException if either the a or b parameter is null, and will throw an 
IllegalStateException if a equals b (since it is not allowed to return null). To allow nulls you would 
have to annotate the parameters/return value like this:

```java
@Nullable
public String implicit(@Nullable String a, @Nullable String b) {
    if (a.equals(b)) {
        return null;
    }
    return a + b;
}
```

which would throw a NullPointerException if a is null, return null if a equals b, and otherwise append the 
Strings (or a + null if b is null).

**Note** that when using implicit you need to specify the Nullable annotations (not the NotNull).


## Turn off Instrumentation

The property `se.eris.notnull.instrument=true/false` turns on/off the instrumentation. This may seem like a 
stupid feature but it is really useful when you have multiple maven profiles and only one of them, eg Sonar/Findbugs, 
should build without instrumentation since it messes up the statistics (branch coverage, complexity, etc).

`mvn clean install -Dse.eris.notnull.instrument=false`

## Exclusion

To ease migration to implicit it is now possible to exclude certain class files from instrumentation. This 
is still a bit experimental the exclusion rules might change (depending on feedback).

There are three patterns

* __.__  matching package boundary
* __\*__  matching anything except package boundaries
* __\*\*__  matching anything (including package boundaries)
* __.\*\*__  matching any number of package levels

Example:
```xml
<configuration>
    <implicit>true</implicit>
    <excludes>
        <classes>**.wsdl.**</classes>
        <classes>com.*.*Spec</classes>
    </excludes>
</configuration>
```

Would exclude all files which have wsdl in any package part and classes with names ending 
in Spec under com.&lt;package&gt; for example com.a.UnitSpec but not com.a.b.UnitSpec or 
com.UnitSpec.

Things I am thinking about (want feedback):

* Allow full regexp by allowing quoting of the regexp chars I treat special (_._, _*_, and _$_). 
