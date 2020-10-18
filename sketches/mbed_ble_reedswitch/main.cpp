/*
*
* Description  : This program advertise some data through GAP Advertising Data
 *               It does not use Standard BLE Services (EnvironmentalService, BatteryService).
*                So BLE Client are not forced to connect to this BLE Beacon in order to discover services and characteritics associated.                
*
*               Careful, before compiling this program, make sure you have altered function addData of GapAvertisingData.h file as follow :
*                
*                ble_error_t addData(DataType_t advDataType, const uint8_t *payload, uint8_t len)
*                {
*                    //Find field 
*                    //uint8_t* field = findField(advDataType);
*            
*                    //if (field) {
*                        // Field type already exist, either add to field or replace 
*                    //    return addField(advDataType, payload, len, field);
*                    //} else {
*                        // Field doesn't exists, insert new 
*                        return appendField(advDataType, payload, len);
*                    //}
*                }
*                                
*               For more info : 
*                    - https://os.mbed.com/questions/83683/How-can-add-multiple-SERVICE_DATA/
*                    - https://os.mbed.com/questions/83675/BLE-advertising-data/
*
* Library Requirements : BLE_API, nRF51822, mbed
* Author               : F.Guiet
* Creation             : 2019032019
*
* Modification         : 
*
*/
//Includes
#include "mbed.h"
#include "ble/BLE.h"

#define DEBUG_MODE 0 //enables serial output for debug, consumes ~1mA when idle

#if DEBUG_MODE
   // if you see ~1mA consumption during sleep, that's because DEBUG_MODE==1, it's enabled.
   // TX, RX
   Serial _device(p24, p25);  //Check to see which pin should be used
#endif

#define BATTERY_UUID_SERVICE 0x180F
//0x181C = User Data = Reed switch state in our case
#define USER_DATA_UUID_SERVICE 0x181C

uint8_t _reedswitch_state=0;
float _battery = 0;

//mac address for this module
uint8_t mac_reverse[6] = {0x0,0x0,0x0,0x0,0x0,0x0};  

//device name                                             
const static char     DEVICE_NAME[]        = "Fred";

//to trigger sensor polling in main thread
static volatile bool _triggerSensorPolling = false;

//transmit sensor data on a periodic basis
static Ticker _pollDataTimer;
//debounce I/O
static Ticker _debounceTimer;

//poll data interval
const uint16_t POLL_DATA_INTERVAL_IN_S = 60; //In seconds

//advertise interval in ms
//less advertising = less baterry consumption
const uint16_t ADVERTISE_INTERVAL_IN_MS = 5000; 

InterruptIn button1(p0);    //nRF51822 P0.0
InterruptIn button2(p1);    //nRF51822 P0.1

/* ****************************************
 * Read battery voltage using bandgap reference
 * shunt Vdd to ADC, thanks to Marcelo Salazar's notes here:
 * https://developer.mbed.org/users/MarceloSalazar/notebook/measuring-battery-voltage-with-nordic-nrf51x/
*******************************************/
uint16_t read_bat_volt(void)
{
    //10 bit resolution, route Vdd as analog input, set ADC ref to VBG band gap
    //disable analog pin select "PSEL" because we're using Vdd as analog input
    //no external voltage reference
    NRF_ADC->CONFIG = (ADC_CONFIG_RES_10bit << ADC_CONFIG_RES_Pos) |
                      (ADC_CONFIG_INPSEL_SupplyOneThirdPrescaling << ADC_CONFIG_INPSEL_Pos) |
                      //(ADC_CONFIG_INPSEL_AnalogInputOneThirdPrescaling << ADC_CONFIG_INPSEL_Pos) |
                      (ADC_CONFIG_REFSEL_VBG << ADC_CONFIG_REFSEL_Pos) |
                      (ADC_CONFIG_PSEL_Disabled << ADC_CONFIG_PSEL_Pos) |
                      //(ADC_CONFIG_PSEL_AnalogInput4 << ADC_CONFIG_PSEL_Pos) |
                      (ADC_CONFIG_EXTREFSEL_None << ADC_CONFIG_EXTREFSEL_Pos);

    //NRF_ADC->CONFIG     &= ~ADC_CONFIG_PSEL_Msk;
    //NRF_ADC->CONFIG     |= ADC_CONFIG_PSEL_Disabled << ADC_CONFIG_PSEL_Pos;
    NRF_ADC->ENABLE = ADC_ENABLE_ENABLE_Enabled;
    NRF_ADC->TASKS_START = 1;
    
    
    //while loop doesn't actually loop until reading comlete, use a wait.
    while (((NRF_ADC->BUSY & ADC_BUSY_BUSY_Msk) >> ADC_BUSY_BUSY_Pos) == ADC_BUSY_BUSY_Busy) {};
    wait_ms(1);

    //save off RESULT before disabling.
    //uint16_t myresult = (uint16_t)NRF_ADC->RESULT;
    
    //disable ADC to lower bat consumption
    NRF_ADC->TASKS_STOP = 1;
    NRF_ADC->ENABLE = ADC_ENABLE_ENABLE_Disabled;    //disable to shutdown ADC & lower bat consumption
    
    return (uint16_t)NRF_ADC->RESULT; // 10 bit
    //return myresult;
}  //end read_bat_volt

