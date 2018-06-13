#include "BluetoothSerial.h"

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

//Pin 21 SDA
//Pin 22 SCL
#define LED_PIN 12 // (Arduino is 13, Teensy is 11, Teensy++ is 6)
#define INTERRUPT_PIN 26  // use pin 2 on Arduino Uno & most boards

char ctrlChar='a';
bool writeToFile = false;
bool printCurrentValues = true;
String currentFilenameString = "/output.txt";
char currentFilename[30];

bool bluetoothInitiated = false;

BluetoothSerial SerialBT;

#include "MPU6050_6Axis_MotionApps20.h"

// Arduino Wire library is required if I2Cdev I2CDEV_ARDUINO_WIRE implementation
// is used in I2Cdev.h
#if I2CDEV_IMPLEMENTATION == I2CDEV_ARDUINO_WIRE
    #include "Wire.h"
#endif

#include "FS.h"
#include "SD_MMC.h"

File file;

// class default I2C address is 0x68
// specific I2C addresses may be passed as a parameter here
// AD0 low = 0x68 (default for SparkFun breakout and InvenSense evaluation board)
// AD0 high = 0x69
MPU6050 mpu[5];

#define TCADDR 0x70

int sensorNumber = 0;
//MPU6050 mpu(0x69); // <-- use for AD0 high

#define OUTPUT_READABLE_YAWPITCHROLL

// MPU control/status vars
bool dmpReady = false;  // set true if DMP init was successful
uint8_t mpuIntStatus;   // holds actual interrupt status byte from MPU
uint8_t devStatus;      // return status after each device operation (0 = success, !0 = error)
uint16_t packetSize;    // expected DMP packet size (default is 42 bytes)
uint16_t fifoCount;     // count of all bytes currently in FIFO
uint8_t fifoBuffer[64]; // FIFO storage buffer

// orientation/motion vars
Quaternion q;           // [w, x, y, z]         quaternion container
VectorInt16 aa;         // [x, y, z]            accel sensor measurements
VectorInt16 aaReal;     // [x, y, z]            gravity-free accel sensor measurements
VectorInt16 aaWorld;    // [x, y, z]            world-frame accel sensor measurements
VectorFloat gravity;    // [x, y, z]            gravity vector
float euler[3];         // [psi, theta, phi]    Euler angle container
float ypr[3];           // [yaw, pitch, roll]   yaw/pitch/roll container and gravity vector

// packet structure for InvenSense teapot demo
//uint8_t teapotPacket[14] = { '$', 0x02, 0,0, 0,0, 0,0, 0,0, 0x00, 0x00, '\r', '\n' };



// ================================================================
// ===               INTERRUPT DETECTION ROUTINE                ===
// ================================================================

volatile bool mpuInterrupt = false;     // indicates whether MPU interrupt pin has gone high
void dmpDataReady() {
    mpuInterrupt = true;
}

//void openFile(fs::FS &fs, const char * path, const char * message);

//void appendFile(fs::FS &fs, const char * path, const char * message);


