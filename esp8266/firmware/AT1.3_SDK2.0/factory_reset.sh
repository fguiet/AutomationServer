#!/bin/bash
echo "Now you need to reset the module and connect GPIO0 to Ground"
esptool.py --port /dev/ttyUSB0 write_flash 0x00000 boot_v1.6.bin 0x01000 user1.1024.new.2.bin 0x3fc000 esp_init_data_default.bin 0xfe000 blank.bin 0x3fe000 blank.bin
