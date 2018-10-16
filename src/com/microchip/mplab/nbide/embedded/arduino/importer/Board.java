package com.microchip.mplab.nbide.embedded.arduino.importer;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class Board extends ArduinoDataSource {


    public static final PathMatcher SOURCE_FILE_MATCHER = FileSystems.getDefault().getPathMatcher("glob:*.{c,cpp,S}");    
    public static final String VARIANTS_DIRNAME = "variants";
    
    private static final Logger LOGGER = Logger.getLogger(Board.class.getName());
    
    private final BoardId boardId;
    
    Board(Platform platform, BoardId boardId, Map <String,String> data) {
        super(platform, data);
        this.boardId = boardId;
        putValue("fqbn", createFQBN());
        putValue("build.arch", platform.getArchitecture().toUpperCase());
        String ldScript = data.get("ldscript");
        if ( ldScript != null ) {
            String ldScriptDebug = ldScript.substring( 0, ldScript.lastIndexOf(".") ) + "-debug.ld";
            putValue("ldscript-debug", ldScriptDebug);
        }
    }

    @Override
    public String toString() {
        return "Board{" + "boardId=" + boardId + '}';
    }
    
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + Objects.hashCode(this.boardId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Board other = (Board) obj;
        return Objects.equals(this.boardId, other.boardId);
    }

    public Platform getPlatform() {
        return (Platform) parent;
    }
    
    public BoardId getBoardId() {
        return boardId;
    }
    
    public String getArchitecture() {
        return getPlatform().getArchitecture();
    }
    
    public boolean isPIC32() {
        return getPlatform().isPIC32();
    }
    
    public boolean isAVR() {
        return getPlatform().isAVR();
    }
    
    public boolean isSAMD() {
        return getPlatform().isSAMD();
    }
    
    public Optional<String> getDeviceLinkerScriptFilename() {
        return getValue("ldscript");
    }
    
    public Optional<String> getDeviceDebugLinkerScriptFilename() {
        return getValue("ldscript-debug");
    }

    public Optional<String> getCommonLinkerScriptFilename() {
        return getValue("ldcommon");
    }
    
    @Override
    public Optional<String> getValue( String dataKey, ArduinoDataSource context, Map <String,String> auxData ) {
        String value = auxData != null ? auxData.get(dataKey) : null;
        if ( value == null ) value = data.get( createCompleteKey(dataKey) );
        if ( value == null ) value = data.get( createShortKey(dataKey) ); 
        if ( value != null ) {
            value = resolveTokens(value, context, auxData);
            return Optional.of(value);
        } else {
            return parent.getValue(dataKey, context, auxData).map( v -> resolveTokens(v, context, auxData) );
        }
    }
    
    @Override
    public void putValue(String dataKey, String value) {
        String completeKey = createCompleteKey(dataKey);
        super.putValue( completeKey, value );
    }
        
    public List <Path> getCoreDirPaths() {
        List <Path> ret = new ArrayList<>();
        getValue("build.core.path").ifPresent( val -> ret.add( Paths.get( val ) ) );
        getValue("build.variant.path").ifPresent( val -> ret.add( Paths.get( val ) ) );
        return ret;
    }
    
    public Path getCoreDirectoryPath() {
        Optional<String> coreOpt = getValue("build.core");
        if ( coreOpt.isPresent() ) {
            String coreValue = coreOpt.get();
            Path coresDirPath;
            // TODO: Improve core parsing
            if ( coreValue.equals("arduino:arduino") ) {
                coresDirPath = ArduinoConfig.getInstance().getDefaultArduinoPlatformPath().get().resolve("cores").resolve("arduino");
            } else {
                coresDirPath = getPlatform().getRootPath().resolve("cores").resolve(coreValue);
            }                
            if ( Files.exists(coresDirPath) ) {
                return coresDirPath;
            } else {
                LOGGER.log(Level.SEVERE, "Failed to find any core directory under: {0}", coresDirPath);
                return null;
            }
        } else {
            LOGGER.log(Level.SEVERE, "Failed to find Arduino core directory for: {0}", boardId);
        }            
        return null;
    }
    
    public Path getVariantPath() {
        if ( boardId == null ) throw new IllegalArgumentException("Board ID cannot be null!");
        
        try {
            Optional <String> opt = getValue("build.variant");
            Path variantsDirPath = getPlatform().getRootPath().resolve( VARIANTS_DIRNAME );
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
    
    public List <Path> getCoreFilePaths() throws IOException {
        Path variantPath = getVariantPath();
        Path corePath = getCoreDirectoryPath();
        
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
        List <Path> allCoreFilePaths = new ArrayList<>();
        allCoreFilePaths.addAll( variantFilePaths );
        allCoreFilePaths.addAll( coreFilePaths );
        
        return allCoreFilePaths;
    }
    
    
    //***************************************
    //********** PRIVATE METHODS ************
    //***************************************
    private String createFQBN() {
        // E.g: arduino:avr:pro:cpu=8MHzatmega328
        String cpuPart = (boardId.hasCpu() ? ":cpu=" + boardId.getCpu() : "");
        return getPlatform().getVendor() + ":" + getPlatform().getArchitecture() + ":" + boardId.getBoard() + cpuPart;
    }
    
    private String createShortKey( String dataKey ) {
        return boardId.getBoard() + "." + dataKey;
    }
    
    private String createCompleteKey( String dataKey ) {
        return (boardId.hasCpu() ? boardId.getBoard() + ".menu.cpu." + boardId.getCpu() : boardId.getBoard()) + "." + dataKey;
    }
    
}
