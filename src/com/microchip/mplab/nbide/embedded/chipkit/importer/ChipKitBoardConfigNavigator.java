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


import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class ChipKitBoardConfigNavigator {
    
    private static final Logger LOGGER = Logger.getLogger(ChipKitBoardConfigNavigator.class.getName());
    
    public static final List <String> CURRENTLY_UNSUPPORTED_BOARD_IDS = Arrays.asList("clicker2", "cui32", "CUI32stem", "usbono_pic32");
    
    public static final String BOARDS_FILENAME = "boards.txt";
    public static final String PLATFORM_FILENAME = "platform.txt";
    public static final String VARIANTS_DIRNAME = "variants";
    
    private final Path chipKitHardwarePath;
    private final Path chipKitCorePath;
    
    public static List<Path> findChipKitHardwareDirectories( ArduinoConfig arduinoPathResolver ) throws IOException {
        
        Path settingsPath = arduinoPathResolver.getSettingsPath();
                        
        if ( settingsPath == null ) {
            LOGGER.severe( "Failed to find the Arduino settings directory!" );
            throw new FileNotFoundException("Failed to find the Arduino settings directory!");
        }
        
        // Find all paths containing a "platform.txt" file
        LOGGER.log(Level.INFO, "Searching for platform files in {0}", settingsPath);
        FileFinder finder = new FileFinder(PLATFORM_FILENAME);
        Files.walkFileTree(settingsPath, finder);
        List <Path> platformPaths = finder.getMatchingPaths();
        
        List <Path> chipkitPlatformPaths = platformPaths.stream()
            .filter( path -> isValidChipKitPlatformFile(path) )
            .map( path -> path.getParent() )  // Convert file paths to parent directory paths
            .collect( Collectors.toList() );
        
        if ( chipkitPlatformPaths.size() > 0 ) {
            LOGGER.log(Level.INFO, "Found {0} platform path(s): {1}", new Object[]{chipkitPlatformPaths.size(), chipkitPlatformPaths});
        } else {
            LOGGER.severe("Failed to find chipKIT platform files!");
            throw new FileNotFoundException("Failed to find chipKIT platform files!");
        }

        return chipkitPlatformPaths;
    }
    
    public static boolean isValidChipKitHardwarePath( Path hardwarePath ) {
        Path platformFilePath = hardwarePath.resolve( PLATFORM_FILENAME );
        if ( Files.exists(platformFilePath) ) {
            return isValidChipKitPlatformFile(platformFilePath);
        } else {
            return false;
        }
    }
    
    public static boolean isValidChipKitPlatformFile( Path platformFilePath ) {
        // Check if the file contains an entry: "name=chipKIT"
        try {
            Optional <String> opt = Files.lines(platformFilePath).filter( line -> line.replaceAll("\\s+","").equals("name=chipKIT") ).findAny();
            return opt.isPresent();
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Exception caught while parsing " + PLATFORM_FILENAME + " file", ex);
            return false;
        }
    }
    
    public ChipKitBoardConfigNavigator( Path chipKitHardwarePath ) {
        this.chipKitHardwarePath = chipKitHardwarePath;
        this.chipKitCorePath = chipKitHardwarePath.resolve( Paths.get("cores", "pic32") );
    }

    public Path getChipKitHardwarePath() {
        return chipKitHardwarePath;
    }

    public Path getChipKitCorePath() {
        return chipKitCorePath;
    }
    
    public Path getChipKitBoardsFilePath() {
        return chipKitHardwarePath.resolve( BOARDS_FILENAME );
    }
    
    public Path getChipKitPlatformFilePath() {
        return chipKitHardwarePath.resolve( PLATFORM_FILENAME );
    }
    
    public Path getPackagesRootPath() {
        int chipkitIndex = findChipKitPathIndex(chipKitHardwarePath);
        return chipKitHardwarePath.getRoot().resolve( chipKitHardwarePath.subpath(0, chipkitIndex) );
    }
    
    public String getFullyQualifiedBoardName( String boardId ) throws IOException {
        // FQBN arduino:avr:nano - [vendor folder name]:[architecture folder name]:[boardId].
        // TODO: Implement proper Vendor/Architecture resolution
        int chipkitIndex = findChipKitPathIndex(chipKitHardwarePath);
        if ( chipkitIndex == -1 ) {
            throw new RuntimeException("Failed to find chipKIT hardware directory in " + chipKitHardwarePath);
        }
        String chipkitCoreName = chipKitHardwarePath.getName(chipkitIndex).toString();
        return chipkitCoreName + ":pic32:" + boardId;
    }
    
    public Path getChipKitVariantBoardsFilePath( String boardId ) {
        Path variantDirPath = getChipKitVariantPath(boardId);
        if ( variantDirPath == null || !Files.exists(variantDirPath) ) return null;
        
        Path boardsFilePath = variantDirPath.resolve( BOARDS_FILENAME );
        if ( !Files.exists( boardsFilePath ) ) {
            return null;
        }
        
        return boardsFilePath;
    }
    
    public Path getPic32ProgPath() {
        Path p = chipKitHardwarePath;
        for ( int i=0; i<chipKitHardwarePath.getNameCount(); i++ ) {
            Path toolsDirPath = p.resolve("tools");
            if ( Files.exists(toolsDirPath) ) {
                return findPic32ProgInToolsDir(toolsDirPath);
            }                
            p = p.getParent();
        }
        LOGGER.log( Level.WARNING, "Failed to find the pic32prog" );
        return null;
    }
    
    public Optional <String> getChipKitVariantName( String boardId ) {
        if ( boardId == null ) throw new IllegalArgumentException("Board ID cannot be null!");
        Path mainBoardFilePath = chipKitHardwarePath.resolve( BOARDS_FILENAME );
        String variantPathKey = boardId+".build.variant";
        try {
            return Files.lines(mainBoardFilePath)
                .map( line -> line.trim() )
                .filter( line -> !line.startsWith("#") )
                .filter( line -> line.startsWith(variantPathKey) )
                .map( line -> {                        
                    String[] tokens = line.split("=");
                    // Make sure that variantPathKey is the whole key and not just some part of it
                    return ( tokens.length == 2 && variantPathKey.equals(tokens[0]) ) ? tokens[1] : null;
                })
                .filter( value -> value != null )
                .findAny();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public Path getChipKitVariantPath( String boardId ) {
        if ( boardId == null ) throw new IllegalArgumentException("Board ID cannot be null!");
        
        try {
            Optional <String> opt = getChipKitVariantName(boardId);
            Path variantsDirPath = chipKitHardwarePath.resolve( VARIANTS_DIRNAME );
            if ( opt.isPresent() ) {
                Path variantPath = variantsDirPath.resolve( opt.get() );
                // If the path does not exist, it might just be because of the letter casing in the variant name 
                // so go through all directories and compare their lower-case names with lower-case variant name:
                if ( !Files.exists( variantPath ) ) {
                    String lowerCaseDirName = variantPath.getFileName().toString().toLowerCase();
                    Optional<Path> findAny = Files.list( variantsDirPath ).filter( p -> p.getFileName().toString().toLowerCase().equals( lowerCaseDirName ) ).findAny();
                    if ( findAny.isPresent() ) {
                        variantPath = findAny.get();
                    } else {
                        throw new IllegalArgumentException("Did not find any variant directory for board \"" + boardId + "\"");
                    }
                }                
                return variantPath;
            } else {
                throw new IllegalArgumentException("Did not find any variant directory for board \"" + boardId + "\"");
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Map<String,String> parseBoardNamesToIDsLookup() throws IOException {
        Map <String,String> ret = new HashMap<>();
        Path boardsFilePath = chipKitHardwarePath.resolve( BOARDS_FILENAME );
        Path variantsDirPath = chipKitHardwarePath.resolve( VARIANTS_DIRNAME );
        String buildVariantKey = ".build.variant";
        List<String> boardsFileLines = Files.readAllLines(boardsFilePath);
        
        Files.list(variantsDirPath)
            .parallel()
            .map( variantDir -> variantDir.getFileName().toString() )
            .forEach( variantId -> {                
                Pattern p1 = Pattern.compile(".+\\.build\\.variant\\s*=\\s*"+variantId);
                String boardId = boardsFileLines.stream()
                    .filter( line -> p1.matcher(line).matches() )
                    .findAny()
                    .map( line -> line.split(buildVariantKey)[0] )
                    .orElse("");
                
                if ( !boardId.isEmpty() ) {
                    String boardBaseId = boardId.split("\\.")[0];  // First part of the ID - the part before the first dot
                    String boardName = findBoardName(boardsFileLines, boardId);
                    
                    if ( boardName.isEmpty() ) {
                        boardId = boardBaseId;
                        boardName = findBoardName(boardsFileLines, boardId);                    
                    }
                    
                    if ( !boardId.isEmpty() && !CURRENTLY_UNSUPPORTED_BOARD_IDS.contains(boardBaseId) ) {
                        ret.put( boardName, boardId );
                    }
                }
            });
        return ret;
    }
    
    public Set<String> parseBoardIDs( Path boardDirPath ) {
        try {
            Path boardFilePath = boardDirPath.resolve( BOARDS_FILENAME );
            return Files.lines(boardFilePath)
                .filter( line -> !line.isEmpty() && !line.trim().startsWith("#") )
                .map( line -> line.substring(0, line.indexOf(".")) ).collect( Collectors.toSet() );            
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public String parseDeviceName( String boardId ) throws IOException {
        String mcu = parseMCU(boardId);
        return "PIC"+mcu;
    }
    
    public String parseMCU( String boardId ) throws IOException {
        Path boardsFilePath = chipKitHardwarePath.resolve( BOARDS_FILENAME );
        String key = boardId + ".build.mcu";
        Optional <String> result = Files.readAllLines(boardsFilePath).stream().filter( line -> !line.startsWith("#") && line.contains(key) ).findFirst();
        String line = result.get();
        if ( line != null ) {
            return line.substring( line.indexOf("=")+1 );
        } else {
            return null;
        }
    }
    
    public ChipKitBoardConfig readCompleteBoardConfig( String boardId, Path coreDirPath, Path variantDirPath, Path ldScriptDirPath ) throws IOException {
        Map <String,String> config = new HashMap<>();
        config.put("build.arch", "PIC32");
        config.put("runtime.ide.version", "10805");
        config.put("build.core.path", coreDirPath.toString() );
        config.put("build.variant.path", variantDirPath != null ? variantDirPath.toString() : "" );
        config.put("build.ldscript_dir.path", ldScriptDirPath != null ? ldScriptDirPath.toString() : "" );
        config.put("fqbn", getFullyQualifiedBoardName(boardId));
        config.put("packagesRoot", getPackagesRootPath().toString() );

        readPlatformFile( getChipKitPlatformFilePath(), config );
        readBoardsFile( getChipKitBoardsFilePath(), boardId, config );
        readBoardsFile( getChipKitVariantBoardsFilePath(boardId), boardId, config );
        
        Pattern tokenPattern = Pattern.compile("(\\{[\\.|\\w]*\\})");
        config.entrySet().forEach( e -> {
            Matcher m = tokenPattern.matcher(e.getValue());
            String newValue = e.getValue();
            while ( m.find() ) {
                String tokenWithBraces = m.group(1);
                String token = tokenWithBraces.substring(1, tokenWithBraces.length()-1);
                String tokenValue = config.get( token );
                if ( tokenValue != null ) {
                    newValue = newValue.replace( tokenWithBraces, tokenValue );
                }
            }
            config.put( e.getKey(), newValue );
        });
        
        String ldScript = config.get("ldscript");
        String ldScriptDebug = ldScript.substring( 0, ldScript.lastIndexOf(".") ) + "-debug.ld";
        config.put("ldscript-debug", ldScriptDebug);
        
        return new ChipKitBoardConfig(config);
    }
    
    private void readPlatformFile( Path configFilePath, Map <String,String> config ) throws IOException {
        LOGGER.log(Level.INFO, "Reading platform file from  \"{0}\".", configFilePath);
        Files.lines(configFilePath).forEach( (line) -> {            
            String[] pair = splitLineToKeyValuePair(line);
            if ( pair == null ) return;
            config.put(pair[0], pair[1]);
        });
    }
    
    private void readBoardsFile( Path configFilePath, String boardId, Map <String,String> config ) throws IOException {
        if ( configFilePath == null ) return;
        LOGGER.log(Level.INFO, "Reading boards file for board \"{0}\" from  \"{1}\".", new Object[]{boardId, configFilePath});
        String keyStart = boardId+".";
        Files.lines(configFilePath).forEach( (line) -> {           
            String[] pair = splitLineToKeyValuePair(line);            
            if ( pair == null ) return;
            if ( !pair[0].startsWith( keyStart ) ) return;
            pair[0] = pair[0].substring(pair[0].indexOf(".")+1);
            config.put(pair[0], pair[1]);
        });
    }
    
    private String[] splitLineToKeyValuePair( String line ) {
        line = line.trim();
        if ( line.isEmpty() || line.startsWith("#") ) {
            return null;
        }
        int separatorIndex = line.indexOf("=");
        String key = line.substring(0, separatorIndex);
        String value = separatorIndex < line.length()-1 ? line.substring(separatorIndex+1) : "";
        return new String[] {key.trim(), value.trim()};
    }
    
    private int findChipKitPathIndex( Path p ) {
        int pic32Index = -1;
        for ( int i=p.getNameCount()-1; i>=0; i-- ) {
            String dir = p.getName(i).toString();            
            if ( dir.equals("pic32") ) {
                pic32Index = i;
            } else if ( pic32Index != -1 && dir.toLowerCase().contains("chipkit") ) {
                return i;
            }
        }
        return -1;
    }    
    
    private Path findPic32ProgInToolsDir( Path toolsDir ) {
        try {
            Optional<Path> opt = Files.walk( toolsDir )
                .filter( f -> !Files.isDirectory(f) && f.getFileName().toString().startsWith("pic32prog") )
                .findAny();
            if ( opt.isPresent() ) {
                return opt.get();
            } else {
                LOGGER.log(Level.WARNING, "Failed to find the pic32prog in {0}", toolsDir);
            }
        } catch (IOException ex) {
            LOGGER.log( Level.WARNING, "Failed to find the pic32prog in " + toolsDir, ex );
        }
        return null;
    }
    
    private String findBoardName( List<String> boardsFileLines, String boardId ) {
        Pattern p2 = Pattern.compile( boardId + "(\\.name)*\\s*=.*" );
        return boardsFileLines.stream()
            .filter( line -> p2.matcher(line).matches() )
            .findAny()
            .map( line -> line.split("=")[1] )
            .orElse("");
    }
    
}
