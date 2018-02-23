package mehdi.me.offlinemap;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.modules.ArchiveFileFactory;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.modules.OfflineTileProvider;
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource;
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
    private Context mContext;
    private MapView mMap;
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
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE);
        }

        org.osmdroid.config.Configuration.getInstance().load(mContext, PreferenceManager.getDefaultSharedPreferences(mContext));


        //Inflate and create the map
        setContentView(R.layout.activity_map);


        mRootView = findViewById(android.R.id.content);

        mMap = findViewById(R.id.map);
//        mMap.setTileSource(TileSourceFactory.MAPNIK);
        mMap.setUseDataConnection(false);


        //Init offline storage
        initOfflineStorage();


        mMap.setMultiTouchControls(true);

        //Add my location overlay
        mMyLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(mContext), mMap);
        mMyLocationOverlay.enableMyLocation();
        mMap.getOverlays().add(mMyLocationOverlay);

        GeoPoint here = mMyLocationOverlay.getMyLocation();
        mController = mMap.getController();
        mController.setZoom(15.0f);
        mController.setCenter(here);
        MapTileProviderBasic a;


    }

    private void initOfflineStorage() {
        //Here, we set up offline map store
        //custom image placeholder for files that aren't available
        mMap.getTileProvider().setTileLoadFailureImage(getResources().getDrawable(R.drawable.notfound));

        //first we'll look at the default location for tiles that we support
        File f = new File(Environment.getExternalStorageDirectory().getPath() + "/osmdroid/");
        Log.d(TAG, "addOverlaysFromOfflineStore: " + f.getAbsolutePath());
        if (f.exists()) {

            File[] list = f.listFiles();
            Log.d(TAG, "initOfflineStorage: Number of files: " + f.length());
            if (list != null) {
                for (File aList : list) {
                    Log.d(TAG, "initOfflineStorage: archive: " + aList.getName());
                    if (aList.isDirectory()) {
                        continue;
                    }
                    String name = aList.getName().toLowerCase();
                    if (!name.contains(".")) {
                        continue; //skip files without an extension
                    }
                    name = name.substring(name.lastIndexOf(".") + 1);
                    if (name.length() == 0) {
                        continue;
                    }
                    if (ArchiveFileFactory.isFileExtensionRegistered(name)) {
                        Log.d(TAG, "initOfflineStorage: isFileExtensionRegistered: " + name+ ":" + ArchiveFileFactory.isFileExtensionRegistered(name) );
                        try {

                            //ok found a file we support and have a driver for the format, for this demo, we'll just use the first one

                            //create the offline tile provider, it will only do offline file archives
                            //again using the first file
                            OfflineTileProvider tileProvider = new OfflineTileProvider(new SimpleRegisterReceiver(this),
                                    new File[]{aList});
                            Log.d(TAG, "initOfflineStorage: Number of offline files: " + tileProvider.getArchives().length );

                            //tell osmdroid to use that provider instead of the default rig which is (asserts, cache, files/archives, online
                            mMap.setTileProvider(tileProvider);

                            //this bit enables us to find out what tiles sources are available. note, that this action may take some time to run
                            //and should be ran asynchronously. we've put it inline for simplicity

                            String source = "";
                            IArchiveFile[] archives = tileProvider.getArchives();
                            if (archives.length > 0) {
                                //cheating a bit here, get the first archive file and ask for the tile sources names it contains
                                Log.d(TAG, "addOverlaysFromOfflineStore: " + archives[0].toString());
                                Set<String> tileSources = archives[0].getTileSources();
                                //presumably, this would be a great place to tell your users which tiles sources are available
                                if (!tileSources.isEmpty()) {
                                    //ok good, we found at least one tile source, create a basic file based tile source using that name
                                    //and set it. If we don't set it, osmdroid will attempt to use the default source, which is "MAPNIK",
                                    //which probably won't match your offline tile source, unless it's MAPNIK
                                    source = tileSources.iterator().next();
                                    Log.d(TAG, "addOverlaysFromOfflineStore: tileSource: " + source);
                                    this.mMap.setTileSource(FileBasedTileSource.getSource(source));
                                } else {
                                    this.mMap.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
                                }

                            } else {
                                this.mMap.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
                            }

                            Snackbar.make(findViewById(android.R.id.content), "Using " + aList.getAbsolutePath() + " " + source, Snackbar.LENGTH_LONG).show();
                            this.mMap.invalidate();
                            return;
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
            Snackbar.make(mRootView, f.getAbsolutePath() + " did not have any files I can open! Try using MOBAC", Snackbar.LENGTH_LONG).show();
            Log.d(TAG, "addOverlaysFromOfflineStore: " + f.getAbsolutePath() + " did not have any files I can open! Try using MOBAC");
        } else {
            Snackbar.make(mRootView, f.getAbsolutePath() + " dir not found!", Snackbar.LENGTH_LONG).show();
            Log.d(TAG, "addOverlaysFromOfflineStore: " + f.getAbsolutePath() + " dir not found!");
        }

        Log.d(TAG, "addOverlaysFromOfflineStore: " + mMap.getTileProvider().getTileSource().toString());

    }
}
