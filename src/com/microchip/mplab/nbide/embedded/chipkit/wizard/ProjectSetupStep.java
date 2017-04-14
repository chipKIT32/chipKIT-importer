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

import com.microchip.crownking.opt.Version;
import com.microchip.mplab.mdbcore.MessageMediator.ActionList;
import com.microchip.mplab.mdbcore.MessageMediator.DialogBoxType;
import com.microchip.mplab.mdbcore.MessageMediator.Message;
import com.microchip.mplab.mdbcore.MessageMediator.MessageMediator;
import com.microchip.mplab.nbide.embedded.api.ui.TypeAheadComboBox;
import com.microchip.mplab.nbide.embedded.chipkit.importer.ArduinoConfig;
import com.microchip.mplab.nbide.embedded.chipkit.importer.ChipKitBoardConfigNavigator;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
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
import static com.microchip.mplab.nbide.embedded.chipkit.wizard.ChipKitImportWizardProperty.*;
import static com.microchip.mplab.nbide.embedded.chipkit.importer.Requirements.MINIMUM_ARDUINO_VERSION;
import com.microchip.mplab.nbide.embedded.makeproject.ui.wizards.WizardProjectConfiguration;
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

public class ProjectSetupStep implements WizardDescriptor.Panel<WizardDescriptor> {

    
    private static final String MAKEFILE_NAME = "Makefile";   // NOI18N
    
    private final Set<ChangeListener> listeners = new HashSet<>();
    private Map<String, String> chipKitBoardIdLookup = new HashMap<>();
    private final ArduinoConfig arduinoConfig;

    private ChipKitBoardConfigNavigator boardConfigNavigator;
    private WizardDescriptor wizardDescriptor;
    private ProjectSetupPanel view;

    public ProjectSetupStep( ArduinoConfig arduinoConfig ) {
        this.arduinoConfig = arduinoConfig;
    }

    @Override
    public Component getComponent() {
        if (view == null) {
            view = new ProjectSetupPanel(this);
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

        if (!isArduinoDirectoryValid()) {
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, NbBundle.getMessage(ProjectSetupPanel.class, "MSG_IllegalArduinoDirectory"));
            return false;
        }
        
        if (!isArduinoVersionValid()) {
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, NbBundle.getMessage(ProjectSetupPanel.class, "MSG_IllegalArduinoVersion"));
            return false;
        }
        
