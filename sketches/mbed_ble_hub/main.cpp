/*
*
* Description  : Sensor Hub
*
* Library Requirements : BLE_API, HTU21D, nRF51822, mbed
* Author               : F.Guiet
* Creation             : 20181215
*
* Modification         : 20180120 - Change battery from percent to voltage
*                        20190310 - Add Reedswitch sensors
*                        20201018 - Change Office mac address
*
*/


//Includes
#include "mbed.h"
#include "ble/BLE.h"

//Reedswitch sensor
//c5:f7:7b:9b:24:46 - Salon main door
BLEProtocol::AddressBytes_t  reedswitch_sensors_address [] = {
    //Upstairs
    //Downstairs
    {0x46, 0x24, 0x9b, 0x7b, 0xf7, 0xc5} //Salon main door   
};

//Environmental sensor
//d2:48:c8:a5:35:4c - M
//c7:b9:43:94:24:3a - N
//e9:3d:63:97:39:5e - P
BLEProtocol::AddressBytes_t  env_sensors_address [] = { //Careful !! Little Indian Order
                                                           
                                                           //Upstairs 
                                                           //{0x4c, 0x35, 0xa5, 0xc8, 0x48, 0xd2},
                                                           //{0x3a, 0x24, 0x94, 0x43, 0xb9, 0xc7},
                                                           //{0x5e, 0x39, 0x97, 0x63, 0x3d, 0xe9},
                                                           //{0x4d, 0x2c, 0xff, 0xdc, 0x15, 0xd8} //Bathroom                                                           
                                                           
                                                           //Downstairs
                                                           {0x6a, 0xd8, 0x6f, 0xc6, 0xa4, 0xf4}, //Salon
                                                           {0xc1, 0x99, 0x0e, 0xcf, 0xe0, 0xd7} //Bureau
                                                           //{0x46, 0x24, 0x9b, 0x7b, 0xf7, 0xc5} //Testing purpose
                                                        };


Serial device(p0, p1);  //nRF51822 uart:  TX=p0.  RX=p1
//DigitalIn pinHandShake(p9);  //handshake uart to prevent output before bridge MCU is ready.  Flow control.
//DigitalOut pinLed(p7); //Led pin on CJMCU-8223 purple board

#define DEBUG_MODE 0

