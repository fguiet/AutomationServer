/****
 * Rollershutter manager
 * 
 * F. Guiet 
 * Creation           : 20170910
 * Last modification  : 20170930
 * 
 * Version            : 13
 * 
 * History            : 1.0 - Initial version
 *                      1.1 - Set Wifi to STA mode only (no AP)
 *                      1.2 - Remove webserver and replace with mqtt
 *                      1.3 - Add OTA Update handling
 * 
 * Note               : OTA only work correcly only and only if a hard reset is done AFTER serial port upload otherwise ESP will fail to start up on when OTA update occurs
 *                      https://github.com/esp8266/Arduino/issues/1782                     
 *                      https://github.com/esp8266/Arduino/issues/1017
 *                      Wemos D1 Datasheet : https://wiki.wemos.cc/products:d1:d1_mini
 */
 
#include <PubSubClient.h>
#include <ESP8266WiFi.h>
#include <SimpleTimer.h>
#include <ArduinoJson.h>
#include <ESP8266HTTPClient.h>
#include <ESP8266httpUpdate.h>
#include <Bounce2.h>

Bounce debouncer = Bounce();

#define UP_BUTTON_PIN_1 D3
#define DOWN_BUTTON_PIN_2 D5
#define RELAY_UP D1
#define RELAY_DOWN D2
#define UP_REED D6
#define DOWN_REED D7
#define UP_LED D8
#define DOWN_LED D0
#define MAX_RETRY 50

String SENSORID= "7";   //west
//String SENSORID= "8";   //north
IPAddress ip_wemos(192, 168, 1, 40); //west
//IPAddress ip_wemos(192, 168, 1, 41); //north
IPAddress gateway_ip(192, 168, 1, 1); // set gateway to match your network
IPAddress subnet_mask(255, 255, 255,   0);
const char* ssid = "DUMBLEDORE";
const char* password = "***";
//Mqtt settings

#define mqtt_server "192.168.1.25"
//#define mqtt_user ""
//#define mqtt_password ""

const int CURRENT_FIRMWARE_VERSION = 13;
String CHECK_FIRMWARE_VERSION_URL = "http://192.168.1.25:8510/automationserver-webapp/api/firmware/getversion/" + SENSORID;
String BASE_FIRMWARE_URL = "http://192.168.1.25:8510/automationserver-webapp/api/firmware/getfirmware/" + SENSORID;
String MQTT_CLIENT_ID = "RollerShutterSensor_" + SENSORID;
#define sub_topic1 "/guiet/openhab/rollershutter"
#define sub_topic2 "/guiet/automationserver/rollershutter"
#define pub_topic "/guiet/rollershutter"

WiFiClient espClient;
PubSubClient client(espClient);

// Instantiate a Bounce object
Bounce upButton = Bounce(); 

// Instantiate another Bounce object
Bounce downButton = Bounce(); 

Bounce upReed = Bounce(); 
Bounce downReed = Bounce(); 

// the timer object
SimpleTimer timer;
SimpleTimer blinkTimer;
SimpleTimer checkForUpdateTimer;
SimpleTimer closeTimer;

bool upAsked = false;
bool downAsked = false;
bool stopAsked = false;
bool resetUpButton = true;
bool resetDownButton = true;
bool isClosed = false;
bool isOpened = false;
bool isLedOn = false;
int timerId = -1;
int closeTimerId = -1;

//Max Time needed to rollershutter to up or down completly (time to shutdown + 5000ms)
int upDownTimeMs = 40000; 
//Time needed so rollershutter is completly closed
int completeDownMS = 5000;

StaticJsonBuffer<300> JSONBuffer;

