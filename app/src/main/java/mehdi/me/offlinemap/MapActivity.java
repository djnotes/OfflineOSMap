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
    private static final String[] PERMISSIONS_STORAGE = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private Context mContext;
    private Activity mActivity = this;
    private MapView mMapView;
    private IMapController mController;
    private MyLocationNewOverlay mMyLocationOverlay;
    private static final String TAG = "MapActivity";
    private View mRootView;
    private OfflineTileProvider offlineProvider;
    private String myTileSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Get context
        mContext = getApplicationContext();

        org.osmdroid.config.Configuration.getInstance().load(mContext, PreferenceManager.getDefaultSharedPreferences(mContext));

        //Inflate and create the map
        mMapView = new MapView(mContext);
        setContentView(mMapView);
        mMapView.setTileSource(TileSourceFactory.HIKEBIKEMAP);

        //Add my location overlay
        mMyLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(mContext), mMapView);
        mMyLocationOverlay.enableMyLocation();
        mMapView.getOverlays().add(mMyLocationOverlay);

        GeoPoint here = mMyLocationOverlay.getMyLocation();
        mController = mMapView.getController();
        mController.setZoom(15.0f);
        mController.setCenter(here);


        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermission();
        }
        else {
            initOfflineMap();
        }




    }

    private void requestStoragePermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Snackbar.make(mRootView, R.string.storage_access_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ActivityCompat.requestPermissions(mActivity, PERMISSIONS_STORAGE, REQUEST_STORAGE);
                        }
                    })
                    .show();
        }
        ActivityCompat.requestPermissions(mActivity, PERMISSIONS_STORAGE, REQUEST_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initOfflineMap();
        }
        else {
            Snackbar.make(mRootView, R.string.permission_not_granted_please_allow, Snackbar.LENGTH_LONG).show();
        }
        //Init offline storage
    }

    private void initOfflineMap() {
        mMapView.setUseDataConnection(false);

        File file;
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
        mMapView.setTileProvider(offlineProvider);
        mMapView.setTileSource(FileBasedTileSource.getSource(myTileSource)); //Set a file-based tile source
        Snackbar.make(findViewById(android.R.id.content), "Using " + file.getAbsolutePath() , Snackbar.LENGTH_LONG).show();
    }

}