void generateAdvertisingData(BLE &ble) {
    
    /* Setup advertising. */
    
    ble.gap().clearAdvertisingPayload();
    
    /* set modes "no EDR", "discoverable" for beacon type advertisements */
    ble.gap().accumulateAdvertisingPayload(GapAdvertisingData::BREDR_NOT_SUPPORTED | GapAdvertisingData::LE_GENERAL_DISCOVERABLE);
    
    /* List of services */
    ///ble.gap().accumulateAdvertisingPayload(GapAdvertisingData::COMPLETE_LIST_16BIT_SERVICE_IDS, (uint8_t *)uuid16_list, sizeof(uuid16_list));

    //Battery
    uint8_t service_data[4];
    service_data[0] = BATTERY_UUID_SERVICE & 0xff;
    service_data[1] = BATTERY_UUID_SERVICE >> 8;
    
    int temp = _battery * 100;   
    char hex[4]; 
    sprintf(hex, "%x", temp);
    service_data[2] = temp & 0xff;
    service_data[3] = temp >> 8;
            
    ble.gap().accumulateAdvertisingPayload(GapAdvertisingData::SERVICE_DATA,  (uint8_t *)service_data, sizeof(service_data));
    
    //Reedswitch state
    uint8_t service_data1[3];
    service_data1[0] = USER_DATA_UUID_SERVICE & 0xff;
    service_data1[1] = USER_DATA_UUID_SERVICE >> 8;
    service_data1[2] = _reedswitch_state;    
    ble.gap().accumulateAdvertisingPayload(GapAdvertisingData::SERVICE_DATA,  (uint8_t *)service_data1, sizeof(service_data1));
    
    /* Appearance */
    ble.gap().accumulateAdvertisingPayload(GapAdvertisingData::GENERIC_TAG);
    
    /* Set local Name */
    ble.gap().accumulateAdvertisingPayload(GapAdvertisingData::COMPLETE_LOCAL_NAME, (uint8_t *)DEVICE_NAME, sizeof(DEVICE_NAME));
            
    /* Setup advertising parameters:  not connectable */
    ble.gap().setAdvertisingType(GapAdvertisingParams::ADV_NON_CONNECTABLE_UNDIRECTED);
    ble.gap().setAdvertisingInterval(ADVERTISE_INTERVAL_IN_MS); 
}
float get_battery_level() {
            
    float bat_reading = (float)read_bat_volt();    
    bat_reading = (bat_reading * 3.6) / 1024.0;
    
    #if DEBUG_MODE
       _device.printf("battery reading: %f \r\n", bat_reading);
    #endif    
    
    return bat_reading;
    
    //write battery voltage
    //uint8_t total_chars;
    //memset(&bat_volt_char[0], 0, sizeof(bat_volt_char));      //clear out buffer
    //convert battery voltage float value to string reprsentation to 2 decimal places, and save the size of string.
    //total_chars = sprintf (bat_volt_char, "%.2f", bat_reading);
}

void bleInitComplete(BLE::InitializationCompleteCallbackContext *params)
{
    BLE &ble          = params->ble;
    ble_error_t error = params->error;

    if (error != BLE_ERROR_NONE) {
        return;
    }

    //Re advertising again when client is disconnecting
    //Useful when client is connecting to this Beacon, in this case...not useful nobody's connecting...
    //ble.gap().onDisconnection(disconnectionCallback);
        
    generateAdvertisingData(ble);   
    
    /* Ok Advertise now !! */
    ble.gap().startAdvertising();     
}

void pollDataCallback(void)
{
    /* Note that the pollDataCallBack() executes in interrupt context, so it is safer to do
     * heavy-weight sensor polling from the main thread. */
    _triggerSensorPolling = true;
}

void debounceCallback(void)
{
    _debounceTimer.detach();
    _triggerSensorPolling = true;  //start advertising
    /* Note that the buttonPressedCallback() executes in interrupt context, so it is safer to access
     * BLE device API from the main thread. */
}
 
//ISR for I/O interrupt
void buttonPressedCallback(void)
{
    _debounceTimer.attach(debounceCallback, 1); //ok to attach multiple times, recent one wins    
}
 