void setup() {

  //Initialize relay PIN (LOW = RELAY ON, HIGH = REALY OFF)
  pinMode(RELAY_UP,OUTPUT);
  digitalWrite(RELAY_UP, HIGH); 

  pinMode(RELAY_DOWN,OUTPUT);
  digitalWrite(RELAY_DOWN, HIGH); 
  
  //Setup the LED :
  pinMode(BUILTIN_LED,OUTPUT);  
  digitalWrite(BUILTIN_LED, HIGH);

  pinMode(UP_LED,OUTPUT);  
  digitalWrite(UP_LED, LOW);

  pinMode(DOWN_LED,OUTPUT);  
  digitalWrite(DOWN_LED, LOW);

  // Setup the first button with an internal pull-up :
  pinMode(UP_BUTTON_PIN_1,INPUT_PULLUP);
  // After setting up the button, setup the Bounce instance :
  upButton.attach(UP_BUTTON_PIN_1);
  upButton.interval(5); // interval in ms
  
   // Setup the second button with an internal pull-up :
  pinMode(DOWN_BUTTON_PIN_2,INPUT_PULLUP);
  // After setting up the button, setup the Bounce instance :
  downButton.attach(DOWN_BUTTON_PIN_2);
  downButton.interval(5); // interval in ms

  pinMode(UP_REED,INPUT_PULLUP);
  // After setting up the button, setup the Bounce instance :
  upReed.attach(UP_REED);
  upReed.interval(5); // interval in ms

  pinMode(DOWN_REED,INPUT_PULLUP);
  // After setting up the button, setup the Bounce instance :
  downReed.attach(DOWN_REED);
  downReed.interval(5); // interval in ms
     

  Serial.begin(115200);

  connectToWifi();

  //initializeWebServer();
  connectToMqtt();

  initUpDownReedSwitch();

  blinkTimer.setInterval(1000, blinkBuiltInLed);
  checkForUpdateTimer.setInterval(60000, checkForUpdate);

  Serial.println("Ready...");
  
}

String sendState() {

  String mess = "SETRSSTATE;"+String(SENSORID)+";UNDETERMINED"; 
  
  if (isOpened) {
    mess = "SETRSSTATE;"+String(SENSORID)+";OPENED";         
  }

  if (isClosed) {
    mess = "SETRSSTATE;"+String(SENSORID)+";CLOSED";        
  }

  client.publish(pub_topic,mess.c_str()); 
}

void manageUpDownLed() {
    
  if (isOpened) {
    //rollshutter is opened
    digitalWrite(UP_LED, HIGH);     
    //Turn off internal led 
    digitalWrite(BUILTIN_LED, HIGH);    
  }
  else {
    //rollshutter is closed
    digitalWrite(UP_LED, LOW);         
  }

  if (isClosed) {
    //rollshutter is closed
    digitalWrite(DOWN_LED, HIGH);    
    //Turn off internal led 
    digitalWrite(BUILTIN_LED, HIGH);  
    //Send state here to automation server via mqtt
  } else {
    //rollshutter is opened
    digitalWrite(DOWN_LED, LOW);       
  } 

  sendState();
}

void checkForUpdate() {

  HTTPClient httpClient;
  httpClient.setTimeout(2000);
  Serial.println("Calling : " + CHECK_FIRMWARE_VERSION_URL);
  httpClient.begin( CHECK_FIRMWARE_VERSION_URL );
  int httpCode = httpClient.GET();
  Serial.println("Http code received : "+String(httpCode));
  if( httpCode == 200 ) {  
    String result = httpClient.getString();
    Serial.print("Checking version : "+ result);
    JsonObject& parsed= JSONBuffer.parseObject(result);
    if (parsed.success()) {      
        int lastVersion = parsed["lastversion"];        
        Serial.println("Version courante : " + String(CURRENT_FIRMWARE_VERSION) + ", dernière version : " + String(lastVersion));
        if( lastVersion > CURRENT_FIRMWARE_VERSION ) {
          doOTAUpdate(lastVersion);
        }
    }
  }
  
  httpClient.end();  
}

void doOTAUpdate(int version) {

  t_httpUpdate_return ret  = ESPhttpUpdate.update(BASE_FIRMWARE_URL +  "/" + String(version));
  
  switch (ret) {
    case HTTP_UPDATE_FAILED:
      Serial.println("[update] Update failed.");      
      break;
    case HTTP_UPDATE_NO_UPDATES:
      Serial.println("[update] Update no Updates.");           
      break;
    case HTTP_UPDATE_OK:  
      Serial.println("[update] Update ok.");   
      break;
  }  
}

