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

package com.microchip.mplab.nbide.embedded.chipkit.actions;

import com.microchip.mplab.nbide.embedded.api.LanguageToolchain;
import com.microchip.mplab.nbide.embedded.chipkit.importer.ArduinoConfig;
import com.microchip.mplab.nbide.embedded.chipkit.utils.LanguageToolchainLocator;
import com.microchip.mplab.nbide.embedded.chipkit.wizard.ChipKitImportWizardIterator;
import com.microchip.mplab.nbide.embedded.chipkit.importer.Requirements;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ui.OpenProjects;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

@ActionID(
    category = "File",
    id = "com.microchip.mplab.nbide.embedded.chipkit.ShowChipKitImportWizardAction"
)
@ActionRegistration(
    iconBase = "com/microchip/mplab/nbide/embedded/chipkit/actions/arduino_16.png",
    displayName = "#CTL_ShowChipKitImportWizardAction"
)
@ActionReference(path = "Menu/File/Import", position = 200)
public final class ShowChipKitImportWizardAction implements ActionListener {

    private static final Logger LOGGER = Logger.getLogger(ShowChipKitImportWizardAction.class.getName());

    @Override
    public void actionPerformed(ActionEvent e) {
        LOGGER.log(Level.INFO, "Initializing chipKIT Import procedure");
        
        LanguageToolchain languageToolchain = checkLanguageToolchain();
        if ( languageToolchain == null ) {
            return;
        }
        
        ArduinoConfig arduinoConfig = ArduinoConfig.getInstance();

        ChipKitImportWizardIterator wizIterator = new ChipKitImportWizardIterator( languageToolchain, arduinoConfig );
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
    
    private LanguageToolchain checkLanguageToolchain() {
        Optional<LanguageToolchain> opt = new LanguageToolchainLocator().findSuitableLanguageToolchain();
        if (!opt.isPresent()) {
            LOGGER.log(Level.INFO, "No valid XC32 toolchain found. Asking the user to download one and aborting the procedure");
            String messageTemplate = NbBundle.getMessage(getClass(), "LanguageToolchainVersionErrorDialog.message");
            String message = MessageFormat.format(messageTemplate, Requirements.MINIMUM_XC_TOOLCHAIN_VERSION);

            JEditorPane messagePane = createMessagePane(message);

            JPanel contentPane = new JPanel();
            contentPane.setLayout(new BorderLayout(5, 5));
            contentPane.add(messagePane, BorderLayout.CENTER);

            DialogDescriptor dd = new DialogDescriptor(contentPane, NbBundle.getMessage(getClass(), "LanguageToolchainVersionErrorDialog.title"));
            dd.setMessageType(DialogDescriptor.ERROR_MESSAGE);
            dd.setOptionsAlign(DialogDescriptor.BOTTOM_ALIGN);
            dd.setOptions(new Object[]{DialogDescriptor.OK_OPTION});

            DialogDisplayer.getDefault().notify(dd);
            return null;
        } else {
            LOGGER.log(Level.INFO, "Found XC32 toolchain. Version: {0}", opt.get().getVersion());
            return opt.get();
        }
    } 
    
    private JEditorPane createMessagePane( String htmlMessage ) {
        JEditorPane messagePane = new JEditorPane("text/html", htmlMessage);
        messagePane.setEditable(false);
        messagePane.setOpaque(false);
        messagePane.addHyperlinkListener( (HyperlinkEvent hyperLink) -> {
            if (HyperlinkEvent.EventType.ACTIVATED.equals(hyperLink.getEventType())) {
                try {
                    Desktop.getDesktop().browse( hyperLink.getURL().toURI() );
                } catch (URISyntaxException | IOException ex) {
                    LOGGER.log( Level.WARNING, "Failed to open URL: " + hyperLink.getURL(), ex );
                }
            }
        });
        return messagePane;
    }
    

}
