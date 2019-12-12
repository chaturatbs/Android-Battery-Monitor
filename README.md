# Android Battery Monitor

A simple app that allows one to run experiments to characterise the display power consumption of an android device.
An apk is provided for easy install. The apk is configured with 60s:20s split for test image and baseline measurement (black screen) respectively.
It reads an experiment config file at 'Internal storage\Android\data\com.example.batterymonitor\files\inputConfig\testConfig.csv'. Put the files referred to by the csv file in 'Internal storage\Android\data\com.example.batterymonitor\files\inputImages'
The measurement result file is save to 'Internal storage\Android\data\com.example.batterymonitor\files\measurementLogs'
