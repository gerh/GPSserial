package com.example.gpsserial;
/*
public class GrabLocationDetails extends Service implements LocationListener {

    double lat, lng;
    private LocationManager locationManager;
    private String provider;
    boolean isGps;
    private ArrayList<String> mList;
    Context GLDContext;

    public GrabLocationDetails(Context cont) {
        this.GLDContext = cont;
    }

    public GrabLocationDetails() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mList = new ArrayList<String>();
        isGps = false;
        lat = 0.0;
        lng = 0.0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //super.onStart(intent, startId);

        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            provider = locationManager.getBestProvider(criteria, false);
            LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
            boolean enabled = service.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (!enabled) {
                isGps = false;
                ListAddItem(isGps);
                SendBroadcast();
            } else {
                isGps = true;
                Location location = locationManager.getLastKnownLocation(provider);
                lat = (location.getLatitude());
                lng = (location.getLongitude());
                ListAddItem(true);
                SendBroadcast();
                locationManager.requestLocationUpdates(provider, 400, 1, this);
            }

        } catch (Exception e) {
            ListAddItem(isGps);
            SendBroadcast();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //locationManager.removeUpdates(this);
    }

    public void SendBroadcast() {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(CommandExecutionModule.LocationDetails);
        broadcastIntent.putExtra("Data", mList);
        sendBroadcast(broadcastIntent);

    }

    public void ListAddItem(boolean GPS) {
        //if(!GPS)
        //mList.add("0");
        //else
        //mList.add("1");
        mList.add(Double.toString(lat));
        mList.add(Double.toString(lng));
    }

    //**************************************************************************************************************

    @Override
    public void onLocationChanged(Location location) {
        locationManager.requestLocationUpdates(provider, 400, 1, this);
        mList.clear();
        lat = (location.getLatitude());
        lng = (location.getLongitude());
        ListAddItem(isGps);
        SendBroadcast();
        locationManager.removeUpdates(this);
        stopSelf();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        locationManager.requestLocationUpdates(provider, 400, 1, this);
    }

    @Override
    public void onProviderEnabled(String provider) {
        isGps = true;
    }

    @Override
    public void onProviderDisabled(String provider) {
        isGps = false;
        lat = 0.0;
        lng = 0.0;
        mList.clear();
        ListAddItem(isGps);
        //SendBroadcast();
    }
}

 */