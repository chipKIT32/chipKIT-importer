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

package com.microchip.mplab.nbide.embedded.arduino.wizard.pic32;

import com.microchip.mplab.nbide.embedded.arduino.importer.LibCoreBuilder;
import com.microchip.mplab.nbide.embedded.arduino.importer.ProjectImporter;
import com.microchip.mplab.nbide.embedded.arduino.wizard.ProjectConfigurationImporter;
import com.microchip.mplab.nbide.embedded.makeproject.api.configurations.MakeConfiguration;
import com.microchip.mplab.nbide.embedded.makeproject.api.configurations.MakeConfigurationBook;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Predicate;

public final class PIC32ProjectConfigurationImporter extends ProjectConfigurationImporter {
    
    private static final String DEFAULT_OPTIMIZATION_OPTION = "-O1";
    private static final String DEBUG_CONF_NAME = "debug";
    
    public PIC32ProjectConfigurationImporter(ProjectImporter importer, boolean copyFiles, MakeConfigurationBook projectDescriptor, File targetProjectDir) {
        super(importer, copyFiles, projectDescriptor, targetProjectDir);
    }

    @Override
    public void run() throws IOException {
        Set<String> cppAppendOptionsSet = getExtraOptionsCPP();        
        boolean cppExceptions = !cppAppendOptionsSet.remove("-fno-exceptions");        
        String cppAppendOptions = String.join(" ", cppAppendOptionsSet);
        
        String includeDirectories = assembleIncludeDirectories();
        String preprocessorMacros = getCompilerMacros();
        String ldOptions = String.join( " ", getExtraOptionsLD(false, isCopyFiles()) );
        String ldDebugOptions = String.join( " ", getExtraOptionsLD(true, isCopyFiles()) );
        String ldAppendOptions;
        ldAppendOptions = String.format("-L%s,-l%s -lm -T%s -T%s", 
            ProjectImporter.CORE_DIRECTORY_NAME,
            LibCoreBuilder.LIB_CORE_NAME,
            findDeviceLinkerScript(),
            findCommonLinkerScript()
        );
        String cAppendOptions = String.join(" ", getExtraOptionsC());
        
        getProjectDescriptor().getConfs().getConfigurtions().forEach( c-> {
            MakeConfiguration mc = (MakeConfiguration) c;            
            setAuxOptionValue(mc, "C32Global", "common-include-directories", includeDirectories);
            setAuxOptionValue(mc, "C32Global", "legacy-libc", "false");
            setAuxOptionValue(mc, "C32", "preprocessor-macros", preprocessorMacros);
            setAuxOptionValue(mc, "C32", "optimization-level", DEFAULT_OPTIMIZATION_OPTION );
            setAuxOptionValue(mc, "C32CPP", "preprocessor-macros", preprocessorMacros);
            setAuxOptionValue(mc, "C32CPP", "optimization-level", DEFAULT_OPTIMIZATION_OPTION );
            setAuxOptionValue(mc, "C32CPP", "exceptions", Boolean.toString(cppExceptions));
            setAuxOptionValue(mc, "C32-LD", "oXC32ld-extra-opts", c.getName().equals(DEBUG_CONF_NAME) ? ldDebugOptions : ldOptions );
            setAuxOptionValue(mc, "C32-LD", "remove-unused-sections", "true");
            setAppendixValue(mc, "C32", cAppendOptions);
            setAppendixValue(mc, "C32CPP", cppAppendOptions);
            setAppendixValue(mc, "C32-LD", ldAppendOptions);
        });
    }

    private String findDeviceLinkerScript() throws IOException {
        return findLinkerScript( p -> !p.getFileName().toString().endsWith("COMMON.ld") );
    }
    
    private String findCommonLinkerScript() throws IOException {
        return findLinkerScript( p -> p.getFileName().toString().endsWith("COMMON.ld") );
    }
    
    private String findLinkerScript( Predicate<Path> filter ) throws IOException {
        final Path projectPath = getTargetProjectDir().toPath().toAbsolutePath();
        return getImporter().getLinkerScriptPaths()
            .filter( filter )
            .findFirst()
            .map( p -> projectPath.relativize(p) )
            .map( p -> p.toString() )
            .orElse("");
    }
    
}
