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

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.Properties;
import java.util.TooManyListenersException;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.Exceptions;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import purejavacomm.NoSuchPortException;
import purejavacomm.PortInUseException;
import purejavacomm.UnsupportedCommOperationException;

@ConvertAsProperties(
    dtd = "-//com.microchip.mplab.nbide.embedded.serialmonitor//SerialMonitor//EN",
    autostore = false
)
@TopComponent.Description(
    preferredID = "SerialMonitorTopComponent",
    iconBase="com/microchip/mplab/nbide/embedded/serialmonitor/serialPort.png", 
    persistenceType = TopComponent.PERSISTENCE_NEVER
)
@TopComponent.Registration(mode = "output", openAtStartup = false)
@ActionID(category = "Tools", id = "com.microchip.mplab.nbide.embedded.serialmonitor.SerialMonitorTopComponent")
@ActionReference(path = "Menu/Window/Tools", position = 950)
@TopComponent.OpenActionRegistration(
    displayName = "#CTL_SerialMonitorAction",
    preferredID = "SerialMonitorTopComponent"
)
@Messages({
    "CTL_SerialMonitorAction=Serial Monitor",
    "CTL_SerialMonitorTopComponent=Serial Monitor",
    "LBL_Config=Configuration",
    "HINT_SerialMonitorTopComponent=This is a SerialMonitor window"
})
public final class SerialMonitorTopComponent extends TopComponent {

    
    private static final Logger LOGGER = Logger.getLogger( SerialMonitorTopComponent.class.getName() );
    
    private SerialPortCommunicator communicator;
    private SerialMonitorConfigModel configModel;
    
    public SerialMonitorTopComponent() {        
        configModel = new DefaultSerialMonitorConfigModel();
        initComponents();
    }
    
    private void initComponents() {
        setName(Bundle.CTL_SerialMonitorTopComponent() + " - " + Bundle.LBL_Config());
        setToolTipText(Bundle.HINT_SerialMonitorTopComponent());
        setLayout( new BorderLayout() );
        add( new SerialMonitorConfigPane( configModel, (event) -> handleConnect() ));
    }    

    public void disconnect() {
        if ( communicator != null ) {
            communicator.disconnect();
        }
    }
    
    public void reconnect() {
        if ( communicator != null ) {
            try {
                communicator.reconnect();
            } catch (NoSuchPortException | PortInUseException | IOException | UnsupportedCommOperationException | TooManyListenersException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    @Override
    public void componentOpened() {
        // ignore
    }

    @Override
    public void componentClosed() {
        if ( communicator != null ) {
            communicator.disconnect();
        }
    }

    void writeProperties(Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO: Store Serial Monitor settings
    }

    void readProperties(Properties p) {
        String version = p.getProperty("version");
        // TODO: Read Serial Monitor settings according to version
    }
    
    
    private void handleConnect() {
        SerialPortConfig config = configModel.getCurrentConfig();
        communicator = new SerialPortCommunicator( config );
        SwingUtilities.invokeLater( () -> {
            removeAll();
            add( new SerialMonitorDisplayPane(communicator, (event) -> handleConfigure()) );
            setName( Bundle.CTL_SerialMonitorTopComponent() + " - " + communicator.getConfig().getPortName() );
            revalidate();
        });
    }

    private void handleConfigure() {
        if ( communicator != null ) {
            communicator.disconnect();
        }
        SwingUtilities.invokeLater( () -> {
            removeAll();
            add( new SerialMonitorConfigPane( configModel, (event) -> handleConnect()) );
            setName(Bundle.CTL_SerialMonitorTopComponent() + " - " + Bundle.LBL_Config());
            revalidate();
        });
    }
}
