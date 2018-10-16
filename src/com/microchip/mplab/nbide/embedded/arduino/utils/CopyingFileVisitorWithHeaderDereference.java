package com.microchip.mplab.nbide.embedded.arduino.utils;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CopyingFileVisitorWithHeaderDereference extends CopyingFileVisitor {

    private static final Logger LOGGER = Logger.getLogger(CopyingFileVisitorWithHeaderDereference.class.getName());
    private static final PathMatcher SOURCE_FILE_MATCHER = FileSystems.getDefault().getPathMatcher("glob:*.{c,C,cpp,CPP,s,S,H,h,X,x}");

    public CopyingFileVisitorWithHeaderDereference(Path source, Path target) {
        super(source, target, SOURCE_FILE_MATCHER);
    }

    @Override
    protected void copyFile(Path source, Path target) {
        if (fileMatcher != null && !fileMatcher.matches(source.getFileName())) {
            // Don't copy files other than don't match the file matcher
            return;
        }

        try {
            String filename = source.getFileName().toString();
            if (filename.endsWith(".h") || filename.endsWith(".H")) {
                List<String> allLines = new ArrayList<>();
                appendLinesFromFile( allLines, source );
                Files.write(target, allLines, new OpenOption[] {StandardOpenOption.CREATE} );
            } else {
                Files.copy(source, target, options);
            }
        } catch (IOException x) {
            LOGGER.log(Level.WARNING, "Unable to copy: " + source, x);
        }
    }
    
    private void appendLinesFromFile( List<String> lines, Path filePath ) throws IOException {
        List<String> fileLines = Files.readAllLines(filePath);
        for ( int i=0; i<fileLines.size(); i++ ) {
            String line = fileLines.get(i).trim();
            if ( line.startsWith("#include ") ) {
                String[] tokens = line.split("\\s+");
                if ( tokens.length > 1 ) {
                    String includeString = tokens[1];
                    if ( includeString.startsWith("\"..") ) {
                        includeString = includeString.substring(1, includeString.length()-1);
                        Path includePath = Paths.get(source.toString(), includeString).normalize();
                        lines.add( "/*** Including contents of: " + includeString + " ***/ " );
                        appendLinesFromFile( lines, includePath );
                        lines.add( "/*** End of: " + includeString + " ***/" );
                    } else {
                        lines.add( line );
                    }
                } else {
                    lines.add( line );
                }
            } else {
                lines.add( line );
            }
        }
    }

}