void setup() {
  mpu[0] = MPU6050(0x68);
  mpu[1] = MPU6050(0x69);
  mpu[2] = MPU6050(0x68);
  mpu[3] = MPU6050(0x69);
  mpu[4] = MPU6050(0x68);

    initiateBluetooth();
  
    // join I2C bus (I2Cdev library doesn't do this automatically)
    #if I2CDEV_IMPLEMENTATION == I2CDEV_ARDUINO_WIRE
        Wire.begin();
        Wire.setClock(300000); // 400kHz I2C clock. Comment this line if having compilation difficulties
    #elif I2CDEV_IMPLEMENTATION == I2CDEV_BUILTIN_FASTWIRE
        Fastwire::setup(300, true);
    #endif

    // initialize serial communication
    // (115200 chosen because it is required for Teapot Demo output, but it's
    // really up to you depending on your project)
    Serial.begin(115200);
    while (!Serial); // wait for Leonardo enumeration, others continue immediately

    // initialize device
    Serial.println(F("Initializing I2C devices..."));
    while(sensorNumber<5){
      if(sensorNumber==0 || sensorNumber==1){
        tcaselect(7);
      } else if(sensorNumber==2 || sensorNumber==3){
        tcaselect(6);
      } else if(sensorNumber==4){
        tcaselect(5);
      }
        mpu[sensorNumber].initialize();
        //pinMode(INTERRUPT_PIN, INPUT);
    
        // verify connection
        //while(!mpu.testConnection()){
          //delay(500);
        //}
        Serial.println(F("Testing device connections..."));
        Serial.println(mpu[sensorNumber].testConnection() ? F("MPU6050 connection successful") : F("MPU6050 connection failed"));
    
        // wait for ready
        Serial.println(F("\nSend any character to begin DMP programming and demo: "));
        while (Serial.available() && Serial.read()); // empty buffer
        while (!Serial.available());                 // wait for data
        while (Serial.available() && Serial.read()); // empty buffer again
    
        // load and configure the DMP
        Serial.println(F("Initializing DMP..."));
        devStatus = mpu[sensorNumber].dmpInitialize();
    
        // supply your own gyro offsets here, scaled for min sensitivity
        mpu[sensorNumber].setXGyroOffset(220);
        mpu[sensorNumber].setYGyroOffset(76);
        mpu[sensorNumber].setZGyroOffset(-85);
        mpu[sensorNumber].setZAccelOffset(1788); // 1688 factory default for my test chip
    
        // make sure it worked (returns 0 if so)
        if (devStatus == 0) {
            // turn on the DMP, now that it's ready
            Serial.println(F("Enabling DMP..."));
            mpu[sensorNumber].setDMPEnabled(true);
    
            // enable Arduino interrupt detection
            Serial.println(F("Enabling interrupt detection (Arduino external interrupt 0)..."));
            //attachInterrupt(digitalPinToInterrupt(INTERRUPT_PIN), dmpDataReady, RISING);
            //mpuIntStatus = mpu[sensorNumber].getIntStatus();
    
            // set our DMP Ready flag so the main loop() function knows it's okay to use it
            //Serial.println(F("DMP ready! Waiting for first interrupt..."));
            dmpReady = true;
    
            // get expected DMP packet size for later comparison
            packetSize = mpu[sensorNumber].dmpGetFIFOPacketSize();
        } else {
            // ERROR!
            // 1 = initial memory load failed
            // 2 = DMP configuration updates failed
            // (if it's going to break, usually the code will be 1)
            Serial.print(F("DMP Initialization failed (code "));
            Serial.print(devStatus);
            Serial.println(F(")"));
        }
        sensorNumber++;
    }
    sensorNumber=0;
    // configure LED for output
    pinMode(LED_PIN, OUTPUT);

    //Opening File/SD

    if(!SD_MMC.begin()){
        Serial.println("Card Mount Failed");
    }

    readConfig(SD_MMC);

    Serial.print("CurrentFilename: ");
    Serial.println(currentFilenameString);

    openFile(SD_MMC, currentFilename, "Sensor Data:\n");
    
}

void loop() {
  //Bluetooth Part
  if(SerialBT.hasClient()){
    bluetoothInitiated = false;
      if (Serial.available()) {
        SerialBT.write(Serial.read());
      }
      if (SerialBT.available()) {
        ctrlChar = SerialBT.read();
        Serial.write(ctrlChar);
      }
      if(ctrlChar == 'r'){  //readContent
        String reqPath = "";
        char reqPathBuf[50];
        while(ctrlChar != 's'){
          while(!SerialBT.available());
          ctrlChar = SerialBT.read();
          if(ctrlChar!='s'){
            reqPath.concat(ctrlChar);
          }else{
            reqPath.toCharArray(reqPathBuf,40);    
          }
        }
        readFile(SD_MMC, reqPathBuf);
      }
      if(ctrlChar == 'F'){  //writeToFile
        writeToFile = true;
      }
      if(ctrlChar == 'f'){  //stopWriteToFile
        writeToFile = false;
      }
      if(ctrlChar == 'D'){  //sendCurrentData
        printCurrentValues = true;
      }
      if(ctrlChar == 'd'){  //stopSendCurrentData
        printCurrentValues = false;
      }
      if(ctrlChar == 'C'){  //CalibrationStart
        setCalibrationMarker(true);
      }
      if(ctrlChar == 'c'){  //CalibrationEnd
        setCalibrationMarker(false);
      }
      if(ctrlChar == 't'){  //time
        String timeString = "millis,";
        char timeBuf[50];
        int digit=0;
        while(ctrlChar != 's'){
          while(!SerialBT.available());
          ctrlChar = SerialBT.read();
          if(ctrlChar!='s'){
            timeString.concat(ctrlChar);
          }else{
            timeString.concat(",");
            timeString.concat(millis());
            timeString.concat("\n");
            timeString.toCharArray(timeBuf,40);
            appendFile(SD_MMC, currentFilename, timeBuf);
          }
        }
      }
  }else{
     if(!bluetoothInitiated){
      initiateBluetooth();
     }
  }
  //MPU6050 Part
  readSensors();
}

