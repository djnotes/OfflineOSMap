package mehdi.me.offlinemap;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.modules.MapTileDownloader;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.util.CloudmadeUtil;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.util.constants.OverlayConstants;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int STORAGE_REQUEST = 100;
    private static final int LOCATION_REQUEST = 101;
    private Context mContext = this;
    private MapView mapView;
    private EditText latitude;
    private EditText longitude;
    private Button searchLocation;
    IMapController mapController;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(mContext, PreferenceManager.getDefaultSharedPreferences(mContext));
        Configuration.getInstance().setUserAgentValue("Dalvik");


        setContentView(R.layout.activity_main);


        //Check Permissions
        if(ActivityCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST);
        }

        if(ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
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
        GeoPoint startPoint = new GeoPoint(35.710551, 51.4129533);
        mapController.setCenter(startPoint);


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //Check and decide
        if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(mContext, R.string.permission_not_granted_please_allow , Toast.LENGTH_LONG).show();
        }


    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.searchLocation) {
            double lat = Double.valueOf( latitude.getText().toString() );
            double lng = Double.valueOf( longitude.getText().toString() ) ;

            GeoPoint geoPoint = new GeoPoint(lat, lng);
            mapController.setCenter(geoPoint);
        }
    }
}
