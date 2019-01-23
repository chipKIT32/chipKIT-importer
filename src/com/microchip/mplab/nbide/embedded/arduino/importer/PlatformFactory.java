package com.microchip.mplab.nbide.embedded.arduino.importer;


import static com.microchip.mplab.nbide.embedded.arduino.importer.ArduinoConfig.ROOT_PLATFORM_ARCH;
import static com.microchip.mplab.nbide.embedded.arduino.importer.ArduinoConfig.ROOT_PLATFORM_VENDOR;
import com.microchip.mplab.nbide.embedded.arduino.importer.pic32.PIC32Platform;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PlatformFactory {

    public static final String BOARDS_FILENAME = "boards.txt";
    public static final String PLATFORM_FILENAME = "platform.txt";

    private static final Logger LOGGER = Logger.getLogger(PlatformFactory.class.getName());

    private final List<Platform> allPlatforms = new ArrayList<>();

    public List<Platform> getAllPlatforms(Path arduinoSettingsPath) throws IOException {
        if (allPlatforms.isEmpty()) {

            Path settingsPath = validateArduinoSettingsPath(arduinoSettingsPath);

            // Find all paths containing a "platform.txt" file
            LOGGER.log(Level.INFO, "Searching for platform files in {0}", settingsPath);
            FileFinder finder = new FileFinder(PLATFORM_FILENAME);
            Files.walkFileTree(settingsPath, finder);
            List<Path> platformPaths = finder.getMatchingPaths();

            Platform rootPlatform = createRootPlatform();
            if ( rootPlatform == null ) {
                throw new RuntimeException("Failed to load the root platform!");
            }

            platformPaths.stream().map(path -> createPlatformFromFile(rootPlatform, path)).forEach(allPlatforms::add);
            
            // Add the root platform but only if there is no platform in the user directory with the same vendor/arch:
            if ( !allPlatforms.stream().anyMatch( 
                platform -> ROOT_PLATFORM_VENDOR.equalsIgnoreCase(platform.getVendor()) && ROOT_PLATFORM_ARCH.equalsIgnoreCase(platform.getArchitecture())
            )) {
                allPlatforms.add( rootPlatform );
            }
        }

        return Collections.unmodifiableList(allPlatforms);
    }

    // TODO: Optimize single platform creation so that it is not necessary to create all platforms
    public Platform createPlatform(Path arduinoSettingsPath, String vendor, String architecture) throws IOException {
        if (allPlatforms.isEmpty()) {
            getAllPlatforms(arduinoSettingsPath);
        }
        for (Platform platform : allPlatforms) {
            if (vendor.equalsIgnoreCase(platform.getVendor()) && architecture.equalsIgnoreCase(platform.getArchitecture())) {
                return platform;
            }
        }
        return null;
    }

    public Platform createPlatformFromRootDirectory(Path platformRootPath) throws IOException {
        Path platformFilePath = platformRootPath.resolve(PLATFORM_FILENAME);
        Platform rootPlatform = createRootPlatform();
        return createPlatformFromFile(rootPlatform, platformFilePath);
    }

    public boolean isValidPlatformRootPath(Path rootPath) {
        return rootPath != null && Files.exists(rootPath.resolve(PLATFORM_FILENAME));
    }

    private static Platform createPlatformFromFile(Platform rootPlatform, Path platformFilePath) {
        // Pattern: /home/user/.arduino15/packages/{vendor}/hardware/{architecture}/x.x.x/platform.txt
        int hardwareIndex = -1;
        for (int i = platformFilePath.getNameCount() - 1; i >= 0; i--) {
            if ("hardware".equalsIgnoreCase(platformFilePath.getName(i).toString())) {
                hardwareIndex = i;
                break;
            }
        }
        String vendor = platformFilePath.getName(hardwareIndex - 1).toString();
        String architecture = platformFilePath.getName(hardwareIndex + 1).toString();

        try {
//            if (architecture.equalsIgnoreCase("pic32")) {
                return new PIC32Platform(rootPlatform, vendor, platformFilePath.getParent());
//            } else {
//                return new Platform(rootPlatform, vendor, architecture, platformFilePath.getParent());
//            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, String.format("Failed to create a platform for %s / %s ", vendor, architecture), ex);
            return null;
        }

    }

    private static Platform createRootPlatform() throws IOException {
        Path arduinoPlatformPath = ArduinoConfig.getInstance().getDefaultArduinoPlatformPath().get();
        return new Platform(null, ROOT_PLATFORM_VENDOR, ROOT_PLATFORM_ARCH, arduinoPlatformPath);
    }

    private static Path validateArduinoSettingsPath(Path settingsPath) throws FileNotFoundException {
        if (!Files.exists(settingsPath)) {
            LOGGER.severe("Failed to find the Arduino settings directory!");
            throw new FileNotFoundException("Failed to find the Arduino settings directory!");
        }
        return settingsPath;
    }

}
