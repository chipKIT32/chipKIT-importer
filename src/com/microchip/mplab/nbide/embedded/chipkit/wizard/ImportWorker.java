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

package com.microchip.mplab.nbide.embedded.chipkit.wizard;

import com.microchip.crownking.mplabinfo.DeviceSupport;
import com.microchip.mplab.nbide.embedded.api.LanguageToolchain;
import com.microchip.mplab.nbide.embedded.chipkit.importer.ArduinoBuilderRunner;
import com.microchip.mplab.nbide.embedded.chipkit.importer.ArduinoConfig;
import com.microchip.mplab.nbide.embedded.chipkit.importer.BootloaderPathProvider;
import com.microchip.mplab.nbide.embedded.chipkit.importer.ChipKitBoardConfig;
import com.microchip.mplab.nbide.embedded.chipkit.importer.ChipKitBoardConfigNavigator;
import com.microchip.mplab.nbide.embedded.chipkit.importer.ChipKitProjectImporter;
import static com.microchip.mplab.nbide.embedded.chipkit.importer.ChipKitProjectImporter.CHIPKIT_PROPERTIES_FILENAME;
import com.microchip.mplab.nbide.embedded.chipkit.importer.GCCToolFinder;
import com.microchip.mplab.nbide.embedded.chipkit.importer.LibCoreBuilder;
import com.microchip.mplab.nbide.embedded.chipkit.utils.DeletingFileVisitor;
import static com.microchip.mplab.nbide.embedded.chipkit.wizard.ChipKitImportWizardProperty.ARDUINO_DIR;
import static com.microchip.mplab.nbide.embedded.chipkit.wizard.ChipKitImportWizardProperty.CHIPKIT_BOARD_CONFIG_NAVIGATOR;
import static com.microchip.mplab.nbide.embedded.chipkit.wizard.ChipKitImportWizardProperty.CHIPKIT_BOARD_ID;
import static com.microchip.mplab.nbide.embedded.chipkit.wizard.ChipKitImportWizardProperty.COPY_CHIPKIT_FILES;
import com.microchip.mplab.nbide.embedded.makeproject.MakeProject;
import com.microchip.mplab.nbide.embedded.makeproject.api.configurations.Folder;
import com.microchip.mplab.nbide.embedded.makeproject.api.configurations.Item;
import com.microchip.mplab.nbide.embedded.makeproject.api.configurations.LoadableItem;
import com.microchip.mplab.nbide.embedded.makeproject.api.configurations.MakeConfiguration;
import com.microchip.mplab.nbide.embedded.makeproject.api.configurations.MakeConfigurationBook;
import com.microchip.mplab.nbide.embedded.makeproject.api.configurations.OptionConfiguration;
import com.microchip.mplab.nbide.embedded.makeproject.api.configurations.StringConfiguration;
import com.microchip.mplab.nbide.embedded.makeproject.api.remote.FilePathAdaptor;
import com.microchip.mplab.nbide.embedded.makeproject.api.support.MakeProjectGenerator;
import com.microchip.mplab.nbide.embedded.makeproject.api.wizards.WizardProperty;
import static com.microchip.mplab.nbide.embedded.makeproject.api.wizards.WizardProperty.PROJECT_DIR;
import static com.microchip.mplab.nbide.embedded.makeproject.api.wizards.WizardProperty.SOURCE_PROJECT_DIR;
import com.microchip.mplab.nbide.embedded.makeproject.ui.utils.PathPanel;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.modules.cnd.utils.CndPathUtilities;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.Exceptions;

public class ImportWorker extends SwingWorker<Set<FileObject>, String> {

    private static final Logger LOGGER = Logger.getLogger(ImportWorker.class.getName());
    
    private static final String DEFAULT_CONF_NAME = "default";
    private static final String DEBUG_CONF_NAME = "debug";
    
    private static final String DEFAULT_OPTIMIZATION_OPTION = "-O1";
    
    private Exception exception;
    private final LanguageToolchain languageToolchain;
    private final WizardDescriptor wizardDescriptor;
    private volatile boolean multiConfigBoard;

    public ImportWorker(LanguageToolchain languageToolchain, WizardDescriptor wizardDescriptor) {
        this.languageToolchain = languageToolchain;
        this.wizardDescriptor = wizardDescriptor;
    }

