#include <LowPower.h>

//#include <avr/interrupt.h>        // Library to use interrupt
//#include <avr/sleep.h>            // Library for putting our arduino into sleep modes

const int M0_PIN = 3; //D3
const int M1_PIN = 4; //D4
const int REED_SWITCH_INTERRUPT = 0; //interrupt 0 at arduino nano pin D2
const int REED_SWITCH_PIN = 2; //pin 32 at arduino nano pin D2
const int LED_PIN = 13;            // external LED or relay connected to pin 13
const int SENSOR_PIN = A0;

//volatile int bucketFlipFlopCounter = 0;  // Bucket flip flop counter
volatile bool wakeUpByFliFlop = false;

const unsigned long debouncingTime = 200; //200ms

void setup() {
  pinMode(LED_PIN, OUTPUT);    // initialize pin 13 as an output pin for LED or relay etc.
  pinMode(M0_PIN, OUTPUT);    
  pinMode(M1_PIN, OUTPUT);    

  digitalWrite(M0_PIN, LOW); //
  digitalWrite(M1_PIN, LOW); //low, low = normal mode
  
  pinMode(REED_SWITCH_PIN, INPUT_PULLUP);        // define interrupt pin D2 as input to read interrupt received by Reed Switch
  digitalWrite(REED_SWITCH_PIN, HIGH); //necessary ? maybe not but just in case

  attachInterrupt(REED_SWITCH_INTERRUPT,wakeUpNow, FALLING);   // Attach interrupt at pin D2  (int 0 is at pin D2  for nano, UNO)

  Serial.begin(9600);     // initialize serial communication only for debugging purpose   
}

void loop() {
  Hibernate();   // go to sleep - calling sleeping function
}

void sendMessage(bool bWakeUpByFlipFlop) {
  
  blinkLed();
 
  digitalWrite(M0_PIN, LOW); //
  digitalWrite(M1_PIN, LOW); //low, low = normal mode
  
  delay(100);  
  
  //Serial.println("Flip flop count : " + String(bucketFlipFlopCounter));   

  float vin = ReadVoltage();
  String message = "SETRAINGAUGEINFO;" + String(vin,2) + ";";
  
  if (bWakeUpByFlipFlop) {
    //wakeUpByFliFlop = false;
    message = message + "1";
  }
  else {
    message = message + "0";
  }
  
  Serial.print(message);
  
  Serial.end();
  delay(30);
  Serial.begin(9600);
  delay(70); //The rest of requested delay. So 100 - 30 = 70
}

void blinkLed(){
  for (int i=1; i <= 3;i++) {
    digitalWrite(LED_PIN, HIGH); 
    delay(100);         
    digitalWrite(LED_PIN, LOW); 
    delay(100); 
  }
}

void wakeUpNow(){                  // Interrupt service routine or ISR  

  static unsigned long lastFlipFlop= 0;
  
  unsigned long date = millis();
  if ((date - lastFlipFlop) > debouncingTime) {    
    wakeUpByFliFlop = true;
    lastFlipFlop = date;
  }  
}

float ReadVoltage() {

  float sensorValue = 0.0f;
  float R1 = 32450.0;
  float R2 = 7560.0;
  float vin = 0.0;
  float vout = 0.0;
 
  sensorValue = analogRead(SENSOR_PIN);
  vout = (sensorValue * 3.3) / 1024.0;
  vin = vout / (R2 / (R1 + R2));

  return vin;
}

void Hibernate()         // here arduino is put to sleep/hibernation
{

 //LoRa in Sleep mode
 digitalWrite(M0_PIN, HIGH); //
 digitalWrite(M1_PIN, HIGH); //high, high sleep mode

 //60*60 / 8 = 450 = publication toutes les heures!
 for (int i=1; i<=450;i++) {
  
    LowPower.powerDown(SLEEP_8S, ADC_OFF, BOD_OFF);
    
    if (wakeUpByFliFlop) {
       wakeUpByFliFlop = false;
       sendMessage(true);
    }
 }

 //One hour has passed send only Voltage info
 sendMessage(false);
 
 //LowPower.powerDown(SLEEP_FOREVER, ADC_OFF, BOD_OFF); 

 //detachInterrupt(REED_SWITCH_INTERRUPT);   // we detach interrupt from pin D2, to avoid further interrupts until our ISR is finished

 //delay(50);
 
 //set_sleep_mode(SLEEP_MODE_PWR_DOWN);  // lowest power consumption mode 
 //"The Power-down mode saves the register contents but freezes the Oscillator, disabling all other chip functions 
 // until the next interrupt or hardware reset."  text from ATMEGA328P datasheet

 //ADCSRA &= ~(1 << 7);   // Disable ADC - don't forget to flip back after waking up if you need ADC in your application ADCSRA |= (1 << 7);  (From Kevin's sketch)
    
 //sleep_enable();                       // enable the sleep mode function
 //sleep_bod_disable();                  //to disable the Brown Out Detector (BOD) before going to sleep. 

 //attachInterrupt(REED_SWITCH_INTERRUPT,wakeUpNow, FALLING);   // Attach interrupt at pin D2  (int 0 is at pin D2  for nano, UNO)
  // here since PIR sensor has inbuilt timer to swtich its state from OFF to ON, we are detecting its CHANGE IN STATE to control our LED/relay at pin 13. 
   // therefore, we will not need to use arduino delay timer to Set "ON time" of our LED/relay, it can be adjusted physically using potentiometer provided on PIR sensor board.
   // This further helps in using SLEEP_MODE_PWR_DOWN which is ultimate lowest power consumption mode for ATMEGA8328P chip  
   //(please note - because of onboard power regulators of arduino boards, power consumption cannot be reduced to predicted few microAmps level of bare chips. 
   //To achieve further reduction in current consumption, we will need bare ATMEGA328P chip)
 
  //for (int i = 0; i < 20; i++) {
  //  if(i != 13 && i!=M0_PIN && i!=M1_PIN)//  because the LED/Relay is connected to digital pin 13
  //    pinMode(i, INPUT);
  //}
 
 //sleep_mode();                // calls function to put arduino in sleep mode
 
 //sleep_disable();            // when interrupt is received, sleep mode is disabled and program execution resumes from here
 
 //detachInterrupt(REED_SWITCH_INTERRUPT);   // we detach interrupt from pin D2, to avoid further interrupts until our ISR is finished
}
