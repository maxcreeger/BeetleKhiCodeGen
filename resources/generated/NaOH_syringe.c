//mSyringeByAlexandre

// Includes
#include <Wire.h>
#include <stdio.h>

// Helper methods
unsigned char checksum (unsigned char *ptr, size_t sz) {
  unsigned char chk = 0;
  while (sz-- != 0)
    chk -= *ptr++;
    return chk;
  }
}
void putInt(unsigned char *ptr, int16_t number) {
  ptr[        0         ] = (number >>  8) & 0xFF;
  ptr[  sizeof( int16_t)] =  number        & 0xFF;
}
void putUnsignedInt(unsigned char *ptr, uint16_t number) {
  ptr[        0         ] = (number >>  8) & 0xFF;
  ptr[  sizeof(uint16_t)] =  number        & 0xFF;
}
void putLong(unsigned char *ptr, int32_t number) {
  ptr[        0         ] = (number >> 24) & 0xFF;
  ptr[  sizeof( int32_t)] = (number >> 16) & 0xFF;
  ptr[2*sizeof( int32_t)] = (number >>  8) & 0xFF;
  ptr[3*sizeof( int32_t)] =  number        & 0xFF;
}
void putUnsignedLong(unsigned char *ptr, uint32_t number) {
  ptr[        0         ] = (number >> 24) & 0xFF;
  ptr[  sizeof(uint32_t)] = (number >> 16) & 0xFF;
  ptr[2*sizeof(uint32_t)] = (number >>  8) & 0xFF;
  ptr[3*sizeof(uint32_t)] =  number        & 0xFF;
}
int16_t readInt(unsigned char *ptr) {
  int16_t number;
  number = ptr[0];
  number = number << 8 | ptr[  sizeof(int16_t)];
  return number;
}
uint16_t readUnsignedInt(unsigned char *ptr) {
  uint16_t number;
  number = ptr[0];
  number = number << 8 | ptr[  sizeof(int16_t)];
  return number;
}
int32_t readLong(unsigned char *ptr) {
  int32_t number;
  number = ptr[0];
  number = number << 8 | ptr[  sizeof(int32_t)];
  number = number << 8 | ptr[2*sizeof(int32_t)];
  number = number << 8 | ptr[3*sizeof(int32_t)];
  return number;
}
uint32_t readUnsignedLong(unsigned char *ptr) {
  uint32_t number;
  number = ptr[0];
  number = number << 8 | ptr[  sizeof(uint32_t)];
  number = number << 8 | ptr[2*sizeof(uint32_t)];
  number = number << 8 | ptr[3*sizeof(uint32_t)];
  return number;
}

// Event Variables
boolean mSyringeByAlexandre_LOWER_STOP = false;
boolean mSyringeByAlexandre_UPPER_STOP = false;
boolean mSyringeByAlexandre_DONE = false;
boolean mSyringeByAlexandre_UNEXPECTED_STOP = false;

// State Variables (Sensor values)
boolean bLowerLimit = false;
boolean bUpperLimit = false;
int iCurrentVolume = 0;

// Messaging
char receivedMessage[32] = "";
boolean bMessageReceived = false;

// Received Commands
long iTargetVolume = 0;
int iFlowRate = 0;
boolean bMoveToLowerLimit = false;
boolean bMoveToUpperLimit = false;

// Setup
void setup() {
  Wire.begin(1); // Join i2c bus
  Wire.onRequest(requestedI2cMesssage); // Register 'Master requests a message' event
  Wire.onReceive(receiveI2cMesssage);   // Register 'Master sends a message' event
  Serial.begin(9600);                   // start serial for output
	//Set interruptions

	pinMode(iLowerLimitSwitch_pin, INPUT);//Lower Limit Detection
	attachInterrupt(digitalPinToInterrupt(iLowerLimitSwitch_pin), LowerLimitSwitchISR, CHANGE);

	pinMode(iUpperLimitSwitch_pin, INPUT);//Upper Limit Detection
	attachInterrupt(digitalPinToInterrupt(iUpperLimitSwitch_pin), UpperLimitSwitchISR, CHANGE);

		
}

void loop() {
  delay(100);
	double vitT = (temp - temp_old) /0.01;
	double cmd = P*(temp - target) + D * vitT;
	sendCurrent(cmd);

		
}


// Receive communication from master Node
void receiveI2cMesssage(int size) {
  char receivedMessage[32];
  int i = 0;
  while (1 < Wire.available() && i < 32) {        // loop through all but the last
    receivedMessage[i] = Wire.read();   // receive byte as a character
    i++;
  }
  if(receivedMessage[0] == 'P' &&
     receivedMessage[1] == 'U' &&
     receivedMessage[2] == 'M' &&
     receivedMessage[3] == 'P') {
    // Parse attribute values
    // Parse volume
    iTargetVolume = 0;
    for(int i = 0; i < 9; i++) {
      iTargetVolume = iTargetVolume * 10 + receivedMessage[5 + i] - '0';
    }
    // Parse flowRate
    iFlowRate = 0;
    for(int i = 0; i < 6; i++) {
      iFlowRate = iFlowRate * 10 + receivedMessage[16 + i] - '0';
    }
    return;
  }
  if(receivedMessage[0] == 'M' &&
     receivedMessage[1] == 'O' &&
     receivedMessage[2] == 'V' &&
     receivedMessage[3] == 'E' &&
     receivedMessage[4] == '_' &&
     receivedMessage[5] == 'T' &&
     receivedMessage[6] == 'O' &&
     receivedMessage[7] == '_' &&
     receivedMessage[8] == 'L' &&
     receivedMessage[9] == 'O' &&
     receivedMessage[10] == 'W' &&
     receivedMessage[11] == 'E' &&
     receivedMessage[12] == 'R' &&
     receivedMessage[13] == '_' &&
     receivedMessage[14] == 'S' &&
     receivedMessage[15] == 'T' &&
     receivedMessage[16] == 'O' &&
     receivedMessage[17] == 'P') {
    // Parse attribute values
    // Parse yesOrNo
    bMoveToLowerLimit = false;
    bMoveToLowerLimit = receivedMessage[20] == 't';
    return;
  }
  if(receivedMessage[0] == 'M' &&
     receivedMessage[1] == 'O' &&
     receivedMessage[2] == 'V' &&
     receivedMessage[3] == 'E' &&
     receivedMessage[4] == '_' &&
     receivedMessage[5] == 'T' &&
     receivedMessage[6] == 'O' &&
     receivedMessage[7] == '_' &&
     receivedMessage[8] == 'U' &&
     receivedMessage[9] == 'P' &&
     receivedMessage[10] == 'P' &&
     receivedMessage[11] == 'E' &&
     receivedMessage[12] == 'R' &&
     receivedMessage[13] == '_' &&
     receivedMessage[14] == 'S' &&
     receivedMessage[15] == 'T' &&
     receivedMessage[16] == 'O' &&
     receivedMessage[17] == 'P') {
    // Parse attribute values
    // Parse yesOrNo
    bMoveToUpperLimit = false;
    bMoveToUpperLimit = receivedMessage[20] == 't';
    return;
  }
  bMessageReceived = true;
}

void requestedI2cMesssage() {
  int16_t num = 1234;  // number to send
  byte myArray[2];
  putInt(myArray, num);
  Wire.write(myArray); // Send message
}

// Methods dedicated to the module:

void sendCurrent() {
	printf("hello world?");

			
}
void inject() {
	printf("hello world !!!!");

			
}
