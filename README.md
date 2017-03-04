# chipKIT Import Plugin for MPLAB X IDE
This plugin contains an import wizard that can be launched from File->Import->Import chipKIT Project. 
The wizard will allow you to select a sketch file (*.ino) that was created in Arduino IDE using chipKIT-core in the Arduino Boards Manager. 
Next, it will conver the sketch into an MPLAB X Makefile project. 
With this project you will be able to build and debug the chipKIT sketch in MPLAB X.

A complete set of Help documentation is included. 
Note that the wizard can import a project in two different ways:  
1) Retaining links to external source code and libraries that exist in the chipKIT/Arduino environment, or  
2) Copying all source code and libraries, in order to create a stand-alone MPLAB X project which is logically separate from the original.

The chipKIT Import Plugin requires the following software to be installed:  
- MPLAB X v3.55 or later
- MPLAB XC32 C/C++ compiler v1.43 or later
- Arduino IDE v1.81 or later
- chipKIT-core v1.40 or later


### Changelog:

Version 0.9.9
- Moved the Program/Download toolbar controls further right
- Redirected output from Arduino Builder to a preprocess.log file

Version 0.9.8
- Modified the source project file chooser to show .ino files. The current implementation allows the user to select either a project directory or an .ino file
- Fixed an error with the no-copy import that caused the preprocessing operation to be called in a wrong location

Version 0.9.7
- Included the ".x" estension into the source file name pattern
- Added monitoring of used serial ports to detect when they become unavailable

Version 0.9.6
- Added logic that turns on DTR on a serial port in the Serial Monitor
