package mehdi.me.offlinemap;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, LocationListener {
    private static final int STORAGE_REQUEST = 100;
    private static final int LOCATION_REQUEST = 101;
    private static final String TAG = "MainActivity";
    private Context mContext;
    private MapView mMap;
    private EditText latitude;
    private EditText longitude;
    private Button searchLocation;
    IMapController mapController;

    private MyLocationNewOverlay locationOverlay;

    ArrayList<OverlayItem> items = new ArrayList<>();

    /**
     * Compass overlay
     */
    private CompassOverlay mCompassOverlay;


    private RotationGestureOverlay mRotationOverlay;

    private ScaleBarOverlay mScaleBarOverlay;


    private MyLocationNewOverlay mMyLocationOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getApplicationContext();

        Configuration.getInstance().load(mContext, PreferenceManager.getDefaultSharedPreferences(mContext));
        Configuration.getInstance().setUserAgentValue("Dalvik");


        setContentView(R.layout.activity_main);


        //Check Permissions
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST);
        }

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
        }


        mMap = findViewById(R.id.mapView);
        mMap.setTileSource(TileSourceFactory.MAPNIK);


        mMap.setBuiltInZoomControls(true);
        mMap.setMultiTouchControls(true);


        latitude = findViewById(R.id.latitude);
        longitude = findViewById(R.id.longitude);
        searchLocation = findViewById(R.id.searchLocation);
        searchLocation.setOnClickListener(this);


        mapController = mMap.getController();
        mapController.setZoom(9.0f);

        //Add my location

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);

        final LocationManager locMgr = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        final String provider = locMgr.getBestProvider(criteria, true);
        locMgr.requestLocationUpdates(provider, 1000, 100, this);

        Location loc = locMgr.getLastKnownLocation(provider);
        Log.d(TAG, "onCreate: " + loc.getLatitude() + ", " + loc.getLongitude());

        //Start on Iran
        GeoPoint iran = new GeoPoint(32.4279, 53.6880);
        mapController.setCenter(iran);


        //Add my location overlay
        mMyLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(mContext), mMap);
        mMyLocationOverlay.enableMyLocation();
        mMap.getOverlays().add(mMyLocationOverlay);

        //Add compass overlay
        mCompassOverlay = new CompassOverlay(mContext, mMap);
        mCompassOverlay.enableCompass();
        mCompassOverlay.setEnabled(true);
        mMap.getOverlays().add(mCompassOverlay);



        //Add rotation gesture overlay
        mRotationOverlay = new RotationGestureOverlay(mMap);
        mRotationOverlay.setEnabled(true);
        mMap.setMultiTouchControls(true);
        mMap.getOverlays().add(mRotationOverlay);


        //Add map scale bar overlay
        mScaleBarOverlay = new ScaleBarOverlay(mMap);
        mScaleBarOverlay.setCentred(true);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 30);
        mMap.getOverlays().add(mScaleBarOverlay);



    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //Check and decide
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(mContext, R.string.permission_not_granted_please_allow, Toast.LENGTH_LONG).show();
        }

    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.searchLocation) {
            double lat = Double.valueOf(latitude.getText().toString());
            double lng = Double.valueOf(longitude.getText().toString());

            GeoPoint geoPoint = new GeoPoint(lat, lng);
            mapController.setCenter(geoPoint);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMyLocationOverlay.setEnabled(false);
        mMap.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMyLocationOverlay.setEnabled(true);
        mMap.onResume();

    }

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }
}