        if (!isChipKitDirectoryValid()) {
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, NbBundle.getMessage(ProjectSetupPanel.class, "MSG_IllegalChipKitDirectory"));
            return false;
        }
        
        if (!isBoardValid()) {
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, NbBundle.getMessage(ProjectSetupPanel.class, "MSG_UnknownChipKitBoard"));
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
        
        // Arduino Install Location
        File arduinoDir = (File) wizardDescriptor.getProperty(ARDUINO_DIR.key());
        if (arduinoDir == null) {
            String lastArduinoLocation = NbPreferences.forModule(SelectProjectInfoPanel.class).get(LAST_ARDUINO_LOCATION.key(), null);
            if ( lastArduinoLocation != null && Files.exists( Paths.get(lastArduinoLocation) ) ) {
                arduinoDir = new File(lastArduinoLocation);
            }
        }
        if (arduinoDir != null) {
            String currentArduinoLocation = readLocationStringFromField( view.arduinoLocationField );
            if ( !currentArduinoLocation.equals( arduinoDir.getAbsolutePath() ) ) {
                view.arduinoLocationField.setText(arduinoDir.getAbsolutePath());
            }
        }
        
        // chipKIT Core Location
        File chipKitCoreDir = (File) wizardDescriptor.getProperty(CHIPKIT_CORE_DIR.key());
        if (chipKitCoreDir == null) {
            String lastChipKitCoreLocation = NbPreferences.forModule(SelectProjectInfoPanel.class).get(LAST_CHIPKIT_CORE_LOCATION.key(), null);
            if ( lastChipKitCoreLocation != null && Files.exists( Paths.get(lastChipKitCoreLocation) ) ) {
                chipKitCoreDir = new File(lastChipKitCoreLocation);
            }
        }
        if (chipKitCoreDir != null) {
            view.chipkitCoreLocationField.setText(chipKitCoreDir.getAbsolutePath());
        } else {
            try {
                List<Path> chipKitCorePaths = ChipKitBoardConfigNavigator.findChipKitHardwareDirectories( arduinoConfig );
                view.chipkitCoreLocationField.setText( chipKitCorePaths.get(0).toString() );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
                view.chipkitCoreLocationField.setText("");
            }
        }
        
        // Target Device:
        String boardName = (String) wizardDescriptor.getProperty(CHIPKIT_BOARD_NAME.key());
        loadChipKitDevicesToCombo();
        if (boardName == null) {
            boardName = NbPreferences.forModule(SelectProjectInfoPanel.class).get(CHIPKIT_BOARD_NAME.key(), null);
        }
        if (boardName != null) {
            view.chipKitBoardCombo.setSelectedItem(boardName);
        }
        
        // Encoding:
        Object encodingCharset = wizardDescriptor.getProperty(PROJECT_ENCODING.key());
        if (encodingCharset != null) {
            view.encodingCombo.setSelectedItem( encodingCharset );
        }

        // Copy all chipKIT dependencies:
        Object copyChipKitFiles = wizardDescriptor.getProperty(COPY_CHIPKIT_FILES.key());
        view.copyChipKitFilesCheckBox.setSelected( copyChipKitFiles != null ? (boolean) copyChipKitFiles : true);
        
        // Target Project Directory:
        setTargetProjectDirectoryField();
    }

    @Override
    public void storeSettings(WizardDescriptor settings) {
        String projectName = readLocationStringFromField( view.projectNameField );
        String sourceProjectDir = readLocationStringFromField( view.sourceProjectLocationField );
        String arduinoDir = readLocationStringFromField( view.arduinoLocationField );
        String chipKitCoreDir = readLocationStringFromField( view.chipkitCoreLocationField );
        String boardName = readSelectedValueFromComboBox(view.chipKitBoardCombo);
        String boardId = chipKitBoardIdLookup.get(boardName);
        String targetLocation = readLocationStringFromField( view.targetProjectLocationField );
        String targetDir = readLocationStringFromField( view.projectDirectoryField );
        boolean copyChipKitFiles = view.copyChipKitFilesCheckBox.isSelected();

        settings.putProperty(SOURCE_PROJECT_DIR.key(), new File(sourceProjectDir));
        settings.putProperty(ARDUINO_DIR.key(), new File(arduinoDir));
        settings.putProperty(CHIPKIT_CORE_DIR.key(), new File(chipKitCoreDir));
        settings.putProperty(CHIPKIT_BOARD_ID.key(), boardId);
        settings.putProperty(CHIPKIT_BOARD_NAME.key(), boardName);
        settings.putProperty(CHIPKIT_BOARD_CONFIG_NAVIGATOR.key(), boardConfigNavigator);
        settings.putProperty(COPY_CHIPKIT_FILES.key(), copyChipKitFiles);

        if (boardId != null) {
            settings.putProperty(DEVICE_HEADER_PRESENT.key(), false);
            settings.putProperty(PLUGIN_BOARD_PRESENT.key(), false);
            try {
                WizardProjectConfiguration.storeDeviceHeaderPlugin(settings, boardConfigNavigator.parseDeviceName(boardId));
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        settings.putProperty(PROJECT_DIR.key(), new File(targetDir));
        settings.putProperty(PROJECT_NAME.key(), projectName);
        settings.putProperty(MAKE_FILENAME.key(), MAKEFILE_NAME);
        settings.putProperty(PROJECT_ENCODING.key(), ((Charset) view.encodingCombo.getSelectedItem()));

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
        NbPreferences.forModule(SelectProjectInfoPanel.class).put(LAST_ARDUINO_LOCATION.key(), arduinoDir);
        NbPreferences.forModule(SelectProjectInfoPanel.class).put(LAST_CHIPKIT_CORE_LOCATION.key(), chipKitCoreDir);
        NbPreferences.forModule(SelectProjectInfoPanel.class).put(CHIPKIT_BOARD_NAME.key(), boardName);
    }

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

    void sourceProjectLocationBrowseButtonActionPerformed(ActionEvent evt) {
        showDirectoryChooser(view.sourceProjectLocationField, "DLG_SourceProjectLocation");
        String projectDir = readLocationStringFromField( view.sourceProjectLocationField );
        if (projectDir != null && !projectDir.isEmpty()) {
            view.projectNameField.setText(Paths.get(projectDir).getFileName().toString());
            setTargetProjectDirectoryField();
        }
        fireChangeEvent();
    }

    void targetProjectLocationBrowseButtonActionPerformed(ActionEvent evt) {
        showDirectoryChooser(view.targetProjectLocationField, "DLG_TargetProjectLocation");
        setTargetProjectDirectoryField();
    }

    void arduinoLocationBrowseButtonActionPerformed(ActionEvent evt) {
        File arduinoDir = showDirectoryChooser(view.arduinoLocationField, "DLG_ArduinoDirectory");
        if ( arduinoDir != null ) {
            fireChangeEvent();
        }
    }

    void chipkitCoreLocationBrowseButtonActionPerformed(ActionEvent evt) {
        showDirectoryChooser(view.chipkitCoreLocationField, "DLG_ChipKitCoreDirectory");
        onChipKitCoreLocationChanged();
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

    void chipkitCoreLocationFieldFocusLost(FocusEvent evt) {
        onChipKitCoreLocationChanged();
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
    
    void chipKitBoardComboItemStateChanged(ItemEvent evt) {
        fireChangeEvent();
    }

    
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
        JFileChooser chooser = createSourceProjectFileChooser( readLocationStringFromField(pathField) );
        chooser.setDialogTitle( NbBundle.getMessage(ProjectSetupPanel.class, dialogTitleKey) );
        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(view)) { //NOI18N
            File selectedFile = chooser.getSelectedFile();
            File projectDir = selectedFile.isDirectory() ? selectedFile : selectedFile.getParentFile();
            pathField.setText(projectDir.getAbsolutePath());
            return projectDir;
        }
        return null;
    }

    private JFileChooser createSourceProjectFileChooser(String currentDir) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(null);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setFileFilter( new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".ino");
            }

            @Override
            public String getDescription() {
                // TODO: Move file type name to Bundle
                return "Arduino Project Files";
            }
        });
        if (currentDir.length() > 0) {
            File f = new File(currentDir);
            if (f.exists()) {
                if (f.isFile()) {
                    chooser.setCurrentDirectory(f.getParentFile());
                } else if (f.isDirectory()) {
                    chooser.setCurrentDirectory(f);
                }
            }
        } else {
            File f = new File(System.getProperty("netbeans.projects.dir"));		// NOI18N
            if (f.exists()) {
                chooser.setCurrentDirectory(f);
            }
        }
        return chooser;
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

    private boolean isArduinoDirectoryValid() {
        Path p = Paths.get( readLocationStringFromField( view.arduinoLocationField ) );
        return arduinoConfig.isValidArduinoInstallPath(p);
    }
    
    private boolean isArduinoVersionValid() {
        Path p = Paths.get( readLocationStringFromField( view.arduinoLocationField ) );
        return arduinoConfig.isCurrentVersionValid(p, new Version(MINIMUM_ARDUINO_VERSION) );
    }

    private boolean isChipKitDirectoryValid() {
        Path p = Paths.get( readLocationStringFromField( view.chipkitCoreLocationField ) );
        return ChipKitBoardConfigNavigator.isValidChipKitHardwarePath(p);
    }
    
    private boolean isBoardValid() {
        String boardName = readSelectedValueFromComboBox(view.chipKitBoardCombo);
        String boardId = chipKitBoardIdLookup.get(boardName);
        return boardId != null;
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
        } catch (java.lang.Exception ex) {
            // failed to create
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

    private void onChipKitCoreLocationChanged() {
        loadChipKitDevicesToCombo();        
        fireChangeEvent();
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

    private void loadChipKitDevicesToCombo() {
        try {
            String currentlySelectedBoardName = (view.chipKitBoardCombo.getSelectedItem() != null) ? view.chipKitBoardCombo.getSelectedItem().toString() : null;
            String chipKitCoreLocation = readLocationStringFromField( view.chipkitCoreLocationField );            
            Path chipKitCorePath = Paths.get( chipKitCoreLocation );
            if ( ChipKitBoardConfigNavigator.isValidChipKitHardwarePath(chipKitCorePath) ) {
                boardConfigNavigator = new ChipKitBoardConfigNavigator(chipKitCorePath);
                chipKitBoardIdLookup = boardConfigNavigator.parseBoardNamesToIDsLookup();
                List<String> boardNames = new ArrayList<>(chipKitBoardIdLookup.keySet());
                // Sort the board names list in alphabetical order:
                Collections.sort(boardNames);
                // Set up the combo box:
                DefaultComboBoxModel<String> cbm = new DefaultComboBoxModel<>(boardNames.toArray(new String[boardNames.size()]));
                view.chipKitBoardCombo.setModel(cbm);
                TypeAheadComboBox.enable(view.chipKitBoardCombo);
                if ( currentlySelectedBoardName != null ) {
                    view.chipKitBoardCombo.setSelectedItem(currentlySelectedBoardName);
                }
            } else {
                boardConfigNavigator = null;
                chipKitBoardIdLookup = null;
                view.chipKitBoardCombo.setModel( new DefaultComboBoxModel<>() );
                view.chipKitBoardCombo.setSelectedItem(null);
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
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
