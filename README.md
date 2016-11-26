feedback4s
==========
[![Build Status](https://travis-ci.org/rvanheest/feedback4s.png?branch=master)](https://travis-ci.org/rvanheest/feedback4s)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.rvanheest/feedback4s/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.rvanheest/feedback4s)
[![DOI](https://zenodo.org/badge/67620912.svg)](https://zenodo.org/badge/latestdoi/67620912)

An API for creating Feedback Control Systems in Scala.

A feedback system is created by composing several `Component`s. Here a `Component` is a class
which wraps a function of type `Observable[I] => Observable[O]`. Using various ways of linear
and parallel composition (such as `concat`, `combine` and Rx-like operators such as `map`,
`filter`, `take` and `scan`) complex behavior can be created. Calling one of the `feedback`
operators will connect the `Component`'s output to its input, which creates a looping behavior.


Documentation
-------------

 * [Blog about feedback control](http://rvanheest.github.io/Literature-Study-Feedback-Control/)
 * [Demo's for intended use](/demo/src/main/scala/com/github/rvanheest/feedback4s/demo)
 * [API Documentation](https://rvanheest.github.io/feedback4s/)


Binaries
--------

Binaries and dependency information for Maven, Ivy, Gradle and others can be found at [http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Ccom.github.rvanheest.feedback4s).

Example for Gradle

```groovy
compile 'com.github.rvanheest:feedback4s:x.y.z'
```

and for Maven:

```xml
<dependency>
    <groupId>com.github.rvanheest</groupId>
    <artifactId>feedback4s</artifactId>
    <version>x.y.z</version>
</dependency>
```

and for Ivy:
```xml
<dependency org="com.github.rvanheest" name="feedback4s" rev="x.y.z" />
```


Build
-----

To build:

```
    $ git clone git@github.com:rvanheest/feedback4s.git
    $ cd feedback4s/
    $ mvn clean install
```


Bugs and Feedback
-----------------

For bugs, questions and discussion please use the [GitHub Issues](https://github.com/rvanheest/feedback4s/issues).


LICENSE
-------

*feedback4s* is available under the Apache 2 License. Please see the [license](LICENSE) for more information.