// callback when BLE stack scans and finds a BLE ADV packet
// Parse ADV and determine if it's one of ours, output data to serial if it is.
void advertisementCallback(const Gap::AdvertisementCallbackParams_t *params) {
    
    // parse the advertising payload, looking for data type SERVICE_DATA
    // The advertising payload is a collection of key/value records where
    // byte 0: length of the record excluding this byte
    // byte 1: The key, it is the type of the data
    // byte [2..N] The value. N is equal to byte0 - 1
    
    bool isEnvSensorMine = false;
    size_t size = sizeof(env_sensors_address) / sizeof(BLEProtocol::AddressBytes_t);
    for (int i=0;i<size;i++) {
        
        if (0 == memcmp(params->peerAddr, env_sensors_address[i], sizeof (env_sensors_address[i]))) {           
           isEnvSensorMine = true;
           break;
        }
    }
    
    bool isReedswitchSensorMine = false;
    size = sizeof(reedswitch_sensors_address) / sizeof(BLEProtocol::AddressBytes_t);
    for (int i=0;i<size;i++) {
        
        if (0 == memcmp(params->peerAddr, reedswitch_sensors_address[i], sizeof (reedswitch_sensors_address[i]))) {           
           isReedswitchSensorMine = true;
           break;
        }
    }
    
    if (isReedswitchSensorMine) {
        
        //Publish data via uart
        device.printf("{");
        
        device.printf("\"Address\":\"%02x:%02x:%02x:%02x:%02x:%02x\"",
                    params->peerAddr[5], 
                    params->peerAddr[4], 
                    params->peerAddr[3], 
                    params->peerAddr[2], 
                    params->peerAddr[1], 
                    params->peerAddr[0]);
        
        device.printf(",");
        //device.printf("\r\n");
        device.printf("\"Rssi\":");
        device.printf("\"%d\"",params->rssi);
        
        uint8_t serviceDataOrder = 1;
        //Parse advertising payload...we are looking for service data
        for (uint8_t i = 0; i < params->advertisingDataLen; ++i) {

            const uint8_t record_length = params->advertisingData[i];
            if (record_length == 0) {
                continue;
            }
            
            const uint8_t type = params->advertisingData[i + 1];
            const uint8_t* value = params->advertisingData + i + 2;
            const uint8_t value_length = record_length - 1;
            
            if (type == GapAdvertisingData::SERVICE_DATA) {
                char dest[10];
                char buffer_tmp[10];
                char buffer[10];
                memcpy(dest, value, value_length);
                dest[value_length] = '\0'; //null terminate string
                
                size_t len = strlen(dest);
                
                if (serviceDataOrder == 1) {
                    device.printf(",");
                    //device.printf("\r\n");
                    device.printf("\"Battery\":");
                }
                
                if (serviceDataOrder == 2) {
                    device.printf(",");
                    //device.printf("\r\n");
                    device.printf("\"State\":");
                }
                
                device.printf("\"");
                //j begin at 2 because we skip service data uuid
                uint8_t cpt = 0;
                
                for (uint8_t j = 2; j < len; ++j) {
                    
                    #if DEBUG_MODE                    
                        device.printf("%02x", dest[len - j + 1]); //reverse little indian order 
                    #endif
                                                       
                    sprintf(buffer_tmp, "%02x", dest[len - j + 1]); //convert to hex
                    
                    //Assemble hex string
                    for(uint8_t z =0; z < strlen(buffer_tmp);++z) {
                        buffer[cpt] = buffer_tmp[z];
                        cpt++;
                    }
                }      
                buffer[cpt] = '\0';  //null terminate string
                
                //Convert to float
                float valueFloat = (float)strtol(buffer, 0, 16);
                
                //Convert back to string
                char valueString[10];
                
                //Battery
                if (serviceDataOrder == 1) {
                    float valueFloat = valueFloat / 100;
                    sprintf(valueString, "%.2f", valueFloat);
                }
                
                //State
                if (serviceDataOrder == 2) {
                    sprintf(valueString, "%.0f", valueFloat);
                }
                
                device.printf("%s", valueString); 
                
                #if DEBUG_MODE             
                    device.printf("\r\n Buffer : %s \r\n", buffer);   
                    device.printf("\r\n Value String : %s \r\n", valueString); 
                #endif 
                
                device.printf("\"");
                
                serviceDataOrder++;
            }
        
            //Next record!
            i += record_length;
        }
        
        device.printf("}");
        device.printf("\r\n"); //End of line
        
        //pin led ON
        //pinLed = 0; 
        
        wait_ms(60);  //needed to give gateway time to assert flow control handshake pin
        //while (pinHandShake.read() == 1)    //normally pulled down, so loop when gateway processing;
        //{
            //uart flow control
            //blocking until gateway has processed ADV data from uart
        //}
        
        //pin led OFF
        //pinLed = 1;
        
    }
    
    if (isEnvSensorMine) {
        
        //Publish data via uart
        device.printf("{");
        
        device.printf("\"Address\":\"%02x:%02x:%02x:%02x:%02x:%02x\"",
                    params->peerAddr[5], 
                    params->peerAddr[4], 
                    params->peerAddr[3], 
                    params->peerAddr[2], 
                    params->peerAddr[1], 
                    params->peerAddr[0]);
        
        device.printf(",");
        //device.printf("\r\n");
        device.printf("\"Rssi\":");
        device.printf("\"%d\"",params->rssi);
        
        uint8_t serviceDataOrder = 1;
        //Parse advertising payload...we are looking for service data
        for (uint8_t i = 0; i < params->advertisingDataLen; ++i) {

            const uint8_t record_length = params->advertisingData[i];
            if (record_length == 0) {
                continue;
            }
            
            const uint8_t type = params->advertisingData[i + 1];
            const uint8_t* value = params->advertisingData + i + 2;
            const uint8_t value_length = record_length - 1;
            
            if (type == GapAdvertisingData::SERVICE_DATA) {
                char dest[10];
                char buffer_tmp[10];
                char buffer[10];
                memcpy(dest, value, value_length);
                dest[value_length] = '\0'; //null terminate string
                
                size_t len = strlen(dest);
                           
                if (serviceDataOrder == 1) {
                    device.printf(",");
                    //device.printf("\r\n");
                    device.printf("\"Temperature\":");
                }
                
                if (serviceDataOrder == 2) {
                    device.printf(",");
                    //device.printf("\r\n");
                    device.printf("\"Battery\":");
                }
                
                if (serviceDataOrder == 3) {
                    device.printf(",");
                    //device.printf("\r\n");
                    device.printf("\"Humidity\":");
                }
                
                device.printf("\"");
                //j begin at 2 because we skip service data uuid
                uint8_t cpt = 0;
                
                for (uint8_t j = 2; j < len; ++j) {
                    
                    #if DEBUG_MODE                    
                        device.printf("%02x", dest[len - j + 1]); //reverse little indian order 
                    #endif
                                                       
                    sprintf(buffer_tmp, "%02x", dest[len - j + 1]); //convert to hex
                    
                    //Assemble hex string
                    for(uint8_t z =0; z < strlen(buffer_tmp);++z) {
                        buffer[cpt] = buffer_tmp[z];
                        cpt++;
                    }
                }      
                buffer[cpt] = '\0';  //null terminate string
                
                //Convert to float
                float valueFloat = (float)strtol(buffer, 0, 16);
                
                //Convert back to string
                char valueString[10];
                
                //Temperature
                if (serviceDataOrder == 1) {
                    float valueFloat = valueFloat / 100;
                    sprintf(valueString, "%.2f", valueFloat);
                }
                
                //Battery
                if (serviceDataOrder == 2) {
                    float valueFloat = valueFloat / 100;
                    sprintf(valueString, "%.2f", valueFloat);
                }
                
                //Humidity
                if (serviceDataOrder == 3) {
                    sprintf(valueString, "%.0f", valueFloat);
                }
                
                device.printf("%s", valueString); 
                
                #if DEBUG_MODE             
                    device.printf("\r\n Buffer : %s \r\n", buffer);   
                    device.printf("\r\n Value String : %s \r\n", valueString); 
                #endif 
                
                device.printf("\"");
                
                serviceDataOrder++;
            }
        
            //Next record!
            i += record_length;
        }
        
        device.printf("}");
        device.printf("\r\n"); //End of line
        
        //pin led ON
        //pinLed = 0; 
        
        wait_ms(60);  //needed to give gateway time to assert flow control handshake pin
        //while (pinHandShake.read() == 1)    //normally pulled down, so loop when gateway processing;
        //{
            //uart flow control
            //blocking until gateway has processed ADV data from uart
        //}
        
        //pin led OFF
        //pinLed = 1;
    }
}

