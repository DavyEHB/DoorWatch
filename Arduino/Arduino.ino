/*
  Arduino Yun Bridge example

 This example for the Arduino Yun shows how to use the
 Bridge library to access the digital and analog pins
 on the board through REST calls. It demonstrates how
 you can create your own API when using REST style
 calls through the browser.

 Possible commands created in this shetch:

 * "/arduino/digital/13"     -> digitalRead(13)
 * "/arduino/digital/13/1"   -> digitalWrite(13, HIGH)
 * "/arduino/analog/2/123"   -> analogWrite(2, 123)
 * "/arduino/analog/2"       -> analogRead(2)
 * "/arduino/mode/13/input"  -> pinMode(13, INPUT)
 * "/arduino/mode/13/output" -> pinMode(13, OUTPUT)

 This example code is part of the public domain
 
 In the curlScript.sh : 
 #!/bin/bash
 curl -X POST -k -H \"Authorization: key=AIzaSyDmojymkbqj7hLR_idVX4mCTXG0xtS97Dg\" -H \"Content-Type: application/json\" -d '{\"to\": \"/topics/test\",\"data\": {\"message\": \"Test\"}}' https://android.googleapis.com/gcm/send
 

 http://arduino.cc/en/Tutorial/Bridge

 */

#include <Bridge.h>
#include <YunServer.h>
#include <YunClient.h>

// Listen on default port 5555, the webserver on the Yun
// will forward there all the HTTP requests for us.
YunServer server;
long linuxBaud = 250000;
boolean commandMode = false;

const int buttonPin = 7;    // the number of the pushbutton pin
const int ledPin = 11;      // the number of the LED pin
const String curlParameters = "curl -X POST -k -H \"Authorization: key=AIzaSyDmojymkbqj7hLR_idVX4mCTXG0xtS97Dg\" -H \"Content-Type: application/json\" -d '{\"to\": \"/topics/test\",\"data\": {\"message\": \"Test\"}}' https://android.googleapis.com/gcm/send";
const unsigned int iButtonInterval = 50;
const unsigned int iYunInterval = 5;

// Variables will change:
int ledState = HIGH;         // the current state of the output pin
int buttonState;             // the current reading from the input pin
int lastButtonState = LOW;   // the previous reading from the input pin

// the following variables are long's because the time, measured in miliseconds,
// will quickly become a bigger number than can be stored in an int.
long lastDebounceTime = 0;  // the last time the output pin was toggled
long debounceDelay = 50;    // the debounce time; increase if the output flickers

int iTime;
int iPrevYunTime;
int iPrevButtonTime;

Process p; 




void setup() {
  delay(2500);
  // Bridge startup
  pinMode(12, OUTPUT);
  
  pinMode(buttonPin, INPUT_PULLUP);
  pinMode(ledPin, OUTPUT);
  
  digitalWrite(12, LOW);
  Bridge.begin();
  digitalWrite(12, HIGH);
  delay(1000);
  digitalWrite(12, LOW);
  // Listen for incoming connection only from localhost
  // (no one from the external network could connect)
  server.listenOnLocalhost();
  server.begin();
  
  // Serial CPU monitor
  Serial.begin(115200);     
  //Serial1.begin(linuxBaud); 
  
}

void loop() {
  // put your main code here, to run repeatedly: 
  
  iTime = millis();
  if ((iTime - iPrevYunTime) > iYunInterval) runYUN();
  if ((iTime - iPrevButtonTime) > iButtonInterval){
    checkButton();
    readProcess();
  }
  
  // CPU monitor
   //serBridge();
}
  

//void(* reset) (void) = 0;//declare reset function at address 0
/*
void reboot() {
  Process p;        // Create a process and call it "p"
  p.begin("reboot");  // Process that launch the "reboot" command
  p.runAsynchronously();      // Run the process
}

void reset() {
  Process p;        // Create a process and call it "p"
  p.begin("reset-mcu");  // Process that launch the "reset-mcu" command
  p.runAsynchronously();      // Run the process
}
*/

void curl() {
       // Create a process and call it "p"
  Serial.println("Curling");
  
  p.begin("sh");
 
  p.addParameter("/root/curlScript.sh");
  
  p.runAsynchronously();     // Run the process
}


void process(YunClient client) {
  // read the command
  String command = client.readStringUntil('/');
  
  // is "digital" command?
  if (command == "digital") {
    digitalCommand(client);
  }

  // is "analog" command?
  if (command == "analog") {
    analogCommand(client);
  }

  // is "mode" command?
  if (command == "mode") {
    modeCommand(client);
  }
 // 
  /* is "cpu" command?
  if (command == "cpu") {
    cpuCommand(client);
  }
  */
}

/*
void cpuCommand(YunClient client) {

  String command = client.readStringUntil('\r');
    
  if (command == "reset"){
    client.println(F("Rebooting Linux and AVR"));
    delay(1000);
    reboot();
  }
  
  if (command == "reset-avr"){
    client.println(F("Resetting AVR"));
    delay(1000);
    reset();
  }  
}
*/

