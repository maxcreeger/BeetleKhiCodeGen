//mReactorByBernard

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

// State Variables (Sensor values)

// Messaging
char receivedMessage[32] = "";
boolean bMessageReceived = false;

// Received Commands
double targetTemperature = 0;

// Setup
void setup() {
  Wire.begin(3); // Join i2c bus
  Wire.onRequest(requestedI2cMesssage); // Register 'Master requests a message' event
  Wire.onReceive(receiveI2cMesssage);   // Register 'Master sends a message' event
  Serial.begin(9600);                   // start serial for output




		
}

void loop() {
  delay(100);

  pollTemperature();


		
}


// Receive communication from master Node
void receiveI2cMesssage(int size) {
  char receivedMessage[32];
  int i = 0;
  while (1 < Wire.available() && i < 32) {        // loop through all but the last
    receivedMessage[i] = Wire.read();   // receive byte as a character
    i++;
  }
  if(receivedMessage[0] == 'S' &&
     receivedMessage[1] == 'E' &&
     receivedMessage[2] == 'T' &&
     receivedMessage[3] == '_' &&
     receivedMessage[4] == 'T' &&
     receivedMessage[5] == 'E' &&
     receivedMessage[6] == 'M' &&
     receivedMessage[7] == 'P') {
    // Parse attribute values
    // Parse temp
    targetTemperature = 0;
// unknown type: double, cannot parse!
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

