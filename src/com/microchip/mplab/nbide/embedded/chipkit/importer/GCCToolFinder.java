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

import com.microchip.mplab.nbide.embedded.api.LanguageTool;
import com.microchip.mplab.nbide.embedded.api.LanguageToolchain;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.openide.util.Utilities;

public class GCCToolFinder {
    
    
    private LanguageToolchain toolchain;
    private Path rootToolsPath;

    
    public GCCToolFinder(LanguageToolchain toolchain) {
        this.toolchain = toolchain;
    }
    
    public GCCToolFinder(Path rootToolsPath) {
        this.rootToolsPath = rootToolsPath;
    }
    
    public Path findTool( int toolId ) {
        if ( toolchain != null ) {
            String pathStrnig = ( toolId == LanguageTool.MakeTool && Utilities.isWindows() ) ? "make" : toolchain.getTool(toolId).getPath();
            return Paths.get(pathStrnig);
        } else {
            String suffix;
            switch ( toolId ) {
                case LanguageTool.Archiver:
                    suffix = "-ar";
                    break;
                case LanguageTool.CCCompiler:
                    suffix = "-g++";
                    break;
                case LanguageTool.CCompiler:
                    suffix = "-gcc";
                    break;
                case LanguageTool.Assembler:
                    suffix = "-as";
                    break;
                case LanguageTool.MakeTool:  // Special case for Make
                    return Paths.get("make");
                default:
                    throw new RuntimeException("Unsupported Tool ID: " + toolId);
            }

            try {
                return Files.list(rootToolsPath)
                        .filter( file -> !Files.isDirectory(file) )
                        .filter( file -> {
                            String filename = file.getFileName().toString();
                            // Remove the extension if there is one:
                            int extensionIndex = filename.indexOf('.');
                            if ( extensionIndex != -1 ) {
                                filename = filename.substring(0, extensionIndex);
                            }
                            return filename.endsWith(suffix);
                        })
                        .findAny()
                        .get();
            } catch (IOException ex) {
                throw new RuntimeException("Failed to locate tool: " + toolId, ex);
            }
        }
    }
}
