//#include <Wire.h>  
#include "SSD1306.h" 

SSD1306Wire   display(0x3c, D1, D2); //Address set here 0x3C that I found in the scanner, and pins defined as D2 (SDA/Serial Data), and D5 (SCK/Serial Clock).

// Tested with ESP8266 v2.5 (Lolin) WEMOS D1 R2 & Mini
// RadioHead Library v1.89
// Adafruit SSD1306 v1.2.9
// ESP8266 ssd1306 v4.0 (https://github.com/ThingPulse/esp8266-oled-ssd1306)

// rf95_server.pde
// -*- mode: C++ -*-
// Example sketch showing how to create a simple messageing server
// with the RH_RF95 class. RH_RF95 class does not provide for addressing or
// reliability, so you should only use RH_RF95  if you do not need the higher
// level messaging abilities.
// It is designed to work with the other example rf95_client
// Tested with Anarduino MiniWirelessLoRa, Rocket Scream Mini Ultra Pro with
// the RFM95W, Adafruit Feather M0 with RFM95
#include <SPI.h>
#include <RH_RF95.h>
// Singleton instance of the radio driver
#define RFM95_CS D4
#define RFM95_RST D3
#define RFM95_INT D8

#define RF95_FREQ 868.0

int16_t packetnum = 0;  // packet counter, we increment per xmission

RH_RF95 rf95(RFM95_CS, RFM95_INT);
//RH_RF95 rf95(5, 2); // Rocket Scream Mini Ultra Pro with the RFM95W
//RH_RF95 rf95(8, 3); // Adafruit Feather M0 with RFM95 
// Need this on Arduino Zero with SerialUSB port (eg RocketScream Mini Ultra Pro)
//#define Serial SerialUSB
//int led = 9;
void setup() 
{  
    pinMode(RFM95_RST, OUTPUT);
    digitalWrite(RFM95_RST, HIGH);
    
  // Rocket Scream Mini Ultra Pro with the RFM95W only:
  // Ensure serial flash is not interfering with radio communication on SPI bus
//  pinMode(4, OUTPUT);
//  digitalWrite(4, HIGH);
  //pinMode(led, OUTPUT);     
  Serial.begin(9600);
  while (!Serial) ; // Wait for serial port to be available

// manual reset
  digitalWrite(RFM95_RST, LOW);
  delay(10);
  digitalWrite(RFM95_RST, HIGH);
  delay(10);
  
  if (!rf95.init()) {
    Serial.println("init failed");  
    while(1);
  }


 // The default transmitter power is 13dBm, using PA_BOOST.
  // If you are using RFM95/96/97/98 modules which uses the PA_BOOST transmitter pin, then 
  // you can set transmitter powers from 5 to 23 dBm:
  //rf95.setTxPower(23, false);
  //rf95.setTxPower(23, false);
   //Example 4: Bw = 125 kHz, Cr = 4/8, Sf = 4096chips/symbol, CRC on. Slow+long range

  //rf95.setSpreadingFactor(7);
  //rf95.setSignalBandwidth(125E3);
  //rf95.setCodingRate4(5);
 // rf95.setTxPower(20,false);
  
  // Defaults after init are 434.0MHz, 13dBm, Bw = 125 kHz, Cr = 4/5, Sf = 128chips/symbol, CRC on
  // The default transmitter power is 13dBm, using PA_BOOST.
  // If you are using RFM95/96/97/98 modules which uses the PA_BOOST transmitter pin, then 
  // you can set transmitter powers from 5 to 23 dBm:
//  driver.setTxPower(23, false);
  // If you are using Modtronix inAir4 or inAir9,or any other module which uses the
  // transmitter RFO pins and not the PA_BOOST pins
  // then you can configure the power transmitter power for -1 to 14 dBm and with useRFO true. 
  // Failure to do that will result in extremely low transmit powers.
//  driver.setTxPower(14, true);

  //rf95.setModemConfig(RH_RF95::Bw125Cr45Sf128);
  rf95.setModemConfig(RH_RF95::Bw125Cr48Sf4096);
  rf95.setTxPower(23,false);


  if (!rf95.setFrequency(RF95_FREQ)) {
     Serial.println("setFrequency failed");
     while (1);
  }  
  Serial.print("Set Freq to: "); Serial.println(RF95_FREQ);


  Serial.println("Ready...");

   display.init();
  display.flipScreenVertically();
  display.drawString(0, 0, "Ready...");
  display.display();
}
void loop()
{
  //display.clear();  
  
  if (rf95.available())
  {
    // Should be a message for us now   
    uint8_t buf[RH_RF95_MAX_MESSAGE_LEN];
    uint8_t len = sizeof(buf);
    if (rf95.recv(buf, &len))
    {
      packetnum++;

      display.clear(); 
      display.drawString(0, 0, "Received : " + String((char*)buf));      
      display.drawString(0, 16, "Packet n°: " + String(packetnum));
      display.drawString(0, 32, "RSSI : " + String(rf95.lastRssi()));      
      
//      digitalWrite(led, HIGH);
      RH_RF95::printBuffer("request: ", buf, len);
      Serial.print("got request: ");
      Serial.println((char*)buf);
      Serial.print("RSSI: ");
      Serial.println(rf95.lastRssi(), DEC);
      
      // Send a reply
      uint8_t data[] = "And hello back to you";
      rf95.send(data, sizeof(data));
      rf95.waitPacketSent();
      Serial.println("Sent a reply");
      // digitalWrite(led, LOW);
    }
    else
    {
      Serial.println("recv failed");
    }
  }

  display.display();
}
