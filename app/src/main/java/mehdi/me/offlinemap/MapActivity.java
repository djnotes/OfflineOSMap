package mehdi.me.offlinemap;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.modules.ArchiveFileFactory;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class MapActivity extends AppCompatActivity {
    private static final int REQUEST_LOCATION = 100;
    private static final int REQUEST_STORAGE = 101;
    private Context mContext;
    private MapView mMap;
    private IMapController mController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get context
        mContext = getApplicationContext();

        if(ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        }
        if(ActivityCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE);
        }

        org.osmdroid.config.Configuration.getInstance().load(mContext, PreferenceManager.getDefaultSharedPreferences(mContext));


        //Inflate and create the map
        setContentView(R.layout.activity_map);

        mMap = findViewById(R.id.map);
        mMap.setTileSource(TileSourceFactory.MAPNIK);

        GeoPoint tehran = new GeoPoint(35.6892, 51.3890);
        mController = mMap.getController();
        mController.setCenter(tehran);



    }
}
