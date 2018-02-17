package mehdi.me.offlinemap;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.FrameLayout;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_CODE = 100;
    private Context mContext = this;
    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handleDangerousPermissions();

        Configuration.getInstance().load(mContext, PreferenceManager.getDefaultSharedPreferences(mContext));
        mapView = new MapView(mContext);
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        FrameLayout mapFrame = findViewById(R.id.mapFrame);
        mapFrame.addView(mapView);


        //Add zoom control
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        //Access map controller
        IMapController mapController = mapView.getController();
        mapController.setZoom(9);
        GeoPoint startPoint = new GeoPoint(35.7078731, 51.412981);
        mapController.setCenter(startPoint);

    }

    private void handleDangerousPermissions() {
        String permissions [] = new String[3];
        int i = 0;
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            permissions[i++] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            permissions[i++] = Manifest.permission.ACCESS_FINE_LOCATION;
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            permissions[i] = Manifest.permission.ACCESS_COARSE_LOCATION;

        //Request permissions
        if(permissions.length > 0) {
            ActivityCompat.requestPermissions(MainActivity.this, permissions, PERMISSIONS_CODE);
        }

    }
}
