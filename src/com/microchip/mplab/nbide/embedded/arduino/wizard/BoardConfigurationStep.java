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

import com.microchip.mplab.nbide.embedded.arduino.importer.Board;
import com.microchip.mplab.nbide.embedded.arduino.importer.BoardConfiguration;
import com.microchip.mplab.nbide.embedded.arduino.importer.BoardOption;
import static com.microchip.mplab.nbide.embedded.arduino.wizard.ImportWizardProperty.BOARD;
import static com.microchip.mplab.nbide.embedded.arduino.wizard.ImportWizardProperty.BOARD_CONFIGURATION;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;


public class BoardConfigurationStep implements WizardDescriptor.Panel<WizardDescriptor> {

    private final Set<ChangeListener> listeners = new HashSet<>();
    private WizardDescriptor wizardDescriptor;
    private BoardConfigurationPanel view;
    private final MPLABDeviceAssistant deviceAssistant;
    private Board board;
    private BoardConfiguration boardConfiguration;

    public BoardConfigurationStep( MPLABDeviceAssistant deviceAssistant ) {
        this.deviceAssistant = deviceAssistant;
    }
    
    void optionValueItemStateChanged(ItemEvent evt) {
        updateBoardConfiguration();
        fireChangeEvent();
    }
    
    @Override
    public Component getComponent() {
        if (view == null) {            
            view = new BoardConfigurationPanel(this);
        }
        return view;
    }

    @Override
    public HelpCtx getHelp() {
        return null;
    }

    @Override
    public void readSettings(WizardDescriptor wizardDescriptor) {        
        this.wizardDescriptor = wizardDescriptor;
        this.board = (Board) wizardDescriptor.getProperty(BOARD.key());
        this.view.buildContentPane(board);
        updateBoardConfiguration();
    }
    
    @Override
    public void storeSettings(WizardDescriptor wizardDescriptor) {
        wizardDescriptor.putProperty(BOARD_CONFIGURATION.key(), boardConfiguration);
        deviceAssistant.storeSettings(wizardDescriptor);
    }

    @Override
    public boolean isValid() {
        if (!isToolchainValid()) {
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, NbBundle.getMessage(ProjectSetupPanel.class, "MSG_NoMatchingToolchainFound"));
            return false;
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
    
    private boolean isToolchainValid() {        
        return deviceAssistant.isToolchainValid();
    }
    
    private void updateBoardConfiguration() {
        Map <BoardOption,String> optionValues = new HashMap<>();
        board.getOptions().forEach( option -> {
            optionValues.put(option, view.getSelectedOptionValue(option)); 
        });
        boardConfiguration = new BoardConfiguration(board, optionValues);
        deviceAssistant.updateDeviceAndToolchain( boardConfiguration );
    }
}