void digitalCommand(YunClient client) {
  int pin, value;

  // Read pin number
  pin = client.parseInt();

  // If the next character is a '/' it means we have an URL
  // with a value like: "/digital/13/1"
  if (client.read() == '/') {
    value = client.parseInt();
    digitalWrite(pin, value);
    // Send feedback to client
    client.print(F("Light "));
    client.print(pin);
    client.print(F(" is turned "));
    if (value == 1) {
      client.println("on");
    }
    else {
      client.println("off");
    }
  }
  else {
    value = digitalRead(pin);
    client.print(value);
  }

  // Update datastore key with the current pin value
  String key = "D";
  key += pin;
  Bridge.put(key, String(value));
}

void analogCommand(YunClient client) {
  int pin, value;

  // Read pin number
  pin = client.parseInt();

  // If the next character is a '/' it means we have an URL
  // with a value like: "/analog/5/120"
  if (client.read() == '/') {
    // Read value and execute command
    value = client.parseInt();
    analogWrite(pin, value);

    // Send feedback to client
    client.print(F("Pin D"));
    client.print(pin);
    client.print(F(" set to analog "));
    client.println(value);

    // Update datastore key with the current pin value
    String key = "D";
    key += pin;
    Bridge.put(key, String(value));
  }
  else {
    // Read analog pin
    value = analogRead(pin);

    // Send feedback to client
    client.print(F("Pin A"));
    client.print(pin);
    client.print(F(" reads analog "));
    client.println(value);

    // Update datastore key with the current pin value
    String key = "A";
    key += pin;
    Bridge.put(key, String(value));
  }
}

void modeCommand(YunClient client) {
  int pin;

  // Read pin number
  pin = client.parseInt();

  // If the next character is not a '/' we have a malformed URL
  if (client.read() != '/') {
    client.println(F("error"));
    return;
  }

  String mode = client.readStringUntil('\r');

  if (mode == "input") {
    pinMode(pin, INPUT);
    // Send feedback to client
    client.print(F("Pin D"));
    client.print(pin);
    client.print(F(" configured as INPUT!"));
    return;
  }

  if (mode == "output") {
    pinMode(pin, OUTPUT);
    // Send feedback to client
    client.print(F("Pin D"));
    client.print(pin);
    client.print(F(" configured as OUTPUT!"));
    return;
  }

  client.print(F("error: invalid mode "));
  client.print(mode);
}

void serBridge() {
  // copy from virtual serial line to uart and vice versa
  if (Serial.available()) {           // got anything from USB-Serial?
    char c = (char)Serial.read();     // read from USB-serial
    if (commandMode == false) {       // if we aren't in command mode...
      if (c == '~') {                 //    Tilde '~' key pressed?
        commandMode = true;           //       enter in command mode
      } else {
        Serial1.write(c);             //    otherwise write char to Linux
      }
    } else {                          // if we are in command mode...
      if (c == '0') {                 //     '0' key pressed?
        Serial1.begin(57600);         //        set speed to 57600
        Serial.println("Speed set to 57600");
      } else if (c == '1') {          //     '1' key pressed?
        Serial1.begin(115200);        //        set speed to 115200
        Serial.println("Speed set to 115200");
      } else if (c == '2') {          //     '2' key pressed?
        Serial1.begin(250000);        //        set speed to 250000
        Serial.println("Speed set to 250000");
      } else if (c == '3') {          //     '3' key pressed?
        Serial1.begin(500000);        //        set speed to 500000
        Serial.println("Speed set to 500000");
      } else if (c == '~') {
        Serial1.write((uint8_t *)"\xff\0\0\x05XXXXX\x0d\xaf", 11);
        Serial.println("Sending bridge's shutdown command");
      } else {                        //     any other key pressed?
        Serial1.write('~');           //        write '~' to Linux
        Serial1.write(c);             //        write char to Linux
      }
      commandMode = false;            //     in all cases exit from command mode
    }
  }
  if (Serial1.available()) {          // got anything from Linux?         
    char c = (char)Serial1.read();    // read from Linux  
    Serial.write(c);                  // write to USB-serial
  }
}

void checkButton(){
    // read the state of the switch into a local variable:
  int reading = digitalRead(buttonPin);

  // check to see if you just pressed the button
  // (i.e. the input went from LOW to HIGH),  and you've waited
  // long enough since the last press to ignore any noise:

  // If the switch changed, due to noise or pressing:
  if (reading != lastButtonState) {
    // reset the debouncing timer
    lastDebounceTime = millis();
  }

  if ((millis() - lastDebounceTime) > debounceDelay) {
    // whatever the reading is at, it's been there for longer
    // than the debounce delay, so take it as the actual current state:

    // if the button state has changed:
    if (reading != buttonState) {
      buttonState = reading;

      // only toggle the LED if the new button state is HIGH
      if (buttonState == HIGH) {
        //DO STUFF
        Serial.println("Button pressed");
        curl();
        ledState = !ledState;
      }
    }
  }

  // set the LED:
  digitalWrite(ledPin, ledState);

  // save the reading.  Next time through the loop,
  // it'll be the lastButtonState:
  lastButtonState = reading;
}

void runYUN(){
  // Get clients coming from server
  YunClient client = server.accept();

  // There is a new client?
  if (client) {
    // Process request
    process(client);

    // Close connection and free resources.
    client.stop();
  }
}

void readProcess(){
  if (p.available() > 0) Serial.print((char)p.read());
}


