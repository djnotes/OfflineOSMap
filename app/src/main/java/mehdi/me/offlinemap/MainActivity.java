package mehdi.me.offlinemap;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
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

import org.osmdroid.LocationListenerProxy;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.modules.GEMFFileArchive;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.modules.MapTileDownloader;
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider;
import org.osmdroid.tileprovider.modules.MapTileFilesystemProvider;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.modules.NetworkAvailabliltyCheck;
import org.osmdroid.tileprovider.modules.TileWriter;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.MinimapOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.gridlines.LatLonGridlineOverlay2;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, LocationListener {
    private static final int STORAGE_REQUEST = 100;
    private static final int LOCATION_REQUEST = 101;
    private static final String TAG = "MainActivity";
    private Context mContext;
    private MapView mapView;
    private EditText latitude;
    private EditText longitude;
    private Button searchLocation;
    IMapController mapController;

    private MyLocationNewOverlay locationOverlay;

    ArrayList<OverlayItem> items = new ArrayList<>();

    /**
     * Compass overlay
     */
    private CompassOverlay compassOverlay;

    private LatLonGridlineOverlay2 gridlineOverlay;

    private RotationGestureOverlay rotationGestureOverlay;

    private ScaleBarOverlay scaleBarOverlay;

    private MinimapOverlay minimapOverlay;

    private ItemizedIconOverlay<OverlayItem> myLocationOverlay;

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


        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);


        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);


        latitude = findViewById(R.id.latitude);
        longitude = findViewById(R.id.longitude);
        searchLocation = findViewById(R.id.searchLocation);
        searchLocation.setOnClickListener(this);


        mapController = mapView.getController();
        mapController.setZoom(20.0f);

        //Add my location

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);

        LocationManager locMgr = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        String provider = locMgr.getBestProvider(criteria, true);

        Location loc = locMgr.getLastKnownLocation(provider);
        Log.d(TAG, "onCreate: " + loc.getLatitude() + ", " + loc.getLongitude());
        mapController.setCenter(new GeoPoint(loc));

        locMgr.requestLocationUpdates(provider, 1000, 100, this);

        //Add compass overlay
        compassOverlay = new CompassOverlay(mContext, mapView);
        compassOverlay.enableCompass();
        compassOverlay.setEnabled(true);
        mapView.getOverlays().add(compassOverlay);

        //Add gridline overlay
        gridlineOverlay = new LatLonGridlineOverlay2();
//        mapView.getOverlays().add(gridlineOverlay); //Commented out because it's ugly!

        //Add rotation gesture overlay
        rotationGestureOverlay = new RotationGestureOverlay(mapView);
        rotationGestureOverlay.setEnabled(true);
        mapView.setMultiTouchControls(true);
        mapView.getOverlays().add(rotationGestureOverlay);


        //Add map scale bar overlay
        scaleBarOverlay = new ScaleBarOverlay(mapView);
        scaleBarOverlay.setCentred(true);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        scaleBarOverlay.setScaleBarOffset(dm.widthPixels/2,  30);
        mapView.getOverlays().add(scaleBarOverlay);

        items.add(new OverlayItem("Mehdi", "Mehdi Haghgoo", new GeoPoint(loc)));
        myLocationOverlay = new ItemizedIconOverlay<OverlayItem>(items, new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
            @Override
            public boolean onItemSingleTapUp(int i, OverlayItem overlayItem) {
                return false;
            }

            @Override
            public boolean onItemLongPress(int i, OverlayItem overlayItem) {
                return false;
            }
        }, mContext);
        mapView.getOverlays().add(myLocationOverlay);

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
        mapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged: " + location.getLatitude() + ", " + location.getLongitude());
        items.add(new OverlayItem("Here", "New Location", new GeoPoint(location)));
        myLocationOverlay = new ItemizedIconOverlay<OverlayItem>(items, new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
            @Override
            public boolean onItemSingleTapUp(int i, OverlayItem overlayItem) {
                return false;
            }

            @Override
            public boolean onItemLongPress(int i, OverlayItem overlayItem) {
                return false;
            }
        }, mContext);
        mapView.getOverlays().add(myLocationOverlay);

        GeoPoint point = new GeoPoint(location);
        mapController.setCenter(point);
        mapView.invalidate();

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
