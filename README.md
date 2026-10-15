# G2 Dash — App Store build

This folder turns the dashboard into a real iPhone app with native Bluetooth
(Capacitor + @capacitor-community/bluetooth-le). The whole app is `www/index.html`.

## What you need
- A **Mac** with **Xcode** (free, from the Mac App Store)
- **Node.js 20+** (nodejs.org)
- An **Apple Developer account** ($99/year, developer.apple.com) to publish

## 1. Build the iOS project (once)
```sh
npm install
npm run ios:add      # creates the ios/ folder and adds the Bluetooth permission
npm run icons        # makes all icon + splash sizes from resources/
npm run sync
npm run open         # opens Xcode
```

## 2. Run it on your iPhone
1. In Xcode, click **App** in the left list → **Signing & Capabilities**.
2. Tick **Automatically manage signing** and pick your Team.
3. Plug in your iPhone, choose it at the top, press ▶︎ Run.
4. On the phone: Settings → General → VPN & Device Management → trust your profile
   (only needed with a free account).

## 3. Publish to the App Store
1. On appstoreconnect.apple.com → **My Apps → +** → New App
   (bundle ID `com.bandera.g2dash` — change it in `capacitor.config.json` if you want).
2. In Xcode: top bar device = **Any iOS Device**, then **Product → Archive**.
3. In the Organizer window: **Distribute App → App Store Connect → Upload**.
4. Back in App Store Connect fill in: screenshots (6.9" iPhone), description,
   keywords, support URL, **privacy policy URL** (host `PRIVACY.md` anywhere,
   e.g. GitHub Pages), age rating, and App Privacy = "Data not collected".
5. Pick the uploaded build and **Submit for Review**.

## Review tips (avoid rejection)
- **Don't put "Kukirin" in the app name, icon or screenshots** — Apple rejects apps
  that use another company's brand without permission (guideline 5.2). Say
  "for VESC-powered scooters" instead.
- In **App Review notes**, explain that live data needs a VESC controller and that
  the app has a built-in **demo mode** (hold "Hold to ride") so the reviewer can test it.
- After changing `www/index.html`, run `npm run sync` and archive again.
  Bump the version/build number in Xcode for each upload.

## Android app (Google Play) — Bluetooth + USB cable
Works on Samsung, Xiaomi, Pixel, OnePlus, Motorola and other Android phones with USB OTG.

You need Android Studio (free, Windows/Mac/Linux) and a Google Play developer account ($25 one time).
```sh
npm install
npm run android:add     # creates android/ and adds the USB plugin
npm run android:icons
npm run android:sync
npm run android:open    # opens Android Studio
```
- Plug in your phone with USB debugging on, press Run.
- To publish: Build → Generate Signed App Bundle → upload the .aab on play.google.com/console.
- After changing `www/index.html`, run `npm run android:sync` again.
- The USB plugin is in `native/android/`. It reads the VESC's USB serial port with
  Android's own USB host API, so it doesn't need any extra library.

## About USB on iPhone
iPhone can't use the USB cable, including USB-C iPhones. Apple doesn't allow
apps to open USB serial devices like a VESC. On iPhone use Bluetooth.
