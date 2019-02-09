/*
 * Replace AMS117 5v LDO Regulator (http://www.advanced-monolithic.com/pdf/ds1117.pdf)
 * by HT7333 (http://www.angeladvance.com/HT73xx.pdf) so Arduino Nano can run on 3.3v
 * 
 * Nano CH340 Schema
 * http://actrl.cz/blog/wp-content/uploads/nano_ch340_schematics-rev1.pdf
 * 
 * See this post https://forum.arduino.cc/index.php?topic=409415.0
 *               http://forum.arduino.cc/index.php?topic=508722.0
 *               https://www.youtube.com/watch?v=m_0U4DIGsgI
 *               https://i2.wp.com/www.ba0sh1.com/wp-content/uploads/2013/03/ArduinoNano-3.3.png?ssl=1
 *               https://www.ba0sh1.com/blog/2013/03/30/tutorial-3-3v-hacking-for-arduino-nano/
 *               
 * Clock seems to work well at 16Mhz with 3.3v so far...
 * 
 * I only replaced the AMS117 by HT7333..didn't do anything with the CH340 UART converter (don't care if the nano runs at 5v when I am programming it via USB)
 * 
 * Consumption (with voltage divider) : 2.78mA (around 10-13 mA when awake)
 * 
 * if battery is 500mA then device should run 125hours (5 days) with no sun at all
 * 
 * https://www.digikey.com/en/resources/conversion-calculators/conversion-calculator-battery-life
 * 
 */

#include <LowPower.h>

//#include <avr/interrupt.h>        // Library to use interrupt
//#include <avr/sleep.h>            // Library for putting our arduino into sleep modes

const int M0_PIN = 10; //D11
const int M1_PIN = 11; //D12
const int REED_SWITCH_INTERRUPT = 0; //interrupt 0 at arduino nano pin D2
const int REED_SWITCH_PIN = 2; //pin 32 at arduino nano pin D2
const int LED_PIN = 13;            // external LED or relay connected to pin 13
const int SENSOR_PIN = A1;

volatile bool wakeUpByFlipFlop = false;
long debouncing_time = 150; //Debouncing Time in Milliseconds
volatile unsigned long last_micros;

void setup() {
  pinMode(LED_PIN, OUTPUT);    // initialize pin 13 as an output pin for LED or relay etc.
  digitalWrite(LED_PIN, LOW); //necessary ? maybe not but just in case
  
  pinMode(M0_PIN, OUTPUT);    
  pinMode(M1_PIN, OUTPUT);   

  pinMode(SENSOR_PIN, INPUT);

  digitalWrite(M0_PIN, HIGH); //
  digitalWrite(M1_PIN, HIGH); //High = Sleep mode
  
  pinMode(REED_SWITCH_PIN, INPUT_PULLUP);        // define interrupt pin D2 as input to read interrupt received by Reed Switch
  digitalWrite(REED_SWITCH_PIN, HIGH); //necessary ? maybe not but just in case
  
  //Serial.println("Attaching interrupt...");
  attachInterrupt(REED_SWITCH_INTERRUPT,wakeUpNow, FALLING);   // Attach interrupt at pin D2  (int 0 is at pin D2  for nano, UNO)

  Serial.begin(9600);     // initialize serial communication only for debugging purpose   

  blinkLedStartup();  
}

void loop() {
  //ReadVoltage();  
  //delay(1000);
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

  //LoRa in Sleep mode
  digitalWrite(M0_PIN, HIGH); //
  digitalWrite(M1_PIN, HIGH); //high, high sleep mode
}

void blinkLedStartup(){
  for (int i=1; i <= 5;i++) {
    digitalWrite(LED_PIN, HIGH); 
    delay(500);         
    digitalWrite(LED_PIN, LOW); 
    delay(500); 
  }
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

 //detachInterrupt(REED_SWITCH_INTERRUPT);   // we detach interrupt from pin D2, to avoid further interrupts until our ISR is finished
 
 //One hour has passed send only Voltage info
 sendMessage(false);
 
 //LowPower.powerDown(SLEEP_FOREVER, ADC_OFF, BOD_OFF); 

 

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
