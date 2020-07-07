#include <EEPROM.h>
#include <Wire.h>

// Event Variables

// State Variables (Sensor values)

int I2CAddress;

void(* resetFunc) (void) = 0;          // Needed for software reset

void setup() {
    getAddress();
    Wire.begin(I2CAddress);
    wireInit();
}

void getAddress() {
    I2CAddress = EEPROM.read(0x00);                // Slave address is stored at EEPROM address 0x00
    if (I2CAddress == 0x00) {                      // If unset...
        I2CAddress = 0x01;                         //     Then listen to broadcast to obtain a new one
    }
}

void wireInit() {
    if (I2CAddress == 0x01) {                      // If slave address = 0x01 (address not assigned) then:
        Wire.onRequest(resetRequest);              //     make this function execute when ping requested from master,
        Wire.onReceive(resetReceive);              //     and make this function execute to receive new address.
    } else {                                       // Else
        EEPROM.write(0x00, 0x01);                  //     Forget the address, so that when shut off, it will start
        Wire.onReceive(normalReceive);             //     from scratch
        Wire.onRequest(normalRequest); 
    }
}

void resetRequest() {
    Wire.write(0xFF);       // Ping to master ("I need an address!")
}

void resetReceive(int howMany) {
    while(0 < Wire.available()) {
        I2CAddress = Wire.read();                 // Read new address from master,
        EEPROM.write(0x00, I2CAddress);           // set it to EEPROM,
        resetFunc();                              // and reset Arduino slave to acquire it on boot
    }
}

