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

import com.microchip.mplab.nbide.embedded.api.LanguageTool;
import com.microchip.mplab.nbide.embedded.arduino.utils.DeletingFileVisitor;
import static com.microchip.mplab.nbide.embedded.arduino.importer.NativeProcessRunner.NO_ERROR_CODE;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import static java.nio.file.FileVisitResult.CONTINUE;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

public class ArduinoBuilderRunner {

    
    private static final Logger LOGGER = Logger.getLogger(ArduinoBuilderRunner.class.getName());
    
    private List<Path> mainLibraryPaths = new ArrayList<>();
    private List<Path> auxLibraryPaths = new ArrayList<>();
    private Path preprocessDirPath;
    
    private final GCCToolFinder toolFinder;
    private final ArduinoConfig arduinoConfig;
    private final Path arduinoInstallPath;
    private final NativeProcessRunner nativeProcessRunner;
    

    public ArduinoBuilderRunner( GCCToolFinder toolFinder, ArduinoConfig arduinoConfig, Path arduinoInstallPath, Consumer<String> nativeProcessMessageConsumer ) {
        this.toolFinder = toolFinder;
        this.arduinoConfig = arduinoConfig;
        this.arduinoInstallPath = arduinoInstallPath;
        this.nativeProcessRunner = new NativeProcessRunner(nativeProcessMessageConsumer);
    }

    public Path getArduinoInstallPath() {
        return arduinoInstallPath;
    }

    public ArduinoConfig getArduinoPathResolver() {
        return arduinoConfig;
    }

    public GCCToolFinder getToolFinder() {
        return toolFinder;
    }

