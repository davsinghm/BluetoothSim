# BluetoothSim

Android dialer app that communicates over Bluetooth with an external GSM device serving purpose of secondary SIM.

This app manages text messages and calls for external secondary SIM. The UI is very close to native experience and conforms to latest Android APIs. Supports notification channels very similar to Google Phone app. The call logs are exported to Android's database for unified call history in Phone's native dialer.

The app uses Bluetooth's serial port profile to comminucate with GSM module (which can be very small compared to carrying another phone, two stacked credit card sized) for sending AT commands. This low level hardware abstraction layer communicate with upper layers of the app controlling its behavior.
