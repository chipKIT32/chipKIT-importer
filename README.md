# chipKIT Import Plugin for MPLAB X IDE
This plugin contains an import wizard that can be launched from File->Import->Import chipKIT Project. 
The wizard will allow you to select a sketch file (*.ino) that was created in Arduino IDE using chipKIT-core in the Arduino Boards Manager. 
Next, it will convert the sketch into an MPLAB X Makefile project. 
With this project you will be able to build and debug the chipKIT sketch in MPLAB X.

A complete set of Help documentation is included. 
Note that the wizard can import a project in two different ways:  
1) Copying all source code and libraries, in order to create a stand-alone MPLAB X project which is logically separate from the original (default mode) or  
2) Retaining links to external source code and libraries that exist in the chipKIT/Arduino environment,

The chipKIT Import Plugin requires the following software to be installed:  
- MPLAB X v3.60 or later
- MPLAB XC32 C/C++ compiler v1.43 or later
- Arduino IDE v1.81 or later
- chipKIT-core v1.40 or later


### Changelog:

Version 1.0.0
- First release! (no changes from 0.9.14)

Version 0.9.14
- Added user help files

Version 0.9.13
- Added special import logic for Lenny, DP32, Pi, and Fubarino Mini that uses custom .ld scripts and creates special "debug" configurations
- Added some instructions for the user that appear on the last page of the wizard
- Removed support for "clicker2", "cui32", "CUI32stem" and "usbono_pic32" boards

Version 0.9.12
- Added logic that saves all open files prior to rebuilding the Core library
- Fixed a typo in BootloaderPathProvider that caused a NullPointerException for Clicker 2 Board
- Fixed [issue #11](https://github.com/chipKIT32/chipKIT-importer/issues/11)

Version 0.9.11
- Set minimum XC32 compiler version to 1.43

Version 0.9.10
- Added plugin description
- Added tooltips to the fields on wizard's first page

Version 0.9.9
- Moved the Program/Download toolbar controls further right
- Redirected output from Arduino Builder to a preprocess.log file
- Removed the JNA and JNA Platform jars from the plugin and added dependencies on the JNA module from MPLAB X IDE v.3.55

Version 0.9.8
- Modified the source project file chooser to show .ino files. The current implementation allows the user to select either a project directory or an .ino file
- Fixed an error with the no-copy import that caused the preprocessing operation to be called in a wrong location

Version 0.9.7
- Included the ".x" estension into the source file name pattern
- Added monitoring of used serial ports to detect when they become unavailable

Version 0.9.6
- Added logic that turns on DTR on a serial port in the Serial Monitor
