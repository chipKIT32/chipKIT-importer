/* Default linker script, for normal executables */
OUTPUT_FORMAT("elf32-tradlittlemips")
OUTPUT_ARCH(pic32mx)
ENTRY(_reset)
/*
 * Provide for a minimum stack and heap size
 * - _min_stack_size - represents the minimum space that must be made
 *                     available for the stack.  Can be overridden from
 *                     the command line using the linker's --defsym option.
 * - _min_heap_size  - represents the minimum space that must be made
 *                     available for the heap.  Can be overridden from
 *                     the command line using the linker's --defsym option.
 */
EXTERN (_min_stack_size _min_heap_size)
PROVIDE(_min_stack_size = 0x800) ;
PROVIDE(_min_heap_size = 0x800) ;

/*************************************************************************
 * Processor-specific object file.  Contains SFR definitions.
 *************************************************************************/
INPUT("processor.o")
OPTIONAL("libmchp_peripheral_PIC32MX150F128D.a") 

/*************************************************************************
 * Memory Regions
 *
 * Memory regions without attributes cannot be used for orphaned sections.
 * Only sections specifically assigned to these regions can be allocated
 * into these regions.
 *************************************************************************/
MEMORY
{
  exception_mem              : ORIGIN = 0x9D000000, LENGTH = 0x1000
  kseg0_program_mem    (rx)  : ORIGIN = 0x9D001000, LENGTH = 0x1E000
  kseg0_eeprom_mem           : ORIGIN = 0x9D01F000, LENGTH = 0x1000
  kseg0_boot_mem             : ORIGIN = 0x9FC00200, LENGTH = 0
  kseg1_boot_mem             : ORIGIN = 0xBFC00000, LENGTH = 0 
  debug_exec_mem             : ORIGIN = 0xBFC00490, LENGTH = 0
  config3                    : ORIGIN = 0xBFC00BF0, LENGTH = 0x4
  config2                    : ORIGIN = 0xBFC00BF4, LENGTH = 0x4
  config1                    : ORIGIN = 0xBFC00BF8, LENGTH = 0x4
  config0                    : ORIGIN = 0xBFC00BFC, LENGTH = 0x4
  kseg1_data_mem       (w!x) : ORIGIN = 0xA0000000, LENGTH = 0x8000
  sfrs                       : ORIGIN = 0xBF800000, LENGTH = 0x100000
  configsfrs                 : ORIGIN = 0xBFC00BF0, LENGTH = 0x10
}

/*************************************************************************
 * Memory Address Equates
 *************************************************************************/
_ebase_address  	    = ORIGIN(exception_mem);
_IMAGE_PTR_TABLE       	= _ebase_address + 0x0F8;
_IMAGE_HEADER_ADDR      = _ebase_address + 0x0FC;
_GEN_EXCPT_ADDR         = _ebase_address + 0x180;
_RESET_ADDR             = ORIGIN(kseg0_program_mem);
_EEPROM_ADDR            = ORIGIN(kseg0_eeprom_mem);
_BEV_EXCPT_ADDR         = 0xBFC00380;
_DBG_EXCPT_ADDR         = 0xBFC00480;
_DBG_CODE_ADDR          = ORIGIN(debug_exec_mem);

/*************************************************************************
 *  Bootloader program directives.
 *  
 * _IMAGE_TYPE
 *
 *  image type:
 */
     
_imageReserved                      = 0x00000000 ;
_imageMPIDE                         = 0x00000001 ;  /* This is a normal MPIDE sketch                                                                                                    */
_imageBootFlashBootloader           = 0x00000002 ;  /* This is a boot flash bootloader                                                                                                  */
_imageProgramFlashBootloader        = 0x00000004 ;  /* This is a program flash bootloader                                                                                               */
_imageSplitFlashBootloader          = 0x00000008 ;  /* This has bootloader code in both boot and program flash                                                                          */

 /*
 *  Instructions for the bootloader
 */
                                                                       
_imageFullFlashEraseLess4KEEProm    = 0x00010000 ;  /* The original bootloader method of erasing all of program flash except the last 4K reserved for eeprom                            */
_imageJustInTimeFlashErase          = 0x00020000 ;  /* Only flash pages written too needed by the sketch is erased                                                                      */
_imageLinkerSpecifiedFlashErase     = 0x00040000 ;  /* The linker defines the flash range to erase                                                                                      */
_imageFullFlashErase                = 0x00080000 ;  /* All of flash is erased                                                                                                           */
_imageExecutionJumpAddress          = 0x01000000 ;  /* the bootloader will jump to the execution jump address immediately after programming                                             */
_imageExecutionJumpToFirstInFlash   = 0x02000000 ;  /* the bootloader will jump to the first sketch loaded in flash ignoring the execution jump address immediately after programming   */
 
/*  
 * _IMAGE_FLASH_SIZE 
 *
 *      Typically _imageJustInTimeFlashErase is selected to just erase the pages
 *      of flash that code is written too; thus leaving all other flash pages untouched.
 *  
 *      If _imageLinkerSpecifiedFlashErase set, then the range
 *      starting from _ebase_address for _IMAGE_FLASH_SIZE bytes are erased.
 *
 *      If _imageFullFlashErase is specified, than the whole flash
 *      as known by the bootloader will be erased. This will erase eeprom as well
 *
 *      if _imageFullFlashEraseLess4KEEProm is set, all of flash less the last 4K is
 *      erased, this is the old default. This bit could be set to make a program flash bootloader
 *      erasing everything but the old flash. If NOTHING is set, this will be the default as this is the old behavior.
 *  
 *  _JUMP_ADDR
 *  
 *      This is the address that the bootloader will jump to start execution
 *      of the sketch. This is almost always _RESET_ADDR.
 *
 *      However, you can specify an alternate entry execution point for example
 *      if you have alternate starup code that, say, shared
 *      the runtime with other sketches or needed some kind of specific handling
 *
 *      Immediately after programming (avrdude upload) the bootloader will typically
 *      jump to the just loaded sketch, no matter where it was loaded in flash.
 *      _imageExecutionJumpToFirstInFlash will tell the bootloader to jump to the first
 *      sketch in flash even if the just loaded one is not at the beginning of flash.
 *      This is useful when programming sketches in slots of flash and then always
 *      jumping to the program-flash loader (vector sketch) as if the board was just reset.
 *      This bit does not effect jumping to a sketch already in flash after reset.
 *      As of today, after reset, the first program in flash will always be jumped to.
 *
 *************************************************************************/
 _IMAGE_TYPE            = _imageMPIDE | _imageJustInTimeFlashErase | _imageExecutionJumpAddress;
 _IMAGE_FLASH_SIZE      = LENGTH(exception_mem) + LENGTH(kseg0_program_mem);
_JUMP_ADDR              = _RESET_ADDR;

