package com.example.gpsserial;

import android.content.Intent;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
//import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
//iport com.hoho.android.usbserial.driver.ProbeTable;
//import com.hoho.android.usbserial.driver.UsbSerialDriver;
//import com.hoho.android.usbserial.driver.UsbSerialPort;
//import com.hoho.android.usbserial.driver.UsbSerialProber;
//import com.hoho.android.usbserial.util.HexDump;
//import com.hoho.android.usbserial.util.SerialInputOutputManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final int DEFAULT_UPDATE_INTERVAL = 30;
    public static final int FAST_UPDATE_INTERVAL = 5;
    private static final int PERMISSIONS_FINE_LOCATION = 99;
    int updatecount = 0;
    int USBconnected = 0;
    UsbSerialPort port;
    TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, tv_address, text_view;
    Switch sw_gps, sw_locationsupdates;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallBack;

    private enum UsbPermission {Unknown, Requested, Granted, Denied}
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    private static final int WRITE_WAIT_MILLIS = 2000;
    private static final int READ_WAIT_MILLIS = 2000;
    private int deviceId, portNum, baudRate;
    private boolean withIoManager;
    public SerialInputOutputManager ioManager;
//    private void TryUSBconnect(void);

    //    private final BroadcastReceiver broadcastReceiver;
//    private final Handler mainLooper;
    private TextView receiveText;
//    private ControlLines controlLines;

    //    private SerialInputOutputManager usbIoManager;