void loop() {

  //In case of Wifi deconnection...just reboot it is better...
  if (WiFi.status() != WL_CONNECTED || !client.connected()) {
      ESP.restart();
  }

  client.loop();
  
  handleButtons();  
  handleRollershutter();

  blinkTimer.run();
  timer.run();
  closeTimer.run();  
  checkForUpdateTimer.run();
}

//Led blink only if roller shutter not closed nor opened
void blinkBuiltInLed() {

  if (isOpened || isClosed) {    
    return;
  }
   
  if (!isLedOn) {
    digitalWrite(BUILTIN_LED, LOW);
    isLedOn=true;
  }
  else {
    digitalWrite(BUILTIN_LED, HIGH);
    isLedOn=false;
  }
}

void handleButtons() {
  // Update the Bounce instances :
  upButton.update();
  downButton.update();
  upReed.update();
  downReed.update();

  // Get the updated value :
  int upButtonValue = upButton.read();
  int downButtonValue = downButton.read();
    
  if (upButtonValue == LOW && resetUpButton) {
    Serial.println("Up Pressed");
    upAsked = true;
    resetUpButton = false;
  }  

  if (upButton.rose()) {
    Serial.println("Up not pressed anymore!! Stop rollershutter");
    stopAsked = true;
    resetUpButton = true;    
  }

  if (downButtonValue == LOW && resetDownButton) {
    Serial.println("Down Pressed");
    downAsked = true;
    resetDownButton = false;
  }  

  if (downButton.rose()) {
    Serial.println("Down not pressed anymore!! Stop rollershutter");
    stopAsked = true;
    resetDownButton = true;
  }

  if (upReed.fell()) {
    //Here rollershutter is up (reed switch connected
    Serial.println("Volet roulant en haut");        
    stopAsked = true;    
    isOpened = true;
    manageUpDownLed();
  }

  if (upReed.rose()) {    
    isOpened = false;
    manageUpDownLed();
  }

  //ReedSwitch connected, stop rollershutter after 3s so rollershutter is well closed
  if (downReed.fell()) {
    //Here rollershutter is up (reed switch connected)
    Serial.println("Volet roulant en bas");    
 
    closeTimerId = closeTimer.setTimeout(completeDownMS, askToStopRelay);
  }

  //ReedSwitch not connected anymore
  if (downReed.rose()) {  

    if (closeTimerId != -1)
      closeTimer.deleteTimer(closeTimerId);
      
    isClosed = false;
    manageUpDownLed();
  }
}

void askToStopRelay() {
  Serial.println("Time up ! Stop relay asked now...");  
  stopAsked = true;    
  isClosed = true;
  manageUpDownLed();
}

void stopRelay() {

  if (timerId != -1)
      timer.deleteTimer(timerId);
  
  Serial.println("Ok relays arrêtés!!");
  digitalWrite(RELAY_UP, HIGH); 
  digitalWrite(RELAY_DOWN, HIGH); 
}

void activateRelay(bool up) {

  if (timerId != -1)
    timer.deleteTimer(timerId);

   //Shutdown relay in case reed switch are not working anymore...
  timerId = timer.setTimeout(upDownTimeMs, stopRelayGuard);
   
  if (up) {
    //Begin by turn off down relay (avoid counter circuit...)
    digitalWrite(RELAY_DOWN, HIGH); 
    delay(500);
    //Turn up relay on
    digitalWrite(RELAY_UP, LOW);     
    Serial.println("Allez monte!");
  }
  else {
    //Begin by turn off down relay
    digitalWrite(RELAY_UP, HIGH); 
    delay(500);
    //Turn up relay on
    digitalWrite(RELAY_DOWN, LOW); 
    Serial.println("Allez descend!");
  }  
}

void stopRelayGuard() {
  Serial.println("Reed switch not working??? ... Stopping relays ...");
  stopAsked=true;
}

