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

package com.microchip.mplab.nbide.embedded.chipkit.importer;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class BootloaderPathProvider {

    // TODO: Fill in the missing board ID -> bootloader mappings
    private final Map <String,String> boardIdToHexFileNameLookup = new HashMap<String,String>(){{
        put("cerebot32mx4", "Cerebot-32MX4-USB.hex");
        put("cerebot32mx7", "Cerebot-32MX7-USB.hex");
        put("cerebot_mx3ck_512", "Cerebot-MX3cK-512.hex");
        put("cerebot_mx3ck", "Cerebot-MX3cK.hex");
        put("cerebot_mx4ck", "Cerebot-MX4cK.hex");
        put("cerebot_mx7ck", "Cerebot-MX7cK.hex");
        put("chipkit_DP32", "chipKIT-DP32.hex");
        put("chipkit_mx3", "Cerebot-MX3cK.hex");
        put("chipkit_Pi", "chipKIT_Pi.hex");
        put("chipkit_Pi_USB_Serial", "chipKIT-PI-USB.hex"); 
        put("chipkit_pro_mx4", "Cerebot-32MX4-USB.hex");
        put("chipkit_pro_mx7", "Cerebot-32MX7-USB.hex");
        put("chipkit_uc32", "chipKIT-uC32.hex");
        put("chipkit_WF32", "chipKIT-WF32.hex");
        put("chipkit_WiFire_80MHz", "chipKIT-WiFire-80MHZ.hex");
        put("chipkit_WiFire_AB", "chipKIT-WiFire.hex");
        put("chipkit_WiFire", "chipKIT-WiFire-EF.hex");
        put("clicker2", "clicker-2ge.hex");                 // Currently this board is not supported due to a HID bootloader issue
        put("cmod", "chipKIT-Cmod.hex");
        put("cui32", "");                                   // X
        put("CUI32stem", "");                               // X
        put("fubarino_mini_dev", "Fubarino-Mini_USB_48MHz.hex");                       
        put("fubarino_mini", "Fubarino-Mini.hex");          
        put("fubarino_sd", "FUBARINO_SD_512K_USB.hex");     
        put("fubarino_sd_seeed", "FUBARINO_SD_512K_USB.hex"); 
        put("Fubarino_SDZ", "Fubarino_SDZ_UART.hex");
        put("lenny", "MAJENKO_LENNY_40MHZ.hex");
        put("mega_pic32", "chipKIT-Max32.hex");
        put("mega_usb_pic32", "chipKIT-Max32.hex");
        put("Olimex_Pinguino32", "Olimex_PIC32_Pinguino.hex"); 
        put("openbci", "UDB32-MX2-DIP.hex");
        put("OpenScope", "OpenScope.hex");
        put("picadillo_35t", "PICADILLO_35T.hex");
        put("quick240_usb_pic32", "Quick240.hex");
        put("ubw32_mx460", "UBW32_MX460.hex");
        put("ubw32_mx795", "UBW32_MX795.hex");              
        put("uc32_pmod", "chipKIT-uC32.hex");               
        put("uno_pic32", "chipKIT-Uno32.hex");
        put("uno_pmod", "chipKIT-zUno.hex");                
        put("usbono_pic32", "");                            // X
        put("PONTECH_NoFire", "chipKIT-WiFire-80MHZ.hex");  
    }};
    
    private final Function<String,Path> hexFileLocator;    
    
    public BootloaderPathProvider( Function<String,Path> hexFileLocator ) {
        this.hexFileLocator = hexFileLocator;
    }
    
    public Path getBootloaderPath( String boardID ) {
        String hexFileName = findHexFileNameForBoard(boardID);
        if ( hexFileName == null || hexFileName.isEmpty() ) {
            return null;
        } else {
            return hexFileLocator.apply( hexFileName );
        }
    }
    
    private String findHexFileNameForBoard( String boardID ) {
        return boardIdToHexFileNameLookup.get(boardID);
    }
    
}
