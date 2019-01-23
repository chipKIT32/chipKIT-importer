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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class AbstractMakeAssistant {

    
    private static final Logger LOGGER = Logger.getLogger(AbstractMakeAssistant.class.getName());

    private List<String> compilationCommands;
    private List <String> makefileContents;
    private List <String> objectFilenames;
    
    
    
    public List<String> getCompilationCommands() {
        return new ArrayList<>(compilationCommands);
    }

    public List<String> getMakefileContents() {
        return makefileContents;
    }

    public List<String> getObjectFilenames() {
        return objectFilenames;
    }

    public Path getMakefilePath() {
        return getBuildDirPath().resolve( getMakefileName() );
    }    
    
    public Path getToolchainPath() {
        return getToolFinder().findTool( LanguageTool.CCCompiler ).getParent().getParent();
    }

    public abstract Path getBuildDirPath();

    public abstract BoardConfiguration getBoardConfiguration();

    public abstract String getMakefileName();

    public abstract String getTargetName();

    public abstract GCCToolFinder getToolFinder();

    public void cleanup() {
        try {
            Files.walkFileTree(getBuildDirPath(), new DeletingFileVisitor());
        } catch (IOException ex) {
            LOGGER.log( Level.WARNING, "Exception caught while removing the build directory", ex );
        }
    }

    protected void build() throws IOException, InterruptedException {
        build(null, null);
    }
    
    protected void build( Consumer<String> messageConsumer, Consumer<String> errorConsumer ) throws IOException, InterruptedException {        
        checkPrerequisites();        
        generateMakefile();
        writeMakefile();
        invokeMakeTool( messageConsumer, errorConsumer );
    }
    
    protected void checkPrerequisites() {
        if ( getBuildDirPath() == null ) throw new IllegalStateException("Build Directory Path cannot be null!");
        if ( getMakefileName() == null ) throw new IllegalStateException("Makefile Name cannot be null!");
        if ( getToolFinder() == null ) throw new IllegalStateException("Tool Finder cannot be null!");
        if ( getBoardConfiguration()== null ) throw new IllegalStateException("Board Configuration cannot be null!");
        if ( getTargetName() == null ) throw new IllegalStateException("Target Name cannot be null!");        
    }
    
    protected void generateMakefile() throws IOException {
        BoardConfiguration boardConfiguration = getBoardConfiguration();
        
        makefileContents = new ArrayList<>();
        objectFilenames = new ArrayList<>();
        makefileContents.add( getTargetName() + ":" );
        
        // Add variant and core source file paths:
        List <Path> allSourceFiles = getSourceFilePaths(boardConfiguration);
        
        Map <String,String> runtimeData = new HashMap<>();
        runtimeData.put( getToolsPathKey(), getToolchainPath().toString() );
        
        // Generete compilation commands:
        allSourceFiles.forEach(sourceFilePath -> {                
            String sourceFileName = sourceFilePath.getFileName().toString();
            String targetFileName = sourceFileName + ".o";
            objectFilenames.add( targetFileName );
            StringBuilder command = new StringBuilder("\t");
            
            runtimeData.put("source_file", mapSourceFilePath(sourceFilePath));
            runtimeData.put("object_file", targetFileName);
            runtimeData.put("includes", buildIncludesSection(boardConfiguration) );
            
            if (sourceFileName.endsWith(".S")) {
                command.append( boardConfiguration.getValue("recipe.S.o.pattern", runtimeData).get() );
            } else if (sourceFileName.endsWith(".c")) {
                command.append( boardConfiguration.getValue("recipe.c.o.pattern", runtimeData).get() );
            } else if (sourceFileName.endsWith(".cpp")) {
                command.append( boardConfiguration.getValue("recipe.cpp.o.pattern", runtimeData).get() );
            }
            makefileContents.add( command.toString() );
        });                
    }
    
    protected String mapSourceFilePath( Path sourceFilePath ) {
        return sourceFilePath.toString();
    }
    
    protected String getToolsPathKey() {
        Platform platform = getBoardConfiguration().getPlatform();
        if ( platform.isSAMD()) {
            return "runtime.tools.arm-none-eabi-gcc.path";
        } else if ( platform.isPIC32() ) {
            return "runtime.tools.pic32-tools.path";
        } else {
            return "runtime.tools.avr-gcc.path";
        }
    }
    
    protected String buildIncludesSection( BoardConfiguration boardConfiguration ) {
        StringBuilder ret = new StringBuilder();
        Path variantPath = boardConfiguration.getVariantPath();
        Path corePath = boardConfiguration.getCoreDirectoryPath();
        if (variantPath != null && !variantPath.equals(corePath)) {
            ret.append(" \"-I").append(variantPath).append("\"");
        }
        ret.append(" \"-I").append(corePath).append("\"");
        return ret.toString();
    }
    
    protected void writeMakefile() throws IOException {
        Files.write( getMakefilePath(), getMakefileContents() );
    }
    
    protected void invokeMakeTool( Consumer<String> messageConsumer, Consumer<String> errorConsumer ) throws IOException, InterruptedException {
        Path makeToolPath = getToolFinder().findTool( LanguageTool.MakeTool );
        NativeProcessRunner nativeProcessRunner = new NativeProcessRunner(messageConsumer, errorConsumer);
        int result = nativeProcessRunner.runNativeProcess( getBuildDirPath(), makeToolPath.toString(), "V=1", "-f", getMakefilePath().getFileName().toString() );
        if ( result != 0 ) throw new NativeProcessFailureException( "Compilation failed!" );
    }
        
    protected List<Path> getSourceFilePaths( BoardConfiguration boardConfiguration ) throws IOException {
        return boardConfiguration.getCoreFilePaths();
    }

    protected List<String> parseCompilerMacros(String macros) {
        return Arrays.asList(macros.split(";")).stream().map(m -> "-D" + m).collect(Collectors.toList());
    }

}
