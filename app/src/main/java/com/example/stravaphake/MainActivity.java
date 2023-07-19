package com.example.stravaphake;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.method.Touch;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.location.NominatimPOIProvider;
import org.osmdroid.bonuspack.location.POI;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // Zone for variable
    ImageButton search_img;
    EditText search_bar;
    Button btn;
    private static final String LOG_TAG = "a";
    String address = "00:22:12:01:93:8E";
    private final int PERMISSIONS_REQUEST = 1;
    private BluetoothAdapter myBluetooth = null;
    private Set<BluetoothDevice> pairedDevices;
    public static String EXTRA_ADDRESS = "device_address";
    private ProgressDialog progress_bluetooth;
    BluetoothAdapter myBluetooth2 = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    MapView map = null;
    ConnectThread thread;
    Timer timer = new Timer();
    List<GeoPoint> geoPointList = new ArrayList<>();
    Boolean isTracking = false;
    LocationManager locationManager;
    ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
    RoadManager roadManager;
    double longitude , latitude;
    Location lastKnownLoc;
    LocationManager mLocationManager;
    List<String> list = new ArrayList<String>();
    float x1,x2,y1,y2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);


        super.onCreate(savedInstanceState);
//        requestBlePermissions(this,1);
//        myBluetooth = BluetoothAdapter.getDefaultAdapter();
//        new MainActivity.ConnectBT().execute();
//        if ( myBluetooth==null ) {
//            Toast.makeText(getApplicationContext(), "Bluetooth device not available", Toast.LENGTH_LONG).show();
//            finish();
//        }
//        else if ( !myBluetooth.isEnabled() ) {
//            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(turnBTon, 1);
//        }

        Configuration.getInstance().setUserAgentValue("osmbonuspack_6.9.0");
        setContentView(R.layout.activity_main);
        search_img = (ImageButton) findViewById(R.id.search_destination);
        search_bar = (EditText) findViewById(R.id.search_bar);
        btn = (Button) findViewById(R.id.tracking);
        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        MyLocationNewOverlay mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this),map);
        mLocationOverlay.enableMyLocation();
        map.getOverlays().add(mLocationOverlay);
        mLocationOverlay.enableFollowLocation();
        IMapController mapController = map.getController();
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        roadManager = new OSRMRoadManager(this, "osmbonuspack_6.9.0");
        ((OSRMRoadManager)roadManager).setMean(OSRMRoadManager.MEAN_BY_BIKE);
        // 16.064950099999997, 108.15898268465547
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        lastKnownLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastKnownLoc != null) {

            longitude = (double) (lastKnownLoc.getLongitude()) ;
            latitude = (double) (lastKnownLoc.getLatitude() );
            GeoPoint location = new GeoPoint(latitude, longitude);
            mapController.setCenter(location);
            mapController.setZoom(19.5);
            geoPointList.add(location);

            // tìm đường đi

            waypoints.add(location);


            // click vi tri
            MapEventsReceiver mReceive = new MapEventsReceiver() {
                @Override
                public boolean singleTapConfirmedHelper(GeoPoint p) {
                    Toast.makeText(getBaseContext(),p.getLatitude() + " - "+p.getLongitude(),Toast.LENGTH_LONG).show();

                    return false;
                }

                @Override
                public boolean longPressHelper(GeoPoint p) {
                    return false;
                }
            };

        }

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isTracking) {
                    btn.setText("Start");
                    isTracking = false;
                    btn.setBackgroundResource(R.drawable.custom_button_unpress);
                }else {
                    btn.setText("Stop");
                    isTracking = true;
                    btn.setBackgroundResource(R.drawable.custom_button_press);
                }
            }
        });

        search_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(search_bar.getText().toString() == ""){
                    msg("Bạn chưa nhập địa điểm");
                    return;
                }

                GpsMyLocationProvider gps = new GpsMyLocationProvider(MainActivity.this);
                Location tmp = getLastKnownLocation();
                double longtidue = (double) tmp.getLongitude();
                double latidude = (double) tmp.getLatitude();
                GeoPoint location = new GeoPoint(latidude, longtidue);
                NominatimPOIProvider poiProvider = new NominatimPOIProvider("osmbonuspack_6.9.0");
                try{
                        for(int j=1;j<map.getOverlays().size();j++) {
                            Overlay overlay = map.getOverlays().get(j);
                            map.getOverlays().remove(j);
                        }

                    list.add(search_bar.getText().toString());
                    ArrayList<POI> pois = poiProvider.getPOICloseTo(location,search_bar.getText().toString(), 5, 0.1);
                    FolderOverlay poiMarkers = new FolderOverlay(MainActivity.this);
                    map.getOverlays().add(poiMarkers);
                    Drawable poiIcon = getResources().getDrawable(org.osmdroid.library.R.drawable.bonuspack_bubble);
                    for (POI poi:pois){
                        Marker poiMarker = new Marker(map);
                        poiMarker.setId(search_bar.getText().toString());
                        poiMarker.setTitle(poi.mType);
                        poiMarker.setSnippet(poi.mDescription);
                        poiMarker.setPosition(poi.mLocation);
                        poiMarker.setIcon(poiIcon);
                        if (poi.mThumbnail != null){
                            //poiItem.setImage(new BitmapDrawable(poi.mThumbnail));
                        }
                        poiMarkers.add(poiMarker);

                    }
                } catch (Exception e){
                    Log.d("Djt me" , e.toString());
                }



                map.invalidate();
            }
        });


    }

    public boolean onTouchEvent(MotionEvent touchEvent){
        switch (touchEvent.getAction()){
            case MotionEvent.ACTION_DOWN:
                x1 = touchEvent.getX();
                y1 = touchEvent.getY();
                break;
            case MotionEvent.ACTION_UP:
                x2 = touchEvent.getX();
                y2 = touchEvent.getY();
                if(x1 > x2){
                    Intent intent = new Intent(MainActivity.this, specification.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right,R.anim.stay);
                }
                break;
        }
        return false;
    }

    public void onResume(){
        super.onResume();
        map.onResume();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, myLocationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, myLocationListener);

    }

    public void onPause(){
        super.onPause();
        map.onPause();
    }
    private LocationListener myLocationListener
            = new LocationListener(){
        @Override
        public void onLocationChanged(Location location) {
            // TODO Auto-generated method stub
            if(isTracking) updateLoc(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub
        }
    };

    private Location getLastKnownLocation() {
        mLocationManager = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = mLocationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }
    private void updateLoc(Location loc){
        GeoPoint locGeoPoint = new GeoPoint(loc.getLatitude(), loc.getLongitude());

        waypoints.add(locGeoPoint);
        Road road = roadManager.getRoad(waypoints);
        Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
        map.getOverlays().add(roadOverlay);
        map.invalidate();
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess = true;

        @Override
        protected  void onPreExecute () {
            progress_bluetooth = ProgressDialog.show(MainActivity.this, "Đang kết nối thiết bị", "Xin vui lòng đợi");
        }

        @Override
        protected Void doInBackground (Void... devices) {
            try {
                if ( btSocket==null || !isBtConnected ) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                }
            } catch (IOException e) {
                ConnectSuccess = false;
            }

            return null;
        }

        @Override
        protected void onPostExecute (Void result) {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            } else {
                msg("Connected");
                isBtConnected = true;
                //timer.run();

            }

            progress_bluetooth.dismiss();
        }
    }
    private void msg (String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
    public static void requestBlePermissions(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ActivityCompat.requestPermissions(activity, ANDROID_12_BLE_PERMISSIONS, requestCode);
        else
            ActivityCompat.requestPermissions(activity, BLE_PERMISSIONS, requestCode);
    }
    private static final String[] BLE_PERMISSIONS = new String[]{
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
    };

    private static final String[] ANDROID_12_BLE_PERMISSIONS = new String[]{
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
    };
    public void hold_on(){
        try {
            timer.wait(1000);
        }catch (Exception e){
            e.toString();
        }
    }
    public class Timer extends Thread {
        public void run(){
            InputStream inputStream = null;
            try {
                inputStream = btSocket.getInputStream();
                inputStream.skip(inputStream.available());
                byte[] result = new byte[26];
                for (int i = 0; i < 26; i++) {

                    byte b = (byte) inputStream.read();
                    result[i] = b;
                }
                String str = new String(result);
                if(str!=null) {
                    Log.d("hieuhieu" , str);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}