    public void preprocess(BoardConfiguration boardConfiguration, Path inoFilePath) {
        Path tempDirPath = null;
        try {
            tempDirPath = Files.createTempDirectory("preprocess");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        preprocess(boardConfiguration, inoFilePath, tempDirPath);
    }
    
    public void preprocess(BoardConfiguration boardConfiguration, Path inoFilePath, Path preprocessDirPath) {
        try {
            this.preprocessDirPath = preprocessDirPath;
            if ( !Files.exists(preprocessDirPath) ) {
                Files.createDirectories(preprocessDirPath);
            }
            
            // Run Arduino-Builder
            int errorCode = runArduinoBuilder(boardConfiguration, inoFilePath);

            if (errorCode == NO_ERROR_CODE) {
                // Find library paths
                mainLibraryPaths = findMainLibraryPaths();
                // Find library dependencies
                auxLibraryPaths = findAuxLibraryPaths(boardConfiguration, toolFinder, mainLibraryPaths);
            } else {
                String message = "Failed to preprocess file \"" + inoFilePath + "\". Check logs for details.";
                LOGGER.log( Level.SEVERE, message );
                throw new RuntimeException(message);
            }

        } catch (IOException | InterruptedException | RuntimeException | ScriptException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<Path> getMainLibraryPaths() {
        return new ArrayList<>(mainLibraryPaths);
    }
    
    public List<Path> getAuxLibraryPaths() {
        return new ArrayList<>(auxLibraryPaths);
    }
    
    public List<Path> getAllLibraryPaths() {
        List <Path> ret = new ArrayList<>(mainLibraryPaths);
        ret.addAll(auxLibraryPaths);
        return ret;
    }

    public String getCommand() {
        return nativeProcessRunner.getNativeProcessCommandString();
    }

    public Path getPreprocessDirPath() {
        return preprocessDirPath;
    }

    public Path getPreprocessedSketchDirPath() {
        return preprocessDirPath.resolve("sketch");
    }

    public void cleanup() throws IOException {
        Files.walkFileTree(preprocessDirPath, new DeletingFileVisitor());
        preprocessDirPath = null;
        mainLibraryPaths = null;
    }

    // TODO: Improve handling of non-standard scenarios (missing directories etc)
    private int runArduinoBuilder( BoardConfiguration boardConfiguration, Path inoFilePath ) throws IOException, InterruptedException {
        final Path packagesPath = arduinoConfig.getPackagesPath();
        final Path hardwarePath = arduinoConfig.findHardwarePath().get();
        final boolean packagesDirExists = Files.exists(packagesPath);
        final String fqbn = boardConfiguration.getFqbn();
        final Path librariesDirPath = findSketchbookLibrariesDirectoryPath(arduinoConfig, inoFilePath);        
        
        // Run preprocess command        
        if ( packagesDirExists ) {
            return nativeProcessRunner.runNativeProcess(
                preprocessDirPath,
                arduinoConfig.findArduinoBuilderPath(arduinoInstallPath).toString(),
                "-preprocess",
                "-logger=human",
                "-hardware", hardwarePath.toString(),
                "-hardware", packagesPath.toString(),
                "-tools", arduinoConfig.findToolsBuilderPath(arduinoInstallPath).toString(),
                "-tools", arduinoConfig.findToolsPath().map( Path::toString ).orElse(""),
                "-tools", packagesPath.toString(),
                "-built-in-libraries", arduinoConfig.findBuiltInLibrariesPath(arduinoInstallPath).toString(),
                "-libraries", librariesDirPath.toString(), 
                "-fqbn=" + fqbn,
                "-build-path", ".",
                "-verbose",
                inoFilePath.toAbsolutePath().toString()
            );
        } else {
            return nativeProcessRunner.runNativeProcess(
                preprocessDirPath,
                arduinoConfig.findArduinoBuilderPath(arduinoInstallPath).toString(),
                "-preprocess",
                "-logger=human",
                "-hardware", hardwarePath.toString(),
                "-tools", arduinoConfig.findToolsBuilderPath(arduinoInstallPath).toString(),
                "-tools", arduinoConfig.findToolsPath().map( Path::toString ).orElse(""),
                "-built-in-libraries", arduinoConfig.findBuiltInLibrariesPath(arduinoInstallPath).toString(),
                "-libraries", librariesDirPath.toString(), 
                "-fqbn=" + fqbn,
                "-build-path", ".",
                "-verbose",
                inoFilePath.toAbsolutePath().toString()
            );
        }
    }
    
    private Path findSketchbookLibrariesDirectoryPath( ArduinoConfig arduinoConfig, Path inoFilePath ) {
        Path sketchbookDirPath = inoFilePath.getParent().getParent();
        Path librariesDirPath = sketchbookDirPath.resolve("libraries");
        if ( Files.exists(librariesDirPath) ) {
            return librariesDirPath;
        } else {
            return arduinoConfig.findUserLibrariesPath();
        }
    }

    private List<Path> findMainLibraryPaths() throws ScriptException, FileNotFoundException {
        LOGGER.info("Looking for main library paths");
        
        Path includesCachePath = Paths.get(preprocessDirPath.toAbsolutePath().toString(), "includes.cache");
        ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByExtension("js");
        ScriptObjectMirror mirror = (ScriptObjectMirror) scriptEngine.eval(new FileReader(includesCachePath.toString()));
        List<Path> libraryPaths = new ArrayList<>();
        mirror.entrySet().forEach(e -> {
            if (e.getValue() instanceof ScriptObjectMirror) {
                ScriptObjectMirror m = (ScriptObjectMirror) e.getValue();
                Object sourceFile = m.get("Sourcefile");
                if (sourceFile != null && !sourceFile.toString().trim().isEmpty()) {
                    String entry = m.get("Includepath").toString();
                    if ( !entry.trim().isEmpty() ) {
                        if ( entry.endsWith( File.separator+"src") ) {
                            entry = entry.substring(0, entry.length()-4);
                        }
                        LOGGER.log( Level.INFO, "Found library path: {0}", entry );
                        libraryPaths.add( Paths.get(entry) );
                    }
                }
            }
        });
        
        if ( libraryPaths.isEmpty() ) {
            LOGGER.info("No main library dependencies found");
        }
        
        return libraryPaths;
    }

    private List <Path> findAuxLibraryPaths(BoardConfiguration boardConfiguration, GCCToolFinder toolFinder, List<Path> mainLibraries) throws IOException {
        LOGGER.info("Looking for additional library paths");
        
        // TODO: Consider expanding the list of valid library source file extensions
        final PathMatcher librarySourceMatcher = FileSystems.getDefault().getPathMatcher("glob:*.{c,cpp}");
        final Path gccPath = toolFinder.findTool( LanguageTool.CCCompiler );
        final List <Path> coreDirPaths = boardConfiguration.getCoreDirPaths();
        
        final List <Path> allLibraries = new ArrayList<>(mainLibraries);
        final List <Path> ret = new ArrayList<>();

        for (int i = 0; i < allLibraries.size(); i++) {
            Path libDir = allLibraries.get(i);
            final Path librariesDir = libDir.getParent();
            Files.walkFileTree(libDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (Files.isDirectory(file)) {
                        return CONTINUE;
                    }
                    if (!librarySourceMatcher.matches(file.getFileName())) {
                        return CONTINUE;
                    }

                    try {
                        new NativeProcessRunner( (String m) -> {
                            if ( m.startsWith("--") ) {
                                LOGGER.info(m);
                            } else if (m.startsWith(" ")) {
                                String path = m.trim();
                                if ( path.endsWith("\\") ) {
                                    path = path.substring(0, path.length()-2).trim();
                                }
                                Path dependencyFilePath = Paths.get( path );
                                // TODO: If the "path" string does not represent a path then it probably contains error information. Find a way to handle it.
                                if ( Files.exists( dependencyFilePath ) ) {
                                    LOGGER.log(Level.INFO, "Dependency path: {0}", dependencyFilePath);
                                    if ( dependencyFilePath.startsWith( librariesDir ) ) {
                                        Path relativeDependencyPath = librariesDir.relativize(dependencyFilePath.normalize());
                                        String libraryName = relativeDependencyPath.getName(0).toString();
                                        Path libraryPath = librariesDir.resolve(libraryName);
                                        if ( !allLibraries.contains(libraryPath) ) {
                                            LOGGER.log(Level.INFO, "Found library path: {0}", libraryPath);
                                            allLibraries.add(libraryPath);
                                            ret.add(libraryPath);
                                        }
                                    } else {
                                        LOGGER.log(Level.INFO, "Ignoring dependency file path:{0}", dependencyFilePath);
                                    }
                                }
                            }
                        }).runNativeProcess( createDependencyResolutionCommand( gccPath, coreDirPaths, mainLibraries, file ) );
                    } catch ( IOException | InterruptedException ex ) {
                        LOGGER.log( Level.SEVERE, "Failed to resolve additional dependencies for " + file.toAbsolutePath().toString(), ex );
                    }

                    return CONTINUE;
                }
            });
        }
        
        if ( ret.isEmpty() ) {
            LOGGER.info("No additional library dependencies found");
        }
        
        return ret;
    }
    
    private String[] createDependencyResolutionCommand( Path gccPath, List<Path> coreDirPaths, List<Path> libraryPaths, Path file ) {
        List <String> commandElements = new ArrayList<>();
        commandElements.add( gccPath.toString() );
        for ( Path coreDirPath : coreDirPaths ) {
            commandElements.add( "-I" );
            commandElements.add( coreDirPath.toString() );
        }
        for ( Path libPath : libraryPaths ) {
            commandElements.add( "-I" );
            commandElements.add( libPath.toAbsolutePath().toString() );
            Path utilityPath = libPath.resolve("utility");
            if ( Files.exists(utilityPath) ) {
                commandElements.add( "-I" );
                commandElements.add( utilityPath.toAbsolutePath().toString() );
            }
        }        
        commandElements.add( "-MM" );
        commandElements.add( file.toAbsolutePath().toString() );
        return commandElements.toArray( new String[commandElements.size()] );
    }

}
