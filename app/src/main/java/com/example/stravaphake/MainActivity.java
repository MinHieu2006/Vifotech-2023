package com.example.stravaphake;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.location.GeoNamesPOIProvider;
import org.osmdroid.bonuspack.location.NominatimPOIProvider;
import org.osmdroid.bonuspack.location.OverpassAPIProvider;
import org.osmdroid.bonuspack.location.POI;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.PointL;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // Zone for variable
    private static final String LOG_TAG = "a";
    String address = "00:22:12:01:8D:FB";
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);


        super.onCreate(savedInstanceState);
        requestBlePermissions(this,1);
        myBluetooth = BluetoothAdapter.getDefaultAdapter();
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

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        MyLocationNewOverlay mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this),map);
        mLocationOverlay.enableMyLocation();
        map.getOverlays().add(mLocationOverlay);
        IMapController mapController = map.getController();
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        RoadManager roadManager = new OSRMRoadManager(this, "osmbonuspack_6.9.0");
        // 16.064950099999997, 108.15898268465547
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location lastKnownLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastKnownLoc != null) {
            double longitude = (double) (lastKnownLoc.getLongitude()) ;
            double latitude = (double) (lastKnownLoc.getLatitude() );
            GeoPoint location = new GeoPoint(latitude, longitude);
            mapController.setCenter(location);
            mapController.setZoom(19.5);
            geoPointList.add(location);

            // tìm đường đi
            ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
            waypoints.add(location);
            GeoPoint endPoint = new GeoPoint(16.063037876575265, 108.15760859363031);
            waypoints.add(endPoint);
            Road road = roadManager.getRoad(waypoints);
            Polyline roadOverlay = RoadManager.buildRoadOverlay(road);


            Drawable nodeIcon = getResources().getDrawable(org.osmdroid.library.R.drawable.ic_menu_compass);
            for (int i=0; i<road.mNodes.size(); i++){
                RoadNode node = road.mNodes.get(i);
                Marker nodeMarker = new Marker(map);
                nodeMarker.setPosition(node.mLocation);
                nodeMarker.setIcon(nodeIcon);
                nodeMarker.setTitle("Step "+i);
                map.getOverlays().add(nodeMarker);
            }

            NominatimPOIProvider poiProvider = new NominatimPOIProvider("osmbonuspack_6.9.0");
            ArrayList<POI> pois = poiProvider.getPOICloseTo(location, "Trường học", 50, 0.1);
            FolderOverlay poiMarkers = new FolderOverlay(this);
            map.getOverlays().add(poiMarkers);
            Drawable poiIcon = getResources().getDrawable(org.osmdroid.library.R.drawable.bonuspack_bubble);
            for (POI poi:pois){
                Marker poiMarker = new Marker(map);
                poiMarker.setTitle(poi.mType);
                poiMarker.setSnippet(poi.mDescription);
                poiMarker.setPosition(poi.mLocation);
                poiMarker.setIcon(poiIcon);
                if (poi.mThumbnail != null){
                    //poiItem.setImage(new BitmapDrawable(poi.mThumbnail));
                }
                poiMarkers.add(poiMarker);

            }
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


            MapEventsOverlay OverlayEvents = new MapEventsOverlay(getBaseContext(), mReceive);
            map.getOverlays().add(OverlayEvents);

            //

            map.getOverlays().add(roadOverlay);
            map.invalidate();
        }






//        IMapController mapController = map.getController();
//

//        //map.getController().setCenter(mLocationOverlay.getMyLocation());
//        mapController.setCenter(mLocationOverlay.getMyLocation());
//        mapController.setZoom(5.5);
//        //map.getController().setZoom(5.5);
////        GeoPoint tmp = new GeoPoint(1,1);
////        tmp = mLocationOverlay.getMyLocation();

        //mapController.setZoom(12);


    }
    public void onResume(){
        super.onResume();
        map.onResume();

    }

    public void onPause(){
        super.onPause();
        map.onPause();
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
