package com.microchip.mplab.nbide.embedded.arduino.importer;

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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Board extends ArduinoDataSource {


    public static final PathMatcher SOURCE_FILE_MATCHER = FileSystems.getDefault().getPathMatcher("glob:*.{c,cpp,S}");    
    public static final String VARIANTS_DIRNAME = "variants";
    
    private static final Logger LOGGER = Logger.getLogger(Board.class.getName());
    
    private final String boardId;
    private final Map <BoardOption,Set<String>> options;
    
    public Board(Platform platform, String boardId, Map<String,String> data, Map<BoardOption,Set<String>> options) {
        super(platform, data);
        this.boardId = boardId;
        this.options = options;
        putValue("build.arch", platform.getArchitecture().toUpperCase());
        String ldScript = data.get("ldscript");
        if ( ldScript != null ) {
            String ldScriptDebug = ldScript.substring( 0, ldScript.lastIndexOf(".") ) + "-debug.ld";
            putValue("ldscript-debug", ldScriptDebug);
        }
    }

    @Override
    public String toString() {        
        return "Board {boardId=" + boardId + ", options=" + options.keySet() + '}';
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
    
    public String getBoardId() {
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
    
    @Override
    public Optional<String> getValue( String dataKey, ArduinoDataSource context, Map <String,String> auxData ) {
        String value = auxData != null ? auxData.get(dataKey) : null;
        if ( value == null ) value = data.get( dataKey );
        if ( value != null ) {
            value = resolveTokens(value, context, auxData);
            return Optional.of(value);
        } else {
            return parent.getValue(dataKey, context, auxData).map( v -> resolveTokens(v, context, auxData) );
        }
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

    public Set<BoardOption> getOptions() {
        return options.keySet();
    }
    
    public Set<String> getAvailableOptionValues( BoardOption option ) {
        return options.get(option);
    }
    
    public boolean hasOptions() {
        return !options.isEmpty();
    }
    
}
