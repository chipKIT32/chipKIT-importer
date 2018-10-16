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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

final class LinuxArduinoConfig extends ArduinoConfig {

    private static final Logger LOGGER = Logger.getLogger(LinuxArduinoConfig.class.getName());
    
    LinuxArduinoConfig() {};
    
    
    // TODO: Fix error handling
    @Override
    public Path getSettingsPath() {
        Path userHomePath = Paths.get( System.getProperty("user.home") );
        Path arduinoSettingsPath = userHomePath.resolve(".arduino15");
        if ( Files.exists(arduinoSettingsPath) ) {
            return arduinoSettingsPath;
        } else {
            try {
                return findInArduinoSnapDirectory(userHomePath, ".arduino15").orElse(null);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Failed to find Arduino's user settings directory", ex);
                return null;
            }
        }
    }

    @Override
    public Path getDefaultSketchPath() {
        Path userHomePath = Paths.get( System.getProperty("user.home") );
        Path userArduinoPath = userHomePath.resolve("Arduino");
        if ( Files.exists(userArduinoPath) ) {
            return userArduinoPath;
        } else {
            try {
                return findInArduinoSnapDirectory(userHomePath, "Arduino").orElse(null);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Failed to find Arduino's user sketch directory", ex);
                return null;
            }
        }
    }

    @Override
    public Path findArduinoBuilderPath(Path arduinoInstallPath) {
        return arduinoInstallPath.resolve("arduino-builder");
    }
    
    private Optional<Path> findInArduinoSnapDirectory( Path userHomePath, String fileName ) throws IOException {
        Path snapPath = userHomePath.resolve("snap");
        if ( Files.exists(snapPath) ) {
            return Files
                .list(snapPath)
                .filter( p -> p.getFileName().toString().contains("arduino") )
                .map( p -> p.resolve("current") )
                .filter( p -> Files.exists(p) )
                .findFirst()
                .map( p -> p.resolve( fileName ) )
                .filter( p -> Files.exists(p) );
        } else {
            return Optional.empty();
        }
    }
    
}
