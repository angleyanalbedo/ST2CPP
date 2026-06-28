/* STM32F103C8T6 链接脚本 */
MEMORY
{
    FLASH  (rx)  : ORIGIN = 0x08000000, LENGTH = 64K
    RAM    (rwx) : ORIGIN = 0x20000000, LENGTH = 20K
}

SECTIONS
{
    .isr_vector : {
        . = ALIGN(4);
        KEEP(*(.isr_vector))
        . = ALIGN(4);
    } > FLASH

    .text : {
        . = ALIGN(4);
        *(.text*)
        *(.rodata*)
        *(.glue_7)
        *(.glue_7t)
        KEEP(*(.init))
        KEEP(*(.fini))
        . = ALIGN(4);
        _etext = .;
    } > FLASH

    .ARM.extab : {
        . = ALIGN(4);
        *(.ARM.extab* .gnu.linkonce.armextab.*)
        . = ALIGN(4);
    } > FLASH

    .ARM.exidx : {
        . = ALIGN(4);
        PROVIDE(__exidx_start = .);
        *(.ARM.exidx* .gnu.linkonce.armexidx.*)
        . = ALIGN(4);
        PROVIDE(__exidx_end = .);
    } > FLASH

    .preinit_array : {
        . = ALIGN(4);
        PROVIDE(__preinit_array_start = .);
        KEEP(*(.preinit_array*))
        PROVIDE(__preinit_array_end = .);
        . = ALIGN(4);
    } > FLASH

    .init_array : {
        . = ALIGN(4);
        PROVIDE(__init_array_start = .);
        KEEP(*(SORT(.init_array.*)))
        KEEP(*(.init_array*))
        PROVIDE(__init_array_end = .);
        . = ALIGN(4);
    } > FLASH

    .fini_array : {
        . = ALIGN(4);
        PROVIDE(__fini_array_start = .);
        KEEP(*(SORT(.fini_array.*)))
        KEEP(*(.fini_array*))
        PROVIDE(__fini_array_end = .);
        . = ALIGN(4);
    } > FLASH

    _sidata = .;

    .data : AT(_sidata) {
        . = ALIGN(4);
        _sdata = .;
        *(.data*)
        . = ALIGN(4);
        _edata = .;
    } > RAM

    .bss : {
        . = ALIGN(4);
        _sbss = .;
        __bss_start__ = _sbss;
        *(.bss*)
        *(COMMON)
        . = ALIGN(4);
        _ebss = .;
        __bss_end__ = _ebss;
    } > RAM

    PROVIDE(_estack = ORIGIN(RAM) + LENGTH(RAM));
    PROVIDE(_stack = ORIGIN(RAM) + LENGTH(RAM));
    PROVIDE(__stack = ORIGIN(RAM) + LENGTH(RAM));

    PROVIDE(end = .);
    PROVIDE(_end = .);
    __heap_start = .;
}
