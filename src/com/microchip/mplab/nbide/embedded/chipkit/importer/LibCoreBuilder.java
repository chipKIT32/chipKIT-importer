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

import com.microchip.mplab.nbide.embedded.api.LanguageTool;
import com.microchip.mplab.nbide.embedded.chipkit.utils.DeletingFileVisitor;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LibCoreBuilder {

    private static final Logger LOGGER = Logger.getLogger(LibCoreBuilder.class.getName());
    private static final PathMatcher SOURCE_FILE_MATCHER = FileSystems.getDefault().getPathMatcher("glob:*.{c,cpp,S}");
    private static final String TOOLS_DIR = "TOOLS_DIR";
    
    public static final String LIB_CORE_NAME = "Core";
    public static final String LIB_CORE_FILENAME = "lib" + LIB_CORE_NAME + ".a";
    public static final String MAKEFILE_FILENAME = "Makefile-" + LIB_CORE_NAME;

    private Path buildDirPath;
    private Path libCorePath;
    private Path makefilePath;
    private List<String> compilationCommands;
    private String archiveCommand;
    

    public LibCoreBuilder() {
    }

    public List<String> getCompilationCommands() {
        return new ArrayList<>(compilationCommands);
    }

    public String getArchiveCommand() {
        return archiveCommand;
    }

    public Path getLibCorePath() {
        return libCorePath;
    }

    public Path getMakefilePath() {
        return makefilePath;
    }
    
    public void buildLibCore( Path makefilePath, GCCToolFinder toolFinder ) throws IOException, InterruptedException {
        buildLibCore(makefilePath, toolFinder, null);
    }
    
    public void buildLibCore( Path makefilePath, GCCToolFinder toolFinder, Consumer<String> messageConsumer ) throws IOException, InterruptedException {
        buildDirPath = Files.createTempDirectory("build");
        libCorePath = buildDirPath.resolve(LIB_CORE_FILENAME);
        updateMakefile( makefilePath, toolFinder );
        Path makeToolPath = toolFinder.findTool( LanguageTool.MakeTool );
        runMake(buildDirPath, makeToolPath.toString(), makefilePath.toString(), messageConsumer);
    }

    public void buildLibCore(ChipKitBoardConfig config, GCCToolFinder toolFinder) throws IOException, InterruptedException {
        buildLibCore(config, toolFinder, null);
    }
    
    public void buildLibCore(ChipKitBoardConfig config, GCCToolFinder toolFinder, Consumer<String> messageConsumer) throws IOException, InterruptedException {
        buildDirPath = Files.createTempDirectory("build");
        libCorePath = buildDirPath.resolve(LIB_CORE_FILENAME);
        makefilePath = buildDirPath.resolve(MAKEFILE_FILENAME);

        List<String> makefileContents = generateMakefileContents(config, toolFinder);            
        Files.write( makefilePath, makefileContents );
        
        Path makeToolPath = toolFinder.findTool( LanguageTool.MakeTool );
        runMake(buildDirPath, makeToolPath.toString(), makefilePath.getFileName().toString(), messageConsumer);
    }
    
    public List <String> generateMakefileContents( ChipKitBoardConfig config, GCCToolFinder toolFinder ) throws IOException {
        Path compilerPath = toolFinder.findTool( LanguageTool.CCCompiler );
        Path archiverPath = toolFinder.findTool( LanguageTool.Archiver );
        Path variantPath = config.getVariantDirPath();
        Path corePath = config.getCoreDirPath();
        String mcu = config.getMCU();
        
        List <String> makefileLines = new ArrayList<>();
        List <String> objectFilenames = new ArrayList<>();
        makefileLines.add( TOOLS_DIR + "=" + compilerPath.getParent().toString() );
        makefileLines.add( LIB_CORE_FILENAME + ":" );
        
        // Find source files in variant directory:
        List<Path> variantFilePaths = new ArrayList<>();                
        if ( variantPath != null ) {
            variantFilePaths = Files.list(variantPath)
                .filter(filePath -> SOURCE_FILE_MATCHER.matches(filePath.getFileName()))
                .collect( Collectors.toList() );
        }
        
        // Create a list of source file names from the variant directory that will be used to filter core source files:
        List <String> variantFileNames = variantFilePaths.stream().map( p -> p.getFileName().toString() ).collect( Collectors.toList() );
        
        // Find source files in core directory but only those that have not been overriden in the variant directory:
        List <Path> coreFilePaths = Files.list(corePath)
            .filter( p -> SOURCE_FILE_MATCHER.matches( p.getFileName()) )
            .filter( p -> !variantFileNames.contains( p.getFileName().toString() ) )
            .collect( Collectors.toList() );
        
        // Add variant and core source file paths:
        List <Path> allSourceFiles = new ArrayList<>();
        allSourceFiles.addAll( variantFilePaths );
        allSourceFiles.addAll( coreFilePaths );
        
        // Generete compilation commands:
        allSourceFiles.forEach(sourceFilePath -> {                
            String sourceFileName = sourceFilePath.getFileName().toString();
            String targetFileName = "\"" + sourceFileName + ".o\"";
            objectFilenames.add( targetFileName );
            StringBuilder command = new StringBuilder("\t");

            command.append("\"${").append(TOOLS_DIR).append("}").append("/").append(compilerPath.getFileName().toString()).append("\"");
            command.append(" -c");
            command.append( " " );

            if (sourceFileName.endsWith(".S")) {
                command.append( String.join(" ", config.getExtraOptionsAS() ) );
                command.append(" -O1");
            } else if (sourceFileName.endsWith(".c")) {
                command.append(" -g");
                command.append(" -x");
                command.append(" c");
                command.append(" -w");
                command.append(" -O1");
                command.append( " " );
                command.append( String.join(" ", config.getCompilerWarnings()) );
                command.append( " " );
                command.append( String.join(" ", config.getExtraOptionsC()) );
            } else if (sourceFileName.endsWith(".cpp")) {
                command.append(" -g");
                command.append(" -x");
                command.append(" c++");
                command.append(" -w");
                command.append(" -O1");
                command.append( " " );
                command.append( String.join(" ", config.getCompilerWarnings()) );
                command.append( " " );
                command.append( String.join( " ", config.getExtraOptionsCPP() ) );
            }

            command.append(" -mprocessor=").append(mcu);                
            if (variantPath != null) {
                command.append(" -I\"").append(variantPath).append("\"");
            }
            command.append(" -I\"").append(corePath).append("\"");
            command.append( " " );
            command.append( String.join( " ", parseCompilerMacros(config.getCompilerMacros() ) ) );
            command.append( " \"" );
            command.append( sourceFilePath.toString() );
            command.append( "\" -o" );
            command.append( " " );
            command.append( targetFileName );
            makefileLines.add( command.toString() );
        });
        
        // Generate archiver command:
        StringBuilder command = new StringBuilder("\t");
        command.append("\"${").append(TOOLS_DIR).append("}").append("/").append(archiverPath.getFileName().toString()).append("\"");
        command.append(" rcs");
        command.append(" ");
        command.append(LIB_CORE_FILENAME);
        command.append(" ");
        command.append( String.join(" ", objectFilenames) );
        makefileLines.add( command.toString() );
        
        return makefileLines;
    } 

    public void cleanup() {
        try {
            Files.walkFileTree(buildDirPath, new DeletingFileVisitor());
        } catch (IOException ex) {
            LOGGER.log( Level.WARNING, "Exception caught while removing temporary files", ex );
        }
        buildDirPath = null;
    }

    
    //*************************************************
    //*************** PRIVATE METHODS *****************
    //*************************************************
    private static void runMake( Path buildDirPath, String makeLocation, String makefileLocation, Consumer<String> messageConsumer ) throws IOException, InterruptedException {
        NativeProcessRunner nativeProcessRunner = new NativeProcessRunner(messageConsumer);
        nativeProcessRunner.runNativeProcess(buildDirPath, makeLocation, "V=1", "-f", makefileLocation );
    }
    
    private static List<String> parseCompilerMacros(String macros) {
        return Arrays.asList(macros.split(";")).stream().map(m -> "-D" + m).collect(Collectors.toList());
    }

    private static void updateMakefile(Path makefilePath, GCCToolFinder toolFinder) throws IOException {
        Path compilerPath = toolFinder.findTool( LanguageTool.CCCompiler );
        List<String> makefileLines = Files.readAllLines(makefilePath);
        for ( int i=0; i<makefileLines.size(); i++ ) {
            String line = makefileLines.get(i).trim();
            if ( line.startsWith( TOOLS_DIR ) ) {
                makefileLines.set( i, TOOLS_DIR + "=" + compilerPath.getParent().toString() );
                break;
            }
        }
        Files.write(makefilePath, makefileLines);
    }

}
