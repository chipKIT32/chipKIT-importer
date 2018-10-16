/*
 * Copyright (c) 2017 Microchip Technology Inc. and its subsidiaries (Microchip). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.microchip.mplab.nbide.embedded.arduino.importer;


import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.ShlObj;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final class Win32ArduinoConfig extends ArduinoConfig {

    Win32ArduinoConfig() {}
    
    @Override
    public Path getSettingsPath() {        
        Path path = Paths.get( Shell32Util.getFolderPath(ShlObj.CSIDL_MYDOCUMENTS) ).resolve("ArduinoData");
        if (Files.exists(path)) {
            return path;
        }
            
        path = Paths.get( Shell32Util.getFolderPath(ShlObj.CSIDL_LOCAL_APPDATA) ).resolve("Arduino15");
        if ( Files.exists(path) ) {
            return path;
        }
        
        path = Paths.get(Shell32Util.getFolderPath(ShlObj.CSIDL_APPDATA) ).resolve("Arduino15");
        if ( Files.exists(path) ) {
            return path;
        }
        
        return null;
    }

    @Override
    public Path getDefaultSketchPath() {
        return Paths.get( Shell32Util.getFolderPath(ShlObj.CSIDL_MYDOCUMENTS) ).resolve("Arduino");
    }

    @Override
    public Path findArduinoBuilderPath(Path arduinoInstallPath) {
        return arduinoInstallPath.resolve("arduino-builder.exe");
    }

}