    @Override
    public Set<FileObject> doInBackground() {
        try {
            return invokeImporterTasks();
        } catch (Exception ex) {
            this.exception = ex;
            LOGGER.log( Level.SEVERE, "Failed to import project", ex );
            final File projectDir = (File) wizardDescriptor.getProperty(WizardProperty.PROJECT_DIR.key());
            // Delete the project directory after a short delay so that the import process releases all project files.
            Timer t = new Timer(2000, (a) -> {                
                try {
                    deleteExistingProject(projectDir);
                } catch (IOException ex1) {
                    LOGGER.log( Level.SEVERE, "Failed to delete an incompletely imported project", ex1 );
                }
            });
            t.setRepeats(false);
            t.start();
            return new HashSet<>();
        }
    }
    
    public boolean isMultiConfigBoard() {
        return multiConfigBoard;
    }

    public boolean hasFailed() {
        return exception != null;
    }
    
    public Exception getException() {
        return exception;
    }

    
    //**********************************************
    //************** PRIVATE METHODS ***************
    //**********************************************    
    private Set<FileObject> invokeImporterTasks() throws IOException {
        Set<FileObject> resultSet = new HashSet<FileObject>();

        Boolean overwriteExistingProject = (Boolean) wizardDescriptor.getProperty(WizardProperty.OVERWRITE_EXISTING_PROJECT.key());
        if (overwriteExistingProject == null) {
            overwriteExistingProject = false;
        }

        if (overwriteExistingProject) {
            File projectDir = (File) wizardDescriptor.getProperty(WizardProperty.PROJECT_DIR.key());
            deleteExistingProject(projectDir);
        }

        long t0 = System.currentTimeMillis();
        try {
            resultSet.addAll(createProjectFromChipKit());
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            LOGGER.log(Level.INFO, "Elapsed time of import operation: {0} ms", System.currentTimeMillis()-t0);
        }
        
        return resultSet;
    }
    
    private void deleteExistingProject(File projectDir) throws IOException {
        if (projectDir != null) {
            projectDir = FileUtil.normalizeFile(projectDir);
        }
        FileObject dirFO = FileUtil.toFileObject(projectDir);
        MakeProject proj = null;
        if (dirFO != null) {
            proj = (MakeProject) ProjectManager.getDefault().findProject(dirFO);
        }
        if (proj != null) {
            if (OpenProjects.getDefault().isProjectOpen(proj)) {
                OpenProjects.getDefault().close(new MakeProject[]{proj});
            }
            Files.walkFileTree(proj.getProjectDirectoryFile().toPath(), new DeletingFileVisitor());
        }
    }

    private Set<FileObject> createProjectFromChipKit() throws IOException, InterruptedException {
        Set<FileObject> projectRootDirectories = new HashSet<FileObject>(1);
        File projectDirectory = initProjectDirectoryFromWizard(projectRootDirectories);
        String boardId = (String) wizardDescriptor.getProperty(CHIPKIT_BOARD_ID.key());
        
        MakeConfiguration[] confs;
        MakeConfiguration defaultConf = createDefaultMakefileConfiguration(projectDirectory);
        if ( ChipKitProjectImporter.CUSTOM_LD_SCRIPT_BOARD_IDS.contains(boardId) ) {
            MakeConfiguration debugConf = createDebugMakefileConfiguration(projectDirectory);            
            confs = new MakeConfiguration[]{defaultConf, debugConf};
        } else {
            confs = new MakeConfiguration[]{defaultConf};
        }
        
        String projectName = (String) wizardDescriptor.getProperty(WizardProperty.PROJECT_NAME.key());
        String makefileName = (String) wizardDescriptor.getProperty(WizardProperty.MAKE_FILENAME.key());
        String hostDir = projectDirectory.getAbsolutePath();
        MakeProject newProject = MakeProjectGenerator.createProject(projectDirectory, hostDir, projectName, makefileName, confs, null, null, null, true, null);
        importChipKitProjectFiles(newProject);
        setupProjectEncoding(newProject);
        newProject.save();
        return projectRootDirectories;
    }

