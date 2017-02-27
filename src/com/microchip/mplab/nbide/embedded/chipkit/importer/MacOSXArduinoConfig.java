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

package com.microchip.mplab.nbide.embedded.chipkit.importer;

import com.apple.eio.FileManager;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

final class MacOSXArduinoConfig extends ArduinoConfig {

    private static final Logger LOGGER = Logger.getLogger(MacOSXArduinoConfig.class.getName());
    private static final int DOCUMENTS_FOLDER_TYPE = ('d' << 24) | ('o' << 16) | ('c' << 8) | 's';
    private static final int DOMAIN_LIBRARY_FOLDER_TYPE = ('d' << 24) | ('l' << 16) | ('i' << 8) | 'b';
    

    MacOSXArduinoConfig() {
    }

    @Override
    public Path getSettingsPath() {
        try {
            String libLocation = getLibraryFolder();
            return Paths.get(libLocation, "Arduino15");
        } catch (FileNotFoundException ex) {
            LOGGER.log(Level.SEVERE, "Failed to find Mac OS user settings path", ex);
            return null;
        }
    }

    @Override
    public Path getSketchPath() {
        try {
            String docsLocation = getDocumentsFolder();
            return Paths.get(docsLocation, "Arduino");
        } catch (FileNotFoundException ex) {
            LOGGER.log(Level.SEVERE, "Failed to find Mac OS user documents path", ex);
            return null;
        }
    }

    @Override
    public Path findArduinoBuilderPath(Path arduinoInstallPath) {
        return arduinoInstallPath.resolve("arduino-builder");
    }

    private String getLibraryFolder() throws FileNotFoundException {
        return FileManager.findFolder(FileManager.kUserDomain, DOMAIN_LIBRARY_FOLDER_TYPE);
    }

    private String getDocumentsFolder() throws FileNotFoundException {
        return FileManager.findFolder(FileManager.kUserDomain, DOCUMENTS_FOLDER_TYPE);
    }

}
