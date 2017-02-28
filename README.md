# chipKIT-importer
chipKIT importer plugin for MPLAB X


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
