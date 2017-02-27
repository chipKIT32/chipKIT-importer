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

import java.util.Objects;

public final class SerialPortConfig {
    
    private final String portName;
    private final int baudRate;
    private final int flowControl;
    private final int dataBits;
    private final int stopBits;
    private final int parity;

    private SerialPortConfig( Builder b ) {
        this.portName = b.portName;
        this.baudRate = b.baudRate;
        this.flowControl = b.flowControl;
        this.dataBits = b.dataBits;
        this.stopBits = b.stopBits;
        this.parity = b.parity;
    }

    public String getPortName() {
        return portName;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public int getFlowControl() {
        return flowControl;
    }

    public int getDataBits() {
        return dataBits;
    }

    public int getStopBits() {
        return stopBits;
    }

    public int getParity() {
        return parity;
    }
    
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.portName);
        hash = 97 * hash + this.baudRate;
        hash = 97 * hash + this.flowControl;
        hash = 97 * hash + this.dataBits;
        hash = 97 * hash + this.stopBits;
        hash = 97 * hash + this.parity;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SerialPortConfig other = (SerialPortConfig) obj;
        if (this.baudRate != other.baudRate) {
            return false;
        }
        if (this.flowControl != other.flowControl) {
            return false;
        }
        if (this.dataBits != other.dataBits) {
            return false;
        }
        if (this.stopBits != other.stopBits) {
            return false;
        }
        if (this.parity != other.parity) {
            return false;
        }
        if (!Objects.equals(this.portName, other.portName)) {
            return false;
        }
        return true;
    }
    
    
    
    public static class Builder {
        private String portName;
        private int baudRate;
        private int flowControl;
        private int dataBits;
        private int stopBits;
        private int parity;
        
        public Builder() {}
        
        public Builder portName( String portName ) {
            this.portName = portName;
            return this;
        }
        
        public Builder baudRate( int baudRate ) {
            this.baudRate = baudRate;
            return this;
        }
        
        public Builder flowControl( int flowControl ) {
            this.flowControl = flowControl;
            return this;
        }
        
        public Builder dataBits( int dataBits ) {
            this.dataBits = dataBits;
            return this;
        }
        
        public Builder stopBits( int stopBits ) {
            this.stopBits = stopBits;
            return this;
        }
        
        public Builder parity( int parity ) {
            this.parity = parity;
            return this;
        }
        
        public SerialPortConfig build() {
            return new SerialPortConfig(this);
        }
        
    }    
}
