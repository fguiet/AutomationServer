#!/bin/bash
echo "Now you need to reset the module and connect GPIO0 to Ground"
esptool.py --port /dev/ttyUSB0 write_flash 0x00000 esp8266_at0.51_sdk1.5.bin