//    private UsbSerialPort usbSerialPort;
//    private UsbPermission usbPermission = UsbPermission.Unknown;
    private boolean connected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowManager.LayoutParams WMLP = getWindow().getAttributes();
        //       WMLP.screenBrightness = 0;  // minimal brightness
        WMLP.screenBrightness = 0;
        getWindow().setAttributes(WMLP);

        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_accuracy = findViewById(R.id.tv_accuracy);
        tv_speed = findViewById(R.id.tv_speed);
        tv_sensor = findViewById(R.id.tv_sensor);
        tv_updates = findViewById(R.id.tv_updates);
        tv_address = findViewById(R.id.tv_address);
        text_view = findViewById(R.id.text_view);
        sw_gps = findViewById(R.id.sw_gps);
        sw_locationsupdates = findViewById(R.id.sw_locationsupdates);

        locationRequest = LocationRequest.create()
 //               .setInterval(1000 * DEFAULT_UPDATE_INTERVAL)
                .setInterval(1000 * FAST_UPDATE_INTERVAL)
                .setFastestInterval(1000 * FAST_UPDATE_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(100);

        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                updatecount++;
                updateUIValues(location);
            }
        };

        sw_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sw_gps.isChecked()) {
                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    tv_sensor.setText("Using Towers + WIFI");
                } else {
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    tv_sensor.setText("Using GPS sensor");
                }
            }
        });

        sw_locationsupdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sw_locationsupdates.isChecked()) {
                    startLocationUpdates();
                } else {
                    stopLocationUpdates();
                }
            }
        });

        TryUSBconnect();
        updateGPS();
    }

    void TryUSBconnect(){
        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            USBconnected = 0;
        }else {
            USBconnected = 1;
            // Open a connection to the first available driver.
            UsbSerialDriver driver = availableDrivers.get(0);
            UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
            if (connection == null) {
                // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
                USBconnected = 0;
            }else {
                port = driver.getPorts().get(0); // Most devices have just one port (port 0)
                try {
                    port.open(connection);
                } catch (IOException e) {
                    USBconnected = 0;
                    e.printStackTrace();
                }
                try {
                    port.setParameters(19200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                } catch (IOException e) {
                    USBconnected = 0;
                    e.printStackTrace();
                }
            }
        }
        if(USBconnected==1) {
            ioManager = new SerialInputOutputManager(port);
            ioManager.setListener(new SerialInputOutputManager.Listener() {
                public void onNewData(byte[] data) {
                    runOnUiThread(() -> {
                        text_view.setText(new String(data));
                        if (data[0] == 'R') {
                            try {
                                String outpg = tv_lat.getText() + " " + tv_lon.getText() + "\n";
                                port.write(outpg.getBytes(StandardCharsets.UTF_8), WRITE_WAIT_MILLIS);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    });
                }

                @Override
                public void onRunError(Exception e) {
                    e.printStackTrace();
                }
            });
            ioManager.start();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        text_view.setText(new String("Hallo Ger"));
        if("android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(intent.getAction())) {
            TryUSBconnect();
            text_view.setText(new String("Con?"));
        }
        if("android.hardware.usb.action.USB_DEVICE_DETACHED".equals(intent.getAction())) {
            ioManager.stop();
            USBconnected = 0;
            text_view.setText(new String("Discon"));
        }
        super.onNewIntent(intent);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if(!sw_locationsupdates.isChecked()){
            sw_locationsupdates.setChecked(true);
        }
        startLocationUpdates();
        Toast.makeText(getApplicationContext(),"Now onStart() start updates", Toast.LENGTH_LONG).show(); //onStart Called
    }

    private void startLocationUpdates() {
        tv_updates.setText("On");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            tv_updates.setText("Need permissions");
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
        updateGPS();
    }

    private void stopLocationUpdates() {
        tv_updates.setText("Off");
        tv_lat.setText("not tracking");
        tv_lon.setText("not tracking");
        tv_speed.setText("not tracking");
        tv_altitude.setText("not tracking");
//        tv_address.setText("not used");
        tv_accuracy.setText("not tracking");
//        tv_sensor.setText("not tracking");
        fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_FINE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateGPS();
                } else {
                    Toast.makeText(this, "This app requires permission to be granted in order to work properly", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void updateGPS() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    updateUIValues(location);
                }
            });
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
            }
        }
    }

    private void updateUIValues(Location location) {
        if (location != null) {
            tv_lat.setText(String.valueOf(location.getLatitude()));
            tv_lon.setText(String.valueOf(location.getLongitude()));
            tv_accuracy.setText(String.valueOf(location.getAccuracy()));
            if (location.hasAltitude()) {
                double altg = location.getAltitude();
                altg = Math.round(altg * 100) / 100;
                tv_altitude.setText(String.valueOf(altg));
            } else {
                tv_altitude.setText("Not available");
            }
            if (location.hasSpeed()) {
                tv_speed.setText(String.valueOf(location.getSpeed()));
            } else {
                tv_speed.setText("Not available");
            }
            tv_address.setText(String.valueOf(updatecount));
        }
    }

    /**
     * Example of binding and unbinding to the local service.
     * bind to, receiving an object through which it can communicate with the service.
     *
     * Note that this is implemented as an inner class only keep the sample
     * all together; typically this code would appear in some separate class.
     */
    /*
    public static class Binding extends MainActivity {
        // Don't attempt to unbind from the service unless the client has received some
        // information about the service's state.
        private boolean mShouldUnbind;

        // To invoke the bound service, first make sure that this value
        // is not null.
        private GrabLocationDetails mBoundService;

        private ServiceConnection mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                // This is called when the connection with the service has been
                // established, giving us the service object we can use to
                // interact with the service.  Because we have bound to a explicit
                // service that we know is running in our own process, we can
                // cast its IBinder to a concrete class and directly access it.
                mBoundService = ((GrabLocationDetails.LocalBinder) service).getService();

                // Tell the user about this for our demo.
                Toast.makeText(Binding.this, R.string.local_service_connected,
                        Toast.LENGTH_SHORT).show();
            }

            public void onServiceDisconnected(ComponentName className) {
                // This is called when the connection with the service has been
                // unexpectedly disconnected -- that is, its process crashed.
                // Because it is running in our same process, we should never
                // see this happen.
                mBoundService = null;
                Toast.makeText(Binding.this, R.string.local_service_disconnected,
                        Toast.LENGTH_SHORT).show();
            }
        };

        void doBindService() {
            // Attempts to establish a connection with the service.  We use an
            // explicit class name because we want a specific service
            // implementation that we know will be running in our own process
            // (and thus won't be supporting component replacement by other
            // applications).
            if (bindService(new Intent(Binding.this, GrabLocationDetails.class),
                    mConnection, Context.BIND_AUTO_CREATE)) {
                mShouldUnbind = true;
            } else {
                Log.e("MY_APP_TAG", "Error: The requested service doesn't " +
                        "exist, or this client isn't allowed access to it.");
            }
        }

        void doUnbindService() {
            if (mShouldUnbind) {
                // Release information about the service's state.
                unbindService(mConnection);
                mShouldUnbind = false;
            }
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            doUnbindService();
        }
    }

     */
}