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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import purejavacomm.CommPortIdentifier;
import purejavacomm.NoSuchPortException;
import purejavacomm.PortInUseException;
import purejavacomm.SerialPort;
import purejavacomm.SerialPortEvent;
import purejavacomm.UnsupportedCommOperationException;

public class SerialPortCommunicator {
    
    private static final Logger LOGGER = Logger.getLogger( SerialPortCommunicator.class.getName() );
    public static final String PORT_OWNER_NAME = "SerialPortCommunicator";
    
    private final SerialPortConfig config;    
    private Consumer<Boolean> connectionHandler;
    private Consumer<InputStream> inputHandler;
    private SerialPort port;
    private InputStream in;
    private OutputStream out;
    private Thread portScanningThread;

    public SerialPortCommunicator( SerialPortConfig config ) {
        this.config = config;        
    }

    public SerialPortConfig getConfig() {
        return config;
    }
    
    public void connect( Consumer<Boolean> connectionHandler, Consumer<InputStream> inputHandler ) throws TooManyListenersException, UnsupportedCommOperationException, PortInUseException, NoSuchPortException, IOException {
        this.connectionHandler = connectionHandler;
        this.inputHandler = inputHandler;
        CommPortIdentifier portid = CommPortIdentifier.getPortIdentifier( config.getPortName() );
        port = (SerialPort) portid.open( PORT_OWNER_NAME, 1000 );
        connectionHandler.accept(Boolean.FALSE);  // first connection
        setupPort();
    }
    
    public InputStream getIn() {
        return in;
    }
    
    public OutputStream getOut() {
        return out;
    }    
    
    public void reconnect() throws NoSuchPortException, PortInUseException, IOException, UnsupportedCommOperationException, TooManyListenersException {
        disconnect();
        CommPortIdentifier portid = CommPortIdentifier.getPortIdentifier( config.getPortName() );
        if ( portid != null ) {
            port = (SerialPort) portid.open(PORT_OWNER_NAME, 1000);
            connectionHandler.accept(Boolean.TRUE);  // reconnection
            setupPort();
        }
    }
            
    
    public void disconnect() {
        port.close();
    }
    
    public void startScanningForPort() {
        if ( portScanningThread != null && portScanningThread.isAlive() ) return;
        portScanningThread = new Thread( () -> {
            while ( true ) {
                try {                    
                    CommPortIdentifier portid = CommPortIdentifier.getPortIdentifier( config.getPortName() );
                    if ( portid != null ) {
                        port = (SerialPort) portid.open(PORT_OWNER_NAME, 200);
                        connectionHandler.accept(Boolean.TRUE);  // reconnection
                        setupPort();
                        return;
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    return;
                } catch (NoSuchPortException ex) {
                    // ignore
                } catch (Exception ex) {
                    LOGGER.log( Level.WARNING, "Exception caught while scanning serial ports", ex);
                }
            }
        });
        portScanningThread.setDaemon(true);
        portScanningThread.start();
    }
    
    private void setupPort() throws IOException, UnsupportedCommOperationException, TooManyListenersException {
        in = port.getInputStream();
        out = port.getOutputStream();
        port.notifyOnDataAvailable(true);
        port.notifyOnOutputEmpty(false);
        port.setDTR(true);
        port.setRTS(true);
        port.setFlowControlMode( config.getFlowControl() );
        port.setSerialPortParams( config.getBaudRate(), config.getDataBits(), config.getStopBits(), config.getParity() );
        port.addEventListener( (SerialPortEvent event) -> {
            if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
                inputHandler.accept( in );
            }
        });
    }
    
}
