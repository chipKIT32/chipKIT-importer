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
OPTIONAL("libmchp_peripheral_32MX250F128D.a") 

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
  kseg0_program_mem    (rx)  : ORIGIN = 0x9D001000, LENGTH = 0x1D000
  kseg0_eeprom_mem           : ORIGIN = 0x9D01E000, LENGTH = 0x1000
  kseg0_splitflash_mem       : ORIGIN = 0x9D01F000, LENGTH = 0x1000
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
    LONG(0x401A6000)    /* 0x1FC00000 */
    LONG(0x7F5A04C0)    /* 0x1FC00004 */
    LONG(0x13400006)    /* 0x1FC00008 */
    LONG(0x401A6000)    /* 0x1FC0000C */
    LONG(0x3C1BFFBF)    /* 0x1FC00010 */
    LONG(0x377BFFFF)    /* 0x1FC00014 */
    LONG(0x35BD024)             /* 0x1FC00018 */
    LONG(0x409A6000)    /* 0x1FC0001C */
    LONG(0x42000018)    /* 0x1FC00020 */
    LONG(0x3C1DA000)    /* 0x1FC00024 */
    LONG(0x27BD3FF0)    /* 0x1FC00028 */
    LONG(0x3C1CA001)    /* 0x1FC0002C */
    LONG(0x279C85F0)    /* 0x1FC00030 */
    LONG(0x40096002)    /* 0x1FC00034 */
    LONG(0x1205820)             /* 0x1FC00038 */
    LONG(0x7D2A1E80)    /* 0x1FC0003C */
    LONG(0x7D494984)    /* 0x1FC00040 */
    LONG(0x40896002)    /* 0x1FC00044 */
    LONG(0xC0)                  /* 0x1FC00048 */
    LONG(0x41DCE000)    /* 0x1FC0004C */
    LONG(0x408B6002)    /* 0x1FC00050 */
    LONG(0x3C08A000)    /* 0x1FC00054 */
    LONG(0x25080620)    /* 0x1FC00058 */
    LONG(0x3C09A000)    /* 0x1FC0005C */
    LONG(0x25290660)    /* 0x1FC00060 */
    LONG(0x10000006)    /* 0x1FC00064 */
    LONG(0x0)                   /* 0x1FC00068 */
    LONG(0xAD000000)    /* 0x1FC0006C */
    LONG(0xAD000004)    /* 0x1FC00070 */
    LONG(0xAD000008)    /* 0x1FC00074 */
    LONG(0xAD00000C)    /* 0x1FC00078 */
    LONG(0x25080010)    /* 0x1FC0007C */
    LONG(0x109082B)             /* 0x1FC00080 */
    LONG(0x1420FFF9)    /* 0x1FC00084 */
    LONG(0x0)                   /* 0x1FC00088 */
    LONG(0x3C089FC0)    /* 0x1FC0008C */
    LONG(0x25080B38)    /* 0x1FC00090 */
    LONG(0x8D090000)    /* 0x1FC00094 */
    LONG(0x11200018)    /* 0x1FC00098 */
    LONG(0x25080004)    /* 0x1FC0009C */
    LONG(0x8D0A0000)    /* 0x1FC000A0 */
    LONG(0x25080004)    /* 0x1FC000A4 */
    LONG(0x8D0B0000)    /* 0x1FC000A8 */
    LONG(0x11600009)    /* 0x1FC000AC */
    LONG(0x25080004)    /* 0x1FC000B0 */
    LONG(0x910C0000)    /* 0x1FC000B4 */
    LONG(0x254AFFFF)    /* 0x1FC000B8 */
    LONG(0x25080001)    /* 0x1FC000BC */
    LONG(0xA12C0000)    /* 0x1FC000C0 */
    LONG(0x1540FFFB)    /* 0x1FC000C4 */
    LONG(0x25290001)    /* 0x1FC000C8 */
    LONG(0x10000005)    /* 0x1FC000CC */
    LONG(0x0)                   /* 0x1FC000D0 */
    LONG(0xA1200000)    /* 0x1FC000D4 */
    LONG(0x254AFFFF)    /* 0x1FC000D8 */
    LONG(0x1540FFFD)    /* 0x1FC000DC */
    LONG(0x25290001)    /* 0x1FC000E0 */
    LONG(0x25080003)    /* 0x1FC000E4 */
    LONG(0x240AFFFC)    /* 0x1FC000E8 */
    LONG(0x1484024)             /* 0x1FC000EC */
    LONG(0x8D090000)    /* 0x1FC000F0 */
    LONG(0x1520FFE7)    /* 0x1FC000F4 */
    LONG(0x0)                   /* 0x1FC000F8 */
    LONG(0x3C09BFC0)    /* 0x1FC000FC */
    LONG(0x25290000)    /* 0x1FC00100 */
    LONG(0x40897801)    /* 0x1FC00104 */
    LONG(0x30840000)    /* 0x1FC00108 */
    LONG(0x30A50000)    /* 0x1FC0010C */
    LONG(0x3C089FC0)    /* 0x1FC00110 */
    LONG(0x25080B88)    /* 0x1FC00114 */
    LONG(0x1000008)             /* 0x1FC00118 */
    LONG(0x0)                   /* 0x1FC0011C */
    LONG(0xFFFFFFFF)    /* 0x1FC00120 */
    LONG(0xFFFFFFFF)    /* 0x1FC00124 */
    LONG(0xFFFFFFFF)    /* 0x1FC00128 */
    LONG(0xFFFFFFFF)    /* 0x1FC0012C */
    LONG(0xFFFFFFFF)    /* 0x1FC00130 */
    LONG(0xFFFFFFFF)    /* 0x1FC00134 */
    LONG(0xFFFFFFFF)    /* 0x1FC00138 */
    LONG(0xFFFFFFFF)    /* 0x1FC0013C */
    LONG(0xFFFFFFFF)    /* 0x1FC00140 */
    LONG(0xFFFFFFFF)    /* 0x1FC00144 */
    LONG(0xFFFFFFFF)    /* 0x1FC00148 */
    LONG(0xFFFFFFFF)    /* 0x1FC0014C */
    LONG(0xFFFFFFFF)    /* 0x1FC00150 */
    LONG(0xFFFFFFFF)    /* 0x1FC00154 */
    LONG(0xFFFFFFFF)    /* 0x1FC00158 */
    LONG(0xFFFFFFFF)    /* 0x1FC0015C */
    LONG(0xFFFFFFFF)    /* 0x1FC00160 */
    LONG(0xFFFFFFFF)    /* 0x1FC00164 */
    LONG(0xFFFFFFFF)    /* 0x1FC00168 */
    LONG(0xFFFFFFFF)    /* 0x1FC0016C */
    LONG(0xFFFFFFFF)    /* 0x1FC00170 */
    LONG(0xFFFFFFFF)    /* 0x1FC00174 */
    LONG(0xFFFFFFFF)    /* 0x1FC00178 */
    LONG(0xFFFFFFFF)    /* 0x1FC0017C */
    LONG(0xFFFFFFFF)    /* 0x1FC00180 */
    LONG(0xFFFFFFFF)    /* 0x1FC00184 */
    LONG(0xFFFFFFFF)    /* 0x1FC00188 */
    LONG(0xFFFFFFFF)    /* 0x1FC0018C */
    LONG(0xFFFFFFFF)    /* 0x1FC00190 */
    LONG(0xFFFFFFFF)    /* 0x1FC00194 */
    LONG(0xFFFFFFFF)    /* 0x1FC00198 */
    LONG(0xFFFFFFFF)    /* 0x1FC0019C */
    LONG(0xFFFFFFFF)    /* 0x1FC001A0 */
    LONG(0xFFFFFFFF)    /* 0x1FC001A4 */
    LONG(0xFFFFFFFF)    /* 0x1FC001A8 */
    LONG(0xFFFFFFFF)    /* 0x1FC001AC */
    LONG(0xFFFFFFFF)    /* 0x1FC001B0 */
    LONG(0xFFFFFFFF)    /* 0x1FC001B4 */
    LONG(0xFFFFFFFF)    /* 0x1FC001B8 */
    LONG(0xFFFFFFFF)    /* 0x1FC001BC */
    LONG(0xFFFFFFFF)    /* 0x1FC001C0 */
    LONG(0xFFFFFFFF)    /* 0x1FC001C4 */
    LONG(0xFFFFFFFF)    /* 0x1FC001C8 */
    LONG(0xFFFFFFFF)    /* 0x1FC001CC */
    LONG(0xFFFFFFFF)    /* 0x1FC001D0 */
    LONG(0xFFFFFFFF)    /* 0x1FC001D4 */
    LONG(0xFFFFFFFF)    /* 0x1FC001D8 */
    LONG(0xFFFFFFFF)    /* 0x1FC001DC */
    LONG(0xFFFFFFFF)    /* 0x1FC001E0 */
    LONG(0xFFFFFFFF)    /* 0x1FC001E4 */
    LONG(0xFFFFFFFF)    /* 0x1FC001E8 */
    LONG(0xFFFFFFFF)    /* 0x1FC001EC */
    LONG(0xFFFFFFFF)    /* 0x1FC001F0 */
    LONG(0xFFFFFFFF)    /* 0x1FC001F4 */
    LONG(0xFFFFFFFF)    /* 0x1FC001F8 */
    LONG(0xFFFFFFFF)    /* 0x1FC001FC */
    LONG(0x673C64FD)    /* 0x1FC00200 */
    LONG(0xB604F540)    /* 0x1FC00204 */
    LONG(0x67519E80)    /* 0x1FC00208 */
    LONG(0x4A04F050)    /* 0x1FC0020C */
    LONG(0xF520DA81)    /* 0x1FC00210 */
    LONG(0x6A00B41C)    /* 0x1FC00214 */
    LONG(0xF520DC40)    /* 0x1FC00218 */
    LONG(0xDCA0B518)    /* 0x1FC0021C */
    LONG(0xB514F520)    /* 0x1FC00220 */
    LONG(0xF520DCA0)    /* 0x1FC00224 */
    LONG(0xACE0B414)    /* 0x1FC00228 */
    LONG(0x6D01F004)    /* 0x1FC0022C */
    LONG(0xEDECEDAB)    /* 0x1FC00230 */
    LONG(0xF520CCA0)    /* 0x1FC00234 */
    LONG(0xDC40B408)    /* 0x1FC00238 */
    LONG(0xB404F520)    /* 0x1FC0023C */
    LONG(0xF520DC40)    /* 0x1FC00240 */
    LONG(0xAAA0B204)    /* 0x1FC00244 */
    LONG(0xEC8B6C09)    /* 0x1FC00248 */
    LONG(0xCA80ECAC)    /* 0x1FC0024C */
    LONG(0xF5006C01)    /* 0x1FC00250 */
    LONG(0xDA80B218)    /* 0x1FC00254 */
    LONG(0xB214F500)    /* 0x1FC00258 */
    LONG(0x34829A80)    /* 0x1FC0025C */
    LONG(0xF5003486)    /* 0x1FC00260 */
    LONG(0x9A40B210)    /* 0x1FC00264 */
    LONG(0x5A01EA4F)    /* 0x1FC00268 */
    LONG(0x72016758)    /* 0x1FC0026C */
    LONG(0xEC6C6778)    /* 0x1FC00270 */
    LONG(0xB200F500)    /* 0x1FC00274 */
    LONG(0xF2009AA0)    /* 0x1FC00278 */
    LONG(0xED4C6A00)    /* 0x1FC0027C */
    LONG(0x6A0167E6)    /* 0x1FC00280 */
    LONG(0xF4E0250D)    /* 0x1FC00284 */
    LONG(0x9DC0B514)    /* 0x1FC00288 */
    LONG(0x2E08EE4C)    /* 0x1FC0028C */
    LONG(0x68809DC0)    /* 0x1FC00290 */
    LONG(0x2E04EE0C)    /* 0x1FC00294 */
    LONG(0x35BA9DA0)    /* 0x1FC00298 */
    LONG(0xEAACED4E)    /* 0x1FC0029C */
    LONG(0xF050EA8C)    /* 0x1FC002A0 */
    LONG(0xF200C140)    /* 0x1FC002A4 */
    LONG(0xF4C06D00)    /* 0x1FC002A8 */
    LONG(0xDCA0B414)    /* 0x1FC002AC */
    LONG(0xDF806C00)    /* 0x1FC002B0 */
    LONG(0x1A1F2203)    /* 0x1FC002B4 */
    LONG(0x6500022F)    /* 0x1FC002B8 */
    LONG(0xB11E1F)              /* 0x1FC002BC */
    LONG(0x65006500)    /* 0x1FC002C0 */
    LONG(0x40024800)    /* 0x1FC002C4 */
    LONG(0x77F000B4)    /* 0x1FC002C8 */
    LONG(0x0)                   /* 0x1FC002CC */
    LONG(0xF0306500)    /* 0x1FC002D0 */
    LONG(0xF030D958)    /* 0x1FC002D4 */
    LONG(0x6A02D95C)    /* 0x1FC002D8 */
    LONG(0xB404F4A0)    /* 0x1FC002DC */
    LONG(0xF4A0DC40)    /* 0x1FC002E0 */
    LONG(0xDC40B404)    /* 0x1FC002E4 */
    LONG(0x6C00F010)    /* 0x1FC002E8 */
    LONG(0xB21CF480)    /* 0x1FC002EC */
    LONG(0xF402DA80)    /* 0x1FC002F0 */
    LONG(0xF4806C00)    /* 0x1FC002F4 */
    LONG(0xDA80B218)    /* 0x1FC002F8 */
    LONG(0xB314F480)    /* 0x1FC002FC */
    LONG(0xDB406A14)    /* 0x1FC00300 */
    LONG(0xB010F480)    /* 0x1FC00304 */
    LONG(0x6C019840)    /* 0x1FC00308 */
    LONG(0x221EEA8C)    /* 0x1FC0030C */
    LONG(0xB208F480)    /* 0x1FC00310 */
    LONG(0x17F69A40)    /* 0x1FC00314 */
    LONG(0xA151F030)    /* 0x1FC00318 */
    LONG(0x2ACBEA11)    /* 0x1FC0031C */
    LONG(0xA140F050)    /* 0x1FC00320 */
    LONG(0xF4602AC8)    /* 0x1FC00324 */
    LONG(0x9E40B610)    /* 0x1FC00328 */
    LONG(0xEA8C6C01)    /* 0x1FC0032C */
    LONG(0xF4602206)    /* 0x1FC00330 */
    LONG(0x9A80B208)    /* 0x1FC00334 */
    LONG(0x1F91A1F)             /* 0x1FC00338 */
    LONG(0xF050EC11)    /* 0x1FC0033C */
    LONG(0x2A22A15D)    /* 0x1FC00340 */
    LONG(0x9988F070)    /* 0x1FC00344 */
    LONG(0x2C0AF1E0)    /* 0x1FC00348 */
    LONG(0xD51E1F)              /* 0x1FC0034C */
    LONG(0x65006500)    /* 0x1FC00350 */
    LONG(0x40024800)    /* 0x1FC00354 */
    LONG(0x77F000D8)    /* 0x1FC00358 */
    LONG(0x0)                   /* 0x1FC0035C */
    LONG(0xF0306500)    /* 0x1FC00360 */
    LONG(0xF030D954)    /* 0x1FC00364 */
    LONG(0xF030A1D0)    /* 0x1FC00368 */
    LONG(0xE297999C)    /* 0x1FC0036C */
    LONG(0xB40CF420)    /* 0x1FC00370 */
    LONG(0xED83EE86)    /* 0x1FC00374 */
    LONG(0x6D0161CF)    /* 0x1FC00378 */
    LONG(0xB404F420)    /* 0x1FC0037C */
    LONG(0xF030DCA0)    /* 0x1FC00380 */
    LONG(0x17C8D95C)    /* 0x1FC00384 */
    LONG(0x99A4F070)    /* 0x1FC00388 */
    LONG(0xB418F400)    /* 0x1FC0038C */
    LONG(0xF400A442)    /* 0x1FC00390 */
    LONG(0xE7B9B718)    /* 0x1FC00394 */
    LONG(0x4562C640)    /* 0x1FC00398 */
    LONG(0xD964F070)    /* 0x1FC0039C */
    LONG(0xC6016800)    /* 0x1FC003A0 */
    LONG(0x652C7211)    /* 0x1FC003A4 */
    LONG(0xF1206707)    /* 0x1FC003A8 */
    LONG(0x5A12600F)    /* 0x1FC003AC */
    LONG(0x7202602B)    /* 0x1FC003B0 */
    LONG(0x5A03606C)    /* 0x1FC003B4 */
    LONG(0x6F016012)    /* 0x1FC003B8 */
    LONG(0xF160EAEE)    /* 0x1FC003BC */
    LONG(0xF0502A13)    /* 0x1FC003C0 */
    LONG(0x6C01C140)    /* 0x1FC003C4 */
    LONG(0xC190F030)    /* 0x1FC003C8 */
    LONG(0xC14CF050)    /* 0x1FC003CC */
    LONG(0x6C08E069)    /* 0x1FC003D0 */
    LONG(0x4543C280)    /* 0x1FC003D4 */
    LONG(0x6A00E20D)    /* 0x1FC003D8 */
    LONG(0x7203104D)    /* 0x1FC003DC */
    LONG(0x7206605B)    /* 0x1FC003E0 */
    LONG(0x6100F160)    /* 0x1FC003E4 */
    LONG(0xF600A443)    /* 0x1FC003E8 */
    LONG(0xA4A43240)    /* 0x1FC003EC */
    LONG(0x35A035A0)    /* 0x1FC003F0 */
    LONG(0xA4A6EAAD)    /* 0x1FC003F4 */
    LONG(0xA485EAAD)    /* 0x1FC003F8 */
    LONG(0xEA8D3480)    /* 0x1FC003FC */
    LONG(0xF0303244)    /* 0x1FC00400 */
    LONG(0x10BA9964)    /* 0x1FC00404 */
    LONG(0xF0A07214)    /* 0x1FC00408 */
    LONG(0x5A15601B)    /* 0x1FC0040C */
    LONG(0x7213600D)    /* 0x1FC00410 */
    LONG(0x6108F140)    /* 0x1FC00414 */
    LONG(0x3240A443)    /* 0x1FC00418 */
    LONG(0xEAADA4A4)    /* 0x1FC0041C */
    LONG(0x6D0442C3)    /* 0x1FC00420 */
    LONG(0xEEACEDAB)    /* 0x1FC00424 */
    LONG(0x106E67A2)    /* 0x1FC00428 */
    LONG(0xEF4E6F1D)    /* 0x1FC0042C */
    LONG(0x7248273D)    /* 0x1FC00430 */
    LONG(0x6118F120)    /* 0x1FC00434 */
    LONG(0x656AA443)    /* 0x1FC00438 */
    LONG(0x654FA4E4)    /* 0x1FC0043C */
    LONG(0xA486A4E5)    /* 0x1FC00440 */
    LONG(0x678A650C)    /* 0x1FC00444 */
    LONG(0x34E03280)    /* 0x1FC00448 */
    LONG(0xEA8D3480)    /* 0x1FC0044C */
    LONG(0xEA8D678B)    /* 0x1FC00450 */
    LONG(0xF6006788)    /* 0x1FC00454 */
    LONG(0xEA8D3480)    /* 0x1FC00458 */
    LONG(0xD948F070)    /* 0x1FC0045C */
    LONG(0x676BE069)    /* 0x1FC00460 */
    LONG(0x678AC260)    /* 0x1FC00464 */
    LONG(0xC6E4C683)    /* 0x1FC00468 */
    LONG(0xF0704D06)    /* 0x1FC0046C */
    LONG(0x67E8D9A4)    /* 0x1FC00470 */
    LONG(0x1118C6E5)    /* 0x1FC00474 */
    LONG(0xE291B4CD)    /* 0x1FC00478 */
    LONG(0xE3598480)    /* 0x1FC0047C */
    LONG(0x4A01C680)    /* 0x1FC00480 */
    LONG(0x4D0B2CF9)    /* 0x1FC00484 */
    LONG(0xD9A4F070)    /* 0x1FC00488 */
    LONG(0xA443110D)    /* 0x1FC0048C */
    LONG(0xE549B5C8)    /* 0x1FC00490 */
    LONG(0x1008A484)    /* 0x1FC00494 */
    LONG(0xF0704D03)    /* 0x1FC00498 */
    LONG(0xE769D9A4)    /* 0x1FC0049C */
    LONG(0xB5C4A483)    /* 0x1FC004A0 */
    LONG(0xA480E591)    /* 0x1FC004A4 */
    LONG(0x10FEC280)    /* 0x1FC004A8 */
    LONG(0xC2E0E069)    /* 0x1FC004AC */
    LONG(0xC663A466)    /* 0x1FC004B0 */
    LONG(0xF0704545)    /* 0x1FC004B4 */
    LONG(0xC6E4D944)    /* 0x1FC004B8 */
    LONG(0x4D067330)    /* 0x1FC004BC */
    LONG(0x610EE049)    /* 0x1FC004C0 */
    LONG(0x2C04A488)    /* 0x1FC004C4 */
    LONG(0xD9A4F070)    /* 0x1FC004C8 */
    LONG(0x10066C50)    /* 0x1FC004CC */
    LONG(0xF0707401)    /* 0x1FC004D0 */
    LONG(0x6C43D9A4)    /* 0x1FC004D4 */
    LONG(0x6C496101)    /* 0x1FC004D8 */
    LONG(0x100CC280)    /* 0x1FC004DC */
    LONG(0x6E096783)    /* 0x1FC004E0 */
    LONG(0xECCCEECB)    /* 0x1FC004E4 */
    LONG(0xF0707420)    /* 0x1FC004E8 */
    LONG(0x6103D9A4)    /* 0x1FC004EC */
    LONG(0xEC8B6C01)    /* 0x1FC004F0 */
    LONG(0xC2E017F3)    /* 0x1FC004F4 */
    LONG(0x9944F070)    /* 0x1FC004F8 */
    LONG(0xF0704281)    /* 0x1FC004FC */
    LONG(0xE049D984)    /* 0x1FC00500 */
    LONG(0x17D06C00)    /* 0x1FC00504 */
    LONG(0x6007EDC3)    /* 0x1FC00508 */
    LONG(0xE7AD67E9)    /* 0x1FC0050C */
    LONG(0xEFEB6F01)    /* 0x1FC00510 */
    LONG(0x4D01C3EC)    /* 0x1FC00514 */
    LONG(0xF03017F7)    /* 0x1FC00518 */
    LONG(0x251CA1AC)    /* 0x1FC0051C */
    LONG(0x5E00F100)    /* 0x1FC00520 */
    LONG(0xF0706116)    /* 0x1FC00524 */
    LONG(0xF1E099AC)    /* 0x1FC00528 */
    LONG(0xEBAC6B1F)    /* 0x1FC0052C */
    LONG(0xF1002B10)    /* 0x1FC00530 */
    LONG(0x44619C84)    /* 0x1FC00534 */
    LONG(0xF030230C)    /* 0x1FC00538 */
    LONG(0xB79E9964)    /* 0x1FC0053C */
    LONG(0xF030EF8C)    /* 0x1FC00540 */
    LONG(0xE76FD9E4)    /* 0x1FC00544 */
    LONG(0xF070E3B5)    /* 0x1FC00548 */
    LONG(0xF030D9AC)    /* 0x1FC0054C */
    LONG(0x6C00D980)    /* 0x1FC00550 */
    LONG(0xC18CF030)    /* 0x1FC00554 */
    LONG(0x99ACF070)    /* 0x1FC00558 */
    LONG(0xE6B56785)    /* 0x1FC0055C */
    LONG(0x1A1FD215)    /* 0x1FC00560 */
    LONG(0xD61402AE)    /* 0x1FC00564 */
    LONG(0x998CF070)    /* 0x1FC00568 */
    LONG(0x9614B593)    /* 0x1FC0056C */
    LONG(0x2A01A1F)             /* 0x1FC00570 */
    LONG(0x921536CA)    /* 0x1FC00574 */
    LONG(0x996CF070)    /* 0x1FC00578 */
    LONG(0xF070E349)    /* 0x1FC0057C */
    LONG(0x1092D94C)    /* 0x1FC00580 */
    LONG(0x994CF070)    /* 0x1FC00584 */
    LONG(0x36C0A4C3)    /* 0x1FC00588 */
    LONG(0xEE8DA484)    /* 0x1FC0058C */
    LONG(0x650EE659)    /* 0x1FC00590 */
    LONG(0x678867A3)    /* 0x1FC00594 */
    LONG(0x602FEA83)    /* 0x1FC00598 */
    LONG(0x6C00F400)    /* 0x1FC0059C */
    LONG(0xEC4CEC8B)    /* 0x1FC005A0 */
    LONG(0x67E2B386)    /* 0x1FC005A4 */
    LONG(0xB386EF6C)    /* 0x1FC005A8 */
    LONG(0x3362E76D)    /* 0x1FC005AC */
    LONG(0xB785336A)    /* 0x1FC005B0 */
    LONG(0xA360E76D)    /* 0x1FC005B4 */
    LONG(0x67D87301)    /* 0x1FC005B8 */
    LONG(0xF7C0EBCB)    /* 0x1FC005BC */
    LONG(0xEB113363)    /* 0x1FC005C0 */
    LONG(0xF400656B)    /* 0x1FC005C4 */
    LONG(0x676844E0)    /* 0x1FC005C8 */
    LONG(0x6001EBE3)    /* 0x1FC005CC */
    LONG(0x678267E8)    /* 0x1FC005D0 */
    LONG(0x652BE54F)    /* 0x1FC005D4 */
    LONG(0xECE3E38D)    /* 0x1FC005D8 */
    LONG(0x6769600B)    /* 0x1FC005DC */
    LONG(0xE699E079)    /* 0x1FC005E0 */
    LONG(0xA4C0654E)    /* 0x1FC005E4 */
    LONG(0xEBCD676B)    /* 0x1FC005E8 */
    LONG(0xC66067CA)    /* 0x1FC005EC */
    LONG(0x17F04C01)    /* 0x1FC005F0 */
    LONG(0x674467A3)    /* 0x1FC005F4 */
    LONG(0xF07017CE)    /* 0x1FC005F8 */
    LONG(0x4541D94C)    /* 0x1FC005FC */
    LONG(0xD944F070)    /* 0x1FC00600 */
    LONG(0x6A00E0B5)    /* 0x1FC00604 */
    LONG(0x104EC540)    /* 0x1FC00608 */
    LONG(0x9980F030)    /* 0x1FC0060C */
    LONG(0x2641A1F)             /* 0x1FC00610 */
    LONG(0x2A026500)    /* 0x1FC00614 */
    LONG(0x103FB56C)    /* 0x1FC00618 */
    LONG(0x67B14284)    /* 0x1FC0061C */
    LONG(0x4D14F010)    /* 0x1FC00620 */
    LONG(0x1A1F6E01)    /* 0x1FC00624 */
    LONG(0xD21502A0)    /* 0x1FC00628 */
    LONG(0x42879215)    /* 0x1FC0062C */
    LONG(0x67B14C09)    /* 0x1FC00630 */
    LONG(0x4D18F010)    /* 0x1FC00634 */
    LONG(0x2A01A1F)             /* 0x1FC00638 */
    LONG(0x92156E01)    /* 0x1FC0063C */
    LONG(0x4C054287)    /* 0x1FC00640 */
    LONG(0xF01067B1)    /* 0x1FC00644 */
    LONG(0x1A1F4D1C)    /* 0x1FC00648 */
    LONG(0x6E0102A0)    /* 0x1FC0064C */
    LONG(0x42879215)    /* 0x1FC00650 */
    LONG(0x67B14C35)    /* 0x1FC00654 */
    LONG(0x4D08F030)    /* 0x1FC00658 */
    LONG(0x2A01A1F)             /* 0x1FC0065C */
    LONG(0x92156E01)    /* 0x1FC00660 */
    LONG(0xAA8BAAAA)    /* 0x1FC00664 */
    LONG(0x34803480)    /* 0x1FC00668 */
    LONG(0xB558ECAD)    /* 0x1FC0066C */
    LONG(0x2D17ED8C)    /* 0x1FC00670 */
    LONG(0xED8CB557)    /* 0x1FC00674 */
    LONG(0xAAAE250C)    /* 0x1FC00678 */
    LONG(0x3480AA8F)    /* 0x1FC0067C */
    LONG(0xECAD3480)    /* 0x1FC00680 */
    LONG(0xAAB1AAD0)    /* 0x1FC00684 */
    LONG(0x35A035A0)    /* 0x1FC00688 */
    LONG(0xE595EDCD)    /* 0x1FC0068C */
    LONG(0xB2511005)    /* 0x1FC00690 */
    LONG(0x24C0EC4C)    /* 0x1FC00694 */
    LONG(0xB451B550)    /* 0x1FC00698 */
    LONG(0x2AE1A1F)             /* 0x1FC0069C */
    LONG(0x6A016500)    /* 0x1FC006A0 */
    LONG(0xC151F030)    /* 0x1FC006A4 */
    LONG(0x67DD6A1B)    /* 0x1FC006A8 */
    LONG(0xF050C650)    /* 0x1FC006AC */
    LONG(0xC6F1A1F4)    /* 0x1FC006B0 */
    LONG(0x9944F070)    /* 0x1FC006B4 */
    LONG(0xC6923443)    /* 0x1FC006B8 */
    LONG(0x6C0EC653)    /* 0x1FC006BC */
    LONG(0x6D00C694)    /* 0x1FC006C0 */
    LONG(0x3046785)             /* 0x1FC006C4 */
    LONG(0xA6C0E399)    /* 0x1FC006C8 */
    LONG(0x4C01EDCE)    /* 0x1FC006CC */
    LONG(0x61F97405)    /* 0x1FC006D0 */
    LONG(0xC1BCF050)    /* 0x1FC006D4 */
    LONG(0x6D056783)    /* 0x1FC006D8 */
    LONG(0x2251A1F)             /* 0x1FC006DC */
    LONG(0xF050D215)    /* 0x1FC006E0 */
    LONG(0x6C00A1BC)    /* 0x1FC006E4 */
    LONG(0x67C49215)    /* 0x1FC006E8 */
    LONG(0x2605EE4E)    /* 0x1FC006EC */
    LONG(0xA6C0E099)    /* 0x1FC006F0 */
    LONG(0xEDCE4C01)    /* 0x1FC006F4 */
    LONG(0xF05017F8)    /* 0x1FC006F8 */
    LONG(0xB42BC1BC)    /* 0x1FC006FC */
    LONG(0x1A1F67A2)    /* 0x1FC00700 */
    LONG(0xD6140225)    /* 0x1FC00704 */
    LONG(0xF0506791)    /* 0x1FC00708 */
    LONG(0x1A1F4C1C)    /* 0x1FC0070C */
    LONG(0x6D010225)    /* 0x1FC00710 */
    LONG(0xF0709614)    /* 0x1FC00714 */
    LONG(0xF050D9C4)    /* 0x1FC00718 */
    LONG(0x1612C1DD)    /* 0x1FC0071C */
    LONG(0x9E40B61D)    /* 0x1FC00720 */
    LONG(0x4D016DFF)    /* 0x1FC00724 */
    LONG(0x22FAEAAC)    /* 0x1FC00728 */
    LONG(0xB22D3490)    /* 0x1FC0072C */
    LONG(0x2C01EA9B)    /* 0x1FC00730 */
    LONG(0xEA12E8E5)    /* 0x1FC00734 */
    LONG(0xB7164AFF)    /* 0x1FC00738 */
    LONG(0x6A00DF40)    /* 0x1FC0073C */
    LONG(0xD948F070)    /* 0x1FC00740 */
    LONG(0x65001603)    /* 0x1FC00744 */
    LONG(0xBF80F600)    /* 0x1FC00748 */
    LONG(0xBF80F230)    /* 0x1FC0074C */
    LONG(0xAA996655)    /* 0x1FC00750 */
    LONG(0x556699AA)    /* 0x1FC00754 */
    LONG(0xBF80F200)    /* 0x1FC00758 */
    LONG(0xBF886000)    /* 0x1FC0075C */
    LONG(0xBF886100)    /* 0x1FC00760 */
    LONG(0xBF80F200)    /* 0x1FC00764 */
    LONG(0xBF886014)    /* 0x1FC00768 */
    LONG(0xBF886120)    /* 0x1FC0076C */
    LONG(0x9D001000)    /* 0x1FC00770 */
    LONG(0xBF886130)    /* 0x1FC00774 */
    LONG(0xBF80F600)    /* 0x1FC00778 */
    LONG(0xBF886134)    /* 0x1FC0077C */
    LONG(0xBF80FA58)    /* 0x1FC00780 */
    LONG(0xBF80FB2C)    /* 0x1FC00784 */
    LONG(0xBF806200)    /* 0x1FC00788 */
    LONG(0xBF806210)    /* 0x1FC0078C */
    LONG(0xBF806240)    /* 0x1FC00790 */
    LONG(0xBF806210)    /* 0x1FC00794 */
    LONG(0xBF806230)    /* 0x1FC00798 */
    LONG(0x2625A0)              /* 0x1FC0079C */
    LONG(0xBF88603C)    /* 0x1FC007A0 */
    LONG(0xA0000660)    /* 0x1FC007A4 */
    LONG(0xA0000A60)    /* 0x1FC007A8 */
    LONG(0x9FC00B98)    /* 0x1FC007AC */
    LONG(0xA0000EDC)    /* 0x1FC007B0 */
    LONG(0xFFFF0000)    /* 0x1FC007B4 */
    LONG(0xA000066C)    /* 0x1FC007B8 */
    LONG(0x1FFFFC00)    /* 0x1FC007BC */
    LONG(0xE3000000)    /* 0x1FC007C0 */
    LONG(0xA0000E60)    /* 0x1FC007C4 */
    LONG(0x9D01E000)    /* 0x1FC007C8 */
    LONG(0x20000)               /* 0x1FC007CC */
    LONG(0x40000)               /* 0x1FC007D0 */
    LONG(0x80000)               /* 0x1FC007D4 */
    LONG(0x9D01F000)    /* 0x1FC007D8 */
    LONG(0x9D000000)    /* 0x1FC007DC */
    LONG(0x2625A00)             /* 0x1FC007E0 */
    LONG(0xEC11675C)    /* 0x1FC007E4 */
    LONG(0xA27CF050)    /* 0x1FC007E8 */
    LONG(0xF050EB8E)    /* 0x1FC007EC */
    LONG(0xF050C27C)    /* 0x1FC007F0 */
    LONG(0x5D079AB0)    /* 0x1FC007F4 */
    LONG(0x35A86047)    /* 0x1FC007F8 */
    LONG(0xE5D5B624)    /* 0x1FC007FC */
    LONG(0xED809DA0)    /* 0x1FC00800 */
    LONG(0x9FC00821)    /* 0x1FC00804 */
    LONG(0x9FC00831)    /* 0x1FC00808 */
    LONG(0x9FC00839)    /* 0x1FC0080C */
    LONG(0x9FC00843)    /* 0x1FC00810 */
    LONG(0x9FC00851)    /* 0x1FC00814 */
    LONG(0x9FC0085F)    /* 0x1FC00818 */
    LONG(0x9FC0087B)    /* 0x1FC0081C */
    LONG(0x6103741B)    /* 0x1FC00820 */
    LONG(0xF0506B01)    /* 0x1FC00824 */
    LONG(0xF050DA70)    /* 0x1FC00828 */
    LONG(0xE8A0C29C)    /* 0x1FC0082C */
    LONG(0xC294F050)    /* 0x1FC00830 */
    LONG(0x10266B02)    /* 0x1FC00834 */
    LONG(0xF0503480)    /* 0x1FC00838 */
    LONG(0x6B03DA98)    /* 0x1FC0083C */
    LONG(0xF0501021)    /* 0x1FC00840 */
    LONG(0xEB8D9A78)    /* 0x1FC00844 */
    LONG(0xDA78F050)    /* 0x1FC00848 */
    LONG(0x101A6B04)    /* 0x1FC0084C */
    LONG(0xEC6E6B0E)    /* 0x1FC00850 */
    LONG(0xF0702C16)    /* 0x1FC00854 */
    LONG(0x6B05DA80)    /* 0x1FC00858 */
    LONG(0xF0701013)    /* 0x1FC0085C */
    LONG(0x45619AA0)    /* 0x1FC00860 */
    LONG(0xDA60F070)    /* 0x1FC00864 */
    LONG(0xE6B5B60A)    /* 0x1FC00868 */
    LONG(0xF050C582)    /* 0x1FC0086C */
    LONG(0xEB8E9A98)    /* 0x1FC00870 */
    LONG(0x6B062B09)    /* 0x1FC00874 */
    LONG(0x2B031005)    /* 0x1FC00878 */
    LONG(0xF0506B01)    /* 0x1FC0087C */
    LONG(0x6B00C27D)    /* 0x1FC00880 */
    LONG(0xDA70F050)    /* 0x1FC00884 */
    LONG(0x6500E8A0)    /* 0x1FC00888 */
    LONG(0x9FC00804)    /* 0x1FC0088C */
    LONG(0xA0000660)    /* 0x1FC00890 */
    LONG(0xE28F6744)    /* 0x1FC00894 */
    LONG(0x600BEBA2)    /* 0x1FC00898 */
    LONG(0x9B60B306)    /* 0x1FC0089C */
    LONG(0x4E016EFF)    /* 0x1FC008A0 */
    LONG(0x23FAEBCC)    /* 0x1FC008A4 */
    LONG(0xB304A2C0)    /* 0x1FC008A8 */
    LONG(0x4A01DBC0)    /* 0x1FC008AC */
    LONG(0xE8A017F2)    /* 0x1FC008B0 */
    LONG(0xBF806210)    /* 0x1FC008B4 */
    LONG(0xBF806220)    /* 0x1FC008B8 */
    LONG(0x671C64E3)    /* 0x1FC008BC */
    LONG(0x9A40B22E)    /* 0x1FC008C0 */
    LONG(0x4B016BFF)    /* 0x1FC008C4 */
    LONG(0x22F9EA6C)    /* 0x1FC008C8 */
    LONG(0xB22C6B00)    /* 0x1FC008CC */
    LONG(0x6A01DA60)    /* 0x1FC008D0 */
    LONG(0xDB40B32B)    /* 0x1FC008D4 */
    LONG(0xDB40B32B)    /* 0x1FC008D8 */
    LONG(0x9880F030)    /* 0x1FC008DC */
    LONG(0x2641A1F)             /* 0x1FC008E0 */
    LONG(0x2A036500)    /* 0x1FC008E4 */
    LONG(0x9850F010)    /* 0x1FC008E8 */
    LONG(0xF030EAC0)    /* 0x1FC008EC */
    LONG(0xEB11A071)    /* 0x1FC008F0 */
    LONG(0xAA8A230D)    /* 0x1FC008F4 */
    LONG(0x3360AA6B)    /* 0x1FC008F8 */
    LONG(0xEB8D3360)    /* 0x1FC008FC */
    LONG(0xEB8CB422)    /* 0x1FC00900 */
    LONG(0xB4222305)    /* 0x1FC00904 */
    LONG(0x2641A1F)             /* 0x1FC00908 */
    LONG(0x22076500)    /* 0x1FC0090C */
    LONG(0xAA6DAA8C)    /* 0x1FC00910 */
    LONG(0x33603360)    /* 0x1FC00914 */
    LONG(0xF010EB8D)    /* 0x1FC00918 */
    LONG(0xAA80D870)    /* 0x1FC0091C */
    LONG(0x3360AA61)    /* 0x1FC00920 */
    LONG(0xEB8D3360)    /* 0x1FC00924 */
    LONG(0x61DE5B3C)    /* 0x1FC00928 */
    LONG(0xAA7BAA9A)    /* 0x1FC0092C */
    LONG(0x33603360)    /* 0x1FC00930 */
    LONG(0x6790EB8D)    /* 0x1FC00934 */
    LONG(0x4C10F010)    /* 0x1FC00938 */
    LONG(0xED6344AC)    /* 0x1FC0093C */
    LONG(0xAABC61D3)    /* 0x1FC00940 */
    LONG(0x3240AA5D)    /* 0x1FC00944 */
    LONG(0xEAAD3240)    /* 0x1FC00948 */
    LONG(0x61015A09)    /* 0x1FC0094C */
    LONG(0xE4736A08)    /* 0x1FC00950 */
    LONG(0x6001EC43)    /* 0x1FC00954 */
    LONG(0xF0506744)    /* 0x1FC00958 */
    LONG(0x6C00D844)    /* 0x1FC0095C */
    LONG(0x60C2EC4A)    /* 0x1FC00960 */
    LONG(0xF05067B0)    /* 0x1FC00964 */
    LONG(0xE4B54D04)    /* 0x1FC00968 */
    LONG(0xE395A5C0)    /* 0x1FC0096C */
    LONG(0x4C01C5C0)    /* 0x1FC00970 */
    LONG(0x650017F5)    /* 0x1FC00974 */
    LONG(0xBF806210)    /* 0x1FC00978 */
    LONG(0xBF806200)    /* 0x1FC0097C */
    LONG(0xBF886034)    /* 0x1FC00980 */
    LONG(0xBF886018)    /* 0x1FC00984 */
    LONG(0x2000000)             /* 0x1FC00988 */
    LONG(0x9D000000)    /* 0x1FC0098C */
    LONG(0x444CF0F0)    /* 0x1FC00990 */
    LONG(0x44A0F100)    /* 0x1FC00994 */
    LONG(0xEBA3B30D)    /* 0x1FC00998 */
    LONG(0x9A406115)    /* 0x1FC0099C */
    LONG(0x251242A1)    /* 0x1FC009A0 */
    LONG(0xED4C6D03)    /* 0x1FC009A4 */
    LONG(0x42A72D0F)    /* 0x1FC009A8 */
    LONG(0xEBA34D39)    /* 0x1FC009AC */
    LONG(0xAAAE610B)    /* 0x1FC009B0 */
    LONG(0x3360AA6F)    /* 0x1FC009B4 */
    LONG(0xEBAD3360)    /* 0x1FC009B8 */
    LONG(0x5B01EB8E)    /* 0x1FC009BC */
    LONG(0xEB6B6778)    /* 0x1FC009C0 */
    LONG(0xEA6CE820)    /* 0x1FC009C4 */
    LONG(0x6A00E820)    /* 0x1FC009C8 */
    LONG(0x9D01F000)    /* 0x1FC009CC */
    LONG(0x27BDFFF8)    /* 0x1FC009D0 */
    LONG(0xAFBF0004)    /* 0x1FC009D4 */
    LONG(0x7CA5E000)    /* 0x1FC009D8 */
    LONG(0x3C02BF81)    /* 0x1FC009DC */
    LONG(0xAC45F420)    /* 0x1FC009E0 */
    LONG(0x3C02BF81)    /* 0x1FC009E4 */
    LONG(0xAC46F430)    /* 0x1FC009E8 */
    LONG(0x7CC6E000)    /* 0x1FC009EC */
    LONG(0x3C02BF81)    /* 0x1FC009F0 */
    LONG(0xAC46F440)    /* 0x1FC009F4 */
    LONG(0x34844000)    /* 0x1FC009F8 */
    LONG(0x3C02BF81)    /* 0x1FC009FC */
    LONG(0xAC44F400)    /* 0x1FC00A00 */
    LONG(0x40024800)    /* 0x1FC00A04 */
    LONG(0x3C03BF81)    /* 0x1FC00A08 */
    LONG(0x40044800)    /* 0x1FC00A0C */
    LONG(0x822023)              /* 0x1FC00A10 */
    LONG(0x2C840078)    /* 0x1FC00A14 */
    LONG(0x1480FFFC)    /* 0x1FC00A18 */
    LONG(0x3C04AA99)    /* 0x1FC00A1C */
    LONG(0x3C02BF81)    /* 0x1FC00A20 */
    LONG(0x24846655)    /* 0x1FC00A24 */
    LONG(0xAC44F410)    /* 0x1FC00A28 */
    LONG(0x3C045566)    /* 0x1FC00A2C */
    LONG(0x348499AA)    /* 0x1FC00A30 */
    LONG(0xAC44F410)    /* 0x1FC00A34 */
    LONG(0x34048000)    /* 0x1FC00A38 */
    LONG(0x3C02BF81)    /* 0x1FC00A3C */
    LONG(0xAC44F408)    /* 0x1FC00A40 */
    LONG(0x8C62F400)    /* 0x1FC00A44 */
    LONG(0x30428000)    /* 0x1FC00A48 */
    LONG(0x1440FFFD)    /* 0x1FC00A4C */
    LONG(0x0)                   /* 0x1FC00A50 */
    LONG(0x40034800)    /* 0x1FC00A54 */
    LONG(0x40024800)    /* 0x1FC00A58 */
    LONG(0x431023)              /* 0x1FC00A5C */
    LONG(0x2C42000A)    /* 0x1FC00A60 */
    LONG(0x1440FFFC)    /* 0x1FC00A64 */
    LONG(0x3C02BF81)    /* 0x1FC00A68 */
    LONG(0x24034000)    /* 0x1FC00A6C */
    LONG(0xAC43F404)    /* 0x1FC00A70 */
    LONG(0x8FBF0004)    /* 0x1FC00A74 */
    LONG(0x3E00008)             /* 0x1FC00A78 */
    LONG(0x27BD0008)    /* 0x1FC00A7C */
    LONG(0x672464F5)    /* 0x1FC00A80 */
    LONG(0x67466765)    /* 0x1FC00A84 */
    LONG(0x680067E4)    /* 0x1FC00A88 */
    LONG(0x6011E84A)    /* 0x1FC00A8C */
    LONG(0xE4F1E333)    /* 0x1FC00A90 */
    LONG(0x46819CC0)    /* 0x1FC00A94 */
    LONG(0xF0082409)    /* 0x1FC00A98 */
    LONG(0x67A76C01)    /* 0x1FC00A9C */
    LONG(0x1E1FD205)    /* 0x1FC00AA0 */
    LONG(0xD3040274)    /* 0x1FC00AA4 */
    LONG(0x92059304)    /* 0x1FC00AA8 */
    LONG(0x48014F04)    /* 0x1FC00AAC */
    LONG(0x647517ED)    /* 0x1FC00AB0 */
    LONG(0x6500E8A0)    /* 0x1FC00AB4 */
    LONG(0xF40064F6)    /* 0x1FC00AB8 */
    LONG(0xEA4B6A00)    /* 0x1FC00ABC */
    LONG(0xEF4C67E4)    /* 0x1FC00AC0 */
    LONG(0xED4C4DFF)    /* 0x1FC00AC4 */
    LONG(0x4D00F400)    /* 0x1FC00AC8 */
    LONG(0xB018D504)    /* 0x1FC00ACC */
    LONG(0xB218E88C)    /* 0x1FC00AD0 */
    LONG(0x3002E041)    /* 0x1FC00AD4 */
    LONG(0xB217300A)    /* 0x1FC00AD8 */
    LONG(0x9204E041)    /* 0x1FC00ADC */
    LONG(0x6021EF43)    /* 0x1FC00AE0 */
    LONG(0xF400A040)    /* 0x1FC00AE4 */
    LONG(0x2A1A4760)    /* 0x1FC00AE8 */
    LONG(0x69016A05)    /* 0x1FC00AEC */
    LONG(0x6787E92B)    /* 0x1FC00AF0 */
    LONG(0x6004EC6A)    /* 0x1FC00AF4 */
    LONG(0xE9AC9CA0)    /* 0x1FC00AF8 */
    LONG(0x17FA4C04)    /* 0x1FC00AFC */
    LONG(0x240C4181)    /* 0x1FC00B00 */
    LONG(0x6C04F008)    /* 0x1FC00B04 */
    LONG(0x6E0067A7)    /* 0x1FC00B08 */
    LONG(0x1E1FD205)    /* 0x1FC00B0C */
    LONG(0xD3060274)    /* 0x1FC00B10 */
    LONG(0x4AFF9205)    /* 0x1FC00B14 */
    LONG(0x2AEB9306)    /* 0x1FC00B18 */
    LONG(0xC0406A01)    /* 0x1FC00B1C */
    LONG(0x480167E3)    /* 0x1FC00B20 */
    LONG(0x647617DC)    /* 0x1FC00B24 */
    LONG(0x6500E8A0)    /* 0x1FC00B28 */
    LONG(0x1FFFFC00)    /* 0x1FC00B2C */
    LONG(0xE3000000)    /* 0x1FC00B30 */
    LONG(0xA0000E60)    /* 0x1FC00B34 */
    LONG(0xA0000660)    /* 0x1FC00B38 */
    LONG(0x97C)                 /* 0x1FC00B3C */
    LONG(0x0)                   /* 0x1FC00B40 */
    LONG(0xA0000620)    /* 0x1FC00B44 */
    LONG(0x40)                  /* 0x1FC00B48 */
    LONG(0x0)                   /* 0x1FC00B4C */
    LONG(0xA0000600)    /* 0x1FC00B50 */
    LONG(0x20)                  /* 0x1FC00B54 */
    LONG(0x1)                   /* 0x1FC00B58 */
    LONG(0x9D001000)    /* 0x1FC00B5C */
    LONG(0x1000303)             /* 0x1FC00B60 */
    LONG(0x10006)               /* 0x1FC00B64 */
    LONG(0x130F0611)    /* 0x1FC00B68 */
    LONG(0x9D000000)    /* 0x1FC00B6C */
    LONG(0x9D000000)    /* 0x1FC00B70 */
    LONG(0x600)                 /* 0x1FC00B74 */
    LONG(0x1)                   /* 0x1FC00B78 */
    LONG(0x0)                   /* 0x1FC00B7C */
    LONG(0x22222222)    /* 0x1FC00B80 */
    LONG(0x22222222)    /* 0x1FC00B84 */
    LONG(0x77F00080)    /* 0x1FC00B88 */
    LONG(0x0)                   /* 0x1FC00B8C */
    LONG(0x1000FFFF)    /* 0x1FC00B90 */
    LONG(0x0)                   /* 0x1FC00B94 */
    LONG(0x354B5453)    /* 0x1FC00B98 */
    LONG(0x325F3030)    /* 0x1FC00B9C */
    LONG(0x0)                   /* 0x1FC00BA0 */
  } > kseg1_boot_mem
#endif

  .configs ORIGIN(configsfrs) :
  {
    LONG(0xFFFFFFF)
    LONG(0xFFF979D9)
    LONG(0xFF6A0E5B)
    LONG(0x7FFFFFFB)
  } > configsfrs

  /* Boot Sections */
  .reset _RESET_ADDR :
  {
    KEEP(*(.reset))
  } > kseg0_program_mem
}

/* From here out every linker script is the same, so just include it */
/*INCLUDE "chipKIT-application-COMMON.ld"*/
