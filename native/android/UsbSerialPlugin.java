package com.bandera.g2dash;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Minimal USB CDC (virtual serial port) driver for VESC controllers.
 * Uses Android's built-in USB host API, so it works on any Android phone
 * with USB OTG (Samsung, Xiaomi, Pixel, OnePlus, Motorola...).
 */
@CapacitorPlugin(name = "UsbSerial")
public class UsbSerialPlugin extends Plugin {
    private static final String ACTION_PERMISSION = "com.bandera.g2dash.USB_PERMISSION";

    private UsbManager manager;
    private UsbDeviceConnection connection;
    private UsbInterface dataIf, ctrlIf;
    private UsbEndpoint epIn, epOut;
    private Thread reader;
    private volatile boolean running = false;
    private PluginCall pendingCall;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_PERMISSION.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                PluginCall call = pendingCall;
                pendingCall = null;
                if (call == null) return;
                if (granted && device != null) open(device, call);
                else call.reject("USB permission was denied");
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                if (connection != null) {
                    close();
                    notifyListeners("disconnected", new JSObject());
                }
            }
        }
    };

    @Override
    public void load() {
        manager = (UsbManager) getContext().getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter(ACTION_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        ContextCompat.registerReceiver(getContext(), receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private UsbInterface findInterface(UsbDevice d, int cls) {
        for (int i = 0; i < d.getInterfaceCount(); i++) {
            UsbInterface itf = d.getInterface(i);
            if (itf.getInterfaceClass() == cls) return itf;
        }
        return null;
    }

    @PluginMethod
    public void connect(PluginCall call) {
        if (connection != null) close();
        UsbDevice device = null;
        for (UsbDevice d : manager.getDeviceList().values()) {
            if (findInterface(d, UsbConstants.USB_CLASS_CDC_DATA) != null) { device = d; break; }
        }
        if (device == null) {
            call.reject("No VESC found. Check the cable and the USB-C OTG adapter.");
            return;
        }
        if (manager.hasPermission(device)) { open(device, call); return; }
        pendingCall = call;
        int flags = Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0;
        Intent intent = new Intent(ACTION_PERMISSION).setPackage(getContext().getPackageName());
        manager.requestPermission(device, PendingIntent.getBroadcast(getContext(), 0, intent, flags));
    }

    private void open(UsbDevice device, PluginCall call) {
        dataIf = findInterface(device, UsbConstants.USB_CLASS_CDC_DATA);
        ctrlIf = findInterface(device, UsbConstants.USB_CLASS_COMM);
        epIn = null; epOut = null;
        for (int i = 0; i < dataIf.getEndpointCount(); i++) {
            UsbEndpoint ep = dataIf.getEndpoint(i);
            if (ep.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK) continue;
            if (ep.getDirection() == UsbConstants.USB_DIR_IN) epIn = ep; else epOut = ep;
        }
        connection = manager.openDevice(device);
        if (connection == null || epIn == null || epOut == null) {
            close();
            call.reject("Could not open the USB device");
            return;
        }
        if (ctrlIf != null) connection.claimInterface(ctrlIf, true);
        connection.claimInterface(dataIf, true);
        if (ctrlIf != null) {
            int idx = ctrlIf.getId();
            byte[] lineCoding = {(byte) 0x00, (byte) 0xC2, (byte) 0x01, 0x00, 0x00, 0x00, 0x08}; // 115200 8N1
            connection.controlTransfer(0x21, 0x20, 0, idx, lineCoding, lineCoding.length, 1000);
            connection.controlTransfer(0x21, 0x22, 0x01, idx, null, 0, 1000); // DTR on
        }
        running = true;
        reader = new Thread(() -> {
            byte[] buf = new byte[Math.max(64, epIn.getMaxPacketSize())];
            while (running) {
                UsbDeviceConnection c = connection;
                if (c == null) break;
                int n = c.bulkTransfer(epIn, buf, buf.length, 200);
                if (n > 0) {
                    StringBuilder sb = new StringBuilder(n * 3);
                    for (int i = 0; i < n; i++) {
                        if (i > 0) sb.append(' ');
                        sb.append(String.format("%02x", buf[i] & 0xff));
                    }
                    JSObject data = new JSObject();
                    data.put("value", sb.toString());
                    notifyListeners("data", data);
                }
            }
        });
        reader.start();
        JSObject ret = new JSObject();
        String name = device.getProductName();
        ret.put("name", name != null ? name : "VESC");
        call.resolve(ret);
    }

    @PluginMethod
    public void write(PluginCall call) {
        String hex = call.getString("value", "").trim();
        if (connection == null) { call.reject("Not connected"); return; }
        if (hex.isEmpty()) { call.resolve(); return; }
        String[] parts = hex.split("\\s+");
        byte[] bytes = new byte[parts.length];
        for (int i = 0; i < parts.length; i++) bytes[i] = (byte) Integer.parseInt(parts[i], 16);
        int n = connection.bulkTransfer(epOut, bytes, bytes.length, 500);
        if (n < 0) call.reject("USB write failed"); else call.resolve();
    }

    @PluginMethod
    public void disconnect(PluginCall call) {
        close();
        call.resolve();
    }

    private void close() {
        running = false;
        UsbDeviceConnection c = connection;
        connection = null;
        if (c != null) {
            try { if (dataIf != null) c.releaseInterface(dataIf); } catch (Exception ignored) {}
            try { if (ctrlIf != null) c.releaseInterface(ctrlIf); } catch (Exception ignored) {}
            c.close();
        }
    }

    @Override
    protected void handleOnDestroy() {
        close();
        try { getContext().unregisterReceiver(receiver); } catch (Exception ignored) {}
    }
}
