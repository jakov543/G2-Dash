#!/bin/sh
# Adds the Bluetooth permission text Apple requires, allows portrait + landscape (handlebar mounts),
# keeps Bluetooth alive when the screen locks,
# and keeps the dark status bar. Run once after `npx cap add ios`.
PLIST="ios/App/App/Info.plist"
PB=/usr/libexec/PlistBuddy
$PB -c "Delete :NSBluetoothAlwaysUsageDescription" "$PLIST" 2>/dev/null
$PB -c "Add :NSBluetoothAlwaysUsageDescription string 'G2 Dash connects to your scooter controller over Bluetooth to show speed, battery and temperatures.'" "$PLIST"
$PB -c "Delete :UISupportedInterfaceOrientations" "$PLIST" 2>/dev/null
$PB -c "Add :UISupportedInterfaceOrientations array" "$PLIST"
$PB -c "Add :UISupportedInterfaceOrientations:0 string UIInterfaceOrientationPortrait" "$PLIST"
$PB -c "Add :UISupportedInterfaceOrientations:1 string UIInterfaceOrientationLandscapeLeft" "$PLIST"
$PB -c "Add :UISupportedInterfaceOrientations:2 string UIInterfaceOrientationLandscapeRight" "$PLIST"
$PB -c "Delete :UIBackgroundModes" "$PLIST" 2>/dev/null
$PB -c "Add :UIBackgroundModes array" "$PLIST"
$PB -c "Add :UIBackgroundModes:0 string bluetooth-central" "$PLIST"
$PB -c "Delete :UIStatusBarStyle" "$PLIST" 2>/dev/null
$PB -c "Add :UIStatusBarStyle string UIStatusBarStyleLightContent" "$PLIST"
$PB -c "Delete :ITSAppUsesNonExemptEncryption" "$PLIST" 2>/dev/null
$PB -c "Add :ITSAppUsesNonExemptEncryption bool false" "$PLIST"
echo "Info.plist patched."
