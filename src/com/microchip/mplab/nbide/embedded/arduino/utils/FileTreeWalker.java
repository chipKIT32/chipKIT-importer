package com.microchip.mplab.nbide.embedded.arduino.utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class FileTreeWalker {

    private FileTreeWalker() {}
    
    public static Stream<Path> walk( Path start ) throws IOException {
        List <Path> ret = new ArrayList<Path>();
        Files.walkFileTree( 
            start,
            new FileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file , BasicFileAttributes attrs) throws IOException {
                    ret.add( file );
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file , IOException e) throws IOException {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir , BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            }
        );
        return ret.stream();
    }
    
}
