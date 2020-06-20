#include <Wire.h>

const byte BROADCAST_ADDRESS = 0x01;
const byte TOTAL_NODE_COUNT = 0x03; // Dynamically generated value
byte slavesAssigned   = 0x00;     // Holds the last assigned slave ID
byte slavesIdentified = 0x00;

// Table of slaves (generated dynamically, but addresses unknown initially)
byte HCl_syringeAddress  = 0x00; // Unassigned
byte NaOH_syringeAddress = 0x00; // Unassigned
byte TheReactorAddress   = 0x00; // Unassigned

// State machine. Available states:
##define khi_emer  = -1;
##define khi_setup = 0;
##define setup_HCl = 1;
##define send_HCl  = 2;
##define send_NaOH = 3;
##define react     = 4;
int currentState = khi_setup;
bool actionsExecuted = false;

// Triggers received
//  + From node HCl_syringe
bool HCl_syringe_UPPER_STOP = false;
bool HCl_syringe_LOWER_STOP = false;
bool HCl_syringe_DONE = false;
bool HCl_syringe_UNEXPECTED_STOP = false;
//  + From node NaOH_syringe
bool NaOH_syringe_UPPER_STOP = false;
bool NaOH_syringe_LOWER_STOP = false;
bool NaOH_syringe_DONE = false;
bool NaOH_syringe_UNEXPECTED_STOP = false;
//  + From node TheReactor
//    -> No triggers defined

// TODO: all Wire.endTransmission() calls should check return code

void setup() {
    Wire.begin();                            // join i2c bus (address optional for master)
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
    pollErrors(); // TODO check if any error occurred in any module
    stateMachine();
    pollSensors(); // TODO poll Sensor values
}  // end of loop

void stateMachine() {
    switch(currentState) {
    case khi_setup:
        break;
    case khi_emer:
        // TODO Do something to secure the system!
        break;
    case setup_HCl:
        if(!actionsExecuted) {
            execute_MOVE_TO_UPPER_STOP(HCl_syringeAddress);
            actionsExecuted = true;
        } else if(HCl_syringe_UPPER_STOP) {
            currentState = send_HCl;
            actionsExecuted = false;
        }
        break;
    case send_HCl:
        if(!actionsExecuted) {
            execute_inject(HCl_syringe, 80, 30);
            actionsExecuted = true;
        } else if(HCl_syringe_DONE) {
            currentState = send_NaOH;
            actionsExecuted = false;
        }
        break;
    case send_NaOH:
        if(!actionsExecuted) {
            execute_inject(NaOH_syringe, 20, 30);
            actionsExecuted = true;
        } else if(NaOH_syringe_DONE) {
            currentState = send_NaOH;
            actionsExecuted = false;
        }
        break;
    }
}

void execute_MOVE_TO_UPPER_STOP(int nodeAddress) {
    Wire.beginTransmission(nodeAddress);
    Wire.write("SET_TEMP");
    Wire.endTransmission();
}

void execute_inject(int nodeAddress, int volume, int flowRate) {
    Wire.beginTransmission(nodeAddress);
    Wire.write("PUMP");
    Wire.write(volume);
    Wire.write(flowRate);
    Wire.endTransmission();
}

void clearAssignmentsOnRequest() {                // Slave requests confirmation of address when they boot
    Wire.write(0x00);                             // This code means 'reset your address to 0x01'
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
    Wire.write("ASSIGN");                         // Broadcast that Master wants to assign identities
    Wire.endTransmission();
    Wire.requestFrom(0x01, 1);                    // Requests new slaves to respond
    if(Wire.available() > 0) {                    // Some slave(s) have responded
        slavesAssigned++;
        Wire.beginTransmission(BROADCAST_ADDRESS);
        Wire.write(slavesAssigned + 1);           // Send new slave a new Address
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
    Serial.println("What is this node?"); // TODO ask node for its type, first
    name = Serial.readString();
    if(strcmp(name, "HCl_syringe") == 0) {
        slavesIdentified++;
        HCl_syringeAddress = slavesIdentified;
    } else if(strcmp(name, "NaOH_syringe") == 0) {
        slavesIdentified++;
        NaOH_syringeAddress = slavesIdentified;
    }  else if(strcmp(name, "TheReactor") == 0) {
        slavesIdentified++;
        TheReactorAddress = slavesIdentified;
    } 
}