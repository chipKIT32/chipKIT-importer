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

import static com.microchip.mplab.nbide.embedded.serialmonitor.SerialPortCommunicator.PORT_OWNER_NAME;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.openide.util.Utilities;
import purejavacomm.CommPortIdentifier;
import purejavacomm.PortInUseException;
import purejavacomm.PureJavaIllegalStateException;
import purejavacomm.SerialPort;

public class DefaultSerialMonitorConfigModel implements SerialMonitorConfigModel {
    
    private static final Logger LOGGER = Logger.getLogger( DefaultSerialMonitorConfigModel.class.getName() );
    private static final String DEV_PREFFIX = "/dev/";
    
    private String portName;
    private String baudRate;
    private String flowControl;
    private String dataBits;
    private String stopBits;
    private String parity;

    public DefaultSerialMonitorConfigModel() {
        // empty constructor
    }
            
    @Override
    public synchronized String[] getAvailablePortNames() {
        Enumeration <CommPortIdentifier> e = CommPortIdentifier.getPortIdentifiers();
        
        List <CommPortIdentifier> commPortIdentifiers = new ArrayList<>();                
        while (e.hasMoreElements()) {
            CommPortIdentifier portid = e.nextElement();
            if ( portid.getPortType() == CommPortIdentifier.PORT_PARALLEL ) continue;
            commPortIdentifiers.add(portid);
        }
        
        List <String> portNames = commPortIdentifiers.stream()
            .filter( this::isPortAvailable )
            .map( p -> p.getName() )
            .collect( Collectors.toList() );
        
        return portNames.toArray( new String[portNames.size()] );
    }

    @Override
    public String[] getAvailableDeviceNames() {
        if ( Utilities.isWindows() ) return getAvailablePortNames();
        if ( Utilities.isMac() || Utilities.isUnix() ) {
            String[] portNames = getAvailablePortNames();
            String[] deviceNames = new String[portNames.length];
            int i=0;
            for ( String p : portNames ) {
                deviceNames[i++] = DEV_PREFFIX + p;
            }
            return deviceNames;
        }
        throw new UnsupportedOperationException("Getting port device names is not available on this platform!");
    }
    
    @Override
    public boolean isDeviceAvailable(String deviceName) {
        String p = "";
        if ( Utilities.isWindows() ) {
            p = deviceName;
        } else if ( Utilities.isMac() || Utilities.isUnix() ) {
            p = deviceName.substring( DEV_PREFFIX.length() );
        }
        
        Enumeration<CommPortIdentifier> portIdentifiers = CommPortIdentifier.getPortIdentifiers();
        while (portIdentifiers.hasMoreElements()) {
            CommPortIdentifier cpi = portIdentifiers.nextElement();
            if ( p.equals(cpi.getName()) ) {
                return isPortAvailable(cpi);
            }
        }

        return false;
    }    

    @Override
    public String[] getAvailableBaudRates() {
        return new String[] {"300", "600", "1200", "2400", "4800", "9600", "14400", "19200", "28800", "38400", "57600", "115200"};
    }

    @Override
    public String[] getAvailableFlowControl() {
        return new String[] { "NONE", "RTSCTS IN", "RTSCTS OUT", "XONXOFF IN", "XONXOFF OUT" };
    }    

    @Override
    public String[] getAvailableDataBits() {
        return new String[] {"5", "6", "7", "8"};
    }

    @Override
    public String[] getAvailableStopBits() {
        return new String[] {"1", "2"};
    }

    @Override
    public String[] getAvailableParities() {
        return new String[] {"NONE", "ODD", "EVEN", "MARK", "SPACE"};
    }

    @Override
    public String getDefaultBaudRate() {
        return "9600";
    }

    @Override
    public String getDefaultFlowControl() {
        return "NONE";
    }

    @Override
    public String getDefaultDataBits() {
        return "8";
    }

    @Override
    public String getDefaultStopBits() {
        return "1";
    }

