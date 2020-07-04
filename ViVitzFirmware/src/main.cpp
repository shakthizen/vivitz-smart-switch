#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <uMQTTBroker.h>
#include <MQTT.h>

#define IN 2
#define OUT 0

bool lastSwitchState = 0;
bool outputState = 0;

unsigned long lastDebounceTime = 0;
bool debouncing = 0;

unsigned int debounceTime = 100;

String ssid = "ViVitzSmart_" + String(ESP.getChipId());
String pass = "fHD9$g#4";

const String TOPIC = "/vivitz/status";

void setOutput(bool v);

class ViVitzBroker : public uMQTTBroker
{
public:
  virtual bool onConnect(IPAddress addr, uint16_t client_count)
  {
    Serial.println(addr.toString() + " connected");
    return true;
  }

  virtual bool onAuth(String username, String password)
  {
    Serial.println("Username/Password: " + username + "/" + password);
    return true;
  }

  virtual void onData(String topic, const char *data, uint32_t length)
  {
    // char data_chr[length + 1];
    // os_memcpy(data_chr, data, length);
    // data_chr[length] = '\0';

    String data_str = String(data).substring(0, length);

    // Serial.println(topic + " : " + data_str);
    if (topic.equals(TOPIC))
    {
      int v = data_str.toInt();
      outputState = v;
      Serial.println(v);
    }
  }
};

ViVitzBroker viVitzBroker;
// MQTT mqttClient("ViVitz", "127.0.0.1", 1883);

void startWifiAP()
{
  Serial.println("Starting WiFi AP...");
  Serial.print("SSID : ");
  Serial.println(ssid);
  WiFi.mode(WIFI_AP);
  WiFi.softAPConfig(IPAddress(192, 168, 12, 1), IPAddress(192, 168, 12, 1), IPAddress(255, 255, 255, 0));
  WiFi.softAP(ssid, pass, 1, 1, 4);
}

void sendData()
{
  viVitzBroker.publish(TOPIC, String(outputState), 0, 1);
}

void startMQTT()
{
  Serial.println("Starting MQTT...");
  viVitzBroker.init();
  viVitzBroker.subscribe(TOPIC);
  sendData();
}

bool getSwitchState()
{
  return !digitalRead(IN);
}

bool getOutputState()
{
  return outputState;
}

void setOutput(bool v)
{
  digitalWrite(OUT, v);
}

void checkSwitchState()
{
  bool newState = getSwitchState();
  if (!debouncing && newState != lastSwitchState)
  {
    lastDebounceTime = millis();
    debouncing = 1;
  }

  if (debouncing && (millis() - lastDebounceTime) > debounceTime)
  {
    if (newState != lastSwitchState)
    {
      lastSwitchState = newState;
      outputState = newState;
      sendData();
    }
    debouncing = 0;
  }
  setOutput(outputState);
}

void setup()
{
  Serial.begin(9600);
  pinMode(OUT, OUTPUT);
  pinMode(IN, INPUT_PULLUP);

  setOutput(HIGH);
  startWifiAP();
  startMQTT();
}

void loop()
{
  checkSwitchState();
}