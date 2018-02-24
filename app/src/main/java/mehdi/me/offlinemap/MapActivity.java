package mehdi.me.offlinemap;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.modules.ArchiveFileFactory;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.modules.MapTileSqlCacheProvider;
import org.osmdroid.tileprovider.modules.OfflineTileProvider;
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.util.Set;

public class MapActivity extends AppCompatActivity {
    private static final int REQUEST_LOCATION = 100;
    private static final int REQUEST_STORAGE = 101;
    private static final String MAP_FILE = "Iran.sqlite";
    private Context mContext;
    private Activity mActivity = this;
    private MapView mMapView;
    private IMapController mController;
    private MyLocationNewOverlay mMyLocationOverlay;
    private static final String TAG = "MapActivity";
    private View mRootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get context
        mContext = getApplicationContext();

        if(ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        }
        if(ActivityCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE);
        }

        org.osmdroid.config.Configuration.getInstance().load(mContext, PreferenceManager.getDefaultSharedPreferences(mContext));


        //Inflate and create the map
        mMapView = new MapView(mContext);
        setContentView(mMapView);

//        mRootView = findViewById(android.R.id.content);
//
////        mMapView = findViewById(R.id.map);
////        mMapView.setTileSource(TileSourceFactory.MAPNIK);
//        mMapView.setUseDataConnection(false);
//
//        //Add my location overlay
//        mMyLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(mContext), mMapView);
//        mMyLocationOverlay.enableMyLocation();
//        mMapView.getOverlays().add(mMyLocationOverlay);
//
//        GeoPoint here = mMyLocationOverlay.getMyLocation();
//        mController = mMapView.getController();
//        mController.setZoom(15.0f);
//        mController.setCenter(here);
        new OfflineMap().execute();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //Init offline storage

    }

    private class OfflineMap extends AsyncTask<Void, Void, Void> {
            String myTileSource = "";
            OfflineTileProvider offlineProvider;

            File file = null;
        @Override
        protected Void doInBackground(Void... voids) {
            mMapView.setUseDataConnection(false);

            //first we'll look at the default location for tiles that we support
            file = new File(Environment.getExternalStorageDirectory().getPath() + "/osmdroid/" + MAP_FILE);
            Log.d(TAG, "doInBackground: file exists: " + file.exists());
            offlineProvider = null;
            try {
                offlineProvider = new OfflineTileProvider(new SimpleRegisterReceiver(mContext), new File[] {file});
            mMapView.setTileProvider(offlineProvider);
            IArchiveFile[] fileArchives = offlineProvider.getArchives();
            Log.d(TAG, "doInBackground: archives: " + fileArchives.length);
            Set<String> myTileSources = fileArchives[0].getTileSources();
                Log.d(TAG, "doInBackground: tile sources: " + myTileSources.size());
            myTileSource = myTileSources.iterator().next(); //Get the first item
                Log.d(TAG, "doInBackground: myTileSource: " + myTileSource);
            } catch (Exception e) {
                Log.e(TAG, "addOverlays: offline provider error " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(mMapView == null) {
                finishActivity(100);
                return;
            }
            mMapView.setTileProvider(offlineProvider);
            mMapView.setTileSource(FileBasedTileSource.getSource(myTileSource)); //Set a file-based tile source
            Snackbar.make(findViewById(android.R.id.content), "Using " + file.getAbsolutePath() , Snackbar.LENGTH_LONG).show();
        }
    }
}
