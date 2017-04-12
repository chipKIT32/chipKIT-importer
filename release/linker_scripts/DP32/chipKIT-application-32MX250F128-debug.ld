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
    LONG(0x25080628)	/* 0x1FC00044 */
    LONG(0x3C09A000)	/* 0x1FC00048 */
    LONG(0x25290840)	/* 0x1FC0004C */
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
    LONG(0x25080A18)	/* 0x1FC0007C */
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
    LONG(0x25080B40)	/* 0x1FC00100 */
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
    LONG(0xF0F0B50E)	/* 0x1FC00208 */
    LONG(0x4344446C)	/* 0x1FC0020C */
    LONG(0x6A00ED43)	/* 0x1FC00210 */
    LONG(0x9B606114)	/* 0x1FC00214 */
    LONG(0x261143C1)	/* 0x1FC00218 */
    LONG(0xEE6C6E03)	/* 0x1FC0021C */
    LONG(0x43C72E0E)	/* 0x1FC00220 */
    LONG(0xEDC34E39)	/* 0x1FC00224 */
    LONG(0xAB4F610A)	/* 0x1FC00228 */
    LONG(0x3240ABAE)	/* 0x1FC0022C */
    LONG(0xEAAD3240)	/* 0x1FC00230 */
    LONG(0x5A01EA8E)	/* 0x1FC00234 */
    LONG(0xEA4B6758)	/* 0x1FC00238 */
    LONG(0xE8A0EA6C)	/* 0x1FC0023C */
    LONG(0x9D01F000)	/* 0x1FC00240 */
    LONG(0x7CA5E000)	/* 0x1FC00244 */
    LONG(0x3C02BF81)	/* 0x1FC00248 */
    LONG(0xAC45F420)	/* 0x1FC0024C */
    LONG(0x3C02BF81)	/* 0x1FC00250 */
    LONG(0xAC46F430)	/* 0x1FC00254 */
    LONG(0x7CC6E000)	/* 0x1FC00258 */
    LONG(0x3C02BF81)	/* 0x1FC0025C */
    LONG(0xAC46F440)	/* 0x1FC00260 */
    LONG(0x34844000)	/* 0x1FC00264 */
    LONG(0x3C02BF81)	/* 0x1FC00268 */
    LONG(0xAC44F400)	/* 0x1FC0026C */
    LONG(0x40034800)	/* 0x1FC00270 */
    LONG(0x402821)		/* 0x1FC00274 */
    LONG(0x40024800)	/* 0x1FC00278 */
    LONG(0x431023)		/* 0x1FC0027C */
    LONG(0x2C420078)	/* 0x1FC00280 */
    LONG(0x1440FFFC)	/* 0x1FC00284 */
    LONG(0x3C02BF81)	/* 0x1FC00288 */
    LONG(0x3C03AA99)	/* 0x1FC0028C */
    LONG(0x24636655)	/* 0x1FC00290 */
    LONG(0xAC43F410)	/* 0x1FC00294 */
    LONG(0x3C035566)	/* 0x1FC00298 */
    LONG(0x346399AA)	/* 0x1FC0029C */
    LONG(0xAC43F410)	/* 0x1FC002A0 */
    LONG(0x34048000)	/* 0x1FC002A4 */
    LONG(0x3C03BF81)	/* 0x1FC002A8 */
    LONG(0xAC64F408)	/* 0x1FC002AC */
    LONG(0x8CA2F400)	/* 0x1FC002B0 */
    LONG(0x30428000)	/* 0x1FC002B4 */
    LONG(0x1440FFFD)	/* 0x1FC002B8 */
    LONG(0x0)			/* 0x1FC002BC */
    LONG(0x40024800)	/* 0x1FC002C0 */
    LONG(0x40034800)	/* 0x1FC002C4 */
    LONG(0x621823)		/* 0x1FC002C8 */
    LONG(0x2C63000A)	/* 0x1FC002CC */
    LONG(0x1460FFFC)	/* 0x1FC002D0 */
    LONG(0x24034000)	/* 0x1FC002D4 */
    LONG(0x3C02BF81)	/* 0x1FC002D8 */
    LONG(0xAC43F404)	/* 0x1FC002DC */
    LONG(0x3E00008)		/* 0x1FC002E0 */
    LONG(0x0)			/* 0x1FC002E4 */
    LONG(0x64F56A01)	/* 0x1FC002E8 */
    LONG(0x6704EA4B)	/* 0x1FC002EC */
    LONG(0x10116905)	/* 0x1FC002F0 */
    LONG(0x4B049BA0)	/* 0x1FC002F4 */
    LONG(0xEB8AEAAC)	/* 0x1FC002F8 */
    LONG(0x426161FB)	/* 0x1FC002FC */
    LONG(0xF008230E)	/* 0x1FC00300 */
    LONG(0x67B06C04)	/* 0x1FC00304 */
    LONG(0x49FF6E00)	/* 0x1FC00308 */
    LONG(0x911E1F)		/* 0x1FC0030C */
    LONG(0x9204D204)	/* 0x1FC00310 */
    LONG(0xF4002104)	/* 0x1FC00314 */
    LONG(0x67704080)	/* 0x1FC00318 */
    LONG(0x647517EE)	/* 0x1FC0031C */
    LONG(0x6500E8A0)	/* 0x1FC00320 */
    LONG(0xF40064F5)	/* 0x1FC00324 */
    LONG(0xB1116A00)	/* 0x1FC00328 */
    LONG(0x6704EA4B)	/* 0x1FC0032C */
    LONG(0x4DFFE84C)	/* 0x1FC00330 */
    LONG(0xED4CE025)	/* 0x1FC00334 */
    LONG(0xF4003122)	/* 0x1FC00338 */
    LONG(0x312A4D00)	/* 0x1FC0033C */
    LONG(0xB20C1010)	/* 0x1FC00340 */
    LONG(0xA260E149)	/* 0x1FC00344 */
    LONG(0x67902B09)	/* 0x1FC00348 */
    LONG(0x1A1FD205)	/* 0x1FC0034C */
    LONG(0xD50400BA)	/* 0x1FC00350 */
    LONG(0x6B019205)	/* 0x1FC00354 */
    LONG(0x9504C260)	/* 0x1FC00358 */
    LONG(0xF4004901)	/* 0x1FC0035C */
    LONG(0xE8A34800)	/* 0x1FC00360 */
    LONG(0x647561EE)	/* 0x1FC00364 */
    LONG(0x6500E8A0)	/* 0x1FC00368 */
    LONG(0x63000000)	/* 0x1FC0036C */
    LONG(0xA0001040)	/* 0x1FC00370 */
    LONG(0x676664F5)	/* 0x1FC00374 */
    LONG(0x67256704)	/* 0x1FC00378 */
    LONG(0x100F6A00)	/* 0x1FC0037C */
    LONG(0x468199C0)	/* 0x1FC00380 */
    LONG(0xF0082409)	/* 0x1FC00384 */
    LONG(0x67B06C01)	/* 0x1FC00388 */
    LONG(0x1E1FD204)	/* 0x1FC0038C */
    LONG(0xD3050091)	/* 0x1FC00390 */
    LONG(0x92049305)	/* 0x1FC00394 */
    LONG(0x4A014804)	/* 0x1FC00398 */
    LONG(0xEA634904)	/* 0x1FC0039C */
    LONG(0x647561EF)	/* 0x1FC003A0 */
    LONG(0x6500E8A0)	/* 0x1FC003A4 */
    LONG(0x673C64F4)	/* 0x1FC003A8 */
    LONG(0x9990F010)	/* 0x1FC003AC */
    LONG(0x821A1F)		/* 0x1FC003B0 */
    LONG(0x67026500)	/* 0x1FC003B4 */
    LONG(0x67B1223C)	/* 0x1FC003B8 */
    LONG(0xF0304284)	/* 0x1FC003BC */
    LONG(0x1A1F4D00)	/* 0x1FC003C0 */
    LONG(0x6E0100DD)	/* 0x1FC003C4 */
    LONG(0x67B14087)	/* 0x1FC003C8 */
    LONG(0xF0304C09)	/* 0x1FC003CC */
    LONG(0x1A1F4D04)	/* 0x1FC003D0 */
    LONG(0x6E0100DD)	/* 0x1FC003D4 */
    LONG(0x67B14087)	/* 0x1FC003D8 */
    LONG(0xF0304C05)	/* 0x1FC003DC */
    LONG(0x1A1F4D08)	/* 0x1FC003E0 */
    LONG(0x6E0100DD)	/* 0x1FC003E4 */
    LONG(0x67B14087)	/* 0x1FC003E8 */
    LONG(0xF0304C35)	/* 0x1FC003EC */
    LONG(0x1A1F4D0C)	/* 0x1FC003F0 */
    LONG(0x6E0100DD)	/* 0x1FC003F4 */
    LONG(0xA86AA84B)	/* 0x1FC003F8 */
    LONG(0x32403240)	/* 0x1FC003FC */
    LONG(0xB311EA6D)	/* 0x1FC00400 */
    LONG(0x2B1CEB4C)	/* 0x1FC00404 */
    LONG(0xEB4CB310)	/* 0x1FC00408 */
    LONG(0xA88F230C)	/* 0x1FC0040C */
    LONG(0xA8B1A84E)	/* 0x1FC00410 */
    LONG(0x34803480)	/* 0x1FC00414 */
    LONG(0xA850EC4D)	/* 0x1FC00418 */
    LONG(0x35A035A0)	/* 0x1FC0041C */
    LONG(0xE595ED4D)	/* 0x1FC00420 */
    LONG(0xB30A100A)	/* 0x1FC00424 */
    LONG(0xEA6CB40A)	/* 0x1FC00428 */
    LONG(0xB50A2205)	/* 0x1FC0042C */
    LONG(0xB50A1004)	/* 0x1FC00430 */
    LONG(0x1001B407)	/* 0x1FC00434 */
    LONG(0x1A1FB508)	/* 0x1FC00438 */
    LONG(0x650000C9)	/* 0x1FC0043C */
    LONG(0xE8A06474)	/* 0x1FC00440 */
    LONG(0x20000)		/* 0x1FC00444 */
    LONG(0x40000)		/* 0x1FC00448 */
    LONG(0x80000)		/* 0x1FC0044C */
    LONG(0x9D000000)	/* 0x1FC00450 */
    LONG(0x9D01F000)	/* 0x1FC00454 */
    LONG(0x9D01E000)	/* 0x1FC00458 */
    LONG(0x181A64E3)	/* 0x1FC0045C */
    LONG(0x65007E16)	/* 0x1FC00460 */
    LONG(0x6C08B22A)	/* 0x1FC00464 */
    LONG(0xDA806B04)	/* 0x1FC00468 */
    LONG(0xDA60671C)	/* 0x1FC0046C */
    LONG(0xDA80B228)	/* 0x1FC00470 */
    LONG(0x9890F010)	/* 0x1FC00474 */
    LONG(0x1A1FDA60)	/* 0x1FC00478 */
    LONG(0x65000082)	/* 0x1FC0047C */
    LONG(0xF0702242)	/* 0x1FC00480 */
    LONG(0xEB11A069)	/* 0x1FC00484 */
    LONG(0xAA6B230D)	/* 0x1FC00488 */
    LONG(0x3360AA8A)	/* 0x1FC0048C */
    LONG(0xEB8D3360)	/* 0x1FC00490 */
    LONG(0xEB8CB420)	/* 0x1FC00494 */
    LONG(0xB4202305)	/* 0x1FC00498 */
    LONG(0x821A1F)		/* 0x1FC0049C */
    LONG(0x22076500)	/* 0x1FC004A0 */
    LONG(0xAA8CAA6D)	/* 0x1FC004A4 */
    LONG(0x33603360)	/* 0x1FC004A8 */
    LONG(0xF010EB8D)	/* 0x1FC004AC */
    LONG(0xAA61D874)	/* 0x1FC004B0 */
    LONG(0x3360AA80)	/* 0x1FC004B4 */
    LONG(0xEB8D3360)	/* 0x1FC004B8 */
    LONG(0x61235B3C)	/* 0x1FC004BC */
    LONG(0xAA9AAA7B)	/* 0x1FC004C0 */
    LONG(0x33603360)	/* 0x1FC004C4 */
    LONG(0x6790EB8D)	/* 0x1FC004C8 */
    LONG(0x4C10F010)	/* 0x1FC004CC */
    LONG(0xED6344AC)	/* 0x1FC004D0 */
    LONG(0xAABC6118)	/* 0x1FC004D4 */
    LONG(0x3240AA5D)	/* 0x1FC004D8 */
    LONG(0xEAAD3240)	/* 0x1FC004DC */
    LONG(0x61015A09)	/* 0x1FC004E0 */
    LONG(0xE4736A08)	/* 0x1FC004E4 */
    LONG(0x6001EC43)	/* 0x1FC004E8 */
    LONG(0x67906744)	/* 0x1FC004EC */
    LONG(0xD850F050)	/* 0x1FC004F0 */
    LONG(0x4C10F050)	/* 0x1FC004F4 */
    LONG(0xA4A01005)	/* 0x1FC004F8 */
    LONG(0x4C014AFF)	/* 0x1FC004FC */
    LONG(0x4B01C3A0)	/* 0x1FC00500 */
    LONG(0xF0102AFA)	/* 0x1FC00504 */
    LONG(0xEAC09854)	/* 0x1FC00508 */
    LONG(0xBF886134)	/* 0x1FC0050C */
    LONG(0xBF886118)	/* 0x1FC00510 */
    LONG(0x2000000)		/* 0x1FC00514 */
    LONG(0x9D000000)	/* 0x1FC00518 */
    LONG(0xF0D0675C)	/* 0x1FC0051C */
    LONG(0x64C3A266)	/* 0x1FC00520 */
    LONG(0xF0B02306)	/* 0x1FC00524 */
    LONG(0x2203A258)	/* 0x1FC00528 */
    LONG(0x7C4E181A)	/* 0x1FC0052C */
    LONG(0x64436500)	/* 0x1FC00530 */
    LONG(0x6500E8A0)	/* 0x1FC00534 */
    LONG(0x671C64FD)	/* 0x1FC00538 */
    LONG(0x984CF070)	/* 0x1FC0053C */
    LONG(0xA382B3A6)	/* 0x1FC00540 */
    LONG(0xE559B5A6)	/* 0x1FC00544 */
    LONG(0x42C1C680)	/* 0x1FC00548 */
    LONG(0x6F00E5D9)	/* 0x1FC0054C */
    LONG(0x7406C6E0)	/* 0x1FC00550 */
    LONG(0xF07042C2)	/* 0x1FC00554 */
    LONG(0x6075D8CC)	/* 0x1FC00558 */
    LONG(0x60085C07)	/* 0x1FC0055C */
    LONG(0x602F7402)	/* 0x1FC00560 */
    LONG(0x60337403)	/* 0x1FC00564 */
    LONG(0xEC6E6B01)	/* 0x1FC00568 */
    LONG(0x10FB2410)	/* 0x1FC0056C */
    LONG(0x60797413)	/* 0x1FC00570 */
    LONG(0x60045C14)	/* 0x1FC00574 */
    LONG(0xF0E07411)	/* 0x1FC00578 */
    LONG(0x10F3600B)	/* 0x1FC0057C */
    LONG(0xF0A07414)	/* 0x1FC00580 */
    LONG(0x6F1D6015)	/* 0x1FC00584 */
    LONG(0x2429ECEE)	/* 0x1FC00588 */
    LONG(0x6B0110EC)	/* 0x1FC0058C */
    LONG(0x4A03E5D9)	/* 0x1FC00590 */
    LONG(0xC068F070)	/* 0x1FC00594 */
    LONG(0xC6606B08)	/* 0x1FC00598 */
    LONG(0xC098F050)	/* 0x1FC0059C */
    LONG(0xC090F070)	/* 0x1FC005A0 */
    LONG(0xD84CF070)	/* 0x1FC005A4 */
    LONG(0xB38EE2B5)	/* 0x1FC005A8 */
    LONG(0x4B018380)	/* 0x1FC005AC */
    LONG(0x4D01C580)	/* 0x1FC005B0 */
    LONG(0x4A082CFB)	/* 0x1FC005B4 */
    LONG(0xD84CF070)	/* 0x1FC005B8 */
    LONG(0xB28A6B04)	/* 0x1FC005BC */
    LONG(0xA34310D1)	/* 0x1FC005C0 */
    LONG(0xB489A364)	/* 0x1FC005C4 */
    LONG(0xC260E449)	/* 0x1FC005C8 */
    LONG(0xA36310CC)	/* 0x1FC005CC */
    LONG(0xE5D5B486)	/* 0x1FC005D0 */
    LONG(0xA360E46D)	/* 0x1FC005D4 */
    LONG(0xC5604A03)	/* 0x1FC005D8 */
    LONG(0xA3E61031)	/* 0x1FC005DC */
    LONG(0xC680E5D9)	/* 0x1FC005E0 */
    LONG(0xC6E3E559)	/* 0x1FC005E4 */
    LONG(0x7730C684)	/* 0x1FC005E8 */
    LONG(0xF07042C5)	/* 0x1FC005EC */
    LONG(0x610DD8CC)	/* 0x1FC005F0 */
    LONG(0xE5D5A368)	/* 0x1FC005F4 */
    LONG(0x6B502B03)	/* 0x1FC005F8 */
    LONG(0x1016C560)	/* 0x1FC005FC */
    LONG(0x4A067301)	/* 0x1FC00600 */
    LONG(0x60016B49)	/* 0x1FC00604 */
    LONG(0xC5606B43)	/* 0x1FC00608 */
    LONG(0x77201010)	/* 0x1FC0060C */
    LONG(0x77286002)	/* 0x1FC00610 */
    LONG(0xB2726109)	/* 0x1FC00614 */
    LONG(0xE2C96B01)	/* 0x1FC00618 */
    LONG(0x4E01EB6B)	/* 0x1FC0061C */
    LONG(0xF070C260)	/* 0x1FC00620 */
    LONG(0x1005D8CC)	/* 0x1FC00624 */
    LONG(0xC580E5D5)	/* 0x1FC00628 */
    LONG(0xF0704A06)	/* 0x1FC0062C */
    LONG(0xF070D84C)	/* 0x1FC00630 */
    LONG(0xB36A984C)	/* 0x1FC00634 */
    LONG(0xE34D6C00)	/* 0x1FC00638 */
    LONG(0x4A01C380)	/* 0x1FC0063C */
    LONG(0xD84CF070)	/* 0x1FC00640 */
    LONG(0xA3841090)	/* 0x1FC00644 */
    LONG(0x3480A343)	/* 0x1FC00648 */
    LONG(0xF6003480)	/* 0x1FC0064C */
    LONG(0xEA8D3240)	/* 0x1FC00650 */
    LONG(0xA365A386)	/* 0x1FC00654 */
    LONG(0x3360EA8D)	/* 0x1FC00658 */
    LONG(0x3244EA6D)	/* 0x1FC0065C */
    LONG(0x9878F010)	/* 0x1FC00660 */
    LONG(0xA3431041)	/* 0x1FC00664 */
    LONG(0x3240A364)	/* 0x1FC00668 */
    LONG(0x6B04EA6D)	/* 0x1FC0066C */
    LONG(0x4223EB6B)	/* 0x1FC00670 */
    LONG(0x6762E96C)	/* 0x1FC00674 */
    LONG(0xB45D1006)	/* 0x1FC00678 */
    LONG(0xE3916D01)	/* 0x1FC0067C */
    LONG(0xC4A0EDAB)	/* 0x1FC00680 */
    LONG(0xEB234B01)	/* 0x1FC00684 */
    LONG(0xF01061F8)	/* 0x1FC00688 */
    LONG(0x231CA07C)	/* 0x1FC0068C */
    LONG(0x5900F100)	/* 0x1FC00690 */
    LONG(0xF0706116)	/* 0x1FC00694 */
    LONG(0xF1E09894)	/* 0x1FC00698 */
    LONG(0xEB8C6B1F)	/* 0x1FC0069C */
    LONG(0xB3542B10)	/* 0x1FC006A0 */
    LONG(0x43A19B60)	/* 0x1FC006A4 */
    LONG(0xF010250C)	/* 0x1FC006A8 */
    LONG(0xF01098B8)	/* 0x1FC006AC */
    LONG(0xE4B7D870)	/* 0x1FC006B0 */
    LONG(0xEC6CB450)	/* 0x1FC006B4 */
    LONG(0xD898F010)	/* 0x1FC006B8 */
    LONG(0xF070E591)	/* 0x1FC006BC */
    LONG(0x6B00D894)	/* 0x1FC006C0 */
    LONG(0xC07CF010)	/* 0x1FC006C4 */
    LONG(0x98B4F070)	/* 0x1FC006C8 */
    LONG(0x6785D214)	/* 0x1FC006CC */
    LONG(0xC91A1F)		/* 0x1FC006D0 */
    LONG(0xF070E1B5)	/* 0x1FC006D4 */
    LONG(0xB5459894)	/* 0x1FC006D8 */
    LONG(0xDD1A1F)		/* 0x1FC006DC */
    LONG(0xF070362A)	/* 0x1FC006E0 */
    LONG(0x92149874)	/* 0x1FC006E4 */
    LONG(0xF070E349)	/* 0x1FC006E8 */
    LONG(0x103BD854)	/* 0x1FC006EC */
    LONG(0xF070A383)	/* 0x1FC006F0 */
    LONG(0xA3649854)	/* 0x1FC006F4 */
    LONG(0xE4513480)	/* 0x1FC006F8 */
    LONG(0x101EEC6D)	/* 0x1FC006FC */
    LONG(0xF400B73E)	/* 0x1FC00700 */
    LONG(0xEDAB6D00)	/* 0x1FC00704 */
    LONG(0xF400ED4C)	/* 0x1FC00708 */
    LONG(0xE5F54560)	/* 0x1FC0070C */
    LONG(0x35AA35A2)	/* 0x1FC00710 */
    LONG(0xE7B5B73A)	/* 0x1FC00714 */
    LONG(0x7501A5A0)	/* 0x1FC00718 */
    LONG(0x61016DFF)	/* 0x1FC0071C */
    LONG(0xEC636D00)	/* 0x1FC00720 */
    LONG(0x67646009)	/* 0x1FC00724 */
    LONG(0xA2E01007)	/* 0x1FC00728 */
    LONG(0xE625B12C)	/* 0x1FC0072C */
    LONG(0xC1E0EFAD)	/* 0x1FC00730 */
    LONG(0x4A014E01)	/* 0x1FC00734 */
    LONG(0x61F7EA63)	/* 0x1FC00738 */
    LONG(0x61E0EA83)	/* 0x1FC0073C */
    LONG(0xD854F070)	/* 0x1FC00740 */
    LONG(0xE2C9B226)	/* 0x1FC00744 */
    LONG(0x4E016B00)	/* 0x1FC00748 */
    LONG(0xF070C260)	/* 0x1FC0074C */
    LONG(0x1009D8CC)	/* 0x1FC00750 */
    LONG(0xEA1A1F)		/* 0x1FC00754 */
    LONG(0x6A016500)	/* 0x1FC00758 */
    LONG(0xC049F070)	/* 0x1FC0075C */
    LONG(0xB2286B04)	/* 0x1FC00760 */
    LONG(0x677DDA60)	/* 0x1FC00764 */
    LONG(0xC3506A1B)	/* 0x1FC00768 */
    LONG(0xA080F050)	/* 0x1FC0076C */
    LONG(0x984CF070)	/* 0x1FC00770 */
    LONG(0xC3916F1B)	/* 0x1FC00774 */
    LONG(0x334367BD)	/* 0x1FC00778 */
    LONG(0xC572ECEE)	/* 0x1FC0077C */
    LONG(0xC553EB8E)	/* 0x1FC00780 */
    LONG(0x6D0E67DD)	/* 0x1FC00784 */
    LONG(0x6B0EEA6E)	/* 0x1FC00788 */
    LONG(0xC6B4EA6E)	/* 0x1FC0078C */
    LONG(0xF0300404)	/* 0x1FC00790 */
    LONG(0x1A1FC058)	/* 0x1FC00794 */
    LONG(0x6D050147)	/* 0x1FC00798 */
    LONG(0x98ACF070)	/* 0x1FC0079C */
    LONG(0xA078F030)	/* 0x1FC007A0 */
    LONG(0x10056A00)	/* 0x1FC007A4 */
    LONG(0xE291B40D)	/* 0x1FC007A8 */
    LONG(0x4A01A480)	/* 0x1FC007AC */
    LONG(0x6722EB8E)	/* 0x1FC007B0 */
    LONG(0x29F8E9AE)	/* 0x1FC007B4 */
    LONG(0xF030B409)	/* 0x1FC007B8 */
    LONG(0x1A1FC078)	/* 0x1FC007BC */
    LONG(0x65000147)	/* 0x1FC007C0 */
    LONG(0xF0306790)	/* 0x1FC007C4 */
    LONG(0x1A1F4C18)	/* 0x1FC007C8 */
    LONG(0x6D010147)	/* 0x1FC007CC */
    LONG(0xD82CF070)	/* 0x1FC007D0 */
    LONG(0xE8A0647D)	/* 0x1FC007D4 */
    LONG(0xA0000840)	/* 0x1FC007D8 */
    LONG(0xA0000C40)	/* 0x1FC007DC */
    LONG(0x9FC00B50)	/* 0x1FC007E0 */
    LONG(0xBF886138)	/* 0x1FC007E4 */
    LONG(0xA00010BC)	/* 0x1FC007E8 */
    LONG(0xA000084C)	/* 0x1FC007EC */
    LONG(0xA0000944)	/* 0x1FC007F0 */
    LONG(0xFFFF0000)	/* 0x1FC007F4 */
    LONG(0x63000000)	/* 0x1FC007F8 */
    LONG(0xA0001040)	/* 0x1FC007FC */
    LONG(0xBF886134)	/* 0x1FC00800 */
    LONG(0x9A60B24B)	/* 0x1FC00804 */
    LONG(0x671C64E3)	/* 0x1FC00808 */
    LONG(0x6750B44A)	/* 0x1FC0080C */
    LONG(0x4A10F050)	/* 0x1FC00810 */
    LONG(0xB349DA61)	/* 0x1FC00814 */
    LONG(0xDB406A00)	/* 0x1FC00818 */
    LONG(0xB448DB80)	/* 0x1FC0081C */
    LONG(0x6D01F004)	/* 0x1FC00820 */
    LONG(0xDB80EDAB)	/* 0x1FC00824 */
    LONG(0x9C60B446)	/* 0x1FC00828 */
    LONG(0xDC60EBAC)	/* 0x1FC0082C */
    LONG(0xDB40B345)	/* 0x1FC00830 */
    LONG(0xDB40B345)	/* 0x1FC00834 */
    LONG(0x6A08B345)	/* 0x1FC00838 */
    LONG(0x6A04DB40)	/* 0x1FC0083C */
    LONG(0xB444DB40)	/* 0x1FC00840 */
    LONG(0xDB40B344)	/* 0x1FC00844 */
    LONG(0x9A40B244)	/* 0x1FC00848 */
    LONG(0x9C80F002)	/* 0x1FC0084C */
    LONG(0xEA6C6B10)	/* 0x1FC00850 */
    LONG(0xEC8F5A01)	/* 0x1FC00854 */
    LONG(0x5C016758)	/* 0x1FC00858 */
    LONG(0x74016798)	/* 0x1FC0085C */
    LONG(0xEA8C6798)	/* 0x1FC00860 */
    LONG(0x9C80B43E)	/* 0x1FC00864 */
    LONG(0x6B01EC6C)	/* 0x1FC00868 */
    LONG(0xB43D240D)	/* 0x1FC0086C */
    LONG(0xED6C9CA0)	/* 0x1FC00870 */
    LONG(0x9CA02D09)	/* 0x1FC00874 */
    LONG(0xEDCC6E80)	/* 0x1FC00878 */
    LONG(0x9C602D05)	/* 0x1FC0087C */
    LONG(0xEB8C6C40)	/* 0x1FC00880 */
    LONG(0x67785B01)	/* 0x1FC00884 */
    LONG(0x6C10EA6C)	/* 0x1FC00888 */
    LONG(0xDB80B332)	/* 0x1FC0088C */
    LONG(0xB3286C00)	/* 0x1FC00890 */
    LONG(0xC058F050)	/* 0x1FC00894 */
    LONG(0x2203DB80)	/* 0x1FC00898 */
    LONG(0x1171A1F)		/* 0x1FC0089C */
    LONG(0x1E1F6500)	/* 0x1FC008A0 */
    LONG(0x6500022A)	/* 0x1FC008A4 */
    LONG(0x40024800)	/* 0x1FC008A8 */
    LONG(0x77F0022D)	/* 0x1FC008AC */
    LONG(0x0)			/* 0x1FC008B0 */
    LONG(0xF0506500)	/* 0x1FC008B4 */
    LONG(0xF070D85C)	/* 0x1FC008B8 */
    LONG(0x181AD840)	/* 0x1FC008BC */
    LONG(0x65007E4B)	/* 0x1FC008C0 */
    LONG(0x181AB428)	/* 0x1FC008C4 */
    LONG(0x65007C95)	/* 0x1FC008C8 */
    LONG(0x2351E1F)		/* 0x1FC008CC */
    LONG(0x65006500)	/* 0x1FC008D0 */
    LONG(0x40024800)	/* 0x1FC008D4 */
    LONG(0x77F00238)	/* 0x1FC008D8 */
    LONG(0x0)			/* 0x1FC008DC */
    LONG(0xB3226500)	/* 0x1FC008E0 */
    LONG(0xA0A8F070)	/* 0x1FC008E4 */
    LONG(0x9880F070)	/* 0x1FC008E8 */
    LONG(0xD844F070)	/* 0x1FC008EC */
    LONG(0xE293ED66)	/* 0x1FC008F0 */
    LONG(0x6105EC63)	/* 0x1FC008F4 */
    LONG(0xB31D6C08)	/* 0x1FC008F8 */
    LONG(0xF070DB80)	/* 0x1FC008FC */
    LONG(0xF070D840)	/* 0x1FC00900 */
    LONG(0xEA11A049)	/* 0x1FC00904 */
    LONG(0xF0502A03)	/* 0x1FC00908 */
    LONG(0x2203A058)	/* 0x1FC0090C */
    LONG(0x1171A1F)		/* 0x1FC00910 */
    LONG(0x181A6500)	/* 0x1FC00914 */
    LONG(0x65007D1E)	/* 0x1FC00918 */
    LONG(0xA04CF050)	/* 0x1FC0091C */
    LONG(0x1A1F22D5)	/* 0x1FC00920 */
    LONG(0x6500014E)	/* 0x1FC00924 */
    LONG(0xF0506A00)	/* 0x1FC00928 */
    LONG(0x17CEC04C)	/* 0x1FC0092C */
    LONG(0xBF80F600)	/* 0x1FC00930 */
    LONG(0xAA996655)	/* 0x1FC00934 */
    LONG(0xBF80F230)	/* 0x1FC00938 */
    LONG(0x556699AA)	/* 0x1FC0093C */
    LONG(0xBF80F200)	/* 0x1FC00940 */
    LONG(0xBF886000)	/* 0x1FC00944 */
    LONG(0xBF886100)	/* 0x1FC00948 */
    LONG(0xBF886114)	/* 0x1FC0094C */
    LONG(0x9D000000)	/* 0x1FC00950 */
    LONG(0xBF886134)	/* 0x1FC00954 */
    LONG(0xBF886120)	/* 0x1FC00958 */
    LONG(0xBF886130)	/* 0x1FC0095C */
    LONG(0xBF80F600)	/* 0x1FC00960 */
    LONG(0x9D01F2B1)	/* 0x1FC00964 */
    LONG(0x2625A0)		/* 0x1FC00968 */
    LONG(0xBF88613C)	/* 0x1FC0096C */
    LONG(0xF030675C)	/* 0x1FC00970 */
    LONG(0xF030A278)	/* 0x1FC00974 */
    LONG(0xEC119ABC)	/* 0x1FC00978 */
    LONG(0x5D07EB8E)	/* 0x1FC0097C */
    LONG(0xC278F030)	/* 0x1FC00980 */
    LONG(0xE046045)		/* 0x1FC00984 */
    LONG(0xE6B535A4)	/* 0x1FC00988 */
    LONG(0xE5D98DA0)	/* 0x1FC0098C */
    LONG(0x6500EE80)	/* 0x1FC00990 */
    LONG(0x1F000F)		/* 0x1FC00994 */
    LONG(0x310027)		/* 0x1FC00998 */
    LONG(0x4F003F)		/* 0x1FC0099C */
    LONG(0x741B006F)	/* 0x1FC009A0 */
    LONG(0x6B016103)	/* 0x1FC009A4 */
    LONG(0xDA7CF030)	/* 0x1FC009A8 */
    LONG(0xC298F030)	/* 0x1FC009AC */
    LONG(0xF050E8A0)	/* 0x1FC009B0 */
    LONG(0x6B02C280)	/* 0x1FC009B4 */
    LONG(0x34801021)	/* 0x1FC009B8 */
    LONG(0xDA84F050)	/* 0x1FC009BC */
    LONG(0x101C6B03)	/* 0x1FC009C0 */
    LONG(0x9A64F050)	/* 0x1FC009C4 */
    LONG(0xF050EB8D)	/* 0x1FC009C8 */
    LONG(0x6B04DA64)	/* 0x1FC009CC */
    LONG(0x6B0E1015)	/* 0x1FC009D0 */
    LONG(0x6B00EC6E)	/* 0x1FC009D4 */
    LONG(0xF0502C11)	/* 0x1FC009D8 */
    LONG(0x6B05DA88)	/* 0x1FC009DC */
    LONG(0xF050100D)	/* 0x1FC009E0 */
    LONG(0xB50C9A68)	/* 0x1FC009E4 */
    LONG(0xC582E575)	/* 0x1FC009E8 */
    LONG(0x9A84F050)	/* 0x1FC009EC */
    LONG(0xF0504B01)	/* 0x1FC009F0 */
    LONG(0xEB8EDA68)	/* 0x1FC009F4 */
    LONG(0x6B062B0B)	/* 0x1FC009F8 */
    LONG(0xDA7CF030)	/* 0x1FC009FC */
    LONG(0x2B03E8A0)	/* 0x1FC00A00 */
    LONG(0xF0506B01)	/* 0x1FC00A04 */
    LONG(0x6B00C26C)	/* 0x1FC00A08 */
    LONG(0xDA7CF030)	/* 0x1FC00A0C */
    LONG(0x6500E8A0)	/* 0x1FC00A10 */
    LONG(0xA0000840)	/* 0x1FC00A14 */
    LONG(0xA0000840)	/* 0x1FC00A18 */
    LONG(0x97C)			/* 0x1FC00A1C */
    LONG(0x0)			/* 0x1FC00A20 */
    LONG(0xA0000628)	/* 0x1FC00A24 */
    LONG(0x40)			/* 0x1FC00A28 */
    LONG(0x0)			/* 0x1FC00A2C */
    LONG(0xA0000600)	/* 0x1FC00A30 */
    LONG(0x20)			/* 0x1FC00A34 */
    LONG(0x1)			/* 0x1FC00A38 */
    LONG(0x9D000000)	/* 0x1FC00A3C */
    LONG(0x9D001000)	/* 0x1FC00A40 */
    LONG(0x9D000000)	/* 0x1FC00A44 */
    LONG(0x1)			/* 0x1FC00A48 */
    LONG(0x1000109)		/* 0x1FC00A4C */
    LONG(0x4010001)		/* 0x1FC00A50 */
    LONG(0x330F0632)	/* 0x1FC00A54 */
    LONG(0x600)			/* 0x1FC00A58 */
    LONG(0xA00011BC)	/* 0x1FC00A5C */
    LONG(0x470)			/* 0x1FC00A60 */
    LONG(0x0)			/* 0x1FC00A64 */
    LONG(0xA0000668)	/* 0x1FC00A68 */
    LONG(0x8)			/* 0x1FC00A6C */
    LONG(0x0)			/* 0x1FC00A70 */
    LONG(0xA0000620)	/* 0x1FC00A74 */
    LONG(0x8)			/* 0x1FC00A78 */
    LONG(0x1)			/* 0x1FC00A7C */
    LONG(0x1C200)		/* 0x1FC00A80 */
    LONG(0x80000)		/* 0x1FC00A84 */
    LONG(0xA0001800)	/* 0x1FC00A88 */
    LONG(0x200)			/* 0x1FC00A8C */
    LONG(0x0)			/* 0x1FC00A90 */
    LONG(0xA0000670)	/* 0x1FC00A94 */
    LONG(0x38)			/* 0x1FC00A98 */
    LONG(0x0)			/* 0x1FC00A9C */
    LONG(0xA00006C0)	/* 0x1FC00AA0 */
    LONG(0x180)			/* 0x1FC00AA4 */
    LONG(0x0)			/* 0x1FC00AA8 */
    LONG(0x0)			/* 0x1FC00AAC */
    LONG(0x1010112)		/* 0x1FC00AB0 */
    LONG(0x40000002)	/* 0x1FC00AB4 */
    LONG(0xA6620403)	/* 0x1FC00AB8 */
    LONG(0x2010180)		/* 0x1FC00ABC */
    LONG(0x65000100)	/* 0x1FC00AC0 */
    LONG(0x430209)		/* 0x1FC00AC4 */
    LONG(0x80000102)	/* 0x1FC00AC8 */
    LONG(0x409FA)		/* 0x1FC00ACC */
    LONG(0x2020100)		/* 0x1FC00AD0 */
    LONG(0x24050001)	/* 0x1FC00AD4 */
    LONG(0x4011000)		/* 0x1FC00AD8 */
    LONG(0x5000224)		/* 0x1FC00ADC */
    LONG(0x1000624)		/* 0x1FC00AE0 */
    LONG(0x12405)		/* 0x1FC00AE4 */
    LONG(0x81050701)	/* 0x1FC00AE8 */
    LONG(0x10000803)	/* 0x1FC00AEC */
    LONG(0x10409)		/* 0x1FC00AF0 */
    LONG(0xA02)			/* 0x1FC00AF4 */
    LONG(0x82050700)	/* 0x1FC00AF8 */
    LONG(0x4002)			/* 0x1FC00AFC */
    LONG(0x2030507)		/* 0x1FC00B00 */
    LONG(0x40)			/* 0x1FC00B04 */
    LONG(0x4090304)		/* 0x1FC00B08 */
    LONG(0x770322)		/* 0x1FC00B0C */
    LONG(0x770077)		/* 0x1FC00B10 */
    LONG(0x63002E)		/* 0x1FC00B14 */
    LONG(0x750070)		/* 0x1FC00B18 */
    LONG(0x740073)		/* 0x1FC00B1C */
    LONG(0x630069)		/* 0x1FC00B20 */
    LONG(0x2E006B)		/* 0x1FC00B24 */
    LONG(0x6F0063)		/* 0x1FC00B28 */
    LONG(0x312006D)		/* 0x1FC00B2C */
    LONG(0x740053)		/* 0x1FC00B30 */
    LONG(0x35006B)		/* 0x1FC00B34 */
    LONG(0x300030)		/* 0x1FC00B38 */
    LONG(0x320076)		/* 0x1FC00B3C */
    LONG(0x77F00201)	/* 0x1FC00B40 */
    LONG(0x0)			/* 0x1FC00B44 */
    LONG(0x1000FFFF)	/* 0x1FC00B48 */
    LONG(0x0)			/* 0x1FC00B4C */
    LONG(0x354B5453)	/* 0x1FC00B50 */
    LONG(0x325F3030)	/* 0x1FC00B54 */
    LONG(0x65000000)	/* 0x1FC00B58 */
  } > kseg1_boot_mem

  .btl_pt2 ORIGIN(kseg0_splitflash_mem) : {
    LONG(0x677CA481)	/* 0x1D01F000 */
    LONG(0x74206746)	/* 0x1D01F004 */
    LONG(0x7421600C)	/* 0x1D01F008 */
    LONG(0x74016016)	/* 0x1D01F00C */
    LONG(0x60056766)	/* 0x1D01F010 */
    LONG(0x6C00101D)	/* 0x1D01F014 */
    LONG(0x4BFFC580)	/* 0x1D01F018 */
    LONG(0x2BFB4D01)	/* 0x1D01F01C */
    LONG(0x4547E8A0)	/* 0x1D01F020 */
    LONG(0x4B10F030)	/* 0x1D01F024 */
    LONG(0xA5801004)	/* 0x1D01F028 */
    LONG(0xC3804D01)	/* 0x1D01F02C */
    LONG(0xED4A4B01)	/* 0x1D01F030 */
    LONG(0xE82061FA)	/* 0x1D01F034 */
    LONG(0x45876A00)	/* 0x1D01F038 */
    LONG(0x4B10F030)	/* 0x1D01F03C */
    LONG(0xA3C01004)	/* 0x1D01F040 */
    LONG(0xC5C04B01)	/* 0x1D01F044 */
    LONG(0xED8A4D01)	/* 0x1D01F048 */
    LONG(0xE8A061FA)	/* 0x1D01F04C */
    LONG(0x6A00E820)	/* 0x1D01F050 */
    LONG(0x4267B212)	/* 0x1D01F054 */
    LONG(0x4B3964E3)	/* 0x1D01F058 */
    LONG(0x6C00671C)	/* 0x1D01F05C */
    LONG(0x4A04DA80)	/* 0x1D01F060 */
    LONG(0x61FBEA6A)	/* 0x1D01F064 */
    LONG(0xA08CF0D0)	/* 0x1D01F068 */
    LONG(0x67E66E00)	/* 0x1D01F06C */
    LONG(0x7CF4181A)	/* 0x1D01F070 */
    LONG(0xF0D06DFF)	/* 0x1D01F074 */
    LONG(0x6E00A085)	/* 0x1D01F078 */
    LONG(0x181A67E6)	/* 0x1D01F07C */
    LONG(0x6DFF7CF4)	/* 0x1D01F080 */
    LONG(0xA09CF0B0)	/* 0x1D01F084 */
    LONG(0xB6066D00)	/* 0x1D01F088 */
    LONG(0x7CF4181A)	/* 0x1D01F08C */
    LONG(0xF0706F40)	/* 0x1D01F090 */
    LONG(0xEAC09858)	/* 0x1D01F094 */
    LONG(0xE8A06463)	/* 0x1D01F098 */
    LONG(0xA00011BC)	/* 0x1D01F09C */
    LONG(0xA00011FC)	/* 0x1D01F0A0 */
    LONG(0x64F5EC11)	/* 0x1D01F0A4 */
    LONG(0x2C18671C)	/* 0x1D01F0A8 */
    LONG(0xF0706A01)	/* 0x1D01F0AC */
    LONG(0xF0B0C09C)	/* 0x1D01F0B0 */
    LONG(0x6900C058)	/* 0x1D01F0B4 */
    LONG(0xE5291009)	/* 0x1D01F0B8 */
    LONG(0xD504A280)	/* 0x1D01F0BC */
    LONG(0x25C1A1F)		/* 0x1D01F0C0 */
    LONG(0x9605D605)	/* 0x1D01F0C4 */
    LONG(0x49019504)	/* 0x1D01F0C8 */
    LONG(0x61F5E9C2)	/* 0x1D01F0CC */
    LONG(0xA09CF0B0)	/* 0x1D01F0D0 */
    LONG(0xB6166D00)	/* 0x1D01F0D4 */
    LONG(0x10216F40)	/* 0x1D01F0D8 */
    LONG(0xA05EF070)	/* 0x1D01F0DC */
    LONG(0x6D00B414)	/* 0x1D01F0E0 */
    LONG(0xE46D3348)	/* 0x1D01F0E4 */
    LONG(0x4A01DBA0)	/* 0x1D01F0E8 */
    LONG(0xEA6C6B0F)	/* 0x1D01F0EC */
    LONG(0xE4B13548)	/* 0x1D01F0F0 */
    LONG(0xF0709CE0)	/* 0x1D01F0F4 */
    LONG(0x5701C05E)	/* 0x1D01F0F8 */
    LONG(0xF0706113)	/* 0x1D01F0FC */
    LONG(0xEC4AA09F)	/* 0x1D01F100 */
    LONG(0x4C016104)	/* 0x1D01F104 */
    LONG(0xF070EC6C)	/* 0x1D01F108 */
    LONG(0x6B3FC09F)	/* 0x1D01F10C */
    LONG(0xF0D0EA78)	/* 0x1D01F110 */
    LONG(0xB608A08C)	/* 0x1D01F114 */
    LONG(0xEA126D01)	/* 0x1D01F118 */
    LONG(0x181AE659)	/* 0x1D01F11C */
    LONG(0x65007CF4)	/* 0x1D01F120 */
    LONG(0xE8206475)	/* 0x1D01F124 */
    LONG(0x65006A00)	/* 0x1D01F128 */
    LONG(0xA00011FC)	/* 0x1D01F12C */
    LONG(0xA00011BC)	/* 0x1D01F130 */
    LONG(0xA000123C)	/* 0x1D01F134 */
    LONG(0x673C64F5)	/* 0x1D01F138 */
    LONG(0xA146F0D0)	/* 0x1D01F13C */
    LONG(0x226E6705)	/* 0x1D01F140 */
    LONG(0xA15CF070)	/* 0x1D01F144 */
    LONG(0x45672A6B)	/* 0x1D01F148 */
    LONG(0x4B376A3F)	/* 0x1D01F14C */
    LONG(0x2A01EB5B)	/* 0x1D01F150 */
    LONG(0xEA12E8E5)	/* 0x1D01F154 */
    LONG(0xF0704A01)	/* 0x1D01F158 */
    LONG(0x6D10A17E)	/* 0x1D01F15C */
    LONG(0xF070656B)	/* 0x1D01F160 */
    LONG(0x67CBA17F)	/* 0x1D01F164 */
    LONG(0xE67B4E10)	/* 0x1D01F168 */
    LONG(0x2D01EEBA)	/* 0x1D01F16C */
    LONG(0xEE10E8E5)	/* 0x1D01F170 */
    LONG(0x67C52E01)	/* 0x1D01F174 */
    LONG(0x6007EE42)	/* 0x1D01F178 */
    LONG(0x181AD204)	/* 0x1D01F17C */
    LONG(0xD4057D1E)	/* 0x1D01F180 */
    LONG(0x94059204)	/* 0x1D01F184 */
    LONG(0xB52717E8)	/* 0x1D01F188 */
    LONG(0xE5493268)	/* 0x1D01F18C */
    LONG(0x6D3F9A40)	/* 0x1D01F190 */
    LONG(0xE8A3E557)	/* 0x1D01F194 */
    LONG(0x6001658A)	/* 0x1D01F198 */
    LONG(0x6A3F67B0)	/* 0x1D01F19C */
    LONG(0x67ECEB58)	/* 0x1D01F1A0 */
    LONG(0x651D650C)	/* 0x1D01F1A4 */
    LONG(0xE75DEA12)	/* 0x1D01F1A8 */
    LONG(0xE75DB21F)	/* 0x1D01F1AC */
    LONG(0x100C652F)	/* 0x1D01F1B0 */
    LONG(0xA7E067E8)	/* 0x1D01F1B4 */
    LONG(0xC2E06749)	/* 0x1D01F1B8 */
    LONG(0x67E84A01)	/* 0x1D01F1BC */
    LONG(0x6758652A)	/* 0x1D01F1C0 */
    LONG(0x4AFF4F01)	/* 0x1D01F1C4 */
    LONG(0x651A650F)	/* 0x1D01F1C8 */
    LONG(0x376861F3)	/* 0x1D01F1CC */
    LONG(0xE2E9B215)	/* 0x1D01F1D0 */
    LONG(0x67EC651A)	/* 0x1D01F1D4 */
    LONG(0x67F8E5E9)	/* 0x1D01F1D8 */
    LONG(0xDF407610)	/* 0x1D01F1DC */
    LONG(0x6001E0A3)	/* 0x1D01F1E0 */
    LONG(0x4B01200B)	/* 0x1D01F1E4 */
    LONG(0xEB4C6A0F)	/* 0x1D01F1E8 */
    LONG(0x76102805)	/* 0x1D01F1EC */
    LONG(0xC17FF070)	/* 0x1D01F1F0 */
    LONG(0x10146006)	/* 0x1D01F1F4 */
    LONG(0x17C7E4B1)	/* 0x1D01F1F8 */
    LONG(0xC17FF070)	/* 0x1D01F1FC */
    LONG(0x674B100F)	/* 0x1D01F200 */
    LONG(0xEA786B3F)	/* 0x1D01F204 */
    LONG(0xB2073748)	/* 0x1D01F208 */
    LONG(0xF0D0E2E9)	/* 0x1D01F20C */
    LONG(0x9AE0A18C)	/* 0x1D01F210 */
    LONG(0x6D01B605)	/* 0x1D01F214 */
    LONG(0x181AEB12)	/* 0x1D01F218 */
    LONG(0xE6797CF4)	/* 0x1D01F21C */
    LONG(0xE8A06475)	/* 0x1D01F220 */
    LONG(0xA00011BC)	/* 0x1D01F224 */
    LONG(0xA000123C)	/* 0x1D01F228 */
    LONG(0x671C64E3)	/* 0x1D01F22C */
    LONG(0xA05DF070)	/* 0x1D01F230 */
    LONG(0xF0B0220A)	/* 0x1D01F234 */
    LONG(0x6D00A09C)	/* 0x1D01F238 */
    LONG(0x181AB605)	/* 0x1D01F23C */
    LONG(0x6F407CF4)	/* 0x1D01F240 */
    LONG(0xF0706A00)	/* 0x1D01F244 */
    LONG(0x6463C05D)	/* 0x1D01F248 */
    LONG(0x6500E8A0)	/* 0x1D01F24C */
    LONG(0xA00011FC)	/* 0x1D01F250 */
    LONG(0x4267B210)	/* 0x1D01F254 */
    LONG(0x4B3964C3)	/* 0x1D01F258 */
    LONG(0xDAA06D00)	/* 0x1D01F25C */
    LONG(0xEA6A4A04)	/* 0x1D01F260 */
    LONG(0x675C61FB)	/* 0x1D01F264 */
    LONG(0xDA98F070)	/* 0x1D01F268 */
    LONG(0xB40CB60B)	/* 0x1D01F26C */
    LONG(0x181AB50C)	/* 0x1D01F270 */
    LONG(0x65007E09)	/* 0x1D01F274 */
    LONG(0x181AB40B)	/* 0x1D01F278 */
    LONG(0x6D127E0D)	/* 0x1D01F27C */
    LONG(0x181AB40A)	/* 0x1D01F280 */
    LONG(0x6D437E10)	/* 0x1D01F284 */
    LONG(0x181AB409)	/* 0x1D01F288 */
    LONG(0x6D387E13)	/* 0x1D01F28C */
    LONG(0xE8A06443)	/* 0x1D01F290 */
    LONG(0xA00011BC)	/* 0x1D01F294 */
    LONG(0x9D01F0A5)	/* 0x1D01F298 */
    LONG(0x9D01F055)	/* 0x1D01F29C */
    LONG(0x9D01F001)	/* 0x1D01F2A0 */
    LONG(0x9FC00AB0)	/* 0x1D01F2A4 */
    LONG(0x9FC00AC4)	/* 0x1D01F2A8 */
    LONG(0x9FC00B08)	/* 0x1D01F2AC */
    LONG(0x6500E8A0)	/* 0x1D01F2B0 */
    LONG(0x675C64A1)	/* 0x1D01F2B4 */
    LONG(0x102E6B00)	/* 0x1D01F2B8 */
    LONG(0xA6E1E479)	/* 0x1D01F2BC */
    LONG(0x61277705)	/* 0x1D01F2C0 */
    LONG(0x7702A6E3)	/* 0x1D01F2C4 */
    LONG(0xA6E26114)	/* 0x1D01F2C8 */
    LONG(0x200530FE)	/* 0x1D01F2CC */
    LONG(0xEF0C680F)	/* 0x1D01F2D0 */
    LONG(0xC2ECF0D0)	/* 0x1D01F2D4 */
    LONG(0x680F1004)	/* 0x1D01F2D8 */
    LONG(0xF0B0EF0C)	/* 0x1D01F2DC */
    LONG(0x6860C2FC)	/* 0x1D01F2E0 */
    LONG(0xA6C4EF18)	/* 0x1D01F2E4 */
    LONG(0xEF12B00E)	/* 0x1D01F2E8 */
    LONG(0xC7C4E0FD)	/* 0x1D01F2EC */
    LONG(0x77031010)	/* 0x1D01F2F0 */
    LONG(0xA602610E)	/* 0x1D01F2F4 */
    LONG(0xEF0C6F0F)	/* 0x1D01F2F8 */
    LONG(0xEF186860)	/* 0x1D01F2FC */
    LONG(0xC2E5F0D0)	/* 0x1D01F300 */
    LONG(0xB007A6C4)	/* 0x1D01F304 */
    LONG(0xE0FDEF12)	/* 0x1D01F308 */
    LONG(0x6E01C7C4)	/* 0x1D01F30C */
    LONG(0xE479C7C5)	/* 0x1D01F310 */
    LONG(0xE3CDA6C0)	/* 0x1D01F314 */
    LONG(0x61D0EBA2)	/* 0x1D01F318 */
    LONG(0xE8A06421)	/* 0x1D01F31C */
    LONG(0xA00006C0)	/* 0x1D01F320 */
    LONG(0xB3066A01)	/* 0x1D01F324 */
    LONG(0x6C84DB40)	/* 0x1D01F328 */
    LONG(0xDB80B305)	/* 0x1D01F32C */
    LONG(0x6CFFB305)	/* 0x1D01F330 */
    LONG(0xB305DB80)	/* 0x1D01F334 */
    LONG(0xE8A0DB40)	/* 0x1D01F338 */
    LONG(0xBF885250)	/* 0x1D01F33C */
    LONG(0xBF885070)	/* 0x1D01F340 */
    LONG(0xBF885200)	/* 0x1D01F344 */
    LONG(0xBF885210)	/* 0x1D01F348 */
    LONG(0xB31B6C00)	/* 0x1D01F34C */
    LONG(0xDB8064C3)	/* 0x1D01F350 */
    LONG(0x9B80B31A)	/* 0x1D01F354 */
    LONG(0x675C6D02)	/* 0x1D01F358 */
    LONG(0xDB80ECAD)	/* 0x1D01F35C */
    LONG(0x4DFB9B80)	/* 0x1D01F360 */
    LONG(0xDB80ECAC)	/* 0x1D01F364 */
    LONG(0x9A60F090)	/* 0x1D01F368 */
    LONG(0x4380F100)	/* 0x1D01F36C */
    LONG(0x6D001003)	/* 0x1D01F370 */
    LONG(0x4B01C3A0)	/* 0x1D01F374 */
    LONG(0x61FBEB8A)	/* 0x1D01F378 */
    LONG(0xB412B311)	/* 0x1D01F37C */
    LONG(0x6D001003)	/* 0x1D01F380 */
    LONG(0x4B01C3A0)	/* 0x1D01F384 */
    LONG(0x61FBEB8A)	/* 0x1D01F388 */
    LONG(0x9A78F090)	/* 0x1D01F38C */
    LONG(0xB30CA387)	/* 0x1D01F390 */
    LONG(0xF090C384)	/* 0x1D01F394 */
    LONG(0xF0909A90)	/* 0x1D01F398 */
    LONG(0x181A9AB4)	/* 0x1D01F39C */
    LONG(0x65007CAD)	/* 0x1D01F3A0 */
    LONG(0xB2096BFF)	/* 0x1D01F3A4 */
    LONG(0xB309DA60)	/* 0x1D01F3A8 */
    LONG(0x6C189B40)	/* 0x1D01F3AC */
    LONG(0xEA8D6443)	/* 0x1D01F3B0 */
    LONG(0xE8A0DB40)	/* 0x1D01F3B4 */
    LONG(0xBF885260)	/* 0x1D01F3B8 */
    LONG(0xBF885250)	/* 0x1D01F3BC */
    LONG(0xA00006C0)	/* 0x1D01F3C0 */
    LONG(0xA0000840)	/* 0x1D01F3C4 */
    LONG(0xBF885200)	/* 0x1D01F3C8 */
    LONG(0xBF885210)	/* 0x1D01F3CC */
    LONG(0x75FFED11)	/* 0x1D01F3D0 */
    LONG(0x602664A1)	/* 0x1D01F3D4 */
    LONG(0xEC586A60)	/* 0x1D01F3D8 */
    LONG(0xEA12B321)	/* 0x1D01F3DC */
    LONG(0xA244E349)	/* 0x1D01F3E0 */
    LONG(0x6001EAE2)	/* 0x1D01F3E4 */
    LONG(0x6A6067E2)	/* 0x1D01F3E8 */
    LONG(0x3084EC58)	/* 0x1D01F3EC */
    LONG(0xE2A9EA12)	/* 0x1D01F3F0 */
    LONG(0xA342E34D)	/* 0x1D01F3F4 */
    LONG(0x35A4E515)	/* 0x1D01F3F8 */
    LONG(0x675CE2B5)	/* 0x1D01F3FC */
    LONG(0x9A40F090)	/* 0x1D01F400 */
    LONG(0xE2B535AC)	/* 0x1D01F404 */
    LONG(0xEE4CB217)	/* 0x1D01F408 */
    LONG(0x9D40DDC1)	/* 0x1D01F40C */
    LONG(0x6B88A340)	/* 0x1D01F410 */
    LONG(0xF360EA6D)	/* 0x1D01F414 */
    LONG(0xEF6C4B17)	/* 0x1D01F418 */
    LONG(0x37E037E0)	/* 0x1D01F41C */
    LONG(0xDD40EAED)	/* 0x1D01F420 */
    LONG(0x24016B00)	/* 0x1D01F424 */
    LONG(0x6A0D6B10)	/* 0x1D01F428 */
    LONG(0xEA6D7401)	/* 0x1D01F42C */
    LONG(0x5402600C)	/* 0x1D01F430 */
    LONG(0x24066002)	/* 0x1D01F434 */
    LONG(0x74021011)	/* 0x1D01F438 */
    LONG(0x74036009)	/* 0x1D01F43C */
    LONG(0x100C600A)	/* 0x1D01F440 */
    LONG(0xB309EA11)	/* 0x1D01F444 */
    LONG(0xEA111008)	/* 0x1D01F448 */
    LONG(0x1005B308)	/* 0x1D01F44C */
    LONG(0xB308EA11)	/* 0x1D01F450 */
    LONG(0xEA111002)	/* 0x1D01F454 */
    LONG(0xDB40B307)	/* 0x1D01F458 */
    LONG(0xE8A06421)	/* 0x1D01F45C */
    LONG(0xA00006C0)	/* 0x1D01F460 */
    LONG(0x1FFFFFFF)	/* 0x1D01F464 */
    LONG(0xBF885300)	/* 0x1D01F468 */
    LONG(0xBF885310)	/* 0x1D01F46C */
    LONG(0xBF885320)	/* 0x1D01F470 */
    LONG(0xBF885330)	/* 0x1D01F474 */
    LONG(0x671C64F5)	/* 0x1D01F478 */
    LONG(0x98C0F090)	/* 0x1D01F47C */
    LONG(0x261CF1A0)	/* 0x1D01F480 */
    LONG(0x9A40B2DF)	/* 0x1D01F484 */
    LONG(0xEA2C6908)	/* 0x1D01F488 */
    LONG(0x2204F180)	/* 0x1D01F48C */
    LONG(0x9A40B2DD)	/* 0x1D01F490 */
    LONG(0x6C606DF0)	/* 0x1D01F494 */
    LONG(0x35B3ED4C)	/* 0x1D01F498 */
    LONG(0xE94CED98)	/* 0x1D01F49C */
    LONG(0x67385901)	/* 0x1D01F4A0 */
    LONG(0xE96E6B01)	/* 0x1D01F4A4 */
    LONG(0xEB4C6B04)	/* 0x1D01F4A8 */
    LONG(0xB2D7650B)	/* 0x1D01F4AC */
    LONG(0xEB1265AC)	/* 0x1D01F4B0 */
    LONG(0xE291E331)	/* 0x1D01F4B4 */
    LONG(0x5F01A4E0)	/* 0x1D01F4B8 */
    LONG(0x6778A4E2)	/* 0x1D01F4BC */
    LONG(0x5F013378)	/* 0x1D01F4C0 */
    LONG(0x6778C460)	/* 0x1D01F4C4 */
    LONG(0x34A4C462)	/* 0x1D01F4C8 */
    LONG(0x6788E425)	/* 0x1D01F4CC */
    LONG(0x67F85C01)	/* 0x1D01F4D0 */
    LONG(0x67787701)	/* 0x1D01F4D4 */
    LONG(0xE1653124)	/* 0x1D01F4D8 */
    LONG(0xE625312C)	/* 0x1D01F4DC */
    LONG(0x6C3C99C0)	/* 0x1D01F4E0 */
    LONG(0x348BECCC)	/* 0x1D01F4E4 */
    LONG(0xF0E0740D)	/* 0x1D01F4E8 */
    LONG(0x99816118)	/* 0x1D01F4EC */
    LONG(0xB6C7EB12)	/* 0x1D01F4F0 */
    LONG(0x6F21B1C7)	/* 0x1D01F4F4 */
    LONG(0x99C0ECCD)	/* 0x1D01F4F8 */
    LONG(0xEEECEFEB)	/* 0x1D01F4FC */
    LONG(0x6F01D9C0)	/* 0x1D01F500 */
    LONG(0x8CC3E265)	/* 0x1D01F504 */
    LONG(0x654FC1E6)	/* 0x1D01F508 */
    LONG(0x6778A4E0)	/* 0x1D01F50C */
    LONG(0xD962D963)	/* 0x1D01F510 */
    LONG(0x652F676D)	/* 0x1D01F514 */
    LONG(0x651FEF6C)	/* 0x1D01F518 */
    LONG(0x611AF0A0)	/* 0x1D01F51C */
    LONG(0x8CE1A461)	/* 0x1D01F520 */
    LONG(0x650B7306)	/* 0x1D01F524 */
    LONG(0x2B7A6778)	/* 0x1D01F528 */
    LONG(0xC1466A09)	/* 0x1D01F52C */
    LONG(0x32E36B01)	/* 0x1D01F530 */
    LONG(0x2B17EB4E)	/* 0x1D01F534 */
    LONG(0x985CF090)	/* 0x1D01F538 */
    LONG(0x6001EAC2)	/* 0x1D01F53C */
    LONG(0x6B6067C2)	/* 0x1D01F540 */
    LONG(0xB2B1ED78)	/* 0x1D01F544 */
    LONG(0xE271EB12)	/* 0x1D01F548 */
    LONG(0xE34D4B10)	/* 0x1D01F54C */
    LONG(0xF090DCC3)	/* 0x1D01F550 */
    LONG(0x10059858)	/* 0x1D01F554 */
    LONG(0x4EFFA280)	/* 0x1D01F558 */
    LONG(0xC3804A01)	/* 0x1D01F55C */
    LONG(0x2EFA4B01)	/* 0x1D01F560 */
    LONG(0x6B02104F)	/* 0x1D01F564 */
    LONG(0x2B17EB4E)	/* 0x1D01F568 */
    LONG(0x9854F090)	/* 0x1D01F56C */
    LONG(0x6001EAC2)	/* 0x1D01F570 */
    LONG(0x6B6067C2)	/* 0x1D01F574 */
    LONG(0xB2A4ED78)	/* 0x1D01F578 */
    LONG(0xE271EB12)	/* 0x1D01F57C */
    LONG(0xE34D4B10)	/* 0x1D01F580 */
    LONG(0xF090DCC3)	/* 0x1D01F584 */
    LONG(0x10059850)	/* 0x1D01F588 */
    LONG(0x4EFFA280)	/* 0x1D01F58C */
    LONG(0xC3804A01)	/* 0x1D01F590 */
    LONG(0x2EFA4B01)	/* 0x1D01F594 */
    LONG(0x6B031035)	/* 0x1D01F598 */
    LONG(0x2B2EEB4E)	/* 0x1D01F59C */
    LONG(0xEB116767)	/* 0x1D01F5A0 */
    LONG(0x982CF090)	/* 0x1D01F5A4 */
    LONG(0x9888F090)	/* 0x1D01F5A8 */
    LONG(0x10036A00)	/* 0x1D01F5AC */
    LONG(0xA7E0E45D)	/* 0x1D01F5B0 */
    LONG(0x4BFFE2E9)	/* 0x1D01F5B4 */
    LONG(0x270243E1)	/* 0x1D01F5B8 */
    LONG(0x61F8EA22)	/* 0x1D01F5BC */
    LONG(0x6A602708)	/* 0x1D01F5C0 */
    LONG(0xB391ED58)	/* 0x1D01F5C4 */
    LONG(0xE349EA12)	/* 0x1D01F5C8 */
    LONG(0xDA636B00)	/* 0x1D01F5CC */
    LONG(0xE4491019)	/* 0x1D01F5D0 */
    LONG(0xEE62A260)	/* 0x1D01F5D4 */
    LONG(0x67666001)	/* 0x1D01F5D8 */
    LONG(0xED986C60)	/* 0x1D01F5DC */
    LONG(0xEC12B68A)	/* 0x1D01F5E0 */
    LONG(0x4C10E69D)	/* 0x1D01F5E4 */
    LONG(0xE4D1DF63)	/* 0x1D01F5E8 */
    LONG(0xA2C01005)	/* 0x1D01F5EC */
    LONG(0x4A014BFF)	/* 0x1D01F5F0 */
    LONG(0x4C01C4C0)	/* 0x1D01F5F4 */
    LONG(0x10042BFA)	/* 0x1D01F5F8 */
    LONG(0xEA6E6B06)	/* 0x1D01F5FC */
    LONG(0xD9432A01)	/* 0x1D01F600 */
    LONG(0xED586A60)	/* 0x1D01F604 */
    LONG(0xED12B280)	/* 0x1D01F608 */
    LONG(0x4E0945C7)	/* 0x1D01F60C */
    LONG(0xE659E2B5)	/* 0x1D01F610 */
    LONG(0x9D43A5E4)	/* 0x1D01F614 */
    LONG(0x6039EAE2)	/* 0x1D01F618 */
    LONG(0x103767E2)	/* 0x1D01F61C */
    LONG(0x6B0167A8)	/* 0x1D01F620 */
    LONG(0x2D11ED6E)	/* 0x1D01F624 */
    LONG(0x6D026769)	/* 0x1D01F628 */
    LONG(0xF080EBAE)	/* 0x1D01F62C */
    LONG(0xF0802B05)	/* 0x1D01F630 */
    LONG(0xAC622F03)	/* 0x1D01F634 */
    LONG(0xEB8C6C0F)	/* 0x1D01F638 */
    LONG(0xEB98678D)	/* 0x1D01F63C */
    LONG(0xE269EB12)	/* 0x1D01F640 */
    LONG(0xC2E1C2E0)	/* 0x1D01F644 */
    LONG(0x67881079)	/* 0x1D01F648 */
    LONG(0xECCE6E05)	/* 0x1D01F64C */
    LONG(0xF0B02C03)	/* 0x1D01F650 */
    LONG(0x1072C0EC)	/* 0x1D01F654 */
    LONG(0x73096768)	/* 0x1D01F658 */
    LONG(0xF0D06109)	/* 0x1D01F65C */
    LONG(0x678A9848)	/* 0x1D01F660 */
    LONG(0xC086F0D0)	/* 0x1D01F664 */
    LONG(0xF0D04A01)	/* 0x1D01F668 */
    LONG(0x1066D848)	/* 0x1D01F66C */
    LONG(0x750867A8)	/* 0x1D01F670 */
    LONG(0xA1E46163)	/* 0x1D01F674 */
    LONG(0x6C09EB12)	/* 0x1D01F678 */
    LONG(0x6C01C186)	/* 0x1D01F67C */
    LONG(0xECE24B10)	/* 0x1D01F680 */
    LONG(0xC190D983)	/* 0x1D01F684 */
    LONG(0x6001E359)	/* 0x1D01F688 */
    LONG(0x6C0067E4)	/* 0x1D01F68C */
    LONG(0x10586D01)	/* 0x1D01F690 */
    LONG(0x35FE67E9)	/* 0x1D01F694 */
    LONG(0xEB122511)	/* 0x1D01F698 */
    LONG(0xE34D4B10)	/* 0x1D01F69C */
    LONG(0x9844F0B0)	/* 0x1D01F6A0 */
    LONG(0xEA4067A3)	/* 0x1D01F6A4 */
    LONG(0x6C09D305)	/* 0x1D01F6A8 */
    LONG(0x9305C186)	/* 0x1D01F6AC */
    LONG(0x6C00D943)	/* 0x1D01F6B0 */
    LONG(0x67C36D01)	/* 0x1D01F6B4 */
    LONG(0x104467E2)	/* 0x1D01F6B8 */
    LONG(0xEB122608)	/* 0x1D01F6BC */
    LONG(0x4B106C00)	/* 0x1D01F6C0 */
    LONG(0x67A4D9C3)	/* 0x1D01F6C4 */
    LONG(0xA1E4E279)	/* 0x1D01F6C8 */
    LONG(0x6790103B)	/* 0x1D01F6CC */
    LONG(0xF0B06D00)	/* 0x1D01F6D0 */
    LONG(0xF0B09844)	/* 0x1D01F6D4 */
    LONG(0x67C54C10)	/* 0x1D01F6D8 */
    LONG(0xB34E102E)	/* 0x1D01F6DC */
    LONG(0x36C3EE6C)	/* 0x1D01F6E0 */
    LONG(0x2D4736C3)	/* 0x1D01F6E4 */
    LONG(0xA2669961)	/* 0x1D01F6E8 */
    LONG(0x612EEC6A)	/* 0x1D01F6EC */
    LONG(0x610F7409)	/* 0x1D01F6F0 */
    LONG(0x9AE39A62)	/* 0x1D01F6F4 */
    LONG(0xEBEAE66D)	/* 0x1D01F6F8 */
    LONG(0x6005DA62)	/* 0x1D01F6FC */
    LONG(0x4E0943C7)	/* 0x1D01F700 */
    LONG(0x6D016C00)	/* 0x1D01F704 */
    LONG(0x6C00100E)	/* 0x1D01F708 */
    LONG(0x67C467A4)	/* 0x1D01F70C */
    LONG(0x9A621018)	/* 0x1D01F710 */
    LONG(0xE66D9AE3)	/* 0x1D01F714 */
    LONG(0xDA62EBEA)	/* 0x1D01F718 */
    LONG(0x43C76007)	/* 0x1D01F71C */
    LONG(0x4E096C00)	/* 0x1D01F720 */
    LONG(0xE2D967A4)	/* 0x1D01F724 */
    LONG(0x100CE77F)	/* 0x1D01F728 */
    LONG(0x9844F0B0)	/* 0x1D01F72C */
    LONG(0xF0B06790)	/* 0x1D01F730 */
    LONG(0xB5394C10)	/* 0x1D01F734 */
    LONG(0xEAC067C3)	/* 0x1D01F738 */
    LONG(0x6D016C00)	/* 0x1D01F73C */
    LONG(0x67E667C4)	/* 0x1D01F740 */
    LONG(0x7CF4181A)	/* 0x1D01F744 */
    LONG(0x10236500)	/* 0x1D01F748 */
    LONG(0xA08CF0B0)	/* 0x1D01F74C */
    LONG(0xB3332406)	/* 0x1D01F750 */
    LONG(0xF0B09B40)	/* 0x1D01F754 */
    LONG(0xEA8DC0AC)	/* 0x1D01F758 */
    LONG(0xB22BDB40)	/* 0x1D01F75C */
    LONG(0xC2606B00)	/* 0x1D01F760 */
    LONG(0x6B406C00)	/* 0x1D01F764 */
    LONG(0xC26167D0)	/* 0x1D01F768 */
    LONG(0xF0B067A4)	/* 0x1D01F76C */
    LONG(0x6F084E10)	/* 0x1D01F770 */
    LONG(0xF0D017E7)	/* 0x1D01F774 */
    LONG(0xED4EA045)	/* 0x1D01F778 */
    LONG(0x6A09250A)	/* 0x1D01F77C */
    LONG(0xB5239961)	/* 0x1D01F780 */
    LONG(0xF0B0EC4E)	/* 0x1D01F784 */
    LONG(0x5C019848)	/* 0x1D01F788 */
    LONG(0xEA406798)	/* 0x1D01F78C */
    LONG(0x6B08ED6D)	/* 0x1D01F790 */
    LONG(0xDA60B21B)	/* 0x1D01F794 */
    LONG(0x9B80B31A)	/* 0x1D01F798 */
    LONG(0xEC4C6A01)	/* 0x1D01F79C */
    LONG(0x6900241C)	/* 0x1D01F7A0 */
    LONG(0xD305D204)	/* 0x1D01F7A4 */
    LONG(0xC038F0B0)	/* 0x1D01F7A8 */
    LONG(0xC026F0D0)	/* 0x1D01F7AC */
    LONG(0x7CD3181A)	/* 0x1D01F7B0 */
    LONG(0xF0B06500)	/* 0x1D01F7B4 */
    LONG(0xECC09880)	/* 0x1D01F7B8 */
    LONG(0x6D40B413)	/* 0x1D01F7BC */
    LONG(0xC420C4A1)	/* 0x1D01F7C0 */
    LONG(0x6C0067D0)	/* 0x1D01F7C4 */
    LONG(0xF0B067A4)	/* 0x1D01F7C8 */
    LONG(0x181A4E10)	/* 0x1D01F7CC */
    LONG(0x6F087CF4)	/* 0x1D01F7D0 */
    LONG(0x93059204)	/* 0x1D01F7D4 */
    LONG(0xB30ADB40)	/* 0x1D01F7D8 */
    LONG(0x6C109B40)	/* 0x1D01F7DC */
    LONG(0x220CEA8C)	/* 0x1D01F7E0 */
    LONG(0xB50F6A00)	/* 0x1D01F7E4 */
    LONG(0xC058F0B0)	/* 0x1D01F7E8 */
    LONG(0xC046F0D0)	/* 0x1D01F7EC */
    LONG(0x6E119D40)	/* 0x1D01F7F0 */
    LONG(0xEACCEECB)	/* 0x1D01F7F4 */
    LONG(0xDB80DD40)	/* 0x1D01F7F8 */
    LONG(0xE8A06475)	/* 0x1D01F7FC */
    LONG(0xBF885200)	/* 0x1D01F800 */
    LONG(0xBF885240)	/* 0x1D01F804 */
    LONG(0xA00006C0)	/* 0x1D01F808 */
    LONG(0xA0000000)	/* 0x1D01F80C */
    LONG(0xBF885250)	/* 0x1D01F810 */
    LONG(0x3FF0000)		/* 0x1D01F814 */
    LONG(0xA00006D0)	/* 0x1D01F818 */
    LONG(0xBF885260)	/* 0x1D01F81C */
    LONG(0xBF885210)	/* 0x1D01F820 */
    LONG(0xF0B0675C)	/* 0x1D01F824 */
    LONG(0xF0B0DA80)	/* 0x1D01F828 */
    LONG(0xF0B0DAA4)	/* 0x1D01F82C */
    LONG(0xE8A0DAC8)	/* 0x1D01F830 */
    LONG(0xF090675C)	/* 0x1D01F834 */
    LONG(0xF090DA98)	/* 0x1D01F838 */
    LONG(0xE8A0DABC)	/* 0x1D01F83C */
    LONG(0xF090675C)	/* 0x1D01F840 */
    LONG(0xF090DA90)	/* 0x1D01F844 */
    LONG(0xE8A0DAB4)	/* 0x1D01F848 */
    LONG(0xF090675C)	/* 0x1D01F84C */
    LONG(0xF090DA88)	/* 0x1D01F850 */
    LONG(0xE8A0DAAC)	/* 0x1D01F854 */
    LONG(0xF090675C)	/* 0x1D01F858 */
    LONG(0x64F4A244)	/* 0x1D01F85C */
    LONG(0xB22C2254)	/* 0x1D01F860 */
    LONG(0xDA606B08)	/* 0x1D01F864 */
    LONG(0xDA606B04)	/* 0x1D01F868 */
    LONG(0xB22A6810)	/* 0x1D01F86C */
    LONG(0xDA606B08)	/* 0x1D01F870 */
    LONG(0xDA606B04)	/* 0x1D01F874 */
    LONG(0x7E201C1A)	/* 0x1D01F878 */
    LONG(0x65006500)	/* 0x1D01F87C */
    LONG(0x40114800)	/* 0x1D01F880 */
    LONG(0x77407E23)	/* 0x1D01F884 */
    LONG(0x0)			/* 0x1D01F888 */
    LONG(0x181A6500)	/* 0x1D01F88C */
    LONG(0x65007D1E)	/* 0x1D01F890 */
    LONG(0x7E271C1A)	/* 0x1D01F894 */
    LONG(0x65006500)	/* 0x1D01F898 */
    LONG(0x40024800)	/* 0x1D01F89C */
    LONG(0x77407E2A)	/* 0x1D01F8A0 */
    LONG(0x0)			/* 0x1D01F8A4 */
    LONG(0xB31C6500)	/* 0x1D01F8A8 */
    LONG(0xEB43E22B)	/* 0x1D01F8AC */
    LONG(0x48FF60EE)	/* 0x1D01F8B0 */
    LONG(0xB21A28DC)	/* 0x1D01F8B4 */
    LONG(0xB21ADA00)	/* 0x1D01F8B8 */
    LONG(0xB31ADA00)	/* 0x1D01F8BC */
    LONG(0x6C029B40)	/* 0x1D01F8C0 */
    LONG(0xEA8CEC8B)	/* 0x1D01F8C4 */
    LONG(0x6B04DB40)	/* 0x1D01F8C8 */
    LONG(0xDA60B211)	/* 0x1D01F8CC */
    LONG(0x7E361C1A)	/* 0x1D01F8D0 */
    LONG(0x65006500)	/* 0x1D01F8D4 */
    LONG(0x40024800)	/* 0x1D01F8D8 */
    LONG(0x77407E39)	/* 0x1D01F8DC */
    LONG(0x0)			/* 0x1D01F8E0 */
    LONG(0x6C086500)	/* 0x1D01F8E4 */
    LONG(0xDB80B30B)	/* 0x1D01F8E8 */
    LONG(0x7E3D1C1A)	/* 0x1D01F8EC */
    LONG(0x65006500)	/* 0x1D01F8F0 */
    LONG(0x40044800)	/* 0x1D01F8F4 */
    LONG(0x77407E40)	/* 0x1D01F8F8 */
    LONG(0x0)			/* 0x1D01F8FC */
    LONG(0xB30A6500)	/* 0x1D01F900 */
    LONG(0xEB83E453)	/* 0x1D01F904 */
    LONG(0x647460EE)	/* 0x1D01F908 */
    LONG(0x6500E8A0)	/* 0x1D01F90C */
    LONG(0xBF886134)	/* 0x1D01F910 */
    LONG(0xBF88613C)	/* 0x1D01F914 */
    LONG(0x2625A0)		/* 0x1D01F918 */
    LONG(0xBF885250)	/* 0x1D01F91C */
    LONG(0xBF885070)	/* 0x1D01F920 */
    LONG(0xBF885080)	/* 0x1D01F924 */
    LONG(0x989680)		/* 0x1D01F928 */
    LONG(0xB31264F4)	/* 0x1D01F92C */
    LONG(0xF090673C)	/* 0x1D01F930 */
    LONG(0xB411D960)	/* 0x1D01F934 */
    LONG(0x68019C40)	/* 0x1D01F938 */
    LONG(0xDC40EA0D)	/* 0x1D01F93C */
    LONG(0xB20F6C4A)	/* 0x1D01F940 */
    LONG(0xB20FDA80)	/* 0x1D01F944 */
    LONG(0xEA6CB40F)	/* 0x1D01F948 */
    LONG(0x67A33342)	/* 0x1D01F94C */
    LONG(0x3362ED11)	/* 0x1D01F950 */
    LONG(0xEB11DCA0)	/* 0x1D01F954 */
    LONG(0xDC60B40C)	/* 0x1D01F958 */
    LONG(0x3242F600)	/* 0x1D01F95C */
    LONG(0xDB40B30B)	/* 0x1D01F960 */
    LONG(0x7CC9181A)	/* 0x1D01F964 */
    LONG(0xF0906500)	/* 0x1D01F968 */
    LONG(0x6474C104)	/* 0x1D01F96C */
    LONG(0x6500E8A0)	/* 0x1D01F970 */
    LONG(0xA0001800)	/* 0x1D01F974 */
    LONG(0xBF885080)	/* 0x1D01F978 */
    LONG(0xBF8852B0)	/* 0x1D01F97C */
    LONG(0x1FFFFFFF)	/* 0x1D01F980 */
    LONG(0xBF885270)	/* 0x1D01F984 */
    LONG(0xBF8852C0)	/* 0x1D01F988 */
    LONG(0xBF8852D0)	/* 0x1D01F98C */
  } > kseg0_splitflash_mem
#endif

  .configs ORIGIN(configsfrs) :
  {
    LONG(0xCFFFFFFF)
    LONG(0xFFF979D9)
    LONG(0xFF6A0D5B)
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
