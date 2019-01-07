#include <Wire.h>  
#include "SSD1306.h"
#include <ESP8266WiFi.h>


WiFiServer server(80); //Initialize the server on Port 80
SSD1306 display(0x3C, D2, D1);

void setup() {

//  display.begin(SSD1306_SWITCHCAPVCC, 0x3C);  // initialize with the I2C addr 0x3C (for the 64x48)
  //display.display();  

  Serial.begin(115200);
  Serial.println("Beginning...");
  
  WiFi.mode(WIFI_AP); //Our ESP8266-12E is an AccessPoint
  WiFi.softAP("ANEMOMETER-CALIBRATION-AP"); // Provide the (SSID, password);
  server.begin(); // Start the HTTP Server
  
  IPAddress httpServerIP = WiFi.softAPIP();
  Serial.print("Server IP is : ");
  Serial.println(httpServerIP);
  Serial.println("Ready...");

  display.init();

  display.drawString(0,0,"Ready...");
  display.display();

}

void loop() {
  
  WiFiClient client = server.available();
  if (!client) {
    return;   
  } 

  String request = client.readStringUntil('\r');   

  display.clear();
  display.drawString(0,0,"WS : " + request);
  display.display();
}
