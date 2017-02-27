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

package com.microchip.mplab.nbide.embedded.pic32prog.actions;

import com.microchip.mplab.nbide.embedded.serialmonitor.SerialPortComboModel;
import com.microchip.mplab.nbide.embedded.chipkit.importer.ChipKitProjectImporter;
import com.microchip.mplab.nbide.embedded.chipkit.importer.NativeProcessRunner;
import com.microchip.mplab.nbide.embedded.makeproject.HotProject;
import com.microchip.mplab.nbide.embedded.makeproject.MakeActionProvider;
import com.microchip.mplab.nbide.embedded.makeproject.MakeProject;
import com.microchip.mplab.nbide.embedded.makeproject.api.ProjectActionSupport;
import com.microchip.mplab.nbide.embedded.makeproject.api.configurations.MakeConfigurationBook;
import com.microchip.mplab.nbide.embedded.serialmonitor.DefaultSerialMonitorConfigModel;
import com.microchip.mplab.nbide.embedded.serialmonitor.SerialMonitorTopComponent;
import com.microchip.mplab.util.observers.Observer;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ActionProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.actions.Presenter;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.TopComponent;

@ActionID(
        category = "Build",
        id = "com.microchip.mplab.nbide.embedded.chipkit.actions.RunPic32ProgAction"
)
@ActionRegistration(
        lazy = false,
        displayName = "#CTL_RunPic32ProgAction.name"
)
@ActionReference(path = "Toolbars/Build", position = 332)
public final class RunPic32ProgAction extends AbstractAction implements PropertyChangeListener, Presenter.Toolbar {

    private static final RequestProcessor RP = new RequestProcessor("RunPic32ProgTask");
    private static final Logger LOGGER = Logger.getLogger(RunPic32ProgAction.class.getName());

    @StaticResource
    private static final String ICON = "com/microchip/mplab/nbide/embedded/pic32prog/actions/cK_24.png";
    private static final String[] PROJECT_EVENTS = new String[]{"Load"};

    private final JComboBox<String> serialPortsCombo;
    private final JButton programDeviceButton;
    private final JPanel toolbarPresenter;
    private final String actionName;
    
