# ingenico-jpos-driver

JavaPOS driver for Ingenico MICR (Magnetic Ink Characters Recognition) devices. This driver contains a first draft to operate the device in printing mode. However, the printing driver is NOT a JavaPOS service and should only be used in a standard JavaSE application.

The driver will be converted to an Hydra Service in the futur.

This driver is mainly tested on an Ingenico i2200. It is also validated on Elite 200/210 by Damien Carol. It should work on other check reader that implement the Ingenico Protocol. Feed backs are welcome !

# Documentation

Javadoc can be generated from the source files.

# Build instructions & Dependencies

Clone the repo. The source folder will be refered as `<src>`.

## Dependencies
In order to build the driver, you need to resolve some dependencies.

### JavaPOS
Of course, you need the JavaPOS libraries. Add these 2 jar to your build path :

* [jpos.jar](http://www.java2s.com/Code/Jar/j/Downloadjposjar.htm)
* [jposcontrolls110.jar](http://www.java2s.com/Code/Jar/j/Downloadjposcontrols110jar.htm)

You can also use only the jpos110.jar bundled with Postest. If you don't know what is Postest, consider reading the section about it.

### RXTX
RXTX is the driver used for serial communications with the device. To build the project, you don't have to install the native libraries.

* [rxtx-2.1-7-bins-r2.zip](http://rxtx.qbang.org/wiki/index.php/Download)

Download, extract and add RXTXcomm.jar to your build path.
*If you want to run the driver, you also need the native libraries, keep the extracted folder !*

## Build
You can either import the source in your favorite IDE, configure the build path and let the IDE do the work for you, easy ! Ask your IDE to generate a jar archive to be ready to run the driver. You don't have to incluse dependency libraries in the jar, but you can if you want. Run instructions will assume external libraries are NOT included in the generated archive.
Or, you can build directly from the source. This section will be added in the futur. *Contribute if you feel so !*

# Run instructions & Dependencies

In order to run the driver, as no precompiled jar is provided *(futur feature)* you have to compile your own jar.

If you haven't, follow the build instruction prior to the following run instructions.

## Dependencies
You should already have JPos and RXTX's JARs. You also needs them to run the driver. Add them to your classpath !

### RXTX Native libraries
RXTX needs to load native libraries in order to operate the serial port. 

#### x86 environments
If you run a x86 environment, you can use the native libraries you should have already downloaded with the RXTX's jar. Just pick the ones according to your OS. Be sure to 

#### x64 environments
If you are running a x64 environment, you can't use the official RXTX libraries 'cause they are only compiled for x86 environments !

But ... ! As RXTX original project is open-source, someone re-compiled the libraries for x64 environments. Unfortunatly, it has not been recompiled for Mac OSX or Solaris. If you run OSX or Solaris, you have to use the x86 libraries.

* [RXTX x64](http://www.cloudhopper.com/opensource/rxtx/) 

Download the correct archive according to your environment.

**Cloudhopper RXTX fork is bundled with a modified JAR, you don't HAVE to use this one as it is almost 100% compatible with the original RXTX API. You can still use the original JAR you downloaded when you where building the project.** 

#### All environments

To install the native libraries, you have 2 options : 

* Follow the instructions from the INSTALL file located at the root of the RXTX archive

OR

* Add the 2 natives libraries (.so or .dll) to your Java library path. 

### Xerces
Xerces is a XML parser written in Java. It's part of the Apache project. It is used by JPos to parse JPos devices configuration files.

* Official website is currently down. Download Postest, it is bundled with a xerces.jar. Instrutions below.

Download the jar and add it to the classpath.

*To continue ...*
