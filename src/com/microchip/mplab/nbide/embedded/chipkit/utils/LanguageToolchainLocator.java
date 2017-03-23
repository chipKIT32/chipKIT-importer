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

package com.microchip.mplab.nbide.embedded.chipkit.utils;

import com.microchip.crownking.opt.Version;
import com.microchip.mplab.nbide.embedded.api.LanguageToolchain;
import com.microchip.mplab.nbide.embedded.api.LanguageToolchainManager;
import static com.microchip.mplab.nbide.embedded.chipkit.importer.Requirements.MINIMUM_XC_TOOLCHAIN_VERSION;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class LanguageToolchainLocator {
    
    
    public Optional<LanguageToolchain> findSuitableLanguageToolchain() {
        List<LanguageToolchain> installedCompilers = LanguageToolchainManager.getDefault().getToolchains();
        Version minimumToolchainVersion = new Version(MINIMUM_XC_TOOLCHAIN_VERSION);

        Optional<LanguageToolchain> opt = installedCompilers.stream()
            .filter(lt -> lt.getMeta().getID().toLowerCase().equals("xc32"))
            .filter(lt -> minimumToolchainVersion.compareTo(getVersion(lt)) <= 0)
            .max( (LanguageToolchain lt1, LanguageToolchain lt2) -> getVersion(lt1).compareTo(getVersion(lt2)) );
        
        return opt;
    }
    
    private Version getVersion(LanguageToolchain lt) {
        Path p = Paths.get(lt.getDirectory());
        Path versionElement = p.getName(p.getNameCount() - 2);
        String versionString = versionElement.toString();
        if (versionString.startsWith("v")) {
            return new Version(versionString.substring(1));
        } else {
            return new Version(versionString);
        }
    }
}
