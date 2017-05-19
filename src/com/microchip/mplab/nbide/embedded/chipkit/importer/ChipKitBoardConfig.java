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


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


public class ChipKitBoardConfig {

    private final Map <String,String> data;

    public ChipKitBoardConfig( Map <String,String> data ) {        
        this.data = data;
    }

    public String getDeviceLinkerScriptFilename() {
        return data.get("ldscript");
    }
    
    public String getDeviceDebugLinkerScriptFilename() {
        return data.get("ldscript-debug");
    }

    public String getCommonLinkerScriptFilename() {
        return data.get("ldcommon");
    }
    
    public String getMCU() {
        return data.get("build.mcu");
    }
    
    public Path getVariantDirPath() {
        String value = data.get("build.variant.path");
        if ( value != null && !value.isEmpty() ) {
            return Paths.get( value );
        } else {
            return null;
        }
    }
    
    public Path getCoreDirPath() {
        return Paths.get( data.get("build.core.path") );
    }
    
    public String getFullyQualifiedBoardName() {
        return data.get("fqbn");
    }
    
    public Path getPackagesRootPath() {
        return Paths.get( data.get("packagesRoot") );
    }
    
    public Set <String> getExtraOptionsC() {
        Set <String> optionSet = new LinkedHashSet<>();
        parseOptions(optionSet, data.get("compiler.c.flags"));        
        parseOptions(optionSet, data.get("compiler.c.extra_flags"));
        parseOptions(optionSet, data.get("build.extra_flags"));
        removeRedundantCompilerOptions(optionSet);
        optionSet.add("-mnewlib-libc");
        return optionSet;
    }
    
    public Set <String> getCompilerWarnings() {
        Set <String> optionSet = new LinkedHashSet<>();
        parseOptions(optionSet, data.get("compiler.warning_flags.all"));
        return optionSet;
    }
    
    public Set <String> getExtraOptionsAS() {
        Set <String> optionSet = new LinkedHashSet<>();
        parseOptions(optionSet, data.get("compiler.S.flags"));
        parseOptions(optionSet, data.get("compiler.c.extra_flags"));
        parseOptions(optionSet, data.get("build.extra_flags"));
        removeRedundantCompilerOptions(optionSet);
        return optionSet;
    }
    
    public Set <String> getExtraOptionsCPP() {
        Set <String> optionSet = new LinkedHashSet<>();
        parseOptions(optionSet, data.get("compiler.cpp.flags"));
        parseOptions(optionSet, data.get("compiler.cpp.extra_flags"));        
        parseOptions(optionSet, data.get("build.extra_flags"));
        removeRedundantCompilerOptions(optionSet);
        optionSet.add("-mnewlib-libc");
        optionSet.add("-std=gnu++11");
        return optionSet;
    }
    
    public Set <String> getExtraOptionsLD( boolean debug, boolean coreCopied ) {
        String chipkitCoreDirectory = data.get("build.core.path");
        String chipkitVariantDirectory = data.get("build.variant.path");
        String ldScriptDirectory = data.get("build.ldscript_dir.path");
        String ldscript = debug ? data.get("ldscript-debug") : data.get("ldscript");
        String ldcommon = data.get("ldcommon");
        Set <String> optionSet = new LinkedHashSet<>();
        parseOptions( optionSet, data.get("compiler.c.elf.flags") );
        removeRedundantCompilerOptions(optionSet);
        removeRedundantLinkerOptions(optionSet);
        optionSet.add("-mnewlib-libc");
        if ( coreCopied ) {
            optionSet.add("-T\"" + ldscript + "\"");
            optionSet.add("-T\"" + ldcommon + "\"");
        } else {
            Path ldcommonPath = Paths.get( chipkitCoreDirectory, ldcommon );
            Path ldscriptPath = Paths.get( debug && !ldScriptDirectory.isEmpty() ? ldScriptDirectory : chipkitCoreDirectory, ldscript );
            if ( !Files.exists(ldscriptPath) && !debug ) {
                ldscriptPath = Paths.get( chipkitVariantDirectory, ldscript );
            }
            optionSet.add("-T\"" + ldscriptPath.toString() + "\"");
            optionSet.add("-T\"" + ldcommonPath.toString() + "\"");
        }
        return optionSet;
    }
    
    public String getCompilerMacros() {
        return new StringBuilder()
            .append("F_CPU=").append( data.get("build.f_cpu") ).append(";")
            .append("ARDUINO=").append( data.get("runtime.ide.version") ).append(";")
            .append( data.get("build.board") ).append(";")
            // TODO: The next three macros should be read from some chipKIT/Arduino files    
            .append("MPIDEVER=16777998" ).append(";")
            .append("MPIDE=150" ).append(";")
            .append("IDE=Arduino" ).append(";")
            .append("XPRJ_default=default" ).append(";")
            .append("__CTYPE_NEWLIB").toString();
    }
    
    private static void removeRedundantCompilerOptions( Set <String> optionSet ) {
        Iterator <String> iter = optionSet.iterator();
        while ( iter.hasNext() ) {
            String option = iter.next();
            if ( option.equals("-g") || option.equals("-c") || option.equals("-w") || option.startsWith("-O") ) {
                iter.remove();
            }
        }
    }
    
    private static void removeRedundantLinkerOptions( Set <String> optionSet ) {
        Iterator <String> iter = optionSet.iterator();
        while ( iter.hasNext() ) {
            String option = iter.next();
            if ( option.startsWith("-Wl") ) {  // -Wl group will be added later
                iter.remove();
            }
        }
    }
    
    private static void parseOptions( Set <String> optionsSet, String optionsString ) {
        String[] options = optionsString.split("::|\\s+");
        for ( String s : options ) {
            optionsSet.add(s);
        }
    }
    
}