//ISR for I/O interrupt
void buttonReleasedCallback(void)
{   
    _debounceTimer.attach(debounceCallback, 1);  
}

/* ****************************************
 * 
 * Main Loop
 * 
*******************************************/
int main(void)
{
    #if DEBUG_MODE
        _device.baud(9600);
        _device.printf("**************************");
        _device.printf("Initialising BLE Sensor...");
        _device.printf("\r\n");
    #endif
   
    //Init new BLE Instance 
    BLE& ble = BLE::Instance(BLE::DEFAULT_INSTANCE);
    ble.init(bleInitComplete);
   
    /* SpinWait for initialization to complete. This is necessary because the
    * BLE object is used in the main loop below. */
     while (ble.hasInitialized() == false) { /* spin loop */ }
   
    //Poll some data at beginning
    _triggerSensorPolling = true;
   
    #if DEBUG_MODE
        _device.printf("BLE Sensor initialized !");
        _device.printf("\r\n");
     #endif
   
    ble.gap().getAddress(0,mac_reverse);  //last byte of MAC (as shown on phone app) is at mac[0], not mac[6];
    #if DEBUG_MODE
        _device.printf("MAC Address = ");
        for (int i=0; i<6; i++) { //prints out MAC address in reverse order; opps.
             _device.printf("%x:", mac_reverse[i]);
        }
        _device.printf("\r\n");
    #endif

    // Main infinite loop
    while (true) {
        
        //set both pins to pull-up, so they're not floating when we read state
        button1.mode(PullUp);
        button2.mode(PullUp);
        
        //expect either button1 or button2 is grounded, b/c using SPDT reed switch
        //the "common" pin on the reed switch should be on GND
        uint8_t button1_state = button1.read();
        uint8_t button2_state = button2.read();
        
        //let's just update the pins on every wake.  Insurance against const drain.
        //if state == 0, pin is grounded.  Unset interrupt and float pin, set the other pin for ISR
        if ( (button1_state == 0) && (button2_state == 1) )
        {
            _reedswitch_state = 1;
                    
            button1.fall(NULL);     //disable interrupt
            button1.rise(NULL);     //disable interrupt
            button1.mode(PullNone); //float pin to save battery
                    
            button2.fall(buttonReleasedCallback);     //enable interrupt
            button2.rise(buttonReleasedCallback);     //enable interrupt
            button2.mode(PullUp); //pull up on pin to get interrupt
            
            #if DEBUG_MODE
                _device.printf("=== button 1 activated ! \r\n");
            #endif
            
        }  //end if button2
        else if ( (button1_state == 1) && (button2_state == 0) )       //assume other pin is open circuit
        {
            _reedswitch_state = 0;
            
            button1.fall(buttonReleasedCallback);     //enable interrupt
            button1.rise(buttonReleasedCallback);     //enable interrupt
            button1.mode(PullUp); //pull up on pin to get interrupt
            
            
            button2.fall(NULL);     //disable interrupt
            button2.rise(NULL);     //disable interrupt
            button2.mode(PullNone); //float pin to save battery
            
            #if DEBUG_MODE
                _device.printf("=== button 2 activated \r\n");
            #endif
        }  //end if button1
        else    //odd state, shouldn't happen, suck battery and pullup both pins
        {
            _reedswitch_state = 2;
                        
            button1.fall(buttonReleasedCallback);     //disable interrupt
            button1.rise(buttonReleasedCallback);     //disable interrupt
            button1.mode(PullUp); //float pin to save battery
                                    
            button2.fall(buttonReleasedCallback);     //disable interrupt
            button2.rise(buttonReleasedCallback);     //disable interrupt
            button2.mode(PullUp); //float pin to save battery
            
            #if MyDebugEnb
                device.printf("no buttons!! \r\n");
            #endif
        }  //end odd state
        
        //Time to poll data from sensor
       if (_triggerSensorPolling) {
           
           //Is it useful??
           _pollDataTimer.attach(pollDataCallback, POLL_DATA_INTERVAL_IN_S);  //send updated I/O every x seconds
           
           //Get Battery Level
           _battery = get_battery_level();
           
           #if DEBUG_MODE
              _device.printf("Battery percentage : %6.2f \r\n", _battery);
           #endif
           
           #if DEBUG_MODE
              _device.printf("Reedswitch state : %i \r\n", _reedswitch_state);
           #endif
           
           //Generate new advertising data
           generateAdvertisingData(ble);
           
           /* Ok Advertise now !! */
           ble.gap().startAdvertising();                                 
                                 
           _triggerSensorPolling = false;
       }
        
        ble.waitForEvent(); //sleeps until interrupt from ticker or I/O
    }
}