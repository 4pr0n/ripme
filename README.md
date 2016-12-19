RipMe [![Build Status](https://travis-ci.org/arkban/ripme.svg?branch=master)](https://travis-ci.org/arkban/ripme)
=====

Album ripper for various websites. Runs on your computer. Requires Java 1.8

![Screenshot](http://i.imgur.com/kWzhsIu.png)

[Download v1.x](http://rarchives.com/ripme.jar) (ripme.jar)
--------------------------
For information about running the `.jar` file, see [the How To Run wiki](https://github.com/4pr0n/ripme/wiki/How-To-Run-RipMe)

Features
---------------

* Quickly downloads all images in an online album (see supported sites below)
* Easily re-rip albums to fetch new content

Supported sites:
* imgur
* twitter
* tumblr
* instagram
* flickr
* photobucket
* reddit
* gonewild
* motherless
* imagefap
* imagearn
* seenive
* vinebox
* 8muses
* deviantart
* xhamster
* (more)

[Full updated list](https://github.com/4pr0n/ripme/issues/8)

Request more sites by adding a comment to [this Github issue](https://github.com/4pr0n/ripme/issues/8) or by following the wiki guide [How To Create A Ripper for HTML Websites](https://github.com/4pr0n/ripme/wiki/How-To-Create-A-Ripper-for-HTML-websites)

Compiling & Building
--------------------

The project uses [Maven](http://maven.apache.org/). To build the .jar file using Maven, navigate to the root project directory and run:

```bash
mvn clean compile assembly:single
```

This will create a fat JAR that includes all dependencies in the JAR.

