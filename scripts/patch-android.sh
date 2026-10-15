#!/bin/sh
# Copies the USB plugin into the Android project and enables USB host.
# Run once after `npx cap add android`.
set -e
DIR="android/app/src/main/java/com/bandera/g2dash"
mkdir -p "$DIR"
cp native/android/UsbSerialPlugin.java "$DIR/"
cp native/android/MainActivity.java "$DIR/"
MAN="android/app/src/main/AndroidManifest.xml"
if ! grep -q "android.hardware.usb.host" "$MAN"; then
  perl -0pi -e 's#<application#<uses-feature android:name="android.hardware.usb.host" android:required="false" />\n    <application#' "$MAN"
fi
echo "Android project patched."