void handleRollershutter() {
  
  if (upAsked && !isOpened) {       

    //Rollershutter is closing, but user asked to open it meanwhile...
    if (closeTimerId != -1)
      closeTimer.deleteTimer(closeTimerId);
    
    upAsked = false;
    activateRelay(true);      
  }
  else {
    //Reset upAsked in case up is asked and roller shutter is already opened
    upAsked = false;
  }

  if (downAsked && !isClosed) {       
    downAsked = false;    
    activateRelay(false);
  } 
  else {
    downAsked = false;
  }

  if (stopAsked) {
    stopAsked = false;    
    stopRelay();
  }
}

//In case wifi connexion is lost
//and rs is already opened or closed 
//set state correctly
void initUpDownReedSwitch() {
  if (upReed.read() == LOW)
    isOpened=true;

  if (downReed.read() == LOW)
    isClosed=true;
      
  manageUpDownLed();
}

String getValue(String data, char separator, int index)
{
    int maxIndex = data.length() - 1;
    int j = 0;
    String chunkVal = "";

    for (int i = 0; i <= maxIndex && j <= index; i++)
    {
        chunkVal.concat(data[i]);

        if (data[i] == separator)
        {
            j++;

            if (j > index)
            {
                chunkVal.trim();
                return chunkVal;
            }

            chunkVal = "";
        }
        else if ((i == maxIndex) && (j < index)) {
            chunkVal = "";
            return chunkVal;
        }
    }   
}

void mqttCallback(char* topic, byte* payload, unsigned int length) {

  char message_buff[100];

  Serial.println("Message recu =>  topic: " + String(topic));

  int i = 0;
  for(i=0; i<length; i++) {
    message_buff[i] = payload[i];
  }
  message_buff[i] = '\0';
  
  String receivedPayload = String(message_buff);

  Serial.println("Payload: " + receivedPayload);

  String action = getValue(receivedPayload, ';', 0);
  String id = getValue(receivedPayload, ';', 1);
  String request = getValue(receivedPayload, ';', 2);

  if (id == SENSORID) {
    if (action == "SETACTION")  {
      if (request == "UP") {
        upAsked = true;
      }

      if (request == "DOWN") {
        downAsked = true;
      }

      if (request == "STOP") {
        stopAsked = true;
      }

      if (request == "STATE") {
        sendState();
      }
    }    
  }
}

void connectToMqtt() {
  
  client.setServer(mqtt_server, 1883); 
  client.setCallback(mqttCallback);

  int retry = 0;
  Serial.print("Attempting MQTT connection...");
  while (!client.connected()) {   
    if (client.connect(MQTT_CLIENT_ID.c_str())) {
      Serial.println("connected to MQTT Broker...");
    }
    else {
      delay(500);
      Serial.print(".");
    }
  }

  if (retry >= MAX_RETRY) {
    ESP.restart();
  } else {
    client.subscribe(sub_topic1);
    client.subscribe(sub_topic2);
  }
}

void connectToWifi() 
{
  byte mac[6];

  WiFi.disconnect();
  Serial.println("Connecting to WiFi...");
  WiFi.config(ip_wemos, gateway_ip, subnet_mask);
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  int retry = 0;
  while (WiFi.status() != WL_CONNECTED && retry < MAX_RETRY) {
    delay(500);
    Serial.print(".");
    retry++;    
  }

  if (retry >= MAX_RETRY)
    ESP.restart();
  
  Serial.println ( "" );
  Serial.print ( "Connected to " );
  Serial.println ( ssid );
  Serial.print ( "IP address: " );
  Serial.println ( WiFi.localIP() );
  
  WiFi.macAddress(mac);
  Serial.print("MAC: ");
  Serial.print(mac[5],HEX);
  Serial.print(":");
  Serial.print(mac[4],HEX);
  Serial.print(":");
  Serial.print(mac[3],HEX);
  Serial.print(":");
  Serial.print(mac[2],HEX);
  Serial.print(":");
  Serial.print(mac[1],HEX);
  Serial.print(":");
  Serial.println(mac[0],HEX);
}
