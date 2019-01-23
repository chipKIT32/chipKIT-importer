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

package com.microchip.mplab.nbide.embedded.arduino.wizard;

import com.microchip.crownking.opt.Version;
import com.microchip.mplab.mdbcore.MessageMediator.ActionList;
import com.microchip.mplab.mdbcore.MessageMediator.DialogBoxType;
import com.microchip.mplab.mdbcore.MessageMediator.Message;
import com.microchip.mplab.mdbcore.MessageMediator.MessageMediator;
import com.microchip.mplab.nbide.embedded.api.ui.TypeAheadComboBox;
import com.microchip.mplab.nbide.embedded.arduino.importer.ArduinoConfig;
import com.microchip.mplab.nbide.embedded.arduino.importer.Platform;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

import com.microchip.mplab.nbide.embedded.makeproject.ui.wizards.SelectProjectInfoPanel;
import javax.swing.JTextField;

import static com.microchip.mplab.nbide.embedded.makeproject.api.wizards.WizardProperty.*;
import static com.microchip.mplab.nbide.embedded.arduino.wizard.ImportWizardProperty.*;
import static com.microchip.mplab.nbide.embedded.arduino.importer.Requirements.MINIMUM_ARDUINO_VERSION;
import com.microchip.mplab.nbide.embedded.arduino.importer.Board;
import com.microchip.mplab.nbide.embedded.arduino.importer.BoardConfiguration;
import com.microchip.mplab.nbide.embedded.arduino.importer.PlatformFactory;
import com.microchip.mplab.nbide.embedded.arduino.utils.ArduinoProjectFileFilter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.filechooser.FileFilter;
import org.openide.util.Exceptions;


// TODO: Introduce more Optional return types
public class ProjectSetupStep implements WizardDescriptor.Panel<WizardDescriptor> {

    
    private static final String MAKEFILE_NAME = "Makefile";   // NOI18N
    
    private final Set<ChangeListener> listeners = new HashSet<>();
    private Map<String, String> boardIdLookup = new HashMap<>();
    private final ArduinoConfig arduinoConfig;
    private final PlatformFactory platformFactory;
    private final MPLABDeviceAssistant deviceAssistant;
    
    private List<Platform> allPlatforms;
    private Platform currentPlatform;
    private Board board;
    private WizardDescriptor wizardDescriptor;
    private ProjectSetupPanel view;
    
    
    public ProjectSetupStep( ArduinoConfig arduinoConfig, PlatformFactory platformFactory, MPLABDeviceAssistant deviceAssistant ) {
        this.arduinoConfig = arduinoConfig;
        this.platformFactory = platformFactory;
        this.deviceAssistant = deviceAssistant;
    }

    @Override
    public Component getComponent() {
        if (view == null) {
            view = new ProjectSetupPanel(this);
            try {
                allPlatforms = new ArrayList<>(platformFactory.getAllPlatforms(arduinoConfig.getSettingsPath()));
                Collections.sort(allPlatforms, (Platform p1, Platform p2) -> p1.getDisplayName().orElse("").compareTo(p2.getDisplayName().orElse("")));
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return view;
    }

    @Override
    public HelpCtx getHelp() {
        return new HelpCtx("56f8deKxLuo_628366");
    }

    @Override
    public boolean isValid() {
        Boolean overwriteExistingProject = (Boolean) wizardDescriptor.getProperty(OVERWRITE_EXISTING_PROJECT.key());

        if (overwriteExistingProject == null) {
            overwriteExistingProject = false;
        }

        if (!isSourceProjectValid()) {
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, NbBundle.getMessage(ProjectSetupPanel.class, "MSG_IllegalSourceProject"));
            return false;
        }
        
        if (!isArduinoVersionValid()) {
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, NbBundle.getMessage(ProjectSetupPanel.class, "MSG_IllegalArduinoVersion"));
            return false;
        }
        
        if ( currentPlatform == null ) {        
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, NbBundle.getMessage(ProjectSetupPanel.class, "MSG_ChipKitNotFound"));
            return false;
        }
        
