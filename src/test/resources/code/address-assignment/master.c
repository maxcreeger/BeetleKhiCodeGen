#include <Wire.h>

const byte BROADCAST_ADDRESS = 0x01;
byte slavesAssigned = 0x01;     // Holds the last assigned slave ID, starting at 0x01 (0x00 is invalid, 0x01 is broadcast)
bool assignmentDone = false;

// Table of slaves (generated dynamically, but addresses unknown initially)
byte syringe1Address = 0x00; // Unassigned
byte syringe2Address = 0x00; // Unassigned
byte reactorAddress  = 0x00; // Unassigned

void setup() {
    Wire.begin();       // join i2c bus (address optional for master)
}

void loop() {
    if(!assignmentDone) {
        checkNewSlave();
    }
}  // end of loop

void checkNewSlave() {
    Wire.beginTransmission(BROADCAST_ADDRESS);
    Wire.write("ASSIGN");            // Broadcast that Master wants to assign identities
    Wire.endTransmission();
    Wire.requestFrom(0x01, 1);       // Requests new slaves to respond
    if(Wire.available() > 0) {       // Some slave(s) have responded
        slavesAssigned++;
        Wire.beginTransmission(BROADCAST_ADDRESS);
        Wire.write(slavesAssigned);  // Send new slave a new Address
        Wire.endTransmission();
        delay(250);
    }

}
