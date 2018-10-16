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

package com.microchip.mplab.nbide.embedded.arduino.utils;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileVisitResult;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@code FileVisitor} that copies a file-tree ("cp -r")
 * Adapted from: https://docs.oracle.com/javase/tutorial/essential/io/examples/Copy.java
 */
public class CopyingFileVisitor implements FileVisitor<Path> {
        
    private static final Logger LOGGER = Logger.getLogger(CopyingFileVisitor.class.getName());
    
    protected final CopyOption[] options = new CopyOption[] { COPY_ATTRIBUTES, REPLACE_EXISTING };
    protected final Path source;
    protected final Path target;
    protected final PathMatcher directoryMatcher; 
    protected final PathMatcher fileMatcher;

    
    public CopyingFileVisitor(Path source, Path target) {
        this( source, target, null, null );
    }
    
    public CopyingFileVisitor(Path source, Path target, PathMatcher fileMatcher) {
        this( source, target, fileMatcher, null );
    }
        
    public CopyingFileVisitor(Path source, Path target, PathMatcher fileMatcher, PathMatcher directoryMatcher) {
        this.source = source;
        this.target = target;
        this.directoryMatcher = directoryMatcher;  // Useful for skipping directories with test code or examples
        this.fileMatcher = fileMatcher;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {        
        // Skip directories that don't match the directory matcher
        if ( directoryMatcher != null && !directoryMatcher.matches(dir.getFileName()) ) {
            return SKIP_SUBTREE;
        }

        Path newdir = target.resolve(source.relativize(dir));
        if ( !Files.exists(newdir) ) {
            try {
                Files.copy(dir, newdir, options);
            } catch (FileAlreadyExistsException x) {
                // We're always overwriting so this exception should never occur
            } catch (IOException x) {
                LOGGER.log( Level.WARNING, "Unable to create: " + newdir, x);
                return SKIP_SUBTREE;
            }
        }
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        copyFile(file, target.resolve(source.relativize(file)));
        return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        if (exc == null) {
            // Remove the new directory if it turned out to be empty
            Path newdir = target.resolve(source.relativize(dir));
            try {            
                if ( Files.list(newdir).count() == 0 ) {
                    Files.delete(newdir);
                    return CONTINUE;
                }                                
            } catch (IOException ex) {
                LOGGER.log( Level.WARNING, "Unable to delete empty directory: " + newdir, ex );
            }
            
            // Fix up modification time of directory when done
            try {
                FileTime time = Files.getLastModifiedTime(dir);
                Files.setLastModifiedTime(newdir, time);
            } catch (IOException x) {
                LOGGER.log(Level.WARNING, "Unable to copy all attributes to: " + newdir, x);
            }
        }
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        if (exc instanceof FileSystemLoopException) {
            LOGGER.log( Level.WARNING, "Cycle detected: " + file, exc );
        } else {
            LOGGER.log( Level.WARNING, "Unable to copy: " + file, exc );
        }
        return CONTINUE;
    }

    protected void copyFile(Path source, Path target) {
        if ( fileMatcher != null && !fileMatcher.matches(source.getFileName()) ) {
            // Don't copy files other than don't match the file matcher
            return;
        }

        try {
            Files.copy(source, target, options);
        } catch (IOException x) {
            LOGGER.log( Level.WARNING, "Unable to copy: " + source, x );
        }
    }
}    