        if (!isPlatformDirectoryValid()) {
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, NbBundle.getMessage(ProjectSetupPanel.class, "MSG_IllegalPlatformDirectory"));
            return false;
        }
        
        if (!isBoardValid()) {
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, NbBundle.getMessage(ProjectSetupPanel.class, "MSG_UnknownArduinoBoard"));
            return false;
        }
        
        if (!isToolchainValid()) {
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, NbBundle.getMessage(ProjectSetupPanel.class, "MSG_NoMatchingToolchainFound"));
            return false;
        }

        if (!isValidProjectName()) {
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, NbBundle.getMessage(ProjectSetupPanel.class, "MSG_IllegalProjectName"));
            return false;
        }

        File f = new File( readLocationStringFromField(view.targetProjectLocationField) ).getAbsoluteFile();
        if (getCanonicalFile(f) == null) {
            String message = NbBundle.getMessage(ProjectSetupPanel.class, "MSG_IllegalProjectLocation");
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, message);
            return false;
        }
        File destFolder = getCanonicalFile( new File( readLocationStringFromField(view.projectDirectoryField) ).getAbsoluteFile());
        if (destFolder == null) {
            String message = NbBundle.getMessage(ProjectSetupPanel.class, "MSG_ProjectFolderIllegal");
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, message);
            return false;
        }

        File projLoc = getCanonicalFile(new File( readLocationStringFromField( view.targetProjectLocationField ) ).getAbsoluteFile());
        if (projLoc == null) {
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, NbBundle.getMessage(ProjectSetupPanel.class, "MSG_ProjectFolderReadOnly"));
            return false;
        }

        if (destFolder.exists()) {
            if (destFolder.isFile()) {
                wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, NbBundle.getMessage(ProjectSetupPanel.class, "MSG_NotAFolder", MAKEFILE_NAME));
                return false;
            }
            if (new File(destFolder.getPath() + File.separator + MAKEFILE_NAME).exists() && !overwriteExistingProject) {
                // Folder exists and is not empty
                wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, NbBundle.getMessage(ProjectSetupPanel.class, "MSG_MakefileExists", MAKEFILE_NAME));
                return false;
            }
            File nbProj = new File(destFolder.getPath() + File.separator + "nbproject");
            if (nbProj.exists() && nbProj.listFiles().length != 0 && !overwriteExistingProject) {
                // Folder exists and is not empty
                wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, NbBundle.getMessage(ProjectSetupPanel.class, "MSG_ProjectFolderExists"));
                return false;
            }
        }

        if (validatePathLength(destFolder) == false) {
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, NbBundle.getMessage(ProjectSetupPanel.class, "MSG_ErrorProjectNamePathTooLong"));
            return false;
        }
        
        // Set the error message to null if there is no warning message to display
        if (wizardDescriptor.getProperty(WizardDescriptor.PROP_WARNING_MESSAGE) == null) {
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, null);
        }

        return true;
    }

    @Override
    public final void addChangeListener(ChangeListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    @Override
    public final void removeChangeListener(ChangeListener l) {
        synchronized (listeners) {
            listeners.remove(l);// NOI18N
        }
    }    

    @Override
    public void readSettings(WizardDescriptor settings) {
        wizardDescriptor = settings;

        // Source Project Location
        File lastSourceProjectLocation = (File) wizardDescriptor.getProperty(SOURCE_PROJECT_DIR.key());
        if (lastSourceProjectLocation == null) {
            String loc = NbPreferences.forModule(SelectProjectInfoPanel.class).get(LAST_SOURCE_PROJECT_LOCATION.key(), null);
            if (loc == null) {
                loc = System.getProperty("user.home");
            }
            lastSourceProjectLocation = new File(loc);
        }
        view.sourceProjectLocationField.setText(lastSourceProjectLocation.getAbsolutePath());
        
        // Target Project Location
        File lastTargetProjectLocation = (File) wizardDescriptor.getProperty(PROJECT_LOCATION.key());
        if (lastTargetProjectLocation == null) {
            String loc = NbPreferences.forModule(SelectProjectInfoPanel.class).get(LAST_PROJECT_LOCATION.key(), null);
            if (loc == null) {
                loc = System.getProperty("netbeans.projects.dir");
            }
            if (loc == null) {
                loc = System.getProperty("user.home");
            }
            lastTargetProjectLocation = new File(loc);
        }
        view.targetProjectLocationField.setText(lastTargetProjectLocation.getAbsolutePath());        
                
        // Platform Location
        File platformCoreDir = (File) wizardDescriptor.getProperty(ARDUINO_PLATFORM_DIR.key());
        if (platformCoreDir == null) {
            String lastPlatformCoreLocation = NbPreferences.forModule(SelectProjectInfoPanel.class).get(LAST_ARDUINO_PLATFORM_LOCATION.key(), null);
            if ( lastPlatformCoreLocation != null && Files.exists( Paths.get(lastPlatformCoreLocation) ) ) {
                platformCoreDir = new File(lastPlatformCoreLocation);
            }
        }
        if (platformCoreDir != null) {
            view.platformLocationField.setText( platformCoreDir.getAbsolutePath() );
            resolvePlatformFromPath();
        }
        
        // Platform        
        if (currentPlatform == null) {
            try {            
                List<Platform> allPlatforms = new PlatformFactory().getAllPlatforms( arduinoConfig.getSettingsPath() );
                currentPlatform = allPlatforms.stream().filter(p -> p.getVendor().contains("chipKIT") ).findFirst().orElse(null);
                if ( currentPlatform != null ) {
                    view.platformLocationField.setText( currentPlatform.getRootPath().toString() );
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        
        // Target Device:
        String boardName = (String) wizardDescriptor.getProperty(BOARD_NAME.key());
        loadBoardsToCombo();
        if (boardName == null) {
            boardName = NbPreferences.forModule(SelectProjectInfoPanel.class).get(BOARD_NAME.key(), null);
        }
        if (boardName != null) {
            if ( boardIdLookup.containsKey(boardName) ) {
                view.boardCombo.setSelectedItem(boardName);
                updateBoard();
            } else {
                boardName = null;
            }
        }

        // Copy all dependencies:
        Object copyDependencies = wizardDescriptor.getProperty(COPY_CORE_FILES.key());
        view.copyDependenciesCheckBox.setSelected( copyDependencies != null ? (boolean) copyDependencies : true);
        
        // Target Project Directory:
        setTargetProjectDirectoryField();
    }

    @Override
    public void storeSettings(WizardDescriptor settings) {
        String projectName = readLocationStringFromField( view.projectNameField );
        String sourceProjectDir = readLocationStringFromField( view.sourceProjectLocationField );
        String platformDir = readLocationStringFromField(view.platformLocationField );
        String boardName = readSelectedValueFromComboBox(view.boardCombo);        
        String targetLocation = readLocationStringFromField( view.targetProjectLocationField );
        String targetDir = readLocationStringFromField( view.projectDirectoryField );
        boolean copyCoreFiles = view.copyDependenciesCheckBox.isSelected();

        settings.putProperty(SOURCE_PROJECT_DIR.key(), new File(sourceProjectDir));
        settings.putProperty(ARDUINO_DIR.key(), arduinoConfig.findInstallPath().get().toFile() );
        settings.putProperty(ARDUINO_PLATFORM.key(), currentPlatform );
        settings.putProperty(ARDUINO_PLATFORM_DIR.key(), new File(platformDir));
        settings.putProperty(BOARD_NAME.key(), boardName);
        settings.putProperty(BOARD.key(), board);
        
        if ( !board.hasOptions() ) {
            deviceAssistant.storeSettings(settings);
            settings.putProperty(BOARD_CONFIGURATION.key(), new BoardConfiguration(board));
        }
        
        settings.putProperty(COPY_CORE_FILES.key(), copyCoreFiles);
                
        settings.putProperty(DEVICE_HEADER_PRESENT.key(), false);
        settings.putProperty(PLUGIN_BOARD_PRESENT.key(), false);

        settings.putProperty(PROJECT_DIR.key(), new File(targetDir));
        settings.putProperty(PROJECT_NAME.key(), projectName);
        settings.putProperty(MAKE_FILENAME.key(), MAKEFILE_NAME);

        File projectsDir = new File(targetLocation);
        if (projectsDir.isDirectory()) {
            ProjectChooser.setProjectsFolder(projectsDir);
        }

        settings.putProperty(PROJECT_LOCATION.key(), projectsDir);

        //settings.putProperty(SET_AS_MAIN.key(), view.mainProjectCheckBox.isSelected());  // This does not seem to work
        settings.putProperty(MAIN_CLASS.key(), null);

        settings.putProperty(CREATE_MAIN_FILE.key(), Boolean.FALSE);
        settings.putProperty(MAIN_FILENAME.key(), null);
        settings.putProperty(MAIN_FILE_TEMPLATE.key(), null);

        NbPreferences.forModule(SelectProjectInfoPanel.class).put(LAST_PROJECT_LOCATION.key(), projectsDir.getAbsolutePath());
        NbPreferences.forModule(SelectProjectInfoPanel.class).put(LAST_SOURCE_PROJECT_LOCATION.key(), new File(sourceProjectDir).getParent());
        NbPreferences.forModule(SelectProjectInfoPanel.class).put(LAST_ARDUINO_PLATFORM.key(), currentPlatform.getVendor() + ":" + currentPlatform.getArchitecture() );
        NbPreferences.forModule(SelectProjectInfoPanel.class).put(LAST_ARDUINO_PLATFORM_LOCATION.key(), platformDir);
        NbPreferences.forModule(SelectProjectInfoPanel.class).put(BOARD_NAME.key(), boardName);
    }

    
    //**************************************************
    //*************** EVENT LISTENERS ******************
    //**************************************************
    void overwriteCheckBoxActionPerformed(ActionEvent evt) {
        if (view.overwriteCheckBox.isSelected()) {
            MessageMediator mandm = Lookup.getDefault().lookup(MessageMediator.class);
            Message newMessage = new Message( NbBundle.getMessage(ProjectSetupPanel.class, "MSG_OverwriteConfirmationRequest"), "MPLAB X IDE", null, DialogBoxType.QUESTION_BLOCKING_YES_NO );
            int overwrite = mandm.handleMessage(newMessage, ActionList.DialogPopupOnly);
            if (overwrite != 0) {
                view.overwriteCheckBox.setSelected(false);
            }
        }
        wizardDescriptor.putProperty(OVERWRITE_EXISTING_PROJECT.key(), view.overwriteCheckBox.isSelected());
        fireChangeEvent();
    }

    void sourceProjectLocationButtonActionPerformed(ActionEvent evt) {
        showDirectoryChooser( view.sourceProjectLocationField, "DLG_SourceProjectLocation", new ArduinoProjectFileFilter() );
        String projectDir = readLocationStringFromField( view.sourceProjectLocationField );
        if (projectDir != null && !projectDir.isEmpty()) {
            view.projectNameField.setText(Paths.get(projectDir).getFileName().toString());
            setTargetProjectDirectoryField();
        }
        fireChangeEvent();
    }

    void targetProjectLocationButtonActionPerformed(ActionEvent evt) {
        showDirectoryChooser( view.targetProjectLocationField, "DLG_TargetProjectLocation" );
        setTargetProjectDirectoryField();
    }

    void platformLocationButtonActionPerformed(ActionEvent evt) {
        showDirectoryChooser(view.platformLocationField, "DLG_ArduinoCoreDirectory" );
        onPlatformLocationChanged();
    }
    
    void projectNameFieldKeyReleased(KeyEvent evt) {
        String projectName = view.projectNameField.getText().trim();
        if (projectName.endsWith("\\")
                || projectName.endsWith("/")
                || projectName.endsWith(File.separator + File.separator)
                || projectName.endsWith(":")
                || projectName.endsWith("*")
                || projectName.endsWith("?")
                || projectName.endsWith("\"")
                || projectName.endsWith("<")
                || projectName.endsWith(">")
                || projectName.endsWith("|")) {
            String newProjectName = projectName.substring(0, projectName.length() - 1);
            view.projectNameField.setText(newProjectName);
        }
        setTargetProjectDirectoryField();
    }

    void targetProjectLocationFieldKeyReleased(KeyEvent evt) {
        String projectLoc = view.projectDirectoryField.getText().trim();
        String badSlash = File.separator.equals(("/")) ? "\\" : "/";

        if (projectLoc.endsWith(badSlash)
                || projectLoc.endsWith(File.separator + File.separator)
                || projectLoc.endsWith("*")
                || projectLoc.endsWith("?")
                || projectLoc.endsWith("<")
                || projectLoc.endsWith(">")
                || projectLoc.endsWith("|")) {
            String newProjectLoc = projectLoc.substring(0, projectLoc.length() - 1);
            view.projectDirectoryField.setText(newProjectLoc);
        }
        setTargetProjectDirectoryField();
    }

    void platformLocationFieldFocusLost(FocusEvent evt) {
        onPlatformLocationChanged();
    }

    void arduinoLocationFieldFocusLost(FocusEvent evt) {
        fireChangeEvent();
    }

    void targetProjectLocationFieldFocusLost(FocusEvent evt) {
        fireChangeEvent();
    }

    void sourceProjectLocationFieldFocusLost(FocusEvent evt) {
        fireChangeEvent();
    }
    
    void boardComboItemStateChanged(ItemEvent evt) {
        updateBoard();
        fireChangeEvent();
    }

    
    //**************************************************
    //*************** PRIVATE METHODS ******************
    //**************************************************
    private String readLocationStringFromField( JTextField field ) {
        return field.getText().replaceAll("[*?\\\"<>|]", "").trim();
    }
    
    private String readSelectedValueFromComboBox( JComboBox<String> comboBox ) {
        String value = (String) comboBox.getSelectedItem();
        if ( value != null ) {
            return value.trim();
        } else {
            return null;
        }
    }
    
    private File showDirectoryChooser(JTextField pathField, String dialogTitleKey) {
        return showDirectoryChooser(pathField, dialogTitleKey, null);
    }
    
    private File showDirectoryChooser(JTextField pathField, String dialogTitleKey, FileFilter fileFilter) {
        String startDir = readLocationStringFromField(pathField);
        
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(null);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setFileFilter( fileFilter );
        if (startDir.length() > 0) {
            File f = new File(startDir);
            if (f.exists()) {
                if (f.isFile()) {
                    chooser.setCurrentDirectory(f.getParentFile());
                } else if (f.isDirectory()) {
                    chooser.setCurrentDirectory(f);
                }
            }
        } else {
            File f = new File(System.getProperty("netbeans.projects.dir"));	// NOI18N
            if (f.exists()) {
                chooser.setCurrentDirectory(f);
            }
        }
        
        chooser.setDialogTitle( NbBundle.getMessage(ProjectSetupPanel.class, dialogTitleKey) );
        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(view)) { //NOI18N
            File selectedFile = chooser.getSelectedFile();
            File selectedDir = selectedFile.isDirectory() ? selectedFile : selectedFile.getParentFile();
            pathField.setText(selectedDir.getAbsolutePath());
            return selectedDir;
        }
        return null;
    }

    private boolean validatePathLength(File projFolder) {
        // Windows is the only known operating system with a relatively short (260) limitation on path length that can cause issues during the make process
        if (System.getProperty("os.name").contains("Windows")) {
            int workingLength = projFolder.getAbsolutePath().length();
            workingLength += projFolder.getName().length();
            workingLength += "/dist/".length();				// NOI18N
            workingLength += "default".length() + 1;			// NOI18N
            workingLength += 2 * ("production".length() + 1);		// NOI18N
            workingLength += 4;						// extension name and .

            if (workingLength < 259) {
                if (workingLength >= 130) {
                    wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, null);
                    wizardDescriptor.putProperty(WizardDescriptor.PROP_WARNING_MESSAGE, NbBundle.getMessage(ProjectSetupPanel.class, "MSG_WarningProjectNamePathTooLong"));
                } else {
                    wizardDescriptor.putProperty(WizardDescriptor.PROP_WARNING_MESSAGE, null);
                }

                return true;
            } else {
                return false;
            }

        } else {
            return true;
        }

    }

    private boolean isSourceProjectValid() {
        File sourceProjectDir = new File( readLocationStringFromField(view.sourceProjectLocationField ) );
        if (!sourceProjectDir.exists()) {
            return false;
        }
        if (!sourceProjectDir.isDirectory()) {
            return false;
        }

        for (String f : sourceProjectDir.list()) {
            if (f.endsWith(".ino")) {
                return true;
            }
        }

        return false;
    }
    
    private boolean isArduinoVersionValid() {
        return arduinoConfig.isCurrentVersionValid( new Version(MINIMUM_ARDUINO_VERSION) );
    }

    private boolean isPlatformDirectoryValid() {
        Path p = Paths.get( readLocationStringFromField(view.platformLocationField ) );
        return platformFactory.isValidPlatformRootPath(p);
    }
    
    private boolean isBoardValid() {
        return board != null;
    }
    
    private boolean isToolchainValid() {        
        return board.hasOptions() ? true : deviceAssistant.isToolchainValid();
    }
    
    private boolean isValidProjectName() {
        String projectName = view.projectNameField.getText().trim();
        // unix allows a lot of strange names, but let's prohibit this for project
        // using symbols invalid on Windows
        if (projectName.length() == 0 || projectName.startsWith(" ")
                || projectName.contains("\\")
                || projectName.contains("/")
                || projectName.contains(":")
                || projectName.contains("*")
                || projectName.contains("?")
                || projectName.contains("\"")
                || projectName.contains("<")
                || projectName.contains(">")
                || projectName.contains("|")) {

            return false;
        }

        // check ability to create file with specified name on target OS
        boolean ok = false;
        try {
            File file = File.createTempFile(projectName + "dummy", "");
            ok = true;
            file.delete();
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }

        return ok;
    }

    private void setTargetProjectDirectoryField() {
        String targetProjectLocation = readLocationStringFromField( view.targetProjectLocationField );
        String projName = view.projectNameField.getText().trim();
        view.projectDirectoryField.setText( Paths.get( targetProjectLocation, projName + ".X" ).toAbsolutePath().toString() );
        checkForExistingProject();
        fireChangeEvent();
    }

    private void onPlatformLocationChanged() {
        resolvePlatformFromPath();
        loadBoardsToCombo();        
        fireChangeEvent();
    }
    
    private void resolvePlatformFromPath() {
        Path p = Paths.get( readLocationStringFromField(view.platformLocationField) );
        if ( platformFactory.isValidPlatformRootPath(p) ) {
            try {
                currentPlatform = platformFactory.createPlatformFromRootDirectory(p);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        loadBoardsToCombo();
    }
    
    private void checkForExistingProject() {
        //if project already exists, enable the check box
        if ("".equalsIgnoreCase(view.projectNameField.getText().trim())) {
            view.overwriteCheckBox.setEnabled(false);
        } else {
            File projFolder = FileUtil.normalizeFile( new File( readLocationStringFromField(view.projectDirectoryField) ) );
            FileObject dirFO = FileUtil.toFileObject(projFolder);
            Project proj = null;
            if (dirFO != null) {
                try {
                    proj = ProjectManager.getDefault().findProject(dirFO);
                } catch (IOException | IllegalArgumentException ex) {
                    view.overwriteCheckBox.setEnabled(false);
                }
            }
            if (proj == null) {
                view.overwriteCheckBox.setEnabled(false);
            } else {
                view.overwriteCheckBox.setEnabled(true);
            }
        }
    }

    private void loadBoardsToCombo() {
        String currentlySelectedBoardName = (view.boardCombo.getSelectedItem() != null) ? view.boardCombo.getSelectedItem().toString() : null;
        boardIdLookup = currentPlatform.getBoardNamesToIDsLookup();
        List<String> boardNames = new ArrayList<>(boardIdLookup.keySet());
        // Sort the board names list in alphabetical order:
        Collections.sort(boardNames);
        // Set up the combo box:
        DefaultComboBoxModel<String> cbm = new DefaultComboBoxModel<>(boardNames.toArray(new String[boardNames.size()]));
        view.boardCombo.setModel(cbm);
        // TODO: Verify whether calling TypeAheadComboBox.enable many times does not have adverse effects
        TypeAheadComboBox.enable(view.boardCombo);
        if ( currentlySelectedBoardName != null && boardNames.contains(currentlySelectedBoardName) ) {
            view.boardCombo.setSelectedItem(currentlySelectedBoardName);
        }
    }
    
    private void updateBoard() {
        String boardName = readSelectedValueFromComboBox(view.boardCombo);
        String boardId = boardIdLookup.get(boardName);
        board = currentPlatform.getBoard(boardId).orElseThrow( () -> new RuntimeException("Failed to find a board with id: \""+boardId+"\""));
        if ( !board.hasOptions() ) {
            deviceAssistant.updateDeviceAndToolchain( new BoardConfiguration(board) );
        }
    }
    
    private void fireChangeEvent() {
        Iterator<ChangeListener> it;
        synchronized (listeners) {
            it = new HashSet<>(listeners).iterator();
        }
        ChangeEvent ev = new ChangeEvent(this);
        while (it.hasNext()) {
            it.next().stateChanged(ev);
        }
    }

    private static File getCanonicalFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            // TODO: What should we do with this exception?
            return null;
        }
    }        

}
