# Entropy

This repository contains a fork of the Entropy project. As we based many of our experiments and simulations on a modified/legacy version of entropy (2.1.0), we decided to keep a save of this project here.

## Requirements
* java
* maven

## Installation

Clone the project, and then run the following command:

```
$ mvn package -DskipTests
```

It will result in the creation of a jar located here:


```
jonathan@artoo ~/D/w/entropy (master)> ls -lh target
total 29192
drwxr-xr-x  2 jonathan  staff    68B Dec 15 17:25 archive-tmp
drwxr-xr-x  8 jonathan  staff   272B Dec 15 17:17 classes
-rw-r--r--  1 jonathan  staff    13M Dec 15 17:25 entropy-2.1.0-SNAPSHOT-distribution.tar.gz
-rw-r--r--  1 jonathan  staff   909K Dec 15 17:25 entropy-2.1.0-SNAPSHOT.jar
drwxr-xr-x  4 jonathan  staff   136B Dec 15 17:06 generated-sources
drwxr-xr-x  3 jonathan  staff   102B Dec 15 17:17 generated-test-sources
drwxr-xr-x  3 jonathan  staff   102B Dec 15 17:25 maven-archiver
drwxr-xr-x  5 jonathan  staff   170B Dec 15 17:17 test-classes
```

## Modify the project from IntelliJ

### Open the project with IntelliJ

Follow the steps that are described by:

[http://wiki.jetbrains.net/intellij/Creating_and_importing_Maven_projects](http://wiki.jetbrains.net/intellij/Creating_and_importing_Maven_projects)

It should run fine!

### /!\ In case there are a lot of mistakes:

Maybe the maven dependencies have not been correctly managed by IntelliJ: check the following:

1. Open the **"Maven projects"** tab in the right part of intelliJ.
2. Right click on **"entropy"** .
3. Click on **"Reimport"**.

![image](http://dropbox.jonathanpastor.fr/intellij_maven_reimport_steps.png)