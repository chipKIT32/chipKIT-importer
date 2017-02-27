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

public enum ChipKitImportWizardProperty {

    ARDUINO_DIR("arduinoDir"),
    CHIPKIT_CORE_DIR("chipKitCoreDir"),
    CHIPKIT_BOARD_ID("chipKitBoardId"),
    CHIPKIT_BOARD_NAME("chipKitBoardName"),
    CHIPKIT_BOARD_CONFIG_NAVIGATOR("chipKitBoardConfigNavigator"),
    LAST_SOURCE_PROJECT_LOCATION("lastSourceProjectLocation"),
    LAST_CHIPKIT_CORE_LOCATION("lastChipKitCoreLocation"),
    LAST_ARDUINO_LOCATION("lastArduinoLocation"),
    COPY_CHIPKIT_FILES("copyChipKitFiles");

    private final String key;

    private ChipKitImportWizardProperty(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
