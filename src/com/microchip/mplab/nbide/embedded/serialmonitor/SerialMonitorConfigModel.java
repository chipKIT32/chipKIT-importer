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

public interface SerialMonitorConfigModel {
    
    String[] getAvailablePortNames();
    String[] getAvailableDeviceNames();
    boolean isDeviceAvailable( String deviceName );
    String[] getAvailableBaudRates();
    String getDefaultBaudRate();
    String[] getAvailableFlowControl();
    String getDefaultFlowControl();
    String[] getAvailableDataBits();
    String getDefaultDataBits();
    String[] getAvailableStopBits();
    String getDefaultStopBits();
    String[] getAvailableParities();
    String getDefaultParity();
    void setCurrentPortName( String value );
    String getCurrentPortName();
    void setCurrentBaudRate( String value );
    String getCurrentBaudRate();
    void setCurrentFlowControl( String value );
    String getCurrentFlowControl();
    void setCurrentDataBits( String value );
    String getCurrentDataBits();
    void setCurrentStopBits( String value );
    String getCurrentStopBits();
    void setCurrentParity( String value );
    String getCurrentParity();
    SerialPortConfig getCurrentConfig();
    
}
