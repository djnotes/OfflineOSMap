package mehdi.me.offlinemap;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
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
import org.osmdroid.tileprovider.modules.ArchiveFileFactory;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.modules.OfflineTileProvider;
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
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
    private View mRootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getApplicationContext();

        Configuration.getInstance().load(mContext, PreferenceManager.getDefaultSharedPreferences(mContext));
        Configuration.getInstance().setUserAgentValue("Dalvik");


        setContentView(R.layout.activity_main);

        //Set root view
        mRootView = findViewById(android.R.id.content);


        mMap = findViewById(R.id.mapView);


        mMap.setBuiltInZoomControls(true);
        mMap.setMultiTouchControls(true);

        //Disable data
        mMap.setUseDataConnection(false);


        latitude = findViewById(R.id.latitude);
        longitude = findViewById(R.id.longitude);
        searchLocation = findViewById(R.id.searchLocation);
        searchLocation.setOnClickListener(this);


        mapController = mMap.getController();
        mapController.setZoom(9.0f);



        //Here, we set up offline map store
        //custom image placeholder for files that aren't available
        mMap.getTileProvider().setTileLoadFailureImage(getResources().getDrawable(R.drawable.notfound));

        //first we'll look at the default location for tiles that we support
        File f = new File(Environment.getExternalStorageDirectory().getPath() + "/osmdroid/");
        Log.d(TAG, "addOverlaysFromOfflineStore: " + f.getAbsolutePath());
        if (f.exists()) {

            File[] list = f.listFiles();
            if (list != null) {
                for (File aList : list) {
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
                        try {

                            //ok found a file we support and have a driver for the format, for this demo, we'll just use the first one

                            //create the offline tile provider, it will only do offline file archives
                            //again using the first file
                            OfflineTileProvider tileProvider = new OfflineTileProvider(new SimpleRegisterReceiver(this),
                                    new File[]{aList});

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





        //Start on Tehran
        GeoPoint tehran = new GeoPoint(35.6892, 51.3890);
        mapController.setCenter(tehran);


        //Add my location overlay
        OverlayItem newItem = new OverlayItem("Here", "You are here", new GeoPoint(tehran));
        items.add(newItem);

        ItemizedIconOverlay<OverlayItem> myLocOverlay = new ItemizedIconOverlay<OverlayItem>(items,
                getResources().getDrawable(R.drawable.my_location, null),
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(int index, OverlayItem item) {
                        return false;
                    }

                    @Override
                    public boolean onItemLongPress(int index, OverlayItem item) {
                        return false;
                    }
                },
        mContext);
        mMap.getOverlays().add(myLocOverlay);



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
        mMap.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMap.onResume();
    }
}

