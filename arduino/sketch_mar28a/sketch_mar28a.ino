#include <SPI.h>
#include <WiFiNINA.h>

const String LOCKER_ID="1";
String LOCK_STATE="1";

const char ssid[]="Room";
const char password[]="Tmvhswl123!";
int status=WL_IDLE_STATUS;
const char server[]="192.168.0.17";
WiFiClient client;
unsigned long lastConnectionTime=0;
const unsigned long interval= 10L*1000L;

const int AA=9; //모터 A의 A를 6번 핀에 배치
const int AB=10; //모터 B의 B를 7번 핀에 배치
const int speed=100;
String str="";
String targetStr="GPGGA";
String Latitude="";
String Longitude="";
void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
  
  //moter setting
  pinMode(AA,OUTPUT);
  pinMode(AB,OUTPUT);

  while(status!=WL_CONNECTED){
    Serial.print("Attempting to connect to SSID: ");
    Serial.println(ssid);
    status=WiFi.begin(ssid,password);
    delay(10000);
  }

  Serial.println("WiFi connected");
  printWifiStatus();

  Serial.println("Start GPS...");
  Serial1.begin(9600);
 
}

void loop() {
  // put your main code here, to run repeatedly:
  String buf="";
  int headercount=0;
  while(client.available()){
    char c=client.read();
    Serial.print(c);
    if(c=='\n'){
      headercount++;
    }
    if(headercount==6){ //data section
      buf+=c;
    }
  }
  
  if(headercount==7){ //received finish
    Serial.println();
    buf.remove(0,1);
    if(buf=="changed"){
      if(LOCK_STATE=="1"){
        LOCK_STATE="0";
        Serial.println("unlocking...");

        analogWrite(AA,speed);
        analogWrite(AB,0);
        delay(1000);

        analogWrite(AA,0);
        analogWrite(AB,0);
      }
      else{ //LOCK_STATE="0"
        LOCK_STATE="1";
        Serial.println("locking...");

        analogWrite(AA,0);
        analogWrite(AB,speed);
        delay(1000);

        digitalWrite(AA,0);
        digitalWrite(AB,0);
      }
    }
    else{
      Serial.println("state not changed...");
    }

    Serial.print("present state is ");
    Serial.println(LOCK_STATE);
    Serial.println();
  }

  
  if(Serial1.available()){
    char c=Serial1.read();
    if(c=='\n'){

      if(targetStr.equals(str.substring(1,6))){
        //Serial.println(str);
        int first=str.indexOf(",");
        int two=str.indexOf(",",first+1);
        int three=str.indexOf(",",two+1);
        int four=str.indexOf(",",three+1);
        int five=str.indexOf(",",four+1);

        String Lat=str.substring(two+1,three);
        String Long=str.substring(four+1,five);
        String Lat_front=Lat.substring(0,2);
        String Lat_back=Lat.substring(2);
        String Long_front=Long.substring(0,3);
        String Long_back=Long.substring(3);

        double d_Lat=Lat_front.toDouble()+Lat_back.toDouble()/60;
        double d_Long=Long_front.toDouble()+Long_back.toDouble()/60;

        if(d_Lat==0||d_Long==0){
          Latitude="null";
          Longitude="null";
        }
        else{
          Latitude=String();
          Longitude=String();
        }
        
        //Serial.print("Latitude: ");
        //Serial.println(Latitude);
        //Serial.print("Longitude: ");
        //Serial.println(Longitude);
        
      }

      str="";
    }
    else{
      str+=c;
    }
    
  }

  if(millis()-lastConnectionTime>interval){
    stateRequest();
  }


  

  /*
  //정회전
  digitalWrite(AA,HIGH);
  digitalWrite(AB,LOW);
  delay(1000);

  //정지
  digitalWrite(AA,LOW);
  digitalWrite(AB,LOW);
  delay(250);

  //역회전
  digitalWrite(AA,LOW);
  digitalWrite(AB,HIGH);
  delay(1000);

  digitalWrite(AA,LOW);
  digitalWrite(AB,LOW);
  delay(250);

  */

}

void stateRequest(){
  client.stop();

  if(client.connect(server,2259)){
    Serial.println("connecting...");

    client.println("GET /state?id="+LOCKER_ID+"&state="+LOCK_STATE+"&lat="+Latitude+"&long="+Longitude+" HTTP/1.1");
    client.println();

    lastConnectionTime=millis();
  }
  else{
    Serial.println("connection failed");
    delay(5000);
  }
}


void printWifiStatus() {
  // print the SSID of the network you're attached to:
  Serial.print("SSID: ");
  Serial.println(WiFi.SSID());

  // print your board's IP address:
  IPAddress ip = WiFi.localIP();
  Serial.print("IP Address: ");
  Serial.println(ip);

  // print the received signal strength:
  long rssi = WiFi.RSSI();
  Serial.print("signal strength (RSSI):");
  Serial.print(rssi);
  Serial.println(" dBm");
}