    @Override
    public String getDefaultParity() {
        return "NONE";
    }

    @Override
    public void setCurrentPortName(String value) {
        portName = value;
    }

    @Override
    public String getCurrentPortName() {
        return portName;
    }

    @Override
    public void setCurrentBaudRate(String value) {
        baudRate = value;
    }

    @Override
    public String getCurrentBaudRate() {
        return baudRate != null ? baudRate : getDefaultBaudRate();
    }

    @Override
    public void setCurrentFlowControl(String value) {
        flowControl = value;
    }

    @Override
    public String getCurrentFlowControl() {        
        return flowControl != null ? flowControl : getDefaultFlowControl();
    }

    @Override
    public void setCurrentDataBits(String value) {
        dataBits = value;
    }

    @Override
    public String getCurrentDataBits() {
        return dataBits != null ? dataBits : getDefaultDataBits();
    }

    @Override
    public void setCurrentStopBits(String value) {
        stopBits = value;
    }

    @Override
    public String getCurrentStopBits() {
        return stopBits != null ? stopBits : getDefaultStopBits();
    }

    @Override
    public void setCurrentParity(String value) {
        parity = value;
    }

    @Override
    public String getCurrentParity() {
        return parity != null ? parity : getDefaultParity();
    }

    @Override
    public SerialPortConfig getCurrentConfig() {
        return new SerialPortConfig.Builder()
            .portName( getCurrentPortName() )
            .baudRate( Integer.parseInt( getCurrentBaudRate() ) )
            .flowControl( parseFlowControl( getCurrentFlowControl() ) )
            .dataBits( Integer.parseInt( getCurrentDataBits() ) )
            .stopBits( parseStopBits( getCurrentStopBits() ) )
            .parity( parseParity( getCurrentParity() ) )
            .build();
    }
    
    
    private boolean isPortAvailable( CommPortIdentifier p ) {
        if ( Utilities.isUnix() && p.getName().startsWith("ttyS") ) {        
            SerialPort port = null;
            try {            
                if ( !p.isCurrentlyOwned() ) {
                    port = (SerialPort) p.open( PORT_OWNER_NAME, 200 );
                }
                return true;
            } catch ( PureJavaIllegalStateException | PortInUseException ex1 ) {
                // Ignore those exceptions as they mean that a given port is not available at this moment and may be thrown many times
                return false;
            } catch (Exception ex2) {
                LOGGER.log( Level.WARNING, "Exception caught while checking port: " + p.getName(), ex2 );
                return false;
            } finally {
                if ( port != null ) {
                    port.close();
                    port = null;
                }
            }
        } else {
            // On Windows, if a CommPortIdentifier exists then the port exists as well
            return true;
        }
    }
    
    private int parseFlowControl( String value ) {
        int index = Arrays.asList( getAvailableFlowControl() ).indexOf( value );
        switch (index) {
            case 0:
                return SerialPort.FLOWCONTROL_NONE;
            case 1:
                return SerialPort.FLOWCONTROL_RTSCTS_IN;
            case 2:
                return SerialPort.FLOWCONTROL_RTSCTS_OUT;
            case 3:
                return SerialPort.FLOWCONTROL_XONXOFF_IN;
            case 4:
                return SerialPort.FLOWCONTROL_XONXOFF_OUT;
            default:
                throw new IllegalArgumentException("Unknown flow control value: " + value);
        }
    }
    
    private int parseStopBits( String value ) {
        int index = Arrays.asList( getAvailableStopBits()).indexOf( value );
        switch (index) {
            case 0:
                return SerialPort.STOPBITS_1;
            case 1:
                return SerialPort.STOPBITS_2;
            case 2:
                return SerialPort.STOPBITS_1_5;
            default:
                throw new IllegalArgumentException("Unknown stop bits value: " + value);
        }
    }
    
    private int parseParity( String value ) {
        return Arrays.asList( getAvailableParities() ).indexOf( value );
    }
    
}
