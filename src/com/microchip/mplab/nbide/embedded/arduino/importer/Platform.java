package com.microchip.mplab.nbide.embedded.arduino.importer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openide.util.Exceptions;

public class Platform extends ArduinoDataSource {

    private static final Logger LOGGER = Logger.getLogger(Platform.class.getName());

    public static final String PLATFORM_FILENAME = "platform.txt";
    public static final String BOARDS_FILENAME = "boards.txt";
    public static final String VARIANTS_DIRNAME = "variants";

    private final String vendor;
    private final String architecture;
    private final Path rootPath;

    private final Map<String, String> boardNamesToIdsLookup = new HashMap<>();

    public Platform(Platform parent, String vendor, String architecture, Path rootPath) throws IOException {
        super(parent);
        this.vendor = vendor;
        this.architecture = architecture;
        this.rootPath = rootPath;
        this.data = parseDataFile(PLATFORM_FILENAME);
    }

    public Platform getParent() {
        return (Platform) parent;
    }

    public String getArchitecture() {
        return architecture;
    }

    public String getVendor() {
        return vendor;
    }

    public boolean isPIC32() {
        return (architecture != null) ? architecture.toLowerCase().equals("pic32") : false;
    }

    public boolean isAVR() {
        return (architecture != null) ? architecture.toLowerCase().equals("avr") : false;
    }

    public boolean isSAMD() {
        return (architecture != null) ? architecture.toLowerCase().equals("samd") : false;
    }

    public Optional<String> getDisplayName() {
        return getValue("name");
    }

    public Path getRootPath() {
        return rootPath;
    }

    public Path getBoardsFilePath() {
        return rootPath.resolve(BOARDS_FILENAME);
    }

    public Path getPlatformFilePath() {
        return rootPath.resolve(PLATFORM_FILENAME);
    }

    public Map<String, String> getBoardNamesToIDsLookup() {
        if (boardNamesToIdsLookup.isEmpty()) {
            try (Stream<String> lines = Files.lines(rootPath.resolve(BOARDS_FILENAME))) {
                return lines
                        .map(line -> line.trim())
                        .filter(line -> !line.isEmpty() && !line.startsWith("#") && !line.startsWith("menu."))
                        .map(line -> {
                            int splitIndex = line.indexOf("=");
                            String key = line.substring(0, splitIndex).trim();
                            String value = line.substring(splitIndex + 1);
                            String[] keyParts = key.split("\\.");
                            if (key.endsWith(".name") && keyParts.length == 2) {
                                return new String[]{value.trim(),keyParts[0]};
                            } else {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(
                                tokens -> tokens[0],
                                tokens -> tokens.length > 1 ? tokens[1] : "",
                                (val1, val2) -> val2
                        ));
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
                return Collections.EMPTY_MAP;
            }
        } else {
            return Collections.unmodifiableMap(boardNamesToIdsLookup);
        }
    }

    public Set<String> getBoardIDs() {
        return getBoardNamesToIDsLookup().keySet();
    }

    public Optional<Board> getBoard(String boardId) {
        Set<BoardOption> allAvailableOptions = new HashSet<>();
        Map<String, String> boardData = new HashMap<>();

        try (Stream<String> lines = Files.lines(rootPath.resolve(BOARDS_FILENAME))) {
            lines
                    .map(line -> line.trim())
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .forEach(line -> {
                        String[] keyValue = line.split("=");
                        String key = keyValue[0];
                        String value = keyValue.length > 1 ? keyValue[1] : "";
                        if (key.startsWith("menu.")) {
                            allAvailableOptions.add(new BoardOption(key, value));
                        } else if (key.startsWith(boardId)) {
                            int firstDotIndex = key.indexOf(".");
                            String boardValueId = key.substring(firstDotIndex + 1);
                            boardData.put(boardValueId, value);
                        }
                    });
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return Optional.empty();
        }

        Map<BoardOption,Set<String>> boardOptions = new HashMap<>();
        boardData.keySet().forEach(key -> {
            allAvailableOptions.forEach((BoardOption opt) -> {
                if (key.contains(opt.getId())) {
                    Set<String> optionValues = boardOptions.computeIfAbsent(opt, k -> new HashSet<>());
                    String shortKey = key.substring(opt.getId().length()+1);
                    String[] shortKeyParts = shortKey.split("\\.");
                    if ( shortKeyParts.length == 1 ) {
                        String optionValue = shortKey;
                        optionValues.add(optionValue);
                    }
                }
            });
        });

        return Optional.of(new Board(this, boardId, boardData, boardOptions));
    }

    @Override
    public String toString() {
        return "Platform{ vendor=" + vendor + ", architecture=" + architecture + ", rootPath=" + rootPath + '}';
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + Objects.hashCode(this.vendor);
        hash = 37 * hash + Objects.hashCode(this.architecture);
        hash = 37 * hash + Objects.hashCode(this.rootPath);
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
        final Platform other = (Platform) obj;
        if (!Objects.equals(this.vendor, other.vendor)) {
            return false;
        }
        if (!Objects.equals(this.architecture, other.architecture)) {
            return false;
        }
        if (!Objects.equals(this.rootPath, other.rootPath)) {
            return false;
        }
        return true;
    }

    // ***************************************
    // ********** PRIVATE METHODS ************
    // ***************************************    
    private Map<String, String> parseDataFile(String filename) throws IOException {
        try (Stream<String> lines = Files.lines(rootPath.resolve(filename))) {
            return lines
                    .map(line -> line.trim())
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(line -> {
                        int splitIndex = line.indexOf("=");
                        return new String[]{line.substring(0, splitIndex), line.substring(splitIndex + 1)};
                    })
                    .collect(Collectors.toMap(
                            tokens -> tokens[0],
                            tokens -> tokens.length > 1 ? tokens[1] : "",
                            (val1, val2) -> val2
                    ));
        }
    }

}
