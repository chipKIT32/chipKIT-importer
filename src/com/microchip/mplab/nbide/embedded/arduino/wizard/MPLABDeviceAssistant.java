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

import com.microchip.crownking.mplabinfo.DeviceSupport;
import com.microchip.crownking.mplabinfo.DeviceSupportException;
import com.microchip.crownking.opt.Version;
import com.microchip.mplab.nbide.embedded.api.LanguageTool;
import com.microchip.mplab.nbide.embedded.api.LanguageToolchain;
import com.microchip.mplab.nbide.embedded.api.LanguageToolchainManager;
import com.microchip.mplab.nbide.embedded.api.LanguageToolchainMeta;
import com.microchip.mplab.nbide.embedded.arduino.importer.BoardConfiguration;
import static com.microchip.mplab.nbide.embedded.arduino.importer.Requirements.MINIMUM_XC_TOOLCHAIN_VERSION;
import static com.microchip.mplab.nbide.embedded.makeproject.api.wizards.WizardProperty.LANGUAGE_TOOL_META_ID;
import com.microchip.mplab.nbide.embedded.makeproject.ui.wizards.WizardProjectConfiguration;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Logger;
import org.openide.WizardDescriptor;
import org.openide.util.Exceptions;

public class MPLABDeviceAssistant {

    private static final Logger LOGGER = Logger.getLogger(MPLABDeviceAssistant.class.getName());
    
    private String deviceName = "";
    private LanguageToolchain languageToolchain = null;
    
    public void storeSettings(WizardDescriptor settings) {
        WizardProjectConfiguration.storeDeviceHeaderPlugin(settings, deviceName);
        
        Optional.ofNullable( languageToolchain )
                .map( LanguageToolchain::getMeta )
                .map( LanguageToolchainMeta::getID )
                .ifPresent( id -> settings.putProperty(LANGUAGE_TOOL_META_ID.key(), id) );
    }

    public String getDeviceName() {
        return deviceName;
    }

    public LanguageToolchain getLanguageToolchain() {
        return languageToolchain;
    }
    
    public boolean isToolchainValid() {
        return languageToolchain != null;        
    }
    
    public void updateDeviceAndToolchain( BoardConfiguration boardConfiguration ) {
        if ( boardConfiguration != null ) {
            deviceName = boardConfiguration.getValue("build.mcu").flatMap( this::findMPLABDeviceNameForMCU ).orElse("");
            languageToolchain = findMatchingLanguageToolchain(deviceName).orElse(null);
            LOGGER.info("Selected toolchain: " + languageToolchain);
        } else {
            deviceName = "";
            languageToolchain = null;
        }
    }
    
    
    private Optional <String> findMPLABDeviceNameForMCU(String mcu) {
        String lowerCaseMCU = mcu.toLowerCase();
        try {
            return Arrays.stream( DeviceSupport.getInstance().getDeviceNames() )
//                .peek( n -> System.out.println(n.toLowerCase() + " - " + lowerCaseMCU) )
                .filter( n -> n.toLowerCase().contains(lowerCaseMCU) )
                .min( (n1, n2) -> Integer.signum( (n1.length()-mcu.length()) - (n2.length()-mcu.length()) ) );
        } catch (DeviceSupportException ex) {
            Exceptions.printStackTrace(ex);
        }
        return Optional.empty();
    }
    
    private Optional <LanguageToolchain> findMatchingLanguageToolchain( String device ) {
        if ( device != null ) {
            Version minimumToolchainVersion = new Version(MINIMUM_XC_TOOLCHAIN_VERSION);
            
            LOGGER.info("Minimum XC32 version is " + minimumToolchainVersion);
            
            return LanguageToolchainManager.getDefault().getToolchains()
                .stream()                
                .peek( tc -> LOGGER.info(tc.getDirectory() + " : " + tc.getMeta().getSupportedDevices() ) )
                .filter(tc -> tc.getSupport(device).isSupported())
                .filter(tc -> tc.getTool(LanguageTool.CCCompiler) != null)
                .peek(tc -> LOGGER.info( "Toolchain candidate: " + tc ) )
                .filter(tc -> minimumToolchainVersion.compareTo(getVersion(tc)) <= 0)
                .max( (LanguageToolchain lt1, LanguageToolchain lt2) -> getVersion(lt1).compareTo(getVersion(lt2)) );
        } else {
            return Optional.empty();
        }
    }
    
    private Version getVersion(LanguageToolchain tc) {
        Path p = Paths.get(tc.getDirectory());
        Path versionElement = p.getName(p.getNameCount() - 2);
        String versionString = versionElement.toString();
        if (versionString.startsWith("v")) {
            return new Version(versionString.substring(1));
        } else {
            return new Version(versionString);
        }
    }
    
}
