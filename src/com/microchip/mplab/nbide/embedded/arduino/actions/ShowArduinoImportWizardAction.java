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

import com.microchip.mplab.nbide.embedded.arduino.importer.ArduinoConfig;
import com.microchip.mplab.nbide.embedded.arduino.wizard.ImportWizardIterator;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ui.OpenProjects;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

@ActionID(
    category = "File",
    id = "com.microchip.mplab.nbide.embedded.arduino.ShowArduinoImportWizardAction"
)
@ActionRegistration(
    iconBase = "com/microchip/mplab/nbide/embedded/arduino/actions/arduino_16.png",
    displayName = "#CTL_ShowArduinoImportWizardAction"
)
@ActionReference(path = "Menu/File/Import", position = 200)
public final class ShowArduinoImportWizardAction implements ActionListener {

    private static final Logger LOGGER = Logger.getLogger(ShowArduinoImportWizardAction.class.getName());

    @Override
    public void actionPerformed(ActionEvent e) {
        LOGGER.log(Level.INFO, "Initializing Arduino import procedure");
        
        ArduinoConfig arduinoConfig = ArduinoConfig.getInstance();

        ImportWizardIterator wizIterator = new ImportWizardIterator( arduinoConfig );
        WizardDescriptor wiz = new WizardDescriptor(wizIterator);

        Dialog dialog = DialogDisplayer.getDefault().createDialog(wiz);
        dialog.setVisible(true);
        dialog.toFront();

        if ( wiz.getValue() == WizardDescriptor.FINISH_OPTION ) {
            openImportedProject(wiz);
        }
    }
    
    private void openImportedProject( WizardDescriptor wiz ) {
        Set<FileObject> foSet = wiz.getInstantiatedObjects();
        Iterator<FileObject> i = foSet.iterator();
        while (i.hasNext()) {
            FileObject fo = i.next();
            try {
                Project project = ProjectManager.getDefault().findProject(fo);
                Project[] projArray = new Project[]{project};
                OpenProjects.getDefault().open(projArray, false);
            } catch (IOException | IllegalArgumentException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }    

}
