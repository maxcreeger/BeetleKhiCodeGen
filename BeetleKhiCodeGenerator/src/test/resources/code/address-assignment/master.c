#include <Wire.h>

const byte BROADCAST_ADDRESS = 0x01;
const byte TOTAL_NODE_COUNT = 0x03; // Dynamically generated value
byte slavesAssigned   = 0x00;     // Holds the last assigned slave ID
byte slavesIdentified = 0x00;

// Table of slaves (generated dynamically, but addresses unknown initially)
byte waterSyringeAddress = 0x00; // Unassigned
byte acidSyringeAddress  = 0x00; // Unassigned
byte reactorAddress      = 0x00; // Unassigned

// State machine
int currentState = 0;

void setup() {
    Wire.begin();       // join i2c bus (address optional for master)
    Wire.onRequest(requestClearAssignments); //     make this function execute when ping requested from master,
    Wire.onReceive(receiveClearAssignments); //     and make this function execute to receive new address.
}

void loop() {
    if(slavesAssigned < TOTAL_NODE_COUNT) {
        checkNewSlave();
        return;
    }
    if(slavesIdentified < TOTAL_NODE_COUNT) {
        identifySlaves();
        return;
    }
    stateMachine();
}  // end of loop

void stateMachine() {
    // doing nothing
}

void clearAssignmentsOnRequest() { // Slave requests confirmation of address when they boot
    Wire.write(0x00);              // This code means 'reset your address to 0x01'
}

void doStuffOnReceive(int howMany) {
    while(0 < Wire.available()) {
        I2CAddress = Wire.read();                 // Read new address from master,
        EEPROM.write(0x00, I2CAddress);           // set it to EEPROM,
        resetFunc();                              // and reset Arduino slave to acquire it on boot
    }
}

void checkNewSlave() {
    Wire.beginTransmission(BROADCAST_ADDRESS);
    Wire.write("ASSIGN");            // Broadcast that Master wants to assign identities
    Wire.endTransmission();
    Wire.requestFrom(0x01, 1);       // Requests new slaves to respond
    if(Wire.available() > 0) {       // Some slave(s) have responded
        slavesAssigned++;
        Wire.beginTransmission(BROADCAST_ADDRESS);
        Wire.write(slavesAssigned + 1);  // Send new slave a new Address
        Wire.endTransmission();
        delay(250);
    }
}

void identifySlaves() {
    Wire.beginTransmission(slavesIdentified + 1);
    Wire.write("BLINK");
    Wire.endTransmission();
    Serial.begin(9600);
    String name;
    Serial.println("What is this node?");
    name = Serial.readString();
    if(strcmp(name, "waterSyringe") == 0) {
        slavesIdentified++;
        waterSyringeAddress = slavesIdentified;
    } else if(strcmp(name, "acidSyringe") == 0) {
        slavesIdentified++;
        acidSyringeAddress = slavesIdentified;
    }  else if(strcmp(name, "reactor") == 0) {
        slavesIdentified++;
        reactorAddress = slavesIdentified;
    } 
}