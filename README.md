# ingenico-jpos-driver

JavaPOS driver for Ingenico MICR (Magnetic Ink Characters Recognition) devices. This driver contains a first draft to operate the device in printing mode. However, the printing driver is NOT a JavaPOS service and should only be used in a standard JavaSE application.

The driver will be converted to an Hydra Service in the futur.

This driver is mainly tested on an Ingenico i2200. It is also validated on Elite 200/210 by Damien Carol. It should work on other check reader that implement the Ingenico Protocol. Feed backs are welcome !

# Documentation

Javadoc can be generated from the source files.

# Build instructions & Dependencies

Clone the source. The source folder will be refered as <src>.

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
You can either import the source in your favorite IDE, configure the build path and let the IDE do the work for you, easy !

Or, you can build directly from the source. This section will be added in the futur. *Contribute if you feel so !*
