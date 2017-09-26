/****
 * Rollershutter manager
 * 
 * F. Guiet 
 * Creation           : 20170910
 * Last modification  : 20170924
 * 
 * Version            : 1.1
 * 
 * History            : 1.0 - Initial version
 *                      1.1 - Set Wifi to STA mode only (no AP)
 * 
 * 
  ****/

//Light Mqtt library
#include <PubSubClient.h>

#include <ESP8266WiFi.h>
#include <SimpleTimer.h>


/*
 * Wemos D1 Datasheet : https://wiki.wemos.cc/products:d1:d1_mini
 */

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

//IPAddress ip_wemos(192, 168, 1, 40);
//String ROLLERSHUTTERID= "1";   //west
IPAddress ip_wemos(192, 168, 1, 41);
String ROLLERSHUTTERID= "2"; //north
const char* ssid = "DUMBLEDORE";
const char* password = "***";
String apikey="***";
//Mqtt settings
#define mqtt_server "192.168.1.25"
//#define mqtt_user ""
//#define mqtt_password ""

IPAddress gateway_ip(192, 168, 1, 1); // set gateway to match your network
IPAddress subnet_mask(255, 255, 255,   0);

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

  Serial.println("Ready...");
  
}

void manageUpDownLed() {

  if (isOpened) {
    //rollshutter is opened
    digitalWrite(UP_LED, HIGH);     
    //Turn off internal led 
    digitalWrite(BUILTIN_LED, HIGH);

    //Send state here to automation server via mqtt
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

  if (!isOpened && !isClosed) {
    //Send state here to automation server via mqtt
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

/*void handleWebRequest() {

  int returnCode = 400; //bad request
  String jsonKO = "{\"rollershutterid\":\""+ROLLERSHUTTERID + "\",\"success\":false}";
  String jsonOk = "{\"rollershutterid\":\""+ROLLERSHUTTERID + "\",\"success\":true}";
  String json = jsonKO;

  if (server.arg("apikey") == "" || server.arg("apikey") != apikey) {
    returnCode = 403;    
    server.send(returnCode, "text/plain", "");
    return;
  }
  
  if (server.arg("action")== "up") {
     upAsked = true;  
     returnCode = 200;
     json = jsonOk;          
  }  

  if (server.arg("action")== "down") {
    downAsked = true;
    returnCode = 200;
    json = jsonOk;    
  }

  if (server.arg("action")== "stop") {
    stopAsked = true;
    returnCode = 200;
    json = jsonOk;
  }

  if (server.arg("action")== "state") {
    if (isClosed) {
      returnCode = 200;
      json = "{\"rollershutterid\":\""+ROLLERSHUTTERID + "\", \"state\":\"closed\"}";
    }

    if (isOpened) {
      returnCode = 200;
      json = "{\"rollershutterid\":\""+ROLLERSHUTTERID + "\", \"state\":\"opened\"}";      
    }

    if (!isClosed && !isOpened) {
      returnCode = 200;
      json = "{\"rollershutterid\":\""+ROLLERSHUTTERID + "\", \"state\":\"undetermined\"}";       
    }
  }

  server.send(returnCode, "text/json", json);
}*/

/*void initializeWebServer() {
  // Start the server
  
  server.on("/api/rollershutter", handleWebRequest);  

  server.begin();

  Serial.println("Server started");
 
  // Print the IP address
  Serial.print("Use this URL : ");
  Serial.print("http://");
  Serial.print(WiFi.localIP());
  Serial.println("/");
}*/

void mqttCallback(char* topic, byte* payload, unsigned int length) {

  /*int i = 0;
  if ( debug ) {
    Serial.println("Message recu =>  topic: " + String(topic));
    Serial.print(" | longueur: " + String(length,DEC));
  }
  // create character buffer with ending null terminator (string)
  for(i=0; i<length; i++) {
    message_buff[i] = payload[i];
  }
  message_buff[i] = '\0';
  
  String msgString = String(message_buff);
  if ( debug ) {
    Serial.println("Payload: " + msgString);
  }
  
  if ( msgString == "ON" ) {
    digitalWrite(D2,HIGH);  
  } else {
    digitalWrite(D2,LOW);  
  }*/
}


void connectToMqtt() {

  client.setServer(mqtt_server, 1883); 
  client.setCallback(mqttCallback);

  Serial.print("Attempting MQTT connection...");
  while (!client.connected()) {
        
    if (client.connect("ESP8266_RollershutterID"+ROLLERSHUTTERID)) {
      Serial.println("connected to MQTT Broker...");
    }

    delay(500);
    Serial.print(".");
  }
}

void connectToWifi() 
{
  WiFi.disconnect();
  Serial.println("Connecting to WiFi...");
  WiFi.config(ip_wemos, gateway_ip, subnet_mask);
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);
  
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
    //handleButtons();
  }

  Serial.println ( "" );
  Serial.print ( "Connected to " );
  Serial.println ( ssid );
  Serial.print ( "IP address: " );
  Serial.println ( WiFi.localIP() );
}
