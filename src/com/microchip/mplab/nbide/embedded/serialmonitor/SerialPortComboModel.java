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

import javax.swing.DefaultComboBoxModel;
import javax.swing.Timer;

public class SerialPortComboModel extends DefaultComboBoxModel<String> {

    private static final int SERIAL_DEVICE_LIST_REFRESH_INTERVAL = 2000; // ms
    private static final int SERIAL_DEVICE_AVAILABILITY_TEST_INTERVAL = 2000; // ms
    
    private final SerialMonitorConfigModel configModel;
    private String[] deviceNames;
    private long lastSerialDeviceCheckTime = 0;
    private Timer serialPortMonitoringTimer;
    
    public SerialPortComboModel( SerialMonitorConfigModel configModel ) {
        this.configModel = configModel;
    }

    @Override
    public String getElementAt(int index) {
        return deviceNames[index];
    }

    @Override
    public void setSelectedItem(Object anObject) {
        super.setSelectedItem(anObject);
        
        if ( anObject != null ) {
            String deviceName = anObject.toString();
            if ( serialPortMonitoringTimer != null && serialPortMonitoringTimer.isRunning() ) {
                serialPortMonitoringTimer.stop();
            }

            serialPortMonitoringTimer = new Timer(SERIAL_DEVICE_AVAILABILITY_TEST_INTERVAL, (a) -> {
                if ( !configModel.isDeviceAvailable(deviceName) ) {
                    serialPortMonitoringTimer.stop();
                    super.setSelectedItem(null);
                }
            });
            serialPortMonitoringTimer.start();
        }
    }

    @Override
    public int getSize() {
        if ( System.currentTimeMillis()-lastSerialDeviceCheckTime > SERIAL_DEVICE_LIST_REFRESH_INTERVAL ) {
            deviceNames = configModel.getAvailableDeviceNames();
            lastSerialDeviceCheckTime = System.currentTimeMillis();
        }
        return deviceNames.length;
    }
    
}
