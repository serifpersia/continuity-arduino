const int wirePin = 7;  // Pin connected to the wire
const int ledPin = 13;  // Built-in LED on Arduino
const int debounceDelay = 35;  // Adjust this value based on your requirements

int lastWireState = HIGH;  // Initialize the previous state of the wire

void setup() {
  pinMode(ledPin, OUTPUT);         // Set the LED pin as an output
  digitalWrite(ledPin, LOW);       // Turn off the LED initially
  pinMode(wirePin, INPUT_PULLUP);  // Set the wire pin as an input with internal pull-up resistor
  Serial.begin(115200);            // Initialize serial communication
}

void loop() {
  int wireState = digitalRead(wirePin);  // Read the state of the wire pin

  // Check for a change in the wire state
  if (wireState != lastWireState) {
    delay(debounceDelay);  // Add debounce delay
    wireState = digitalRead(wirePin);  // Read the state again

    // If the wire is connected to ground (LOW state), turn on the LED
    if (wireState == LOW) {
      digitalWrite(ledPin, HIGH);
      Serial.write('C');  // Send the character 'C' to serial
      Serial.write('\n'); // Send a newline character
    } else {
      digitalWrite(ledPin, LOW);  // If not connected to ground, turn off the LED
      Serial.write('N');          // Send the character 'N' to serial
      Serial.write('\n'); // Send a newline character
    }
  }

  lastWireState = wireState;  // Update the previous state
}
