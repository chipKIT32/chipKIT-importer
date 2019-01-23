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

import com.microchip.crownking.opt.Version;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openide.util.Utilities;

public abstract class ArduinoConfig {
    
    
    public static final String ROOT_PLATFORM_VENDOR = "arduino";
    public static final String ROOT_PLATFORM_ARCH = "avr";
    
    private static final Logger LOGGER = Logger.getLogger(ArduinoConfig.class.getName());
    private static ArduinoConfig INSTANCE;
    
    public static synchronized ArduinoConfig getInstance() {
        if ( INSTANCE == null ) {
            if ( Utilities.isMac() ){
                INSTANCE = new MacOSXArduinoConfig();
            } else if ( Utilities.isWindows() ) {
                INSTANCE = new Win32ArduinoConfig();
            } else {
                INSTANCE = new LinuxArduinoConfig();
            }
        }
        return INSTANCE;
    }
    
    public abstract Path getSettingsPath();
    public abstract Path getDefaultSketchPath();        
    public abstract Path findArduinoBuilderPath( Path arduinoInstallPath );
    
    // TODO: Change return type to Optional
    public Path getSketchPath() {
        return findInPreferences( line -> line.startsWith("sketchbook.path") )            
            .map( Paths::get )
            .orElseGet( () -> {
                LOGGER.warning("Failed to find sketchbook path in the Arduino preferences file. Using default location.");
                return getDefaultSketchPath(); 
            });
    }
    
    // TODO: Change return type to Optional
    public Path findUserLibrariesPath() {
        return getSketchPath().resolve("libraries");
    }
        
    // TODO: Change return type to Optional
    public Path getPackagesPath() {
        return getSettingsPath().resolve("packages");
    }
    
    // TODO: Change return type to Optional
    public Path getPlatformRootPath( Platform platform ) {
        return getPackagesPath().resolve( platform.getVendor() );
    }
    
    public boolean isValidArduinoInstallPath(Path path) {
        Path p = findArduinoBuilderPath(path);
        return Files.exists(p);
    }

    public Optional<Version> findCurrentVersion() {
        return findHighestArduinoVersionLine().map( 
            line -> new Version( line.split("=")[0].substring(9, 14) ) 
        );
    }    
    
    public Optional<Path> findInstallPath() {
        return findHardwarePath().map( p -> p.getParent() );
    }
    
    public Optional<Path> findHardwarePath() {
//        return findInPreferences( line -> line.split("=")[0].trim().endsWith("hardwarepath") ).map( Paths::get );
        return findHighestArduinoVersionLine().map( line -> Paths.get( line.split("=")[1] ) );
    }
    
    private Optional<String> findHighestArduinoVersionLine() {
        List<String> hardwarePathLines = filterPreferences( line -> line.split("=")[0].trim().endsWith("hardwarepath") ).collect( Collectors.toList() );
        Version highestVersion = new Version("0.0.0");
        String highestVersionLine = null;
        
        for ( String line : hardwarePathLines ) {
            // e.g: last.ide.1.8.2.hardwarepath=...
            Version v = new Version( line.substring(9, 14) );
            if ( v.compareTo(highestVersion) > 0 ) {
                highestVersion = v;
                highestVersionLine = line;
            }
        }
        
        return Optional.ofNullable( highestVersionLine );
    }
    
    public Optional<String> findInPreferences( Predicate<String> predicate ) {
        return filterPreferences(predicate)
            .findFirst()
            .map(line -> {
                String[] tokens = line.split("=");
                return (tokens.length > 1) ? tokens[1].trim() : null;
            });
    }
    
    private Stream<String> filterPreferences( Predicate<String> predicate ) {
        Path preferencesPath = getSettingsPath().resolve("preferences.txt");
        LOGGER.info( "Reading Arduino preferences from: " + preferencesPath );
        try {
            return Files.readAllLines(preferencesPath).stream()
                .map( line -> line.trim() )
                .filter( line -> !line.startsWith("#") )
                .filter( predicate );
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to read the preferences.txt file", ex);
            return Stream.empty();
        }
    }
    
    public Optional <Path> getDefaultArduinoPlatformPath() {
        return findInPreferences( line -> line.split("=")[0].trim().endsWith("hardwarepath") )
            .map( hardwarePath -> Paths.get( hardwarePath, ROOT_PLATFORM_VENDOR, ROOT_PLATFORM_ARCH ) );
    }

    public Optional<Path> findToolsPath() {
        return findHardwarePath().map( hardwarePath -> hardwarePath.resolve("tools").resolve(ROOT_PLATFORM_ARCH) );
    }
    
    // TODO: Change return type to Optional
    public Path findToolsBuilderPath(Path arduinoInstallPath) {
        return arduinoInstallPath.resolve("tools-builder");
    }

    // TODO: Change return type to Optional
    public Path findBuiltInLibrariesPath(Path arduinoInstallPath) {
        return arduinoInstallPath.resolve("libraries");
    }
    
    public boolean isCurrentVersionValid(Version minimumValidVersion) {
        if ( minimumValidVersion == null ) throw new IllegalArgumentException("minimumValidVersion cannot be null");
        return findCurrentVersion().map( v -> v.compareTo( minimumValidVersion ) >= 0 ).get();
    }
    
}