    private Observer projectActionEventListener;

    
    public RunPic32ProgAction() {
        actionName = NbBundle.getMessage(RunPic32ProgAction.class, "CTL_RunPic32ProgAction.name");

        programDeviceButton = new JButton(ImageUtilities.loadImageIcon(ICON, false));
        programDeviceButton.setEnabled(false);
        programDeviceButton.setContentAreaFilled(false);
        programDeviceButton.setMargin(new Insets(0, 0, 0, 0));
        programDeviceButton.setToolTipText(NbBundle.getMessage(RunPic32ProgAction.class, "CTL_RunPic32ProgAction.tooltip"));
        programDeviceButton.addActionListener(this);
        programDeviceButton.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                programDeviceButton.setContentAreaFilled(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                programDeviceButton.setContentAreaFilled(false);
            }

        });

        serialPortsCombo = new JComboBox<>(new SerialPortComboModel(new DefaultSerialMonitorConfigModel()));
        serialPortsCombo.setPreferredSize(new Dimension(150, 24));
        serialPortsCombo.addItemListener((e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                programDeviceButton.setEnabled(isProgrammingPossible());
            } else {
                programDeviceButton.setEnabled(isProgrammingPossible());
            }
        });

        toolbarPresenter = new JPanel(new FlowLayout(FlowLayout.LEADING));
        toolbarPresenter.add(serialPortsCombo);
        toolbarPresenter.add(programDeviceButton);
        toolbarPresenter.setOpaque(false);

        HotProject.addPropertyChangeListener(this);
    }

    @Override
    public Component getToolbarPresenter() {
        return toolbarPresenter;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        Project project = HotProject.getProject();
        if ( project instanceof MakeProject ) {
            try {
                final MakeProject makeProject = (MakeProject) project;
                if ( rebuildRequired(makeProject) ) {
                    projectActionEventListener = (action) -> runProgrammer( makeProject );
                    ProjectActionSupport.getInstance().addProjectActionEventListener( projectActionEventListener, PROJECT_EVENTS );
                    MakeActionProvider actionProvider = makeProject.getLookup().lookup(MakeActionProvider.class);
                    actionProvider.invokeAction(ActionProvider.COMMAND_BUILD, makeProject.getLookup());
                } else {
                    runProgrammer(makeProject);
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String pname = evt.getPropertyName();
        if (pname.startsWith("hot.")) {
            SwingUtilities.invokeLater(() -> programDeviceButton.setEnabled(isProgrammingPossible()));
        }
    }

    private void runProgrammer( final MakeProject makeProject ) {        
        RP.post(() -> {
            ProjectActionSupport.getInstance().removeProjectActionEventListener( projectActionEventListener, PROJECT_EVENTS );
            InputOutput io = IOProvider.getDefault().getIO(actionName, false);
            io.setFocusTaken(true);
            try {
                SerialMonitorTopComponent serialMonitor = null;
                for (TopComponent tc : TopComponent.getRegistry().getOpened()) {
                    if (tc instanceof SerialMonitorTopComponent) {
                        serialMonitor = (SerialMonitorTopComponent) tc;
                        break;
                    }
                }
                disconnectSerialMonitor(serialMonitor);

                String selectedPortName = serialPortsCombo.getSelectedItem().toString();

                String hexFileLocation = makeProject.getActiveConfiguration().getAbsoluteFinalLoadableFile();
                FileObject chipKitConfFile = makeProject.getProjectDirectory().getFileObject("nbproject").getFileObject(ChipKitProjectImporter.CHIPKIT_PROPERTIES_FILENAME);
                Properties chipKitProperties = new Properties();
                chipKitProperties.load(chipKitConfFile.getInputStream());

                String pic32progLocation = chipKitProperties.get("pic32prog") != null ? chipKitProperties.get("pic32prog").toString() : null;

                if (pic32progLocation != null) {
                    NativeProcessRunner nativeProcessRunner = new NativeProcessRunner(m -> io.getOut().println(m));
                    nativeProcessRunner.runNativeProcess(
                            pic32progLocation,
                            "-d",
                            selectedPortName,
                            "-b",
                            "115200",
                            hexFileLocation
                    );
                } else {
                    // Move the message to Bundle
                    io.getErr().println("Failed to locate pic32prog!");
                }

                reconnectSerialMonitor(serialMonitor);
            } catch (IOException | InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                io.getOut().close();
                io.getErr().close();                
            }
        });
    }

    private boolean isProgrammingPossible() {
        Project project = HotProject.getProject();
        if (project == null || !(project instanceof MakeProject)) {
            return false;
        }
        if (serialPortsCombo.getSelectedIndex() == -1) {
            return false;
        }
        return true;
    }

    private void disconnectSerialMonitor(final SerialMonitorTopComponent serialMonitor) {
        if (serialMonitor == null) {
            return;
        }
        try {
            SwingUtilities.invokeAndWait(() -> serialMonitor.disconnect());
        } catch (InterruptedException | InvocationTargetException ex) {
            LOGGER.log(Level.WARNING, "Interrupted while trying to disconnect Serial Monitor", ex);
        }
    }

    private void reconnectSerialMonitor(final SerialMonitorTopComponent serialMonitor) {
        if (serialMonitor == null) {
            return;
        }
        try {
            SwingUtilities.invokeAndWait(() -> serialMonitor.reconnect());
        } catch (InterruptedException | InvocationTargetException ex) {
            LOGGER.log(Level.WARNING, "Interrupted while trying to reconnect Serial Monitor", ex);
        }
    }
    
    // TODO: Consider moving this logic to a separate class as it may be usefull elsewhere
    protected boolean rebuildRequired( MakeProject makeProject ) throws IOException {
        String hexFileLocation = makeProject.getActiveConfiguration().getAbsoluteFinalLoadableFile();
        Path hexFilePath = Paths.get( hexFileLocation );
        if ( !Files.exists(hexFilePath) ) return true;
        FileTime hexLastModifiedTime = Files.getLastModifiedTime(hexFilePath);
        MakeConfigurationBook projectDescriptor = MakeConfigurationBook.getMakeConfigurationDescriptor(makeProject);
        String projectDir = projectDescriptor.getProjectDir();
        return projectDescriptor.getSourceRoots().stream().filter( root -> {
            Path sourceRootPath = Paths.get(projectDir, root);
            try {
                Optional<Path> opt = Files.list( sourceRootPath ).filter( f -> {
                    try {
                        return hexLastModifiedTime.compareTo( Files.getLastModifiedTime(f) ) < 0;
                    } catch (IOException ex) {
                        LOGGER.log(Level.INFO, "Failed to check modification time of file: " + f + ". Assuming rebuild is required", ex);
                        return true;
                    }
                }).findAny();
                return opt.isPresent();
            } catch (IOException ex) {
                LOGGER.log(Level.INFO, "Failed to list source files under: " + sourceRootPath + ". Assuming rebuild is required", ex);
                return true;
            }
        }).findAny().isPresent();
    }

}
