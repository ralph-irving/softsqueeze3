SoftSqueeze Copyright (c) 2004 Richard Titmuss

Softsqueeze is an music player for the SqueezeCenter that emulates the Squeezebox. The 
slim server distribution now includes a binary distribution of Softsqueeze linked 
from the slim server help pages. The Softsqueeze source code is maintained using the
slimdevices cvs server.


To build Softsqueeze you need Java development environment JDK 1.5 (available from
http://java.sun.com) and the Apache Ant (available from http://ant.apache.org/).
Although Softsqueeze needs JDK 1.5 to build, it is compatible with both Java 1.3
(when running as an applet) and Java 1.4 (when running as an applet, application or
using Java Web Start).


To build Softsqueeze on a Unix system use the following commands:

  export ANT_HOME=<where you have installed apache ant>
  export JAVA_HOME=<where you have installed JDK 1.5>
  cd <slimcvs>/softsqueeze3
  ant

Softsqueeze will now build and install itself in the slimserver html directory. Other 
useful ant targets include:

  clean - clean the build directories
  jar - compile the Softsqueeze.jar file
  dist - prepare the Softsqueeze distribution
  zip - create the Softsqueeze.zip file
  mac - create a mac osx dmg file
  media - create all os packages.  requires install4j 3.2.x

