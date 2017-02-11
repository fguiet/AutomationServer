/****
 * Mailbox Notifier
 * 
 * F. Guiet 
 * Creation           : 20170211
 * Last modification  : 20170211 
 * 
 * Version            : 1.0
 */


//Light Mqtt library
#include <PubSubClient.h>

//Wifi library
#include <ESP8266WiFi.h>

ADC_MODE(ADC_VCC); //vcc read mode

//Mqtt settings
#define mqtt_server "192.168.1.25"
//#define mqtt_user ""
//#define mqtt_password ""
#define mailboxnotifier_topic "/guiet/mailbox/gotmail"

// WiFi settings
const char* ssid = "DUMBLEDORE";
const char* password = "frederic";

//Mesure with a voltmeter and calculate that the number mesured from ESP is correct
#define VCC_ADJ 1.038485804

#define PIN_LED 12
char message_buff[100];

WiFiClient espClient;
PubSubClient client(espClient);

void setup() {

  // Serial
  Serial.begin(115200);

  yield();

  //wait a little
  delay(200);
    
  client.setServer(mqtt_server, 1883); 
  pinMode(PIN_LED, OUTPUT);
  digitalWrite(PIN_LED, LOW);

  delay(200);

  makeLedBlink(2, 100);
  Serial.println("Module initialized...");

  delay(200);
  yield();

  connectToWifi();

  yield();
}

void makeLedBlink(int blinkTimes, int millisecond) {

  for (int x = 0; x < blinkTimes; x++) {
    digitalWrite(PIN_LED, HIGH);
    delay(millisecond);
    digitalWrite(PIN_LED, LOW);
    delay(millisecond);
  }
}

void connectToWifi() {

  WiFi.forceSleepWake();
  WiFi.mode(WIFI_STA);
  
  int retry = 0;
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED && retry < 100) {
    retry++;
    delay(500);
    Serial.print(".");
  }

  if (WiFi.status() == WL_CONNECTED) {  
     Serial.println("");
     Serial.println("WiFi connected");  
     // Print the IP address
     Serial.println(WiFi.localIP());
  } else {
    Serial.println("WiFi connection failed...");  
    makeLedBlink(5,100);
    goDeepSleep();
  }  
}

void goDeepSleep() {
  Serial.println("Entering deep sleep mode...good dreams fellows...");
  ESP.deepSleep(0, WAKE_RFCAL); 
  yield();
}

void reconnect() {

  int retry = 0;
  // Loop until we're reconnected
  while (!client.connected() && retry < 100) {
    Serial.print("Attempting MQTT connection...");
    
    if (client.connect("ESP8266_Mailboxnotifier")) {
      Serial.println("connected to MQTT Broker...");
    } else {
      retry++;
      // Wait 5 seconds before retrying
      delay(500);
      yield();
    }
  }

  if (retry == 10) {
    Serial.println("MQTT connection failed...");  
    makeLedBlink(5, 100);
    goDeepSleep();
  }
}

float getVcc() {
  return (float)ESP.getVcc() * VCC_ADJ;  
}

void loop() {
  // put your main code here, to run repeatedly:
  if (!client.connected()) {
    reconnect();
  }

  //Handle MQTT connection
  client.loop();

  float vcc = getVcc();
  String mess = "SETGOTMAIL;" + String(vcc,2);

  Serial.println("Sending message : " + mess);
  mess.toCharArray(message_buff, mess.length()+1);
  
  //Send got mail notification
  client.publish(mailboxnotifier_topic, message_buff);

  yield();

  makeLedBlink(1,100);
  goDeepSleep();
}