SECTIONS
{
#if defined(__DEBUG_D)
  /* Jump from reset vector */
  .jump ORIGIN(kseg1_boot_mem): {
    LONG(0x3C1F9D00)  /* lui ra 0x9d00 */
    LONG(0x37FF1000)  /* ori ra 0x1000 */
    LONG(0x03E00008)  /* jr ra */
    LONG(0x00000000)  /* nop */
  } > kseg1_boot_mem
#else
  /* Insert bootloader */
  .jump ORIGIN(kseg1_boot_mem): {
    LONG(0x3C1ABFC0)	/* 0x1FC00000 */
    LONG(0x275A0010)	/* 0x1FC00004 */
    LONG(0x3400008)		/* 0x1FC00008 */
    LONG(0x0)			/* 0x1FC0000C */
    LONG(0x3C1DA000)	/* 0x1FC00010 */
    LONG(0x27BD1FF8)	/* 0x1FC00014 */
    LONG(0x3C1CA001)	/* 0x1FC00018 */
    LONG(0x279C85F0)	/* 0x1FC0001C */
    LONG(0x40096002)	/* 0x1FC00020 */
    LONG(0x1205820)		/* 0x1FC00024 */
    LONG(0x7D2A1E80)	/* 0x1FC00028 */
    LONG(0x7D494984)	/* 0x1FC0002C */
    LONG(0x40896002)	/* 0x1FC00030 */
    LONG(0xC0)			/* 0x1FC00034 */
    LONG(0x41DCE000)	/* 0x1FC00038 */
    LONG(0x408B6002)	/* 0x1FC0003C */
    LONG(0x3C08A000)	/* 0x1FC00040 */
    LONG(0x25080620)	/* 0x1FC00044 */
    LONG(0x3C09A000)	/* 0x1FC00048 */
    LONG(0x25290660)	/* 0x1FC0004C */
    LONG(0x10000006)	/* 0x1FC00050 */
    LONG(0x0)			/* 0x1FC00054 */
    LONG(0xAD000000)	/* 0x1FC00058 */
    LONG(0xAD000004)	/* 0x1FC0005C */
    LONG(0xAD000008)	/* 0x1FC00060 */
    LONG(0xAD00000C)	/* 0x1FC00064 */
    LONG(0x25080010)	/* 0x1FC00068 */
    LONG(0x109082B)		/* 0x1FC0006C */
    LONG(0x1420FFF9)	/* 0x1FC00070 */
    LONG(0x0)			/* 0x1FC00074 */
    LONG(0x3C089FC0)	/* 0x1FC00078 */
    LONG(0x25080AA8)	/* 0x1FC0007C */
    LONG(0x8D090000)	/* 0x1FC00080 */
    LONG(0x11200018)	/* 0x1FC00084 */
    LONG(0x25080004)	/* 0x1FC00088 */
    LONG(0x8D0A0000)	/* 0x1FC0008C */
    LONG(0x25080004)	/* 0x1FC00090 */
    LONG(0x8D0B0000)	/* 0x1FC00094 */
    LONG(0x11600009)	/* 0x1FC00098 */
    LONG(0x25080004)	/* 0x1FC0009C */
    LONG(0x910C0000)	/* 0x1FC000A0 */
    LONG(0x254AFFFF)	/* 0x1FC000A4 */
    LONG(0x25080001)	/* 0x1FC000A8 */
    LONG(0xA12C0000)	/* 0x1FC000AC */
    LONG(0x1540FFFB)	/* 0x1FC000B0 */
    LONG(0x25290001)	/* 0x1FC000B4 */
    LONG(0x10000005)	/* 0x1FC000B8 */
    LONG(0x0)			/* 0x1FC000BC */
    LONG(0xA1200000)	/* 0x1FC000C0 */
    LONG(0x254AFFFF)	/* 0x1FC000C4 */
    LONG(0x1540FFFD)	/* 0x1FC000C8 */
    LONG(0x25290001)	/* 0x1FC000CC */
    LONG(0x25080003)	/* 0x1FC000D0 */
    LONG(0x240AFFFC)	/* 0x1FC000D4 */
    LONG(0x1484024)		/* 0x1FC000D8 */
    LONG(0x8D090000)	/* 0x1FC000DC */
    LONG(0x1520FFE7)	/* 0x1FC000E0 */
    LONG(0x0)			/* 0x1FC000E4 */
    LONG(0x3C09BFC0)	/* 0x1FC000E8 */
    LONG(0x25290000)	/* 0x1FC000EC */
    LONG(0x40897801)	/* 0x1FC000F0 */
    LONG(0x30840000)	/* 0x1FC000F4 */
    LONG(0x30A50000)	/* 0x1FC000F8 */
    LONG(0x3C089FC0)	/* 0x1FC000FC */
    LONG(0x25080AF0)	/* 0x1FC00100 */
    LONG(0x1000008)		/* 0x1FC00104 */
    LONG(0x0)			/* 0x1FC00108 */
    LONG(0xFFFFFFFF)	/* 0x1FC0010C */
    LONG(0xFFFFFFFF)	/* 0x1FC00110 */
    LONG(0xFFFFFFFF)	/* 0x1FC00114 */
    LONG(0xFFFFFFFF)	/* 0x1FC00118 */
    LONG(0xFFFFFFFF)	/* 0x1FC0011C */
    LONG(0xFFFFFFFF)	/* 0x1FC00120 */
    LONG(0xFFFFFFFF)	/* 0x1FC00124 */
    LONG(0xFFFFFFFF)	/* 0x1FC00128 */
    LONG(0xFFFFFFFF)	/* 0x1FC0012C */
    LONG(0xFFFFFFFF)	/* 0x1FC00130 */
    LONG(0xFFFFFFFF)	/* 0x1FC00134 */
    LONG(0xFFFFFFFF)	/* 0x1FC00138 */
    LONG(0xFFFFFFFF)	/* 0x1FC0013C */
    LONG(0xFFFFFFFF)	/* 0x1FC00140 */
    LONG(0xFFFFFFFF)	/* 0x1FC00144 */
    LONG(0xFFFFFFFF)	/* 0x1FC00148 */
    LONG(0xFFFFFFFF)	/* 0x1FC0014C */
    LONG(0xFFFFFFFF)	/* 0x1FC00150 */
    LONG(0xFFFFFFFF)	/* 0x1FC00154 */
    LONG(0xFFFFFFFF)	/* 0x1FC00158 */
    LONG(0xFFFFFFFF)	/* 0x1FC0015C */
    LONG(0xFFFFFFFF)	/* 0x1FC00160 */
    LONG(0xFFFFFFFF)	/* 0x1FC00164 */
    LONG(0xFFFFFFFF)	/* 0x1FC00168 */
    LONG(0xFFFFFFFF)	/* 0x1FC0016C */
    LONG(0xFFFFFFFF)	/* 0x1FC00170 */
    LONG(0xFFFFFFFF)	/* 0x1FC00174 */
    LONG(0xFFFFFFFF)	/* 0x1FC00178 */
    LONG(0xFFFFFFFF)	/* 0x1FC0017C */
    LONG(0xFFFFFFFF)	/* 0x1FC00180 */
    LONG(0xFFFFFFFF)	/* 0x1FC00184 */
    LONG(0xFFFFFFFF)	/* 0x1FC00188 */
    LONG(0xFFFFFFFF)	/* 0x1FC0018C */
    LONG(0xFFFFFFFF)	/* 0x1FC00190 */
    LONG(0xFFFFFFFF)	/* 0x1FC00194 */
    LONG(0xFFFFFFFF)	/* 0x1FC00198 */
    LONG(0xFFFFFFFF)	/* 0x1FC0019C */
    LONG(0xFFFFFFFF)	/* 0x1FC001A0 */
    LONG(0xFFFFFFFF)	/* 0x1FC001A4 */
    LONG(0xFFFFFFFF)	/* 0x1FC001A8 */
    LONG(0xFFFFFFFF)	/* 0x1FC001AC */
    LONG(0xFFFFFFFF)	/* 0x1FC001B0 */
    LONG(0xFFFFFFFF)	/* 0x1FC001B4 */
    LONG(0xFFFFFFFF)	/* 0x1FC001B8 */
    LONG(0xFFFFFFFF)	/* 0x1FC001BC */
    LONG(0xFFFFFFFF)	/* 0x1FC001C0 */
    LONG(0xFFFFFFFF)	/* 0x1FC001C4 */
    LONG(0xFFFFFFFF)	/* 0x1FC001C8 */
    LONG(0xFFFFFFFF)	/* 0x1FC001CC */
    LONG(0xFFFFFFFF)	/* 0x1FC001D0 */
    LONG(0xFFFFFFFF)	/* 0x1FC001D4 */
    LONG(0xFFFFFFFF)	/* 0x1FC001D8 */
    LONG(0xFFFFFFFF)	/* 0x1FC001DC */
    LONG(0xFFFFFFFF)	/* 0x1FC001E0 */
    LONG(0xFFFFFFFF)	/* 0x1FC001E4 */
    LONG(0xFFFFFFFF)	/* 0x1FC001E8 */
    LONG(0xFFFFFFFF)	/* 0x1FC001EC */
    LONG(0xFFFFFFFF)	/* 0x1FC001F0 */
    LONG(0xFFFFFFFF)	/* 0x1FC001F4 */
    LONG(0xFFFFFFFF)	/* 0x1FC001F8 */
    LONG(0xFFFFFFFF)	/* 0x1FC001FC */
    LONG(0xFFFFFFFF)	/* 0x1FC00200 */
    LONG(0xFFFFFFFF)	/* 0x1FC00204 */
    LONG(0x100B6A00)	/* 0x1FC00208 */
    LONG(0x9B60B307)	/* 0x1FC0020C */
    LONG(0x4E016EFF)	/* 0x1FC00210 */
    LONG(0x23FAEBCC)	/* 0x1FC00214 */
    LONG(0xA3C0E44D)	/* 0x1FC00218 */
    LONG(0x4A01B304)	/* 0x1FC0021C */
    LONG(0xEAA2DBC0)	/* 0x1FC00220 */
    LONG(0xE8A061F3)	/* 0x1FC00224 */
    LONG(0xBF806010)	/* 0x1FC00228 */
    LONG(0xBF806020)	/* 0x1FC0022C */
    LONG(0xF0F0B50E)	/* 0x1FC00230 */
    LONG(0x4344446C)	/* 0x1FC00234 */
    LONG(0x6A00ED43)	/* 0x1FC00238 */
    LONG(0x9B606114)	/* 0x1FC0023C */
    LONG(0x261143C1)	/* 0x1FC00240 */
    LONG(0xEE6C6E03)	/* 0x1FC00244 */
    LONG(0x43C72E0E)	/* 0x1FC00248 */
    LONG(0xEDC34E39)	/* 0x1FC0024C */
    LONG(0xAB4F610A)	/* 0x1FC00250 */
    LONG(0x3240ABAE)	/* 0x1FC00254 */
    LONG(0xEAAD3240)	/* 0x1FC00258 */
    LONG(0x5A01EA8E)	/* 0x1FC0025C */
    LONG(0xEA4B6758)	/* 0x1FC00260 */
    LONG(0xE8A0EA6C)	/* 0x1FC00264 */
    LONG(0x9D020000)	/* 0x1FC00268 */
    LONG(0xB23164E3)	/* 0x1FC0026C */
    LONG(0x6BFF9A40)	/* 0x1FC00270 */
    LONG(0xEA6C4B01)	/* 0x1FC00274 */
    LONG(0x22F9671C)	/* 0x1FC00278 */
    LONG(0xB22E6B00)	/* 0x1FC0027C */
    LONG(0xF400DA60)	/* 0x1FC00280 */
    LONG(0xB22D4B00)	/* 0x1FC00284 */
    LONG(0xB42DDA60)	/* 0x1FC00288 */
    LONG(0x6A00F002)	/* 0x1FC0028C */
    LONG(0xB42CDC40)	/* 0x1FC00290 */
    LONG(0xF010DC60)	/* 0x1FC00294 */
    LONG(0xB32B9890)	/* 0x1FC00298 */
    LONG(0x1A1FDB40)	/* 0x1FC0029C */
    LONG(0x6500008C)	/* 0x1FC002A0 */
    LONG(0xF0702242)	/* 0x1FC002A4 */
    LONG(0xEB11A061)	/* 0x1FC002A8 */
    LONG(0xAA6B230D)	/* 0x1FC002AC */
    LONG(0x3360AA8A)	/* 0x1FC002B0 */
    LONG(0xEB8D3360)	/* 0x1FC002B4 */
    LONG(0xEB8CB424)	/* 0x1FC002B8 */
    LONG(0xB4242305)	/* 0x1FC002BC */
    LONG(0x8C1A1F)		/* 0x1FC002C0 */
    LONG(0x22076500)	/* 0x1FC002C4 */
    LONG(0xAA8CAA6D)	/* 0x1FC002C8 */
    LONG(0x33603360)	/* 0x1FC002CC */
    LONG(0xF010EB8D)	/* 0x1FC002D0 */
    LONG(0xAA61D874)	/* 0x1FC002D4 */
    LONG(0x3360AA80)	/* 0x1FC002D8 */
    LONG(0xEB8D3360)	/* 0x1FC002DC */
    LONG(0x61235B3C)	/* 0x1FC002E0 */
    LONG(0xAA9AAA7B)	/* 0x1FC002E4 */
    LONG(0x33603360)	/* 0x1FC002E8 */
    LONG(0x6790EB8D)	/* 0x1FC002EC */
    LONG(0x4C10F010)	/* 0x1FC002F0 */
    LONG(0xED6344AC)	/* 0x1FC002F4 */
    LONG(0xAABC6118)	/* 0x1FC002F8 */
    LONG(0x3240AA5D)	/* 0x1FC002FC */
    LONG(0xEAAD3240)	/* 0x1FC00300 */
    LONG(0x61015A09)	/* 0x1FC00304 */
    LONG(0xE4736A08)	/* 0x1FC00308 */
    LONG(0x6001EC43)	/* 0x1FC0030C */
    LONG(0x67906744)	/* 0x1FC00310 */
    LONG(0xD848F050)	/* 0x1FC00314 */
    LONG(0x4C08F050)	/* 0x1FC00318 */
    LONG(0xA4A01005)	/* 0x1FC0031C */
    LONG(0x4C014AFF)	/* 0x1FC00320 */
    LONG(0x4B01C3A0)	/* 0x1FC00324 */
    LONG(0xF0102AFA)	/* 0x1FC00328 */
    LONG(0xEAC09854)	/* 0x1FC0032C */
    LONG(0xBF806010)	/* 0x1FC00330 */
    LONG(0xBF806000)	/* 0x1FC00334 */
    LONG(0xBF886034)	/* 0x1FC00338 */
    LONG(0xBF886134)	/* 0x1FC0033C */
    LONG(0xBF886018)	/* 0x1FC00340 */
    LONG(0xBF886118)	/* 0x1FC00344 */
    LONG(0x2000000)		/* 0x1FC00348 */
    LONG(0x9D000000)	/* 0x1FC0034C */
    LONG(0x7CA5E000)	/* 0x1FC00350 */
    LONG(0x3C02BF81)	/* 0x1FC00354 */
    LONG(0xAC45F420)	/* 0x1FC00358 */
    LONG(0x3C02BF81)	/* 0x1FC0035C */
    LONG(0xAC46F430)	/* 0x1FC00360 */
    LONG(0x7CC6E000)	/* 0x1FC00364 */
    LONG(0x3C02BF81)	/* 0x1FC00368 */
    LONG(0xAC46F440)	/* 0x1FC0036C */
    LONG(0x34844000)	/* 0x1FC00370 */
    LONG(0x3C02BF81)	/* 0x1FC00374 */
    LONG(0xAC44F400)	/* 0x1FC00378 */
    LONG(0x40034800)	/* 0x1FC0037C */
    LONG(0x402821)		/* 0x1FC00380 */
    LONG(0x40024800)	/* 0x1FC00384 */
    LONG(0x431023)		/* 0x1FC00388 */
    LONG(0x2C420078)	/* 0x1FC0038C */
    LONG(0x1440FFFC)	/* 0x1FC00390 */
    LONG(0x3C02BF81)	/* 0x1FC00394 */
    LONG(0x3C03AA99)	/* 0x1FC00398 */
    LONG(0x24636655)	/* 0x1FC0039C */
    LONG(0xAC43F410)	/* 0x1FC003A0 */
    LONG(0x3C035566)	/* 0x1FC003A4 */
    LONG(0x346399AA)	/* 0x1FC003A8 */
    LONG(0xAC43F410)	/* 0x1FC003AC */
    LONG(0x34048000)	/* 0x1FC003B0 */
    LONG(0x3C03BF81)	/* 0x1FC003B4 */
    LONG(0xAC64F408)	/* 0x1FC003B8 */
    LONG(0x8CA2F400)	/* 0x1FC003BC */
    LONG(0x30428000)	/* 0x1FC003C0 */
    LONG(0x1440FFFD)	/* 0x1FC003C4 */
    LONG(0x0)			/* 0x1FC003C8 */
    LONG(0x40024800)	/* 0x1FC003CC */
    LONG(0x40034800)	/* 0x1FC003D0 */
    LONG(0x621823)		/* 0x1FC003D4 */
    LONG(0x2C63000A)	/* 0x1FC003D8 */
    LONG(0x1460FFFC)	/* 0x1FC003DC */
    LONG(0x24034000)	/* 0x1FC003E0 */
    LONG(0x3C02BF81)	/* 0x1FC003E4 */
    LONG(0xAC43F404)	/* 0x1FC003E8 */
    LONG(0x3E00008)		/* 0x1FC003EC */
    LONG(0x0)			/* 0x1FC003F0 */
    LONG(0x64F56A01)	/* 0x1FC003F4 */
    LONG(0x6704EA4B)	/* 0x1FC003F8 */
    LONG(0x10116905)	/* 0x1FC003FC */
    LONG(0x4B049BA0)	/* 0x1FC00400 */
    LONG(0xEB8AEAAC)	/* 0x1FC00404 */
    LONG(0x426161FB)	/* 0x1FC00408 */
    LONG(0xF008230E)	/* 0x1FC0040C */
    LONG(0x67B06C04)	/* 0x1FC00410 */
    LONG(0x49FF6E00)	/* 0x1FC00414 */
    LONG(0xD41E1F)		/* 0x1FC00418 */
    LONG(0x9204D204)	/* 0x1FC0041C */
    LONG(0xF4002104)	/* 0x1FC00420 */
    LONG(0x67704080)	/* 0x1FC00424 */
    LONG(0x647517EE)	/* 0x1FC00428 */
    LONG(0x6500E8A0)	/* 0x1FC0042C */
    LONG(0xF40064F5)	/* 0x1FC00430 */
    LONG(0xB1116A00)	/* 0x1FC00434 */
    LONG(0x6704EA4B)	/* 0x1FC00438 */
    LONG(0x4DFFE84C)	/* 0x1FC0043C */
    LONG(0xED4CE025)	/* 0x1FC00440 */
    LONG(0xF4003122)	/* 0x1FC00444 */
    LONG(0x312A4D00)	/* 0x1FC00448 */
    LONG(0xB20C1010)	/* 0x1FC0044C */
    LONG(0xA260E149)	/* 0x1FC00450 */
    LONG(0x67902B09)	/* 0x1FC00454 */
    LONG(0x1A1FD205)	/* 0x1FC00458 */
    LONG(0xD50400FD)	/* 0x1FC0045C */
    LONG(0x6B019205)	/* 0x1FC00460 */
    LONG(0x9504C260)	/* 0x1FC00464 */
    LONG(0xF4004901)	/* 0x1FC00468 */
    LONG(0xE8A34800)	/* 0x1FC0046C */
    LONG(0x647561EE)	/* 0x1FC00470 */
    LONG(0x6500E8A0)	/* 0x1FC00474 */
    LONG(0x63000000)	/* 0x1FC00478 */
    LONG(0xA0000E60)	/* 0x1FC0047C */
    LONG(0x676664F5)	/* 0x1FC00480 */
    LONG(0x67256704)	/* 0x1FC00484 */
    LONG(0x100F6A00)	/* 0x1FC00488 */
    LONG(0x468199C0)	/* 0x1FC0048C */
    LONG(0xF0082409)	/* 0x1FC00490 */
    LONG(0x67B06C01)	/* 0x1FC00494 */
    LONG(0x1E1FD204)	/* 0x1FC00498 */
    LONG(0xD30500D4)	/* 0x1FC0049C */
    LONG(0x92049305)	/* 0x1FC004A0 */
    LONG(0x4A014804)	/* 0x1FC004A4 */
    LONG(0xEA634904)	/* 0x1FC004A8 */
    LONG(0x647561EF)	/* 0x1FC004AC */
    LONG(0x6500E8A0)	/* 0x1FC004B0 */
    LONG(0x673C64F4)	/* 0x1FC004B4 */
    LONG(0x9990F010)	/* 0x1FC004B8 */
    LONG(0x8C1A1F)		/* 0x1FC004BC */
    LONG(0x67026500)	/* 0x1FC004C0 */
    LONG(0x67B1223C)	/* 0x1FC004C4 */
    LONG(0xF0304284)	/* 0x1FC004C8 */
    LONG(0x1A1F4D00)	/* 0x1FC004CC */
    LONG(0x6E010120)	/* 0x1FC004D0 */
    LONG(0x67B14087)	/* 0x1FC004D4 */
    LONG(0xF0304C09)	/* 0x1FC004D8 */
    LONG(0x1A1F4D04)	/* 0x1FC004DC */
    LONG(0x6E010120)	/* 0x1FC004E0 */
    LONG(0x67B14087)	/* 0x1FC004E4 */
    LONG(0xF0304C05)	/* 0x1FC004E8 */
    LONG(0x1A1F4D08)	/* 0x1FC004EC */
    LONG(0x6E010120)	/* 0x1FC004F0 */
    LONG(0x67B14087)	/* 0x1FC004F4 */
    LONG(0xF0304C35)	/* 0x1FC004F8 */
    LONG(0x1A1F4D0C)	/* 0x1FC004FC */
    LONG(0x6E010120)	/* 0x1FC00500 */
    LONG(0xA86AA84B)	/* 0x1FC00504 */
    LONG(0x32403240)	/* 0x1FC00508 */
    LONG(0xB311EA6D)	/* 0x1FC0050C */
    LONG(0x2B1CEB4C)	/* 0x1FC00510 */
    LONG(0xEB4CB310)	/* 0x1FC00514 */
    LONG(0xA88F230C)	/* 0x1FC00518 */
    LONG(0xA8B1A84E)	/* 0x1FC0051C */
    LONG(0x34803480)	/* 0x1FC00520 */
    LONG(0xA850EC4D)	/* 0x1FC00524 */
    LONG(0x35A035A0)	/* 0x1FC00528 */
    LONG(0xE595ED4D)	/* 0x1FC0052C */
    LONG(0xB30A100A)	/* 0x1FC00530 */
    LONG(0xEA6CB40A)	/* 0x1FC00534 */
    LONG(0xB50A2205)	/* 0x1FC00538 */
    LONG(0xB50A1004)	/* 0x1FC0053C */
    LONG(0x1001B407)	/* 0x1FC00540 */
    LONG(0x1A1FB508)	/* 0x1FC00544 */
    LONG(0x6500010C)	/* 0x1FC00548 */
    LONG(0xE8A06474)	/* 0x1FC0054C */
    LONG(0x20000)		/* 0x1FC00550 */
    LONG(0x40000)		/* 0x1FC00554 */
    LONG(0x80000)		/* 0x1FC00558 */
    LONG(0x9D000000)	/* 0x1FC0055C */
    LONG(0x9D020000)	/* 0x1FC00560 */
    LONG(0x9D01F000)	/* 0x1FC00564 */
    LONG(0x671C64FD)	/* 0x1FC00568 */
    LONG(0x9844F070)	/* 0x1FC0056C */
    LONG(0xA382B3A7)	/* 0x1FC00570 */
    LONG(0xE559B5A7)	/* 0x1FC00574 */
    LONG(0x42C1C680)	/* 0x1FC00578 */
    LONG(0x6F00E5D9)	/* 0x1FC0057C */
    LONG(0x7406C6E0)	/* 0x1FC00580 */
    LONG(0xF07042C2)	/* 0x1FC00584 */
    LONG(0x6076D8C4)	/* 0x1FC00588 */
    LONG(0x60085C07)	/* 0x1FC0058C */
    LONG(0x60307402)	/* 0x1FC00590 */
    LONG(0x60347403)	/* 0x1FC00594 */
    LONG(0xEC6E6B01)	/* 0x1FC00598 */
    LONG(0x10FD2410)	/* 0x1FC0059C */
    LONG(0x607A7413)	/* 0x1FC005A0 */
    LONG(0x60045C14)	/* 0x1FC005A4 */
    LONG(0xF0E07411)	/* 0x1FC005A8 */
    LONG(0x10F5600C)	/* 0x1FC005AC */
    LONG(0xF0A07414)	/* 0x1FC005B0 */
    LONG(0x6F1D6016)	/* 0x1FC005B4 */
    LONG(0x242AECEE)	/* 0x1FC005B8 */
    LONG(0x6B0110EE)	/* 0x1FC005BC */
    LONG(0x4A03E5D9)	/* 0x1FC005C0 */
    LONG(0xC060F070)	/* 0x1FC005C4 */
    LONG(0xC6606B08)	/* 0x1FC005C8 */
    LONG(0xC090F050)	/* 0x1FC005CC */
    LONG(0xC088F070)	/* 0x1FC005D0 */
    LONG(0xD844F070)	/* 0x1FC005D4 */
    LONG(0xB38FE2B5)	/* 0x1FC005D8 */
    LONG(0x4B018380)	/* 0x1FC005DC */
    LONG(0x4D01C580)	/* 0x1FC005E0 */
    LONG(0x4A082CFB)	/* 0x1FC005E4 */
    LONG(0xD844F070)	/* 0x1FC005E8 */
    LONG(0x6B00F002)	/* 0x1FC005EC */
    LONG(0x10D2B28A)	/* 0x1FC005F0 */
    LONG(0xA364A343)	/* 0x1FC005F4 */
    LONG(0xE449B489)	/* 0x1FC005F8 */
    LONG(0x10CDC260)	/* 0x1FC005FC */
    LONG(0xB487A363)	/* 0x1FC00600 */
    LONG(0xE46DE5D5)	/* 0x1FC00604 */
    LONG(0x4A03A360)	/* 0x1FC00608 */
    LONG(0x1031C560)	/* 0x1FC0060C */
    LONG(0xE5D9A3E6)	/* 0x1FC00610 */
    LONG(0xE559C680)	/* 0x1FC00614 */
    LONG(0xC684C6E3)	/* 0x1FC00618 */
    LONG(0x42C57730)	/* 0x1FC0061C */
    LONG(0xD8C4F070)	/* 0x1FC00620 */
    LONG(0xA368610D)	/* 0x1FC00624 */
    LONG(0x2B03E5D5)	/* 0x1FC00628 */
    LONG(0xC5606B50)	/* 0x1FC0062C */
    LONG(0x73011016)	/* 0x1FC00630 */
    LONG(0x6B494A06)	/* 0x1FC00634 */
    LONG(0x6B436001)	/* 0x1FC00638 */
    LONG(0x1010C560)	/* 0x1FC0063C */
    LONG(0x60027720)	/* 0x1FC00640 */
    LONG(0x61097728)	/* 0x1FC00644 */
    LONG(0x6B01B272)	/* 0x1FC00648 */
    LONG(0xEB6BE2C9)	/* 0x1FC0064C */
    LONG(0xC2604E01)	/* 0x1FC00650 */
    LONG(0xD8C4F070)	/* 0x1FC00654 */
    LONG(0xE5D51005)	/* 0x1FC00658 */
    LONG(0x4A06C580)	/* 0x1FC0065C */
    LONG(0xD844F070)	/* 0x1FC00660 */
    LONG(0x9844F070)	/* 0x1FC00664 */
    LONG(0x6C00B36A)	/* 0x1FC00668 */
    LONG(0xC380E34D)	/* 0x1FC0066C */
    LONG(0xF0704A01)	/* 0x1FC00670 */
    LONG(0x1091D844)	/* 0x1FC00674 */
    LONG(0xA343A384)	/* 0x1FC00678 */
    LONG(0x34803480)	/* 0x1FC0067C */
    LONG(0x3240F600)	/* 0x1FC00680 */
    LONG(0xA386EA8D)	/* 0x1FC00684 */
    LONG(0xEA8DA365)	/* 0x1FC00688 */
    LONG(0xEA6D3360)	/* 0x1FC0068C */
    LONG(0xF0103244)	/* 0x1FC00690 */
    LONG(0x10419878)	/* 0x1FC00694 */
    LONG(0xA364A343)	/* 0x1FC00698 */
    LONG(0xEA6D3240)	/* 0x1FC0069C */
    LONG(0xEB6B6B04)	/* 0x1FC006A0 */
    LONG(0xE96C4223)	/* 0x1FC006A4 */
    LONG(0x10066762)	/* 0x1FC006A8 */
    LONG(0x6D01B45D)	/* 0x1FC006AC */
    LONG(0xEDABE391)	/* 0x1FC006B0 */
    LONG(0x4B01C4A0)	/* 0x1FC006B4 */
    LONG(0x61F8EB23)	/* 0x1FC006B8 */
    LONG(0xA07CF010)	/* 0x1FC006BC */
    LONG(0xF100231C)	/* 0x1FC006C0 */
    LONG(0x61165900)	/* 0x1FC006C4 */
    LONG(0x988CF070)	/* 0x1FC006C8 */
    LONG(0x6B1FF1E0)	/* 0x1FC006CC */
    LONG(0x2B10EB8C)	/* 0x1FC006D0 */
    LONG(0x9B60B354)	/* 0x1FC006D4 */
    LONG(0x250C43A1)	/* 0x1FC006D8 */
    LONG(0x98B8F010)	/* 0x1FC006DC */
    LONG(0xD870F010)	/* 0x1FC006E0 */
    LONG(0xB451E4B7)	/* 0x1FC006E4 */
    LONG(0xF010EC6C)	/* 0x1FC006E8 */
    LONG(0xE591D898)	/* 0x1FC006EC */
    LONG(0xD88CF070)	/* 0x1FC006F0 */
    LONG(0xF0106B00)	/* 0x1FC006F4 */
    LONG(0xF070C07C)	/* 0x1FC006F8 */
    LONG(0xD21498AC)	/* 0x1FC006FC */
    LONG(0x1A1F6785)	/* 0x1FC00700 */
    LONG(0xE1B5010C)	/* 0x1FC00704 */
    LONG(0x988CF070)	/* 0x1FC00708 */
    LONG(0x1A1FB545)	/* 0x1FC0070C */
    LONG(0x362A0120)	/* 0x1FC00710 */
    LONG(0x986CF070)	/* 0x1FC00714 */
    LONG(0xE3499214)	/* 0x1FC00718 */
    LONG(0xD84CF070)	/* 0x1FC0071C */
    LONG(0xA383103C)	/* 0x1FC00720 */
    LONG(0x984CF070)	/* 0x1FC00724 */
    LONG(0x3480A364)	/* 0x1FC00728 */
    LONG(0xEC6DE451)	/* 0x1FC0072C */
    LONG(0xB73F101E)	/* 0x1FC00730 */
    LONG(0x6D00F400)	/* 0x1FC00734 */
    LONG(0xED4CEDAB)	/* 0x1FC00738 */
    LONG(0x4560F400)	/* 0x1FC0073C */
    LONG(0x35A2E5F5)	/* 0x1FC00740 */
    LONG(0xB73B35AA)	/* 0x1FC00744 */
    LONG(0xA5A0E7B5)	/* 0x1FC00748 */
    LONG(0x6DFF7501)	/* 0x1FC0074C */
    LONG(0x6D006101)	/* 0x1FC00750 */
    LONG(0x6009EC63)	/* 0x1FC00754 */
    LONG(0x10076764)	/* 0x1FC00758 */
    LONG(0xB12DA2E0)	/* 0x1FC0075C */
    LONG(0xEFADE625)	/* 0x1FC00760 */
    LONG(0x4E01C1E0)	/* 0x1FC00764 */
    LONG(0xEA634A01)	/* 0x1FC00768 */
    LONG(0xEA8361F7)	/* 0x1FC0076C */
    LONG(0xF07061E0)	/* 0x1FC00770 */
    LONG(0xB227D84C)	/* 0x1FC00774 */
    LONG(0x6B00E2C9)	/* 0x1FC00778 */
    LONG(0xC2604E01)	/* 0x1FC0077C */
    LONG(0xD8C4F070)	/* 0x1FC00780 */
    LONG(0x1A1F100A)	/* 0x1FC00784 */
    LONG(0x6500012D)	/* 0x1FC00788 */
    LONG(0xF0706A01)	/* 0x1FC0078C */
    LONG(0xF002C041)	/* 0x1FC00790 */
    LONG(0xB2286B00)	/* 0x1FC00794 */
    LONG(0xF070DA60)	/* 0x1FC00798 */
    LONG(0x6A1B9824)	/* 0x1FC0079C */
    LONG(0xC350677D)	/* 0x1FC007A0 */
    LONG(0xA078F030)	/* 0x1FC007A4 */
    LONG(0x3223679D)	/* 0x1FC007A8 */
    LONG(0xC452C471)	/* 0x1FC007AC */
    LONG(0x67FDC433)	/* 0x1FC007B0 */
    LONG(0xC7946C0E)	/* 0x1FC007B4 */
    LONG(0xEB8E6C1B)	/* 0x1FC007B8 */
    LONG(0x6D0EEA6E)	/* 0x1FC007BC */
    LONG(0xEAAEEA2E)	/* 0x1FC007C0 */
    LONG(0xF0300404)	/* 0x1FC007C4 */
    LONG(0x1A1FC050)	/* 0x1FC007C8 */
    LONG(0x6D050082)	/* 0x1FC007CC */
    LONG(0xA090F030)	/* 0x1FC007D0 */
    LONG(0x10056B00)	/* 0x1FC007D4 */
    LONG(0xE349B20E)	/* 0x1FC007D8 */
    LONG(0x4B01A240)	/* 0x1FC007DC */
    LONG(0x6743EC4E)	/* 0x1FC007E0 */
    LONG(0x2AF8EA2E)	/* 0x1FC007E4 */
    LONG(0xC090F030)	/* 0x1FC007E8 */
    LONG(0xB40967B1)	/* 0x1FC007EC */
    LONG(0x821A1F)		/* 0x1FC007F0 */
    LONG(0x6790D214)	/* 0x1FC007F4 */
    LONG(0x4C10F030)	/* 0x1FC007F8 */
    LONG(0x821A1F)		/* 0x1FC007FC */
    LONG(0x92146D01)	/* 0x1FC00800 */
    LONG(0xD844F070)	/* 0x1FC00804 */
    LONG(0xE8A0647D)	/* 0x1FC00808 */
    LONG(0xA0000660)	/* 0x1FC0080C */
    LONG(0xA0000A60)	/* 0x1FC00810 */
    LONG(0x9FC00B00)	/* 0x1FC00814 */
    LONG(0xBF886138)	/* 0x1FC00818 */
    LONG(0xA0000EE0)	/* 0x1FC0081C */
    LONG(0xA000066C)	/* 0x1FC00820 */
    LONG(0xA0000764)	/* 0x1FC00824 */
    LONG(0xFFFF0000)	/* 0x1FC00828 */
    LONG(0x63000000)	/* 0x1FC0082C */
    LONG(0xA0000E60)	/* 0x1FC00830 */
    LONG(0xBF886134)	/* 0x1FC00834 */
    LONG(0xF030675C)	/* 0x1FC00838 */
    LONG(0xF030A270)	/* 0x1FC0083C */
    LONG(0xEC119AB4)	/* 0x1FC00840 */
    LONG(0x5D07EB8E)	/* 0x1FC00844 */
    LONG(0xC270F030)	/* 0x1FC00848 */
    LONG(0xE046045)		/* 0x1FC0084C */
    LONG(0xE6B535A4)	/* 0x1FC00850 */
    LONG(0xE5D98DA0)	/* 0x1FC00854 */
    LONG(0x6500EE80)	/* 0x1FC00858 */
    LONG(0x1F000F)		/* 0x1FC0085C */
    LONG(0x310027)		/* 0x1FC00860 */
    LONG(0x4F003F)		/* 0x1FC00864 */
    LONG(0x741B006F)	/* 0x1FC00868 */
    LONG(0x6B016103)	/* 0x1FC0086C */
    LONG(0xDA74F030)	/* 0x1FC00870 */
    LONG(0xC290F030)	/* 0x1FC00874 */
    LONG(0xF030E8A0)	/* 0x1FC00878 */
    LONG(0x6B02C298)	/* 0x1FC0087C */
    LONG(0x34801021)	/* 0x1FC00880 */
    LONG(0xDA9CF030)	/* 0x1FC00884 */
    LONG(0x101C6B03)	/* 0x1FC00888 */
    LONG(0x9A7CF030)	/* 0x1FC0088C */
    LONG(0xF030EB8D)	/* 0x1FC00890 */
    LONG(0x6B04DA7C)	/* 0x1FC00894 */
    LONG(0x6B0E1015)	/* 0x1FC00898 */
    LONG(0x6B00EC6E)	/* 0x1FC0089C */
    LONG(0xF0502C11)	/* 0x1FC008A0 */
    LONG(0x6B05DA80)	/* 0x1FC008A4 */
    LONG(0xF050100D)	/* 0x1FC008A8 */
    LONG(0xB50C9A60)	/* 0x1FC008AC */
    LONG(0xC582E575)	/* 0x1FC008B0 */
    LONG(0x9A9CF030)	/* 0x1FC008B4 */
    LONG(0xF0504B01)	/* 0x1FC008B8 */
    LONG(0xEB8EDA60)	/* 0x1FC008BC */
    LONG(0x6B062B0B)	/* 0x1FC008C0 */
    LONG(0xDA74F030)	/* 0x1FC008C4 */
    LONG(0x2B03E8A0)	/* 0x1FC008C8 */
    LONG(0xF0506B01)	/* 0x1FC008CC */
    LONG(0x6B00C264)	/* 0x1FC008D0 */
    LONG(0xDA74F030)	/* 0x1FC008D4 */
    LONG(0x6500E8A0)	/* 0x1FC008D8 */
    LONG(0xA0000660)	/* 0x1FC008DC */
    LONG(0x9A60B259)	/* 0x1FC008E0 */
    LONG(0x671C64E3)	/* 0x1FC008E4 */
    LONG(0x6750B458)	/* 0x1FC008E8 */
    LONG(0x4A08F050)	/* 0x1FC008EC */
    LONG(0x6A00DA61)	/* 0x1FC008F0 */
    LONG(0xDB40B356)	/* 0x1FC008F4 */
    LONG(0xB456DB80)	/* 0x1FC008F8 */
    LONG(0x6D01F004)	/* 0x1FC008FC */
    LONG(0xDB80EDAB)	/* 0x1FC00900 */
    LONG(0x9C60B454)	/* 0x1FC00904 */
    LONG(0xDC60EBAC)	/* 0x1FC00908 */
    LONG(0xDB40B353)	/* 0x1FC0090C */
    LONG(0xDB40B353)	/* 0x1FC00910 */
    LONG(0xDB40B353)	/* 0x1FC00914 */
    LONG(0x6B00F400)	/* 0x1FC00918 */
    LONG(0xDA60B252)	/* 0x1FC0091C */
    LONG(0x6A00F002)	/* 0x1FC00920 */
    LONG(0xDB40B351)	/* 0x1FC00924 */
    LONG(0xDB40B351)	/* 0x1FC00928 */
    LONG(0x6B01B251)	/* 0x1FC0092C */
    LONG(0x9A40F002)	/* 0x1FC00930 */
    LONG(0x5A01EA4F)	/* 0x1FC00934 */
    LONG(0xEA6E6758)	/* 0x1FC00938 */
    LONG(0x9B80B34E)	/* 0x1FC0093C */
    LONG(0xEC6C6B04)	/* 0x1FC00940 */
    LONG(0x240D6B01)	/* 0x1FC00944 */
    LONG(0x9CA0B44C)	/* 0x1FC00948 */
    LONG(0x2D09ED6C)	/* 0x1FC0094C */
    LONG(0x6E809CA0)	/* 0x1FC00950 */
    LONG(0x2D05EDCC)	/* 0x1FC00954 */
    LONG(0x6C409C60)	/* 0x1FC00958 */
    LONG(0x5B01EB8C)	/* 0x1FC0095C */
    LONG(0xEA6C6778)	/* 0x1FC00960 */
    LONG(0xC050F050)	/* 0x1FC00964 */
    LONG(0xB2456B04)	/* 0x1FC00968 */
    LONG(0x6B00DA60)	/* 0x1FC0096C */
    LONG(0xDA60B235)	/* 0x1FC00970 */
    LONG(0x25F1E1F)		/* 0x1FC00974 */
    LONG(0x65006500)	/* 0x1FC00978 */
    LONG(0x40024800)	/* 0x1FC0097C */
    LONG(0x77F00262)	/* 0x1FC00980 */
    LONG(0x0)			/* 0x1FC00984 */
    LONG(0x6B056500)	/* 0x1FC00988 */
    LONG(0xD854F050)	/* 0x1FC0098C */
    LONG(0xD858F050)	/* 0x1FC00990 */
    LONG(0xDA60B23B)	/* 0x1FC00994 */
    LONG(0xB23B6B01)	/* 0x1FC00998 */
    LONG(0xF7EFDA60)	/* 0x1FC0099C */
    LONG(0xB23A4B1F)	/* 0x1FC009A0 */
    LONG(0xF412DA60)	/* 0x1FC009A4 */
    LONG(0xB2394B00)	/* 0x1FC009A8 */
    LONG(0x6B14DA60)	/* 0x1FC009AC */
    LONG(0xDA60B238)	/* 0x1FC009B0 */
    LONG(0xB2381002)	/* 0x1FC009B4 */
    LONG(0xB2389A40)	/* 0x1FC009B8 */
    LONG(0x6B019A40)	/* 0x1FC009BC */
    LONG(0x2AF9EA6C)	/* 0x1FC009C0 */
    LONG(0x2731E1F)		/* 0x1FC009C4 */
    LONG(0x65006500)	/* 0x1FC009C8 */
    LONG(0x40024800)	/* 0x1FC009CC */
    LONG(0x77F00276)	/* 0x1FC009D0 */
    LONG(0x0)			/* 0x1FC009D4 */
    LONG(0xB3316500)	/* 0x1FC009D8 */
    LONG(0xA0A0F070)	/* 0x1FC009DC */
    LONG(0x9898F050)	/* 0x1FC009E0 */
    LONG(0xD85CF050)	/* 0x1FC009E4 */
    LONG(0xE293ED66)	/* 0x1FC009E8 */
    LONG(0x6106EC63)	/* 0x1FC009EC */
    LONG(0x6C00F400)	/* 0x1FC009F0 */
    LONG(0xDB80B32B)	/* 0x1FC009F4 */
    LONG(0xD858F050)	/* 0x1FC009F8 */
    LONG(0xA061F070)	/* 0x1FC009FC */
    LONG(0x2B09EB11)	/* 0x1FC00A00 */
    LONG(0xA070F050)	/* 0x1FC00A04 */
    LONG(0xF0502309)	/* 0x1FC00A08 */
    LONG(0xE26B9874)	/* 0x1FC00A0C */
    LONG(0xEB43B325)	/* 0x1FC00A10 */
    LONG(0x1A1F6003)	/* 0x1FC00A14 */
    LONG(0x6500009B)	/* 0x1FC00A18 */
    LONG(0x9A40B21F)	/* 0x1FC00A1C */
    LONG(0xEA6C6B01)	/* 0x1FC00A20 */
    LONG(0xB21C2205)	/* 0x1FC00A24 */
    LONG(0x1A1F9A80)	/* 0x1FC00A28 */
    LONG(0xEC11020E)	/* 0x1FC00A2C */
    LONG(0xA044F050)	/* 0x1FC00A30 */
    LONG(0x1A1F22C7)	/* 0x1FC00A34 */
    LONG(0x6500015A)	/* 0x1FC00A38 */
    LONG(0xF0506A00)	/* 0x1FC00A3C */
    LONG(0x17C0C044)	/* 0x1FC00A40 */
    LONG(0xBF80F600)	/* 0x1FC00A44 */
    LONG(0xAA996655)	/* 0x1FC00A48 */
    LONG(0xBF80F230)	/* 0x1FC00A4C */
    LONG(0x556699AA)	/* 0x1FC00A50 */
    LONG(0xBF80F200)	/* 0x1FC00A54 */
    LONG(0xBF886000)	/* 0x1FC00A58 */
    LONG(0xBF886100)	/* 0x1FC00A5C */
    LONG(0xBF886200)	/* 0x1FC00A60 */
    LONG(0xBF886014)	/* 0x1FC00A64 */
    LONG(0xBF886114)	/* 0x1FC00A68 */
    LONG(0xBF886134)	/* 0x1FC00A6C */
    LONG(0x9D000000)	/* 0x1FC00A70 */
    LONG(0xBF886030)	/* 0x1FC00A74 */
    LONG(0xBF80F600)	/* 0x1FC00A78 */
    LONG(0xBF886034)	/* 0x1FC00A7C */
    LONG(0xBF80FA50)	/* 0x1FC00A80 */
    LONG(0xBF80FB38)	/* 0x1FC00A84 */
    LONG(0xBF806000)	/* 0x1FC00A88 */
    LONG(0xBF806010)	/* 0x1FC00A8C */
    LONG(0xBF806040)	/* 0x1FC00A90 */
    LONG(0xBF806030)	/* 0x1FC00A94 */
    LONG(0xBF806010)	/* 0x1FC00A98 */
    LONG(0x2625A0)		/* 0x1FC00A9C */
    LONG(0xBF88603C)	/* 0x1FC00AA0 */
    LONG(0x26259FF)		/* 0x1FC00AA4 */
    LONG(0xA0000660)	/* 0x1FC00AA8 */
    LONG(0x980)			/* 0x1FC00AAC */
    LONG(0x0)			/* 0x1FC00AB0 */
    LONG(0xA0000620)	/* 0x1FC00AB4 */
    LONG(0x40)			/* 0x1FC00AB8 */
    LONG(0x0)			/* 0x1FC00ABC */
    LONG(0xA0000600)	/* 0x1FC00AC0 */
    LONG(0x20)			/* 0x1FC00AC4 */
    LONG(0x1)			/* 0x1FC00AC8 */
    LONG(0x9D000000)	/* 0x1FC00ACC */
    LONG(0x9D001000)	/* 0x1FC00AD0 */
    LONG(0x9D000000)	/* 0x1FC00AD4 */
    LONG(0x1)			/* 0x1FC00AD8 */
    LONG(0x100010B)		/* 0x1FC00ADC */
    LONG(0x3010001)		/* 0x1FC00AE0 */
    LONG(0x130F0531)	/* 0x1FC00AE4 */
    LONG(0x600)			/* 0x1FC00AE8 */
    LONG(0x0)			/* 0x1FC00AEC */
    LONG(0x77F00238)	/* 0x1FC00AF0 */
    LONG(0x0)			/* 0x1FC00AF4 */
    LONG(0x1000FFFF)	/* 0x1FC00AF8 */
    LONG(0x0)			/* 0x1FC00AFC */
    LONG(0x354B5453)	/* 0x1FC00B00 */
    LONG(0x325F3030)	/* 0x1FC00B04 */
    LONG(0x65000000)	/* 0x1FC00B08 */
  } > kseg1_boot_mem
#endif

  .configs ORIGIN(configsfrs) :
  {
    LONG(0xCFFFFFFF)
    LONG(0xFFF9FFD9)
    LONG(0xFF6ACDDB)
    LONG(0x7FFFFFF3)
  } > configsfrs

  /* Boot Sections */
  .reset _RESET_ADDR :
  {
    KEEP(*(.reset))
  } > kseg0_program_mem
}

/* From here out every linker script is the same, so just include it */
/*INCLUDE "chipKIT-application-COMMON.ld"*/