//On BLE Init Completed
void bleInitComplete(BLE::InitializationCompleteCallbackContext *params)
{
    BLE &ble          = params->ble;
    ble_error_t error = params->error;

    if (error != BLE_ERROR_NONE) {
        return;
    }
    
    /* Ensure that it is the default instance of BLE */
    if(ble.getInstanceID() != BLE::DEFAULT_INSTANCE) {
        return;
    }
 
    //note:  defaults to scanning 3 channels.
    // Window and Interval in ms.  Duty cycle = (interval / window);  200ms/500ms = 40%;  Max is 10000
    //  |---window---|                          |---window---|                       |---window---|
    //  |---------- interval @ ch1 -----| |------- interval @ ch2--------||-----interval @ ch3-------|
    //  set window equal to interval for full duty cycle scanning
    //  set interval to hit all 3 channels of beacon advertising over advertising time duration
    ble.gap().setScanParams(500 /* scan interval */, 500 /* scan window */);
    ble.gap().startScan(advertisementCallback);
}


int main(void)
{
   //pinHandShake.mode(PullDown); //Expecting gateway to set pin high for flow control
    
    //Init new BLE Instance 
   BLE& ble = BLE::Instance(BLE::DEFAULT_INSTANCE);
   ble.init(bleInitComplete);
   
   /* SpinWait for initialization to complete. This is necessary because the
   * BLE object is used in the main loop below. */
   while (ble.hasInitialized() == false) { /* spin loop */ }
   
   #if DEBUG_MODE  
    device.printf("BLE initialized! let's GO !!!");
   #endif
   
   //pinLed = 1; //Set pin led off!
   
   while (true) 
   {
       ble.waitForEvent();  //idle here until callback
   }
}