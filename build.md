**Setup**

    chmod 700 .gnupg
    export GPG_TTY=$(tty)

**Release to oss.sonatype.org**

    mvn clean deploy -P release

[se.eris@oss.sonatype.org](https://oss.sonatype.org/#nexus-search;quick~se.eris)


**Support new Java version**

Update:
* pom.xml: asm version
* TestCompilerOptions.java
* TestCompilerOptionsTest.java
* TestSupportedJavaVersions.java
