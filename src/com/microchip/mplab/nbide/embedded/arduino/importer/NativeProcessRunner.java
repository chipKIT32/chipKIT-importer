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

package com.microchip.mplab.nbide.embedded.arduino.importer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

public class NativeProcessRunner {

    
    public static final int NO_ERROR_CODE = 0;

    private ProcessBuilder processBuilder;
    private final Consumer<String> messageConsumer;
    private final Consumer<String> errorConsumer;  // TODO: Either remove the errorConsumer or find a way to stream errors to this consumer

    
    public NativeProcessRunner() {
        this( null, null );
    }
        
    public NativeProcessRunner( Consumer<String> messageAndErrorConsumer ) {
        this.messageConsumer = messageAndErrorConsumer;
        this.errorConsumer = messageAndErrorConsumer;
    }
    
    public NativeProcessRunner( Consumer<String> messageConsumer, Consumer<String> errorConsumer ) {        
        this.messageConsumer = messageConsumer;
        this.errorConsumer = errorConsumer;
    }
    
    public String getNativeProcessCommandString() {
        final StringBuilder commandBuilder = new StringBuilder();
        processBuilder.command().forEach( entry -> {
            if ( entry.startsWith("-") ) {
                commandBuilder.append(entry).append(' ');
            } else {
                commandBuilder.append('\"').append(entry).append('\"').append(' ');
            }
        });
        return commandBuilder.toString().trim();
    }
    
    public int runNativeProcess( String... args ) throws IOException, InterruptedException {
        return runNativeProcess( null, args );
    }
    
    public int runNativeProcess( Path workingDir, String... args ) throws IOException, InterruptedException {
        return runNativeProcess(workingDir, Arrays.asList(args));
    }
    
    public int runNativeProcess( Path workingDir, List <String> args ) throws IOException, InterruptedException {
        processBuilder = new ProcessBuilder( args )
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .redirectErrorStream(true)
            .directory( workingDir != null ? workingDir.toFile() : null );
        
        if ( messageConsumer != null ) {
            if ( workingDir != null ) {
                // TODO: Create message templates in Bundle and use them here:
                messageConsumer.accept( "-- Running native process in " + workingDir + ": " + getNativeProcessCommandString() + " --" );
            } else {
                messageConsumer.accept( "-- Running native process in default directory: " + getNativeProcessCommandString() + " --" );
            }
        }
        
        Process process = processBuilder.start();
        if ( messageConsumer != null ) {
            Scanner s = new Scanner( process.getInputStream() ).useDelimiter("\n");
            while (s.hasNext()) messageConsumer.accept(s.next());
        }
        process.waitFor();
        return process.exitValue();
    }
    
}
