#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <PubSubClient.h>

#define LIGHT_PIN 15
#define FAN_PIN 14

const char* ssid = "Harbinger";
const char* password = "qwerty12345";
const char* mqtt_server = "192.168.0.101";
const char* subscription_topic = "__some_topic__";

WiFiClient espClient;
PubSubClient client(espClient);
long lastMsg = 0;
char msg[50];
int value = 0;
bool light_status = false;
bool fan_status = false;

void setup_wifi() {
  delay(10);

  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
  }

  randomSeed(micros());
}

void callback(char* topic, byte* payload, unsigned int length) {
  if ((char)payload[0] == 'l') {
    light_status = !light_status;
    digitalWrite(LIGHT_PIN, light_status);
    sprintf(msg, "%d,%d", light_status, fan_status);
    client.publish("__other_topic__", msg);
  }
  else if ((char)payload[0] == 'f') {
    fan_status = !fan_status;
    digitalWrite(FAN_PIN, fan_status);
    sprintf(msg, "%d,%d", light_status, fan_status);
    client.publish("__other_topic__", msg);
  } 
  else {
    sprintf(msg, "%d,%d", light_status, fan_status);
    client.publish("__other_topic__", msg);
  }
}

void reconnect() {
  while (!client.connected()) {
    String clientId = "ESP8266Client-";
    clientId += String(random(0xffff), HEX);
    if (client.connect(clientId.c_str())) {
      client.subscribe(subscription_topic);
    } else {
      delay(5000);
    }
  }
}

void setup() {
  pinMode(LIGHT_PIN, OUTPUT);
  pinMode(FAN_PIN, OUTPUT);
  setup_wifi();
  client.setServer(mqtt_server, 1883);
  client.setCallback(callback);
}

void loop() {
  if (!client.connected()) {
    reconnect();
  }

  client.loop();
}