    private File initProjectDirectoryFromWizard(Set<FileObject> projectRootDirectories) {
        File projectDirectory = (File) wizardDescriptor.getProperty(WizardProperty.PROJECT_DIR.key());
        if (projectDirectory != null) {
            projectDirectory = FileUtil.normalizeFile(projectDirectory);
        }
        projectDirectory.mkdirs();
        FileObject dir = FileUtil.toFileObject(projectDirectory);
        projectRootDirectories.add(dir);
        return projectDirectory;
    }

    private MakeConfiguration createDefaultMakefileConfiguration(File projectDirectory) {
        MakeConfiguration conf = new MakeConfiguration(projectDirectory.getPath(), DEFAULT_CONF_NAME, MakeConfiguration.TYPE_APPLICATION);
        setupMakefileConfiguration(conf);
        return conf;
    }
    
    private MakeConfiguration createDebugMakefileConfiguration(File projectDirectory) {
        MakeConfiguration conf = new MakeConfiguration(projectDirectory.getPath(), DEBUG_CONF_NAME, MakeConfiguration.TYPE_APPLICATION);
        setupMakefileConfiguration(conf);
        return conf;
    }
    
    private MakeConfiguration setupMakefileConfiguration(MakeConfiguration conf) {
        // Device and Header
        conf.getDevice().setValue((String) wizardDescriptor.getProperty(WizardProperty.DEVICE.key()));
        conf.getHeader().setValue((String) wizardDescriptor.getProperty(WizardProperty.HEADER.key()));

        // Platform
        if (wizardDescriptor.getProperty(WizardProperty.PLUGINBOARD.key()) instanceof DeviceSupport.PluginBoard) {
            conf.getPluginBoard().setValue(((DeviceSupport.PluginBoard) wizardDescriptor.getProperty(WizardProperty.PLUGINBOARD.key())).getName());
        } else {
            conf.getPluginBoard().setValue((String) wizardDescriptor.getProperty(WizardProperty.PLUGINBOARD.key()));
        }
        conf.getPlatformTool().setValue((String) wizardDescriptor.getProperty(WizardProperty.PLATFORM_TOOL_META_ID.key()));
        conf.getPlatformToolSN().setValue((String) wizardDescriptor.getProperty(WizardProperty.PLATFORM_TOOL_SERIAL.key()));

        // Toolchain
        conf.getLanguageToolchain().setMetaID(new StringConfiguration(null, languageToolchain.getMeta().getID()));
        conf.getLanguageToolchain().setDir(new StringConfiguration(null, languageToolchain.getDirectory()));
        conf.getLanguageToolchain().setVersion(new StringConfiguration(null, languageToolchain.getVersion()));
        
        return conf;
    }

