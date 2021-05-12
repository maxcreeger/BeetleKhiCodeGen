// Stupid Salted Water Process

// Includes
#include <Wire.h>
#include <stdio.h>
#include <EEPROM.h>

// Constants
#define BROADCAST_ADDRESS 0x01;
#define TOTAL_NODE_COUNT 3;
byte slavesAssigned   = 0x00;
byte slavesIdentified = 0x00;
// Table of slave addresses
byte HCl_syringe_Address = 0x00;
byte TheReactor_Address = 0x00;
byte NaOH_syringe_Address = 0x00;

// State machine. Available states:
#define nbSteps 6;
#define step_khi_emer 0;
#define step_setup_HCl 1;
#define step_send_NaOH 2;
#define step_react 3;
#define step_send_HCl 4;
#define step_khi_setup 5;
#define nbTransitions 5;
#define tran_setup_HCl_send_HCl 0;
#define tran_send_NaOH_react 1;
#define tran_send_HCl_send_NaOH 2;
#define tran_khi_setup_setup_HCl 3;
#define initialStep step_khi_setup;
// State values
bool transitions[nbTransitions];
int stateStartTime[nbSteps];
bool steps[nbSteps + 2];
int currentState = initialStep;

// Events Received:
// + Events from node 'NaOH_syringe'
bool NaOH_syringe_DONE = false;
// + Events from node 'HCl_syringe'
bool HCl_syringe_DONE = false;
bool HCl_syringe_UNEXPECTED_STOP = false;
bool HCl_syringe_UPPER_STOP = false;
// + Events from node 'TheReactor'
//   -> No Events for this Node

// TODO: all Wire.endTransmission() calls should check return code

// Setup
void setup() {
  for(int i=0; i<nbSteps; i++) {
    steps[i] = false;
  }
  steps[initialStep] = true;
  Wire.begin();                              // Join i2c bus as Master
  Wire.onRequest(requestClearAssignments);   //   make this function execute when ping requested from master,
  Wire.onReceive(receiveClearAssignments);   //   and make this function execute to receive new address.
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
  sendMessages();
  pollErrors(); // TODO check if any error occurred in any module
  pollSensors(); // TODO poll Sensor values
  computeTransitions();
  deactivateSteps();
  activateSteps();
}

void sendMessages() {
  // TODO: send messages here
}

void computeTransitions() {
  transitions[tran_setup_HCl_send_HCl] = steps[step_setup_HCl] && (HCl_syringe_UPPER_STOP);
  transitions[tran_send_NaOH_react] = steps[step_send_NaOH] && (NaOH_syringe_DONE);
  transitions[tran_send_HCl_send_NaOH] = steps[step_send_HCl] && (HCl_syringe_DONE);
  transitions[tran_khi_setup_setup_HCl] = steps[step_khi_setup] && (millis() > stateStartTime[step_khi_setup] + 5);
}

void deactivateSteps() {
  if(transitions[tran_setup_HCl_send_HCl]) steps[step_setup_HCl] = false;
  if(transitions[tran_send_NaOH_react]) steps[step_send_NaOH] = false;
  if(transitions[tran_send_HCl_send_NaOH]) steps[step_send_HCl] = false;
  if(transitions[tran_khi_setup_setup_HCl]) steps[step_khi_setup] = false;
}

void activateSteps() {
  if(transitions[tran_setup_HCl_send_HCl]) {
    steps[step_send_HCl] = true;
    stateStartTime[step_send_HCl] = millis();
    execute_inject(HCl_syringe_Address, 80, 30);
    transitions[tran_setup_HCl_send_HCl] = false;
  }
  if(transitions[tran_send_NaOH_react]) {
    steps[step_react] = true;
    stateStartTime[step_react] = millis();
    execute_setTemperature(TheReactor_Address, 120.0);
    transitions[tran_send_NaOH_react] = false;
  }
  if(transitions[tran_send_HCl_send_NaOH]) {
    steps[step_send_NaOH] = true;
    stateStartTime[step_send_NaOH] = millis();
    execute_inject(NaOH_syringe_Address, 20, 30);
    transitions[tran_send_HCl_send_NaOH] = false;
  }
  if(transitions[tran_khi_setup_setup_HCl]) {
    steps[step_setup_HCl] = true;
    stateStartTime[step_setup_HCl] = millis();
    execute_MOVE_TO_UPPER_STOP(HCl_syringe_Address);
    transitions[tran_khi_setup_setup_HCl] = false;
  }
}

// Actions of Modules
// Actions for Modules of type mSyringeByAlexandre
void execute_inject(int nodeAddress, long volume, int flowRate) {
  Wire.beginTransmission(nodeAddress);
  Wire.write("PUMP");
  Wire.write(volume);
  Wire.write(flowRate);
  Wire.endTransmission();
}

void execute_MOVE_TO_UPPER_STOP(int nodeAddress) {
  Wire.beginTransmission(nodeAddress);
  Wire.write("MOVE_TO_UPPER_STOP");
  Wire.endTransmission();
}


// Actions for Modules of type mReactorByBernard
void execute_setTemperature(int nodeAddress, double temp) {
  Wire.beginTransmission(nodeAddress);
  Wire.write("SET_TEMP");
  Wire.write(temp);
  Wire.endTransmission();
}

// Setup functions

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
        HCl_syringe_Address = slavesIdentified;
    } else if(strcmp(name, "NaOH_syringe") == 0) {
        slavesIdentified++;
        NaOH_syringe_Address = slavesIdentified;
    }  else if(strcmp(name, "TheReactor") == 0) {
        slavesIdentified++;
        TheReactor_Address = slavesIdentified;
    } 
}