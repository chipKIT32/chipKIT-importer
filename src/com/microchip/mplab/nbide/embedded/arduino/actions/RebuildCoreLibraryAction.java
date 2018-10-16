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

package com.microchip.mplab.nbide.embedded.arduino.actions;

import com.microchip.mplab.nbide.embedded.api.LanguageToolchain;
import com.microchip.mplab.nbide.embedded.arduino.importer.ProjectImporter;
import static com.microchip.mplab.nbide.embedded.arduino.importer.ProjectImporter.CORE_DIRECTORY_NAME;
import com.microchip.mplab.nbide.embedded.arduino.importer.GCCToolFinder;
import com.microchip.mplab.nbide.embedded.arduino.importer.LibCoreBuilder;
import com.microchip.mplab.nbide.embedded.makeproject.MakeOptions;
import com.microchip.mplab.nbide.embedded.makeproject.MakeProject;
import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DynamicMenuContent;
import org.openide.filesystems.FileObject;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

import static javax.swing.Action.NAME;
import org.openide.LifecycleManager;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;


@ActionID(
    category = "Project",
    id = "com.microchip.mplab.nbide.embedded.arduino.RebuildCoreLibraryAction"
)
@ActionRegistration(
    iconBase = "com/microchip/mplab/nbide/embedded/arduino/actions/arduino_16.png",        
    displayName = "#CTL_RebuildCoreLibraryAction",
    lazy = false
)
@ActionReference(path="Projects/Actions", position = 100)
public class RebuildCoreLibraryAction extends AbstractAction implements ContextAwareAction {

    private static final RequestProcessor RP = new RequestProcessor("RebuildLibCoreTask");
    
    @Override
    public void actionPerformed(ActionEvent e) {assert false;}
    
    @Override
    public Action createContextAwareInstance(Lookup context) {
        return new ContextAction(context);
    }
    
    private static final class ContextAction extends AbstractAction {
        
        private final MakeProject project;
        
        public ContextAction(Lookup context) {
            putValue(DynamicMenuContent.HIDE_WHEN_DISABLED, true);
            putValue(NAME, NbBundle.getMessage(RebuildCoreLibraryAction.class, "CTL_RebuildCoreLibraryAction"));
            project = context.lookup(MakeProject.class);
            if ( project != null ) {
                FileObject importedCoreDir = project.getProjectDirectory().getFileObject( CORE_DIRECTORY_NAME );
                setEnabled( importedCoreDir != null );
            } else {
                setEnabled( false );
            }
        }
        
        @Override        
        public void actionPerformed(ActionEvent e) {            
            RP.post(() -> {
                
                if (MakeOptions.getInstance().getSave()) {
                    LifecycleManager.getDefault().saveAll();
                }
                
                LibCoreBuilder libCoreBuilder = new LibCoreBuilder();
                InputOutput io = IOProvider.getDefault().getIO ( getValue(NAME).toString(), true );
                io.setFocusTaken(true);
                try {                    
                    LanguageToolchain toolchain = project.getActiveConfiguration().getLanguageToolchain().findToolchain();
                    GCCToolFinder toolFinder = new GCCToolFinder(toolchain);
                    Path coreDirPath = Paths.get(project.getProjectDirectory().getFileObject(ProjectImporter.CORE_DIRECTORY_NAME ).getPath() );            
                    Path makefilePath = coreDirPath.resolve( libCoreBuilder.getMakefileName() );
                    libCoreBuilder.build( makefilePath, toolFinder, (m) -> {
                        io.getOut().println(m);
                    } );
                    // TODO: Move message strings to Bundle
                    io.getOut().println("Copying Core Library file...");
                    Files.copy( libCoreBuilder.getLibCorePath(), coreDirPath.resolve( LibCoreBuilder.LIB_CORE_FILENAME ), StandardCopyOption.REPLACE_EXISTING );
                    io.getOut().println("Done");
                } catch (Exception ex) {
                    libCoreBuilder.cleanup();
                    io.getOut().close();
                    io.getErr().close();
                    Exceptions.printStackTrace(ex);
                }
            });
        }
            
    }
    
}
