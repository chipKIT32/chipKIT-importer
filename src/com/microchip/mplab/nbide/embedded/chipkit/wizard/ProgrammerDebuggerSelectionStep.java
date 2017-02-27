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

import com.microchip.mplab.nbide.embedded.makeproject.ui.wizards.SelectToolDescriptorPanel;
import java.awt.Component;
import javax.swing.event.ChangeEvent;
import org.openide.util.NbBundle;

public class ProgrammerDebuggerSelectionStep extends SelectToolDescriptorPanel {

    @Override
    public Component getComponent() {
        Component component = super.getComponent();
        component.setName( NbBundle.getMessage(ProgrammerDebuggerSelectionStep.class, "ProgrammerDebuggerSelectionPanel.title") );
        return component;
    }
    
    @Override
    public void stateChanged(ChangeEvent e) {
        // Overriding the original event handler which changes the name of this page to "..."
    }


}