    //  TODO: Refactor this method. It is too long and contains too much business logic.
    private void importChipKitProjectFiles(MakeProject newProject) throws IOException, InterruptedException {
        MakeConfigurationBook newProjectDescriptor = MakeConfigurationBook.getMakeConfigurationDescriptor(newProject);

        boolean copyFiles = (boolean) wizardDescriptor.getProperty(COPY_CHIPKIT_FILES.key());
        File targetProjectDir = (File) wizardDescriptor.getProperty(PROJECT_DIR.key());
        File sourceProjectDir = (File) wizardDescriptor.getProperty(SOURCE_PROJECT_DIR.key());
        ChipKitBoardConfigNavigator boardConfigNavigator = (ChipKitBoardConfigNavigator) wizardDescriptor.getProperty(CHIPKIT_BOARD_CONFIG_NAVIGATOR.key());
        String boardId = (String) wizardDescriptor.getProperty(CHIPKIT_BOARD_ID.key());
        File arduinoInstallDir = (File) wizardDescriptor.getProperty(ARDUINO_DIR.key());

        GCCToolFinder toolFinder = new GCCToolFinder(newProject.getActiveConfiguration().getLanguageToolchain().findToolchain());
        ArduinoConfig arduinoPathResolver = ArduinoConfig.getInstance();

        ArduinoBuilderRunner arduinoBuilderRunner = new ArduinoBuilderRunner(
            toolFinder, arduinoPathResolver, arduinoInstallDir.toPath(), (m) -> LOGGER.info(m)
        );
        
        BootloaderPathProvider bootloaderPathProvider = new BootloaderPathProvider( (filename) -> {
            File hexFile = InstalledFileLocator.getDefault().locate("bootloaders/" + filename, "com.microchip.mplab.nbide.embedded.chipkit", false);
            return hexFile.toPath();
        });        
        
        File customLinkerScriptsDir = InstalledFileLocator.getDefault().locate("linker_scripts", "com.microchip.mplab.nbide.embedded.chipkit", false);
        Path customLdScriptsDirectoryPath = customLinkerScriptsDir.toPath();

        ChipKitProjectImporter importer = new ChipKitProjectImporter();
        importer.setCopyingFiles( copyFiles );
        importer.setBoardConfigNavigator( boardConfigNavigator );
        importer.setBoardId( boardId );
        importer.setSourceProjectDirectoryPath( sourceProjectDir.toPath() );
        importer.setTargetProjectDirectoryPath( targetProjectDir.toPath() );
        importer.setArduinoBuilderRunner( arduinoBuilderRunner );
        importer.setBootloaderPathProvider( bootloaderPathProvider );
        importer.setCustomLdScriptsPath( customLdScriptsDirectoryPath );
        importer.execute();

        // This will be used to display either the short "how-to" guide or the longer one:
        multiConfigBoard = importer.isCustomLdScriptBoard();
        
        ChipKitBoardConfig boardConfig = importer.getChipKitBoardConfig();

        // Create chipKit Core Logical Folder
        Folder chipkitCoreFolder = newProjectDescriptor.getLogicalFolders().addNewFolder(
            ChipKitProjectImporter.CORE_DIRECTORY_NAME,
            "chipKIT Core",
            false,
            Folder.Kind.SOURCE_LOGICAL_FOLDER
        );
        importer.getChipKitCoreFilePaths().forEach(
            p -> {
                if (copyFiles) {
                    addFileToFolder(chipkitCoreFolder, p, importer.getChipKitCoreDirectoryPath());
                } else {
                    addFileToFolder(chipkitCoreFolder, p, boardConfig.getCoreDirPath(), boardConfig.getVariantDirPath());
                }
            }
        );

        // Create chipKit Libraries Logical Folder
        Folder chipkitLibrariesFolder = newProjectDescriptor.getLogicalFolders().addNewFolder(
            ChipKitProjectImporter.LIBRARIES_DIRECTORY_NAME,
            "chipKIT Libraries",
            true,
            Folder.Kind.SOURCE_LOGICAL_FOLDER
        );
        if (copyFiles) {
            importer.getMainLibraryFilePaths().forEach(p -> addFileToFolder(chipkitLibrariesFolder, p, importer.getLibraryDirectoryPath()));
        } else {
            Set<Path> libraryRootPaths = new HashSet<>();
            importer.getMainLibraryDirPaths().forEach(p -> libraryRootPaths.add(p.getParent()));
            importer.getMainLibraryFilePaths().forEach(p -> addFileToFolder(chipkitLibrariesFolder, p, libraryRootPaths.toArray(new Path[libraryRootPaths.size()])));
        }

        // Add source files
        Folder sourceFolder = newProjectDescriptor.getLogicalFolders().findFolderByName(MakeConfigurationBook.SOURCE_FILES_FOLDER);
        if (copyFiles) {
            importer.getSourceFilePaths().forEach((p) -> {            
                addFileToFolder(sourceFolder, p, importer.getSourceFilesDirectoryPath());
            });
        }

        newProjectDescriptor.addSourceRoot(copyFiles ? ChipKitProjectImporter.SOURCE_FILES_DIRECTORY_NAME : importer.getSourceProjectDirectoryPath().toString());

        if (!copyFiles) {
            Folder sketchSourceFolder = newProjectDescriptor.getLogicalFolders().addNewFolder(
                "sketchSource",
                "Sketch Source",
                false,
                Folder.Kind.IMPORTANT_FILES_FOLDER
            );
            importer.getSourceFilePaths().forEach((p) -> {
                if (p.toString().endsWith(".ino")) {
                    addFileToFolder(sketchSourceFolder, p);
                }
            });

            Folder generatedFolder = sourceFolder.addNewFolder(
                "generated",
                "generated",
                true,
                Folder.Kind.SOURCE_LOGICAL_FOLDER
            );
            importer.getPreprocessedSourceFilePaths().forEach((p) -> {
                addFileToFolder(generatedFolder, p, importer.getPreprocessedSketchDirectoryPath());
            });

            final String arduinoBuilderCommand = importer.getPreprocessingCommand() + " > preprocess.log";  // Redirecting Arduino Builder output to a log file
            
            newProjectDescriptor.getConfs().getConfigurtions().forEach( c -> {
                MakeConfiguration mc = (MakeConfiguration) c;
                mc.getMakeCustomizationConfiguration().setPreBuildStep( arduinoBuilderCommand );
                mc.getMakeCustomizationConfiguration().setApplyPreBuildStep(true);
            });
            
        }

        // Add bootloader .hex file: 
        if ( importer.hasBootloaderPath() ) {
            String loadableItemPath = importer.getProductionBootloaderPath().toString();
            if (PathPanel.getMode() == PathPanel.REL_OR_ABS) {
                loadableItemPath = CndPathUtilities.toAbsoluteOrRelativePath( newProjectDescriptor.getBaseDirFileObject(), loadableItemPath );
            } else if (PathPanel.getMode() == PathPanel.REL) {
                loadableItemPath = CndPathUtilities.toRelativePath( newProjectDescriptor.getBaseDirFileObject(), loadableItemPath );
            }
            loadableItemPath = FilePathAdaptor.normalize(loadableItemPath);
            LoadableItem newLoadableItem = new LoadableItem.FileItem( loadableItemPath );
            newProjectDescriptor.addLoadableItem(newLoadableItem);
        } else if ( !importer.isCustomLdScriptBoard() ) {  // If the board uses a custom .ld script, it is supposed not to use a bootloader
            LOGGER.log(Level.WARNING, "Could not find a bootloader file for device {0}", boardId);
        }
        
        // Set auxiliary configuration options
        Set<String> cppAppendOptionsSet = boardConfig.getExtraOptionsCPP();
        
        boolean cppExceptions = !cppAppendOptionsSet.remove("-fno-exceptions");
        
        String cppAppendOptions = String.join(" ", cppAppendOptionsSet);

        Path projectPath = Paths.get(targetProjectDir.getAbsolutePath());
        Stream<Path> mainLibraryDirPaths = importer.getMainLibraryDirPaths();
        StringBuilder includesBuilder = new StringBuilder();
        if (copyFiles) {
            includesBuilder.append(ChipKitProjectImporter.CORE_DIRECTORY_NAME);
        } else {
            includesBuilder.append(boardConfig.getCoreDirPath())
                    .append(";")
                    .append(boardConfig.getVariantDirPath());
        }
        mainLibraryDirPaths.forEach(path -> {
            includesBuilder.append(";").append(copyFiles ? projectPath.relativize(path) : path.toAbsolutePath());
            Path utilityPath = path.resolve("utility");
            if ( Files.exists(utilityPath) ) {
                includesBuilder.append(";").append(copyFiles ? projectPath.relativize(utilityPath) : utilityPath.toAbsolutePath());
            }
        });

        
        String preprocessorMacros = boardConfig.getCompilerMacros();
        String ldOptions = String.join( " ", boardConfig.getExtraOptionsLD(false, copyFiles) );
        String ldDebugOptions = String.join( " ", boardConfig.getExtraOptionsLD(true, copyFiles) );
        String ldAppendOptions = "-L" + ChipKitProjectImporter.CORE_DIRECTORY_NAME + ",-l" + LibCoreBuilder.LIB_CORE_NAME;
        String cAppendOptions = String.join(" ", boardConfig.getExtraOptionsC());
        
        newProjectDescriptor.getConfs().getConfigurtions().forEach( c-> {
            MakeConfiguration mc = (MakeConfiguration) c;
            setAuxOptionValue(mc, "C32Global", "common-include-directories", includesBuilder.toString());
            setAuxOptionValue(mc, "C32Global", "legacy-libc", "false");
            setAuxOptionValue(mc, "C32", "preprocessor-macros", preprocessorMacros);
            setAuxOptionValue(mc, "C32", "optimization-level", DEFAULT_OPTIMIZATION_OPTION );
            setAuxOptionValue(mc, "C32CPP", "preprocessor-macros", preprocessorMacros);
            setAuxOptionValue(mc, "C32CPP", "optimization-level", DEFAULT_OPTIMIZATION_OPTION );
            setAuxOptionValue(mc, "C32CPP", "exceptions", Boolean.toString(cppExceptions));
            setAuxOptionValue(mc, "C32-LD", "oXC32ld-extra-opts", c.getName().equals(DEBUG_CONF_NAME) ? ldDebugOptions : ldOptions );
            setAuxOptionValue(mc, "C32-LD", "remove-unused-sections", "true");
            setAppendixValue(mc, "C32", cAppendOptions);
            setAppendixValue(mc, "C32CPP", cppAppendOptions);
            setAppendixValue(mc, "C32-LD", ldAppendOptions);
        });
        
        // Create chipKIT properties file:
        Properties chipKitProperties = new Properties();
        chipKitProperties.setProperty("path", importer.getBoardConfigNavigator().getChipKitHardwarePath().toString());
        chipKitProperties.setProperty("pic32prog", importer.getBoardConfigNavigator().getPic32ProgPath().toString());
        Path chipKitPropsFilePath = Paths.get( newProjectDescriptor.getProjectDir(), "nbproject", CHIPKIT_PROPERTIES_FILENAME );
        Files.createFile( chipKitPropsFilePath );
        PrintWriter printWriter = new PrintWriter( chipKitPropsFilePath.toFile() );
        chipKitProperties.store( printWriter, null );
        
    }