void readSensors(){
  if (!dmpReady) return;

    if(sensorNumber==0 || sensorNumber==1){
      tcaselect(7);
    } else if(sensorNumber==2 || sensorNumber==3){
      tcaselect(6);
    } else if(sensorNumber==4){
      tcaselect(5);
    }

    // reset interrupt flag and get INT_STATUS byte
    mpuInterrupt = false;
    mpuIntStatus = mpu[sensorNumber].getIntStatus();

    // get current FIFO count
    fifoCount = mpu[sensorNumber].getFIFOCount();

    // check for overflow (this should never happen unless our code is too inefficient)
    if ((mpuIntStatus & 0x10) || fifoCount == 1024) {
        // reset so we can continue cleanly
        mpu[sensorNumber].resetFIFO();
        //Serial.println(F("FIFO overflow!"));

    // otherwise, check for DMP data ready interrupt (this should happen frequently)
    } else if (mpuIntStatus & 0x02) {
        // wait for correct available data length, should be a VERY short wait
        while (fifoCount < packetSize) fifoCount = mpu[sensorNumber].getFIFOCount();

        // read a packet from FIFO
        mpu[sensorNumber].getFIFOBytes(fifoBuffer, packetSize);
        
        // track FIFO count here in case there is > 1 packet available
        // (this lets us immediately read more without waiting for an interrupt)
        fifoCount -= packetSize;

   
        // display Euler angles in degrees
        mpu[sensorNumber].dmpGetQuaternion(&q, fifoBuffer);
        mpu[sensorNumber].dmpGetGravity(&gravity, &q);
        mpu[sensorNumber].dmpGetYawPitchRoll(ypr, &q, &gravity);
        String tSt = "";
        tSt.concat(sensorNumber);
        tSt.concat(",");
        tSt.concat(millis());
        tSt.concat(",");
        tSt.concat((ypr[0] * 180/M_PI));
        tSt.concat(",");
        tSt.concat((ypr[1] * 180/M_PI));
        tSt.concat(",");
        tSt.concat((ypr[2] * 180/M_PI));
        tSt.concat("\n");
        if(printCurrentValues){
          SerialBT.print(tSt);
        }
        if(writeToFile){
          char buf[60];
          tSt.toCharArray(buf,60);
          appendFile(SD_MMC, currentFilename, buf);
        }
        if(Serial.available()){
          file.close();
          Serial.println("10 Sec");
          delay(10000);
        }
        sensorNumber = (sensorNumber+1)%5;
  }
}

void openFile(fs::FS &fs, const char * path, const char * message){
    Serial.printf("Writing file: %s\n", path);

    file = fs.open(path, FILE_WRITE);
    if(!file){
        Serial.println("Failed to open file for writing");
        return;
    }
    if(file.print(message)){
        Serial.println("File written");
    } else {
        Serial.println("Write failed");
    }
    file = fs.open(path, FILE_APPEND);
}

void appendFile(fs::FS &fs, const char * path, const char * message){
    //Serial.printf("Appending to file: %s\n", path);

    if(!file){
        Serial.println("Failed to open file for appending");
        return;
    }
    file.print(message);
}

void readConfig(fs::FS &fs){
    Serial.println("Reading file");

    file.close();
    file = fs.open("/config.txt");
    if(!file){
        Serial.println("Failed to open file for reading");
        return;
    }

    String tmpFilename="";
    while(file.available()){
      char tm = file.read();
      if(tm == '\n'){
        currentFilenameString = tmpFilename;
        currentFilenameString.toCharArray(currentFilename, 30);
        break;
      }
        tmpFilename.concat(tm);
        Serial.println(tmpFilename);
    }
    String fileIndex = tmpFilename.substring((tmpFilename.indexOf('_')+1),tmpFilename.indexOf('.'));
    int fileIndexInt = fileIndex.toInt();
    Serial.print("CurrentFileIndex: ");
    Serial.println(fileIndexInt);
    fileIndexInt++;
    String newFilename = "/output_";
    newFilename.concat(fileIndexInt);
    newFilename.concat(".txt\n");
    file.close();
    Serial.print("New Filename: ");
    Serial.println(newFilename);
    char newFilenameBuf[50];
    newFilename.toCharArray(newFilenameBuf, 50);
    openFile(SD_MMC, "/config.txt", newFilenameBuf);
    file.close();
}

void readFile(fs::FS &fs, const char * path){
    Serial.printf("Reading file: %s\n", path);

    file.close();
    file = fs.open(path);
    if(!file){
        SerialBT.println("e,404");
        return;
    }

    String line = "";
    Serial.print("Read from file: ");
    while(file.available()){
      char tmpChar = file.read();
      if(tmpChar == '\n'){
        line.concat(tmpChar);
        SerialBT.print(line);
        line = ""; 
      }else{
        line.concat(tmpChar);
      }
    }
    SerialBT.print(line);
    SerialBT.flush();
    file.close();
    file = fs.open(currentFilename, FILE_APPEND);
}

void setCalibrationMarker(bool startCalibration){
  String calibrationString;
  if(startCalibration){
    calibrationString = "C,";
  }else{
    calibrationString = "c,";
  }
  char calibrationBuf[50];
  calibrationString.concat(millis());
  calibrationString.concat("\n");
  calibrationString.toCharArray(calibrationBuf, 40);
  appendFile(SD_MMC, currentFilename, calibrationBuf);
}

void initiateBluetooth(){
  SerialBT.begin("TrackerJacket"); //Bluetooth device name
  Serial.println("Ready for new Bluetooth connection");
  bluetoothInitiated = true;
}

void tcaselect(uint8_t param){
  if(param > 7) return;

  Wire.beginTransmission(TCADDR);
  Wire.write(1 << param);
  Wire.endTransmission();
}

