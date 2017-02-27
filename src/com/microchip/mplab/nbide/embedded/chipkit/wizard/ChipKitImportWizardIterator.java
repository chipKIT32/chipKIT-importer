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


import com.microchip.mplab.nbide.embedded.api.LanguageToolchain;
import com.microchip.mplab.nbide.embedded.chipkit.importer.ArduinoConfig;
import com.microchip.mplab.nbide.embedded.makeproject.api.wizards.WizardProperty;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import static com.microchip.mplab.nbide.embedded.makeproject.api.wizards.NewMakeProjectWizardIterator.TYPE_APPLICATION;
import java.awt.Component;
import java.text.MessageFormat;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;


public class ChipKitImportWizardIterator implements WizardDescriptor.InstantiatingIterator {

    private final LanguageToolchain languageToolchain;
    private final ArduinoConfig arduinoConfig;
    private ImportWorker importWorker;
    private int index;
    private WizardDescriptor.Panel[] panels;
    private WizardDescriptor wiz;

    public ChipKitImportWizardIterator( LanguageToolchain languageToolchain, ArduinoConfig arduinoConfig ) {
        this.languageToolchain = languageToolchain;
        this.arduinoConfig = arduinoConfig;
    }

    @Override
    public void initialize( WizardDescriptor wd ) {
        this.wiz = wd;
        
        importWorker = new ImportWorker(languageToolchain, wiz);
        
        wiz.putProperty(WizardProperty.APPLICATION_TYPE.key(), (Integer) TYPE_APPLICATION);
        wiz.setTitleFormat(new MessageFormat("{0}"));
        wiz.setTitle( NbBundle.getMessage(ChipKitImportWizardIterator.class, "WizardTitle") );        
        
        panels = new WizardDescriptor.Panel[]{            
            new ProjectSetupStep( arduinoConfig ),
            new ProgrammerDebuggerSelectionStep(),
            new ProgressTrackingStep( importWorker )
        };
        
        String[] steps = new String[panels.length];

        for (int i = 0; i < panels.length; i++) {
            Component c = panels[i].getComponent();
            steps[i] = c.getName();
            if (c instanceof JComponent) { // assume Swing components
                JComponent jc = (JComponent) c;
                // Sets step number of a component
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, i);   // NOI18N
                // Sets steps names for a panel
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, steps);                     // NOI18N
                // Turn on subtitle creation on each step
                jc.putClientProperty(WizardDescriptor.PROP_AUTO_WIZARD_STYLE, Boolean.TRUE);     // NOI18N
                // Show steps on the left side with the image on the background
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DISPLAYED, Boolean.TRUE);     // NOI18N
                // Turn on numbering of all steps
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_NUMBERED, Boolean.TRUE);     // NOI18N
            }
        }

    }

    @Override
    public Set<FileObject> instantiate() throws IOException {
        try {
            Set<FileObject> resultSet = importWorker.get();
            return resultSet;
        } catch (InterruptedException | ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
        return new HashSet<>();
    }

    @Override
    public void uninitialize(WizardDescriptor wd) {
        // Do nothing
    }

    @Override
    public String name() {
        return MessageFormat.format("{0} of {1}", new Object[]{index + 1, panels.length});
    }

    @Override
    public boolean hasNext() {
        return index < panels.length - 1;
    }

    @Override
    public boolean hasPrevious() {
        return index > 0 && index < 2;
    }

    @Override
    public void nextPanel() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        index++;
    }

    @Override
    public void previousPanel() {
        if (!hasPrevious()) {
            throw new NoSuchElementException();
        }
        index--;
    }

    @Override
    public WizardDescriptor.Panel current() {
        return panels[index];
    }

    // If nothing unusual changes in the middle of the wizard, simply:
    @Override
    public final void addChangeListener(ChangeListener l) {}

    @Override
    public final void removeChangeListener(ChangeListener l) {}
  
}
