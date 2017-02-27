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

package com.microchip.mplab.nbide.embedded.serialmonitor;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.swing.GroupLayout;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LayoutStyle;
import org.openide.util.NbBundle;

import static javax.swing.GroupLayout.*;


public class SerialMonitorConfigPane extends JPanel {

    
    private static final Logger LOGGER = Logger.getLogger( SerialMonitorConfigPane.class.getName() );
    
    private static final int MIN_COMBO_WIDTH = 100;
    private static final int MIN_PORT_COMBO_WIDTH = 130;
    
    private final SerialMonitorConfigModel model;
    private final List <JComboBox> parameterCombos;
    private final JComboBox <String> portNameCombo;
    private final JComboBox <String> baudRateCombo;
    private final JComboBox <String> flowControlCombo;
    private final JComboBox <String> dataBitsCombo;
    private final JComboBox <String> stopBitsCombo;
    private final JComboBox <String> parityCombo;
    private final JButton connectButton;
            
    
    public SerialMonitorConfigPane( SerialMonitorConfigModel model, ActionListener connectActionHandler ) {
        if ( model == null ) throw new NullPointerException("The model cannot be null!");
        
        this.model = model;
        
        JLabel headerLabel = new JLabel( getLocalizedText("headerLabel") );
        headerLabel.setFont( headerLabel.getFont().deriveFont( Font.BOLD, 16f ) );
        
        parameterCombos = new ArrayList<>();
        
        // Create combos:
        portNameCombo = createPortNameCombo();
        JLabel portNameLabel = createLabel("portNameLabel", portNameCombo);
        
        baudRateCombo = createParameterCombo( model.getAvailableBaudRates(), model.getCurrentBaudRate(), model::setCurrentBaudRate );
        JLabel baudRateLabel = createLabel("baudRateLabel", baudRateCombo);
        
        flowControlCombo = createParameterCombo( model.getAvailableFlowControl(), model.getCurrentFlowControl(), model::setCurrentFlowControl );
        JLabel flowControlLabel = createLabel("flowControlLabel", flowControlCombo);
        
        dataBitsCombo = createParameterCombo( model.getAvailableDataBits(), model.getCurrentDataBits(), model::setCurrentDataBits );
        JLabel dataBitsLabel = createLabel("dataBitsLabel", dataBitsCombo);
        
        stopBitsCombo = createParameterCombo( model.getAvailableStopBits(), model.getCurrentStopBits(), model::setCurrentStopBits );
        JLabel stopBitsLabel = createLabel("stopBitsLabel", stopBitsCombo);

        parityCombo = createParameterCombo( model.getAvailableParities(), model.getCurrentParity(), model::setCurrentParity );
        JLabel parityLabel = createLabel("parityLabel", parityCombo);
        
        // Create the "connect" button:
        connectButton = new JButton( getLocalizedText("connectButton") );
        connectButton.addActionListener( (event) -> {
            if ( connectActionHandler != null ) {
                connectActionHandler.actionPerformed( new ActionEvent(SerialMonitorConfigPane.this, 0, "connect" ) );
            }
        });
        
        // Disable all combos below the port name combo if there is no port name specified in the model:
        if ( model.getCurrentPortName() == null ) {
            disableComponents();
        }
        
        // Add a selection listener to the port name combo to enable all combos below port name combo when a serial port is selected
        portNameCombo.addItemListener( (e) -> {
            if ( e.getStateChange() == ItemEvent.SELECTED ) {
                enableComponents();
            } else if ( e.getStateChange() == ItemEvent.DESELECTED ) {
                disableComponents();
            }
        });
        
        // Layout components:
        GroupLayout layout = new GroupLayout( this );
        setLayout(layout);
        
        layout.setHorizontalGroup( layout.createParallelGroup(Alignment.CENTER)
            .addComponent( headerLabel )
            .addGroup( layout.createSequentialGroup()
                .addContainerGap(DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                    .addComponent(portNameLabel)
                    .addComponent(baudRateLabel)
                    .addComponent(flowControlLabel)
                    .addComponent(dataBitsLabel)
                    .addComponent(parityLabel)
                    .addComponent(stopBitsLabel))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(portNameCombo, MIN_PORT_COMBO_WIDTH, DEFAULT_SIZE, PREFERRED_SIZE)
                    .addComponent(baudRateCombo, MIN_COMBO_WIDTH, DEFAULT_SIZE, PREFERRED_SIZE)
                    .addComponent(flowControlCombo, MIN_COMBO_WIDTH, DEFAULT_SIZE, PREFERRED_SIZE)
                    .addComponent(dataBitsCombo, MIN_COMBO_WIDTH, DEFAULT_SIZE, PREFERRED_SIZE)
                    .addComponent(parityCombo, MIN_COMBO_WIDTH, DEFAULT_SIZE, PREFERRED_SIZE)
                    .addComponent(stopBitsCombo, MIN_COMBO_WIDTH, DEFAULT_SIZE, PREFERRED_SIZE))
                .addContainerGap(DEFAULT_SIZE, Short.MAX_VALUE)
            )
            .addComponent( connectButton, PREFERRED_SIZE, 200, PREFERRED_SIZE )
        );
        layout.setVerticalGroup( layout.createSequentialGroup()
            .addGap(10)
            .addComponent( headerLabel )
            .addContainerGap(DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(portNameLabel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
                .addComponent(portNameCombo, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(baudRateLabel)
                .addComponent(baudRateCombo, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(flowControlLabel)
                .addComponent(flowControlCombo, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(dataBitsLabel)
                .addComponent(dataBitsCombo, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(parityLabel)
                .addComponent(parityCombo, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(stopBitsLabel)
                .addComponent(stopBitsCombo, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
            .addContainerGap(DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(connectButton, PREFERRED_SIZE, 32, PREFERRED_SIZE)
            .addGap(10)
        );
    }
    
    public SerialMonitorConfigModel getModel() {
        return model;
    }
    
    private void enableComponents() {
        parameterCombos.forEach( (c) -> c.setEnabled(true) );
        connectButton.setEnabled(true);
    }
    
    private void disableComponents() {
        parameterCombos.forEach( (c) -> c.setEnabled(false) );
        connectButton.setEnabled(false);
    }
    
    private JLabel createLabel( String name, JComponent targetComponent ) {
        JLabel label = new JLabel( getLocalizedText(name) );
        label.setLabelFor( targetComponent );
        return label;
    }
    
    private JComboBox<String> createPortNameCombo() {
        final JComboBox <String> ret = new JComboBox<>( new SerialPortComboModel(model) );
        ret.addItemListener( (e) -> {            
            if ( e.getStateChange() == ItemEvent.SELECTED ) {
                model.setCurrentPortName( e.getItem().toString() );
            } else {
                model.setCurrentPortName( null );
            }
        });
        ret.setSelectedItem( model.getCurrentPortName() );
        return ret;
    }
    
    private JComboBox<String> createParameterCombo( String[] values, String selectedValue, Consumer<String> modelSetter ) {
        JComboBox <String> ret = new JComboBox<>( values );
        ret.setSelectedItem( selectedValue );
        ret.addItemListener( (e) -> {
            if ( e.getStateChange() == ItemEvent.SELECTED ) {
                modelSetter.accept(e.getItem().toString());
            }
        });
        parameterCombos.add(ret);
        return ret;
    }
    
    private static String getLocalizedText( String name ) {
        return NbBundle.getMessage(SerialMonitorConfigPane.class, "SerialMonitorConfigPane." + name + ".text"); // NOI18N
    }
    
}
