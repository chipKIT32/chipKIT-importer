package com.microchip.mplab.nbide.embedded.arduino.importer.pic32;

import com.microchip.mplab.nbide.embedded.arduino.importer.Platform;
import java.io.IOException;
import java.nio.file.Path;

public class PIC32Platform extends Platform {
    
    public PIC32Platform(Platform parent, String vendor, Path rootPath) throws IOException {
        super(parent, vendor, "pic32", rootPath );
        putValue("compiler.c.cmd", "xc32-gcc");
        putValue("compiler.c.elf.cmd", "xc32-g++");
        putValue("compiler.cpp.cmd", "xc32-g++");
        putValue("compiler.ar.cmd", "xc32-ar");
        putValue("compiler.objcopy.cmd", "xc32-objcopy");
        putValue("compiler.elf2hex.cmd", "xc32-bin2hex");
        putValue("compiler.size.cmd", "xc32-size");
        
        putValue("build.extra_flags", getValue("build.extra_flags").orElse("") + " -mnewlib-libc");
        putValue("compiler.define", getValue("compiler.define").orElse("") + " -D__CTYPE_NEWLIB -DXPRJ_default=default");
        putValue("compiler.cpp.flags", getValue("compiler.cpp.flags").orElse("") + " -std=gnu++11");
        
        data.entrySet().forEach( e -> e.setValue( e.getValue().replaceAll(" -O2 ", " -O1 ") ) );
    }
    
}
