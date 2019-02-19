package com.microchip.mplab.nbide.embedded.arduino.importer;

import static com.microchip.mplab.nbide.embedded.arduino.importer.Board.SOURCE_FILE_MATCHER;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class BoardConfiguration extends ArduinoDataSource {

    private static final Logger LOGGER = Logger.getLogger(BoardConfiguration.class.getName());

    private static final String VARIANTS_DIRNAME = "variants";
    
    private static final String KEY_FQBN = "fqbn";
    private static final String KEY_BUILD_EXTRA_FLAGS = "build.extra_flags";

    private final Board board;
    private final Map<BoardOption, String> boardOptionsToValuesLookup;
    private final Map<String, String> boardOptionIdsToValuesLookup;

    public BoardConfiguration(Board board) {
        this(board, Collections.EMPTY_MAP);
    }

    public BoardConfiguration(Board board, Map<BoardOption, String> boardOptionsToValuesLookup) {
        super(board); 
        assert board != null;
        this.board = board;
        this.boardOptionsToValuesLookup = boardOptionsToValuesLookup;
        this.boardOptionIdsToValuesLookup = boardOptionsToValuesLookup.entrySet()
                .stream()
                .collect(
                        Collectors.toMap(
                                e -> e.getKey().getId(),
                                e -> e.getValue()
                        )
                );
        putValue( KEY_FQBN, createFQBN() );
        putValue( KEY_BUILD_EXTRA_FLAGS, getValue(KEY_BUILD_EXTRA_FLAGS).map( flags -> flags + " -D__CTYPE_NEWLIB -mnewlib-libc").orElse("") );
    }

    public String getFqbn() {
        return getValue(KEY_FQBN).get();  // Risky, but we know we've put it there in the constructor
    }

    @Override
    public String toString() {
        return "BoardConfiguration {boardId=" + board.getBoardId() + "}";
    }

    public Board getBoard() {
        return board;
    }

    public String getBoardId() {
        return board.getBoardId();
    }

    public Platform getPlatform() {
        return board.getPlatform();
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

    public boolean hasOption(String optionId) {
        return boardOptionIdsToValuesLookup.containsKey(optionId);
    }

    public Optional<String> getOptionValue(String optionId) {
        return Optional.ofNullable(boardOptionIdsToValuesLookup.get(optionId));
    }
    
    public Optional<String> getOptionValueLabel(String optionId) {
        return getOptionValue(optionId).flatMap( optionValue -> {
            String dataKey = optionId + "." + optionValue;
            return board.getValue(dataKey);
        });
    }

    @Override
    public Optional<String> getValue(String key, ArduinoDataSource context, Map<String, String> runtimeData) {
        String boardConfigData = data.get(key);
        if (boardConfigData != null) {
            return Optional.of(boardConfigData);
        } else {
            return Optional.ofNullable(boardOptionsToValuesLookup.entrySet().stream().map(e -> {
                String optionId = e.getKey().getId();
                String optionValue = e.getValue();
                String dataKey = optionId + "." + optionValue + "." + key;
                return board.getValue(dataKey, this, runtimeData).orElse(null);
            })
                    .filter(Objects::nonNull)
                    .findAny()
                    // Fallback to board:
                    .orElse(board.getValue(key, this, runtimeData).orElse(null)));
        }
    }

    public List<Path> getCoreDirPaths() {
        List<Path> ret = new ArrayList<>();
        getValue("build.core.path").ifPresent(val -> ret.add(Paths.get(val)));
        getValue("build.variant.path").ifPresent(val -> ret.add(Paths.get(val)));
        return ret;
    }

    public Path getCoreDirectoryPath() {
        return board.getCoreDirectoryPath();
    }

    public Path getVariantPath() {
        try {
            Optional<String> opt = getValue("build.variant");
            Path variantsDirPath = getPlatform().getRootPath().resolve(VARIANTS_DIRNAME);
            if (opt.isPresent()) {
                Path variantPath = variantsDirPath.resolve(opt.get());
                // If the path does not exist, it might just be because of the letter casing in the variant name 
                // so go through all directories and compare their lower-case names with lower-case variant name:
                if (!Files.exists(variantPath)) {
                    String lowerCaseDirName = variantPath.getFileName().toString().toLowerCase();
                    Optional<Path> findAny = Files.list(variantsDirPath).filter(p -> p.getFileName().toString().toLowerCase().equals(lowerCaseDirName)).findAny();
                    if (findAny.isPresent()) {
                        variantPath = findAny.get();
                    } else {
                        throw new IllegalArgumentException("Did not find any variant directory for board \"" + board.getBoardId() + "\"");
                    }
                }
                return variantPath;
            } else {
                throw new IllegalArgumentException("Did not find any variant directory for board \"" + board.getBoardId() + "\"");
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<Path> getCoreFilePaths() throws IOException {
        Path variantPath = getVariantPath();
        Path corePath = getCoreDirectoryPath();

        // Find source files in variant directory:
        List<Path> variantFilePaths = new ArrayList<>();
        if (variantPath != null) {
            variantFilePaths = Files.list(variantPath)
                    .filter(filePath -> SOURCE_FILE_MATCHER.matches(filePath.getFileName()))
                    .collect(Collectors.toList());
        }

        // Create a list of source file names from the variant directory that will be used to filter core source files:
        List<String> variantFileNames = variantFilePaths.stream().map(p -> p.getFileName().toString()).collect(Collectors.toList());

        // Find source files in core directory but only those that have not been overriden in the variant directory:
        List<Path> coreFilePaths = Files.list(corePath)
                .filter(p -> SOURCE_FILE_MATCHER.matches(p.getFileName()))
                .filter(p -> !variantFileNames.contains(p.getFileName().toString()))
                .collect(Collectors.toList());

        // Add variant and core source file paths:
        List<Path> allCoreFilePaths = new ArrayList<>();
        allCoreFilePaths.addAll(variantFilePaths);
        allCoreFilePaths.addAll(coreFilePaths);

        return allCoreFilePaths;
    }

    //***************************************
    //********** PRIVATE METHODS ************
    //***************************************    
    private String createFQBN() {
        // E.g: arduino:avr:pro:cpu=8MHzatmega328
        String base = getPlatform().getVendor() + ":" + getPlatform().getArchitecture() + ":" + board.getBoardId();
        return getOptionValue(BoardOption.OPTION_CPU).map( cpuValue -> {
            String cpuPart = (cpuValue != null ? ":cpu=" + cpuValue : "");
            return base + cpuPart;
        }).orElse( base );
    }

}
