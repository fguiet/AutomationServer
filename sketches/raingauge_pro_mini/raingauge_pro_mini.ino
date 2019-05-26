#include <DHT.h>
#include <DHT_U.h>

//using SparkFun Pro Micro board (3.3v, 8Mhz)
//https://github.com/sparkfun/Arduino_Boards

//The Pro Micro has five external interrupts, which allow you to instantly trigger a function when a pin goes either high or low (or both). 
//If you attach an interrupt to an interrupt-enabled pin, 
//you'll need to know the specific interrupt that pin triggers: pin 3 maps to interrupt 0 (INT0), pin 2 is interrupt 1 (INT1), pin 0 is interrupt 2 (INT2), pin 1 is interrupt 3 (INT3), and pin 7 is interrupt 4 (INT6).

//CAREFUL :
//To upload a sketch, don't forget to set pin 6 to LOW (GROUND), otherwise board will hibernate and no upload is possible

//Consumption with power led on :
//1.6mA while sleeping
//25mA peak while running

#include <LowPower.h>
#include <ArduinoJson.h>

#define DEBUG 0
#define FIRMWARE_VERSION "1.0"

const int UPLOAD_PIN = 6;
const int M0_PIN = 4;
const int M1_PIN = 5;
const int REED_SWITCH_INTERRUPT = 3; //interrupt 0 at arduino nano pin 3
const int REED_SWITCH_PIN = 3; 
const int LED_PIN = 17;            // RXLED Pin is 17 on pro micro
const int SENSOR_PIN = A1;

#define DHTPIN 7
#define DHTTYPE DHT22 // DHT 22 (AM2302)
DHT dht(DHTPIN, DHTTYPE);

struct Sensor {
    String Name;    
    String SensorId;
};

#define SENSORS_COUNT 1
Sensor sensors[SENSORS_COUNT];

volatile bool wakeUpByFlipFlop = false;
long debouncing_time = 150; //Debouncing Time in Milliseconds
volatile unsigned long last_micros;

void setup() {
  pinMode(LED_PIN, OUTPUT);    // initialize pin 17 as an output pin for LED 
  digitalWrite(LED_PIN, HIGH); //necessary ? maybe not but just in case
  
  pinMode(M0_PIN, OUTPUT);    
  pinMode(M1_PIN, OUTPUT);   

  pinMode(SENSOR_PIN, INPUT);

  pinMode(UPLOAD_PIN, INPUT_PULLUP);
  digitalWrite(UPLOAD_PIN, HIGH); //necessary ? maybe not but just in case

  digitalWrite(M0_PIN, HIGH); //
  digitalWrite(M1_PIN, HIGH); //High = Sleep mode
  
  pinMode(REED_SWITCH_PIN, INPUT_PULLUP); // define interrupt pin 3 as input to read interrupt received by Reed Switch
  digitalWrite(REED_SWITCH_PIN, HIGH); //necessary ? maybe not but just in case
  
  //Serial.println("Attaching interrupt...");
  attachInterrupt(digitalPinToInterrupt(REED_SWITCH_INTERRUPT),wakeUpNow, FALLING);   // Attach interrupt at pin D2  (int 0 is at pin D2  for nano, UNO)

  if (DEBUG)
    Serial.begin(9600);     // initialize serial communication only for debugging purpose   
    
  Serial1.begin(9600);
  
  blinkLed(5);  

  InitSensors();
  
  debug_message("Setup completed, starting...",true);
}

void InitSensors() {
  
  sensors[0].Name = "Raingauge";
  sensors[0].SensorId = "1";
}

void loop() {  

  //Hibernate if Upload pin is high
  if (digitalRead(UPLOAD_PIN) == HIGH)
    Hibernate();   // go to sleep - calling sleeping function  
}

String ConvertToJSon(String battery, String humidity, String temperature, String flipflop) {
    //Create JSon object
    DynamicJsonBuffer  jsonBuffer(200);
    JsonObject& root = jsonBuffer.createObject();
    
    root["sensorid"] = sensors[0].SensorId;
    root["name"] = sensors[0].Name;
    root["firmware"]  = FIRMWARE_VERSION;
    root["battery"] = battery;
    root["humidity"] = humidity;
    root["temperature"] = temperature;
    root["flipflop"] = flipflop;
   
    String result;
    root.printTo(result);

    return result;
}

void sendMessage(bool bWakeUpByFlipFlop) {
  
  blinkLed(3);
 
  digitalWrite(M0_PIN, LOW); //
  digitalWrite(M1_PIN, LOW); //low, low = normal mode
  
  delay(100);  

  float h = dht.readHumidity();//on lit l'hygrometrie
  float t = dht.readTemperature();

  String temp;
  String humi;
  if (isnan(h) || isnan(t))
  {
    temp = "NA";
    humi = "NA";
  }
  else {
    temp = String(t,2);
    humi = String(h,2);
  }

  //Serial.println("Flip flop count : " + String(bucketFlipFlopCounter));   

  float vin = ReadVoltage();
  //String message = "SETRAINGAUGEINFOTEST;" + String(vin,2) + ";";

  String flipflop = "0";
  if (bWakeUpByFlipFlop) 
    flipflop = "1";

  String message = ConvertToJSon(String(vin,2), humi, temp, flipflop);
  
  Serial1.print(message);
  
  Serial1.end();
  delay(30);
  Serial1.begin(9600);
  delay(70); //The rest of requested delay. So 100 - 30 = 70

  //LoRa in Sleep mode
  digitalWrite(M0_PIN, HIGH); //
  digitalWrite(M1_PIN, HIGH); //high, high sleep mode
}

void blinkLed(int blinkNumber){
  for (int i=1; i <= blinkNumber;i++) {
    digitalWrite(LED_PIN, LOW); 
    delay(250);         
    digitalWrite(LED_PIN, HIGH); 
    delay(250); 
  }
}

void wakeUpNow(){                  // Interrupt service routine or ISR  

  if((long)(micros() - last_micros) >= debouncing_time * 1000) {
    wakeUpByFlipFlop = true;
    last_micros = micros();
  }  
}

float ReadVoltage() {

  //R1 = 33kOhm
  //R2 = 7.5kOhm

  float sensorValue = 0.0f;
  float R1 = 32800;
  float R2 =  7460;
  float vmes = 0.0f;
  float vin = 0.f;
    
  sensorValue = analogRead(SENSOR_PIN);

  //3.3v is a little lower than expected...
  vmes = (sensorValue * 3.4) / 1024;

  //Calcul issue du pont diviseur
  vin = vmes / (R2 / (R1 + R2));

  return vin;  
}

void Hibernate()         // here arduino is put to sleep/hibernation
{

 //60*60 / 8 = 450 = publication toutes les heures!
 for (int i=1; i<=450;i++) {
  
    LowPower.powerDown(SLEEP_8S, ADC_OFF, BOD_OFF);
    
    if (wakeUpByFlipFlop) {       
       sendMessage(true);
       wakeUpByFlipFlop = false;
    }
 }

 //One hour has passed send only Voltage info
 sendMessage(false);
}

void debug_message(String message, bool doReturnLine) {
  if (DEBUG) {
    if (doReturnLine)
      Serial.println(message);
    else
      Serial.print(message);
  }
}
