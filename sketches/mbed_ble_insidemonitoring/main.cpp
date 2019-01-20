/*
*
* Description  : This program advertise some data through GAP Advertising Data
*               It does not use Standard BLE Services (EnvironmentalService, BatteryService).
*               So BLE Client are not forced to connect to this BLE Beacon in order to discover services and characteritics associated.                
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
* Library Requirements : BLE_API, HTU21D, nRF51822, mbed
* Author               : F.Guiet
* Creation             : 20181215
*
* Modification         : 20190120 - Battery info is now voltage and not percentage, disable ADC to save power
*
*/

//Includes
#include "mbed.h"
#include "ble/BLE.h"
#include "HTU21D.h"

#define THERMOMETER_UUID_SERVICE 0x1809
#define BATTERY_UUID_SERVICE 0x180F
//0x181C = User Data = Humidity Storage in our case
#define USER_DATA_UUID_SERVICE 0x181C

#define DEBUG_MODE 0 //enables serial output for debug, consumes ~1mA when idle

#if DEBUG_MODE
   // if you see ~1mA consumption during sleep, that's because DEBUG_MODE==1, it's enabled.
   Serial _device(p9, p11);  //Check to see which pin should be used
#endif

//Sensor instance
//DA = P1
//CL = P0
HTU21D _sensor(p1,p0);

//mac address for this module
uint8_t mac_reverse[6] = {0x0,0x0,0x0,0x0,0x0,0x0};  

//device name                                             
const static char     DEVICE_NAME[]        = "Fred";

//const static float    VBAT_IN_V = 3.1;

//to trigger sensor polling in main thread
static volatile bool  triggerSensorPolling = false;

//transmit sensor data on a periodic basis
static Ticker _pollDataTimer;  

//poll data interval
const uint16_t POLL_DATA_INTERVAL_IN_S = 60; //In seconds

//advertise interval in ms
//less advertising = less baterry consumption
const uint16_t ADVERTISE_INTERVAL_IN_MS = 5000; //In seconds

float _temperature = 0;
float _battery = 0;
int _humidity = 0;

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
    
    //Temperature
    uint8_t service_data[4];
    
    //UUID
    service_data[0] = THERMOMETER_UUID_SERVICE & 0xff;
    service_data[1] = THERMOMETER_UUID_SERVICE >> 8;
    
    //VALUE
    
    //Example
    //Temp = 20.39 °C
    //2039 = F7 07 
    //247 = F7
    //7 = 07
    int temp = _temperature * 100;
    char hex[4];
    sprintf(hex, "%x", temp);
    service_data[2] = temp & 0xff;
    service_data[3] = temp >> 8; 
    
    ble.gap().accumulateAdvertisingPayload(GapAdvertisingData::SERVICE_DATA,  (uint8_t *)service_data, sizeof(service_data));
    
    //Battery
    uint8_t service_data2[4];
    service_data2[0] = BATTERY_UUID_SERVICE & 0xff;
    service_data2[1] = BATTERY_UUID_SERVICE >> 8;
    
    temp = _battery * 100;    
    sprintf(hex, "%x", temp);
    service_data2[2] = temp & 0xff;
    service_data2[3] = temp >> 8;
            
    ble.gap().accumulateAdvertisingPayload(GapAdvertisingData::SERVICE_DATA,  (uint8_t *)service_data2, sizeof(service_data2));
    
    //Humidity
    uint8_t service_data3[3];
    service_data3[0] = USER_DATA_UUID_SERVICE & 0xff;
    service_data3[1] = USER_DATA_UUID_SERVICE >> 8;
    service_data3[2] = _humidity;    
    ble.gap().accumulateAdvertisingPayload(GapAdvertisingData::SERVICE_DATA,  (uint8_t *)service_data3, sizeof(service_data3));
    
    /* Appearance */
    ble.gap().accumulateAdvertisingPayload(GapAdvertisingData::GENERIC_THERMOMETER);
    
    /* Set local Name */
    ble.gap().accumulateAdvertisingPayload(GapAdvertisingData::COMPLETE_LOCAL_NAME, (uint8_t *)DEVICE_NAME, sizeof(DEVICE_NAME));
            
    /* Setup advertising parameters:  not connectable */
    ble.gap().setAdvertisingType(GapAdvertisingParams::ADV_NON_CONNECTABLE_UNDIRECTED);
    ble.gap().setAdvertisingInterval(ADVERTISE_INTERVAL_IN_MS); 
}

//On BLE Init Completed
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

void pollDataCallBack(void)
{
    /* Note that the pollDataCallBack() executes in interrupt context, so it is safer to do
     * heavy-weight sensor polling from the main thread. */
    triggerSensorPolling = true;
}

//read and convert battery voltage
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

int main() {
   
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
   
   //every X seconds, sends period update, up to 1800 (30 minutes)
   //_pollDataTimer.attach(pollDataCallBack, POLL_DATA_INTERVAL_IN_S);  //send updated I/O every x seconds
   
   //Poll some data at beginning
   triggerSensorPolling = true;
   
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
       
       //Time to poll data from sensor
       if (triggerSensorPolling) {
           
           //Is it useful??
           _pollDataTimer.attach(pollDataCallBack, POLL_DATA_INTERVAL_IN_S);  //send updated I/O every x seconds
           
           //Get Battery Level
           _battery = get_battery_level();
           
           #if DEBUG_MODE
              _device.printf("Battery percentage : %6.2f \r\n", _battery);
           #endif
           
           _temperature = _sensor.sample_ctemp();
           
           #if DEBUG_MODE
              _device.printf("Temperature : %6.2f \r\n", _temperature);
           #endif
           
           _humidity = _sensor.sample_humid();
           
           #if DEBUG_MODE
              _device.printf("Humidity percentage : %d \r\n", _humidity);
           #endif
           
           //Generate new advertising data
           generateAdvertisingData(ble);
           
           /* Ok Advertise now !! */
           ble.gap().startAdvertising();                                 
                                 
           triggerSensorPolling = false;
       }
       
       ble.waitForEvent(); // low power wait for event
   }
}