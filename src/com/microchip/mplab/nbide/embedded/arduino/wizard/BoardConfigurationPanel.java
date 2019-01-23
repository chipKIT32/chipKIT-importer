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
import com.microchip.mplab.nbide.embedded.arduino.importer.BoardOption;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.openide.util.NbBundle;

class BoardConfigurationPanel extends JPanel {

    private BoardConfigurationStep control;
    private final Map<BoardOption,JComboBox<String>> comboBoxLookup;

    BoardConfigurationPanel(BoardConfigurationStep control) {
        this.control = control;
        this.comboBoxLookup = new HashMap<>();
        setName(NbBundle.getMessage(BoardConfigurationPanel.class, "BoardConfigurationPanel.title"));
        setLayout(new GridBagLayout());
        add(new JLabel(), new GridBagConstraints());  // Just a placeholder
        setPreferredSize(new Dimension(662, 432));
    }

    void buildContentPane(Board board) {
        JPanel p0 = new JPanel();

        p0.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.insets = new Insets(3, 3, 3, 3);

        List<BoardOption> options = new ArrayList<>(board.getOptions());
        for (int i = 0; i < options.size(); i++) {
            BoardOption option = options.get(i);

            c.gridy = i;
            c.gridx = 0;
            p0.add(createLabel(option.getName()), c);

            c.gridx = 1;
            JComboBox<String> comboBox = createComboBox(board.getAvailableOptionValues(option));
            comboBoxLookup.put(option,comboBox);
            p0.add(comboBox, c);
        }

        removeAll();

        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.PAGE_START;
        add(p0, c);
        revalidate();
    }
    
    public String getSelectedOptionValue( BoardOption option ) {
        return comboBoxLookup.get(option).getSelectedItem().toString();
    }

    private JLabel createLabel(String text) {
        return new JLabel(text + ":");
    }

    private JComboBox<String> createComboBox(Collection<String> values) {
        JComboBox<String> comboBox = new JComboBox<>(values.toArray(new String[values.size()]));
        comboBox.addItemListener((e) -> {            
            control.optionValueItemStateChanged(e);
        });
        return comboBox;
    }

}
