# sitewhere-android-sdk
Use this SDK to access the SiteWhere API from your Android projects.

# Developer Setup
* [Android Studio](http://developer.android.com/sdk/index.html)
* Android SDK 19 (It is possible to use this SDK with a lower SDK version)
* [Java SE SDK 1.7](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)

# Quickstart
1. Clone this repository
2. Create a new Project
3. Add the following to the settings.gradle file
```
include ':sitewhere-android-sdk'
project(':sitewhere-android-sdk').projectDir = new File('../sitewhere-android-sdk') // <- points to the local repository 
```
4. Select 'Open Model Settings" and add model "sitewhere-android-sdk" in the Dependencies tab

# Sample App
The sample app can be found in the SiteWhereExample folder.  The app demostrates how an Android device can be an IoT gateway and/or client device for SiteWhere.  As an IoT gateway you can register an Android device with SiteWhere and send location and measurement events.  As an IoT client you can register to have events pushed in real-time to an Android device.  Configuring what events get pushed to a specific device is done using server side filters and groovy scripts.  The sample app uses your current location and the accelerometer.