    private void addFileToFolder(Folder folder, Path filePath, Path... rootPaths) {
        if ( filePath == null ) return;
        FileObject fileObject = FileUtil.toFileObject(filePath.toFile());
        Path projectRootPath = Paths.get( folder.getConfigurationDescriptor().getBaseDir() );
        if (rootPaths != null && rootPaths.length > 0) {
            for (Path rootPath : rootPaths) {
                if (!filePath.startsWith(rootPath)) {
                    continue;
                }
                // Create subdirectories if necessary:
                Path relativePath = rootPath.relativize(filePath);
                if (relativePath.getNameCount() > 1) {
                    for (int i = 0; i < relativePath.getNameCount() - 1; i++) {
                        String subfolderName = relativePath.getName(i).toString();
                        boolean subfolderAlreadyExists = false;
                        for (Folder f : folder.getFoldersAsArray()) {
                            if (f.getDisplayName().equals(subfolderName)) {
                                folder = f;
                                subfolderAlreadyExists = true;
                                break;
                            }
                        }
                        if (!subfolderAlreadyExists) {
                            folder = folder.addNewFolder(subfolderName, subfolderName, true, Folder.Kind.SOURCE_LOGICAL_FOLDER);
                        }
                    }
                    break;
                }
            }
            if ( filePath.startsWith(projectRootPath) ) {
                String relativePath = FileUtil.getRelativePath( FileUtil.toFileObject(projectRootPath.toFile()), fileObject );
                folder.addItem( new Item(fileObject, relativePath) );
            } else {
                folder.addItem( new Item(fileObject, filePath.toString()) );
            }
        } else {
            folder.addItem( new Item(fileObject, filePath.toString()) );
        }
    }

    private void setupProjectEncoding(MakeProject newProject) {
        Object encoding = wizardDescriptor.getProperty(WizardProperty.PROJECT_ENCODING.key());
        if (encoding != null && encoding instanceof Charset) {
            newProject.setSourceEncoding(((Charset) encoding).name());
        }
    }

    private void setAuxOptionValue(MakeConfiguration makeConf, String confItemId, String propertyKey, String propertyValue) {        
        OptionConfiguration conf = (OptionConfiguration) makeConf.getAuxObject(confItemId);
        conf.setProperty(propertyKey, propertyValue);
        conf.markChanged();        
    }

    private void setAppendixValue(MakeConfiguration makeConf, String confItemId, String value) {        
        OptionConfiguration conf = (OptionConfiguration) makeConf.getAuxObject(confItemId);
        conf.setAppendix(value);
        conf.markChanged();        
    }

}
