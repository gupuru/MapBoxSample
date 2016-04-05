package gupuru.mapboxsample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";
    public final static String JSON_CHARSET = "UTF-8";
    public final static String JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME";

    private MapView mapView;
    private MapboxMap mapBoxMap;
    private TextView progressTextView;
    private OfflineManager mOfflineManager;
    private OfflineRegion mOfflineRegion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = (MapView) findViewById(R.id.map_view);
        if (mapView != null) {
            mapView.setAccessToken(ApiAccess.getToken(this));
            mapView.setStyle(Style.MAPBOX_STREETS);
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(@NonNull MapboxMap mapboxMap) {
                    mapBoxMap = mapboxMap;
                    mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(33.583549, 130.393819))
                                    .zoom(14)
                                    .bearing(0)
                                    .tilt(0)
                                    .build()));
                    mapboxMap.addMarker(new MarkerOptions()
                            .position(new LatLng(33.583549, 130.393819))
                            .title("Hello World!")
                            .snippet("Welcome to my marker."));
                }
            });
        }

        mOfflineManager = OfflineManager.getInstance(this);
        mOfflineManager.setAccessToken(ApiAccess.getToken(this));

        progressTextView = (TextView) findViewById(R.id.download_status);

        Button downloadBtn = (Button) findViewById(R.id.download);
        if (downloadBtn != null) {
            downloadBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setupDownloadMap();
                }
            });
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    private void setupDownloadMap() {
        String styleURL = mapBoxMap.getStyleUrl();
        LatLngBounds bounds = mapBoxMap.getProjection().getVisibleRegion().latLngBounds;
    //    bounds.intersect(33.596496, 130.389045, 33.563012, 130.430439);
        double minZoom = mapBoxMap.getCameraPosition().zoom;
        double maxZoom = mapBoxMap.getMaxZoom();
        float pixelRatio = this.getResources().getDisplayMetrics().density;
        OfflineTilePyramidRegionDefinition definition = new OfflineTilePyramidRegionDefinition(
                styleURL, bounds, minZoom, maxZoom, pixelRatio);

        byte[] metadata;
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(JSON_FIELD_REGION_NAME, "Fukuoka");
            String json = jsonObject.toString();
            metadata = json.getBytes(JSON_CHARSET);
        } catch (Exception e) {
            metadata = null;
        }
        if (metadata != null) {
            // Create region
            mOfflineManager.createOfflineRegion(definition, metadata, new OfflineManager.CreateOfflineRegionCallback() {
                @Override
                public void onCreate(OfflineRegion offlineRegion) {
                    mOfflineRegion = offlineRegion;
                    progressTextView.setVisibility(View.VISIBLE);
                    launchDownload();
                }

                @Override
                public void onError(String error) {
                    Log.d(TAG, error);
                }
            });
        }
    }

    private void launchDownload() {
        mOfflineRegion.setObserver(new OfflineRegion.OfflineRegionObserver() {
            @Override
            public void onStatusChanged(OfflineRegionStatus status) {

                double percentage = status.getRequiredResourceCount() >= 0 ?
                        (100.0 * status.getCompletedResourceCount() / status.getRequiredResourceCount()) :
                        0.0;

                if (progressTextView != null) {
                    progressTextView.setText(String.valueOf(percentage) + "%");
                }

                if (status.isComplete()) {
                    Log.d(TAG, "ダウンロード完了");
                    progressTextView.setVisibility(View.GONE);
                    return;
                }

                // Debug
                Log.d(TAG, String.format("%s/%s resources; %s bytes downloaded.",
                        String.valueOf(status.getCompletedResourceCount()),
                        String.valueOf(status.getRequiredResourceCount()),
                        String.valueOf(status.getCompletedResourceSize())));
            }

            @Override
            public void onError(OfflineRegionError error) {
                Log.e(TAG, "onError reason: " + error.getReason());
                Log.e(TAG, "onError message: " + error.getMessage());
            }

            @Override
            public void mapboxTileCountLimitExceeded(long limit) {
                Log.e(TAG, "Mapbox tile count limit exceeded: " + limit);
            }
        });

        mOfflineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE);
    }

    private void checkData() {

        mOfflineManager.listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
            @Override
            public void onList(OfflineRegion[] offlineRegions) {
                // Check result
                if (offlineRegions == null || offlineRegions.length == 0) {
                    return;
                }

                // Get regions info
                for (OfflineRegion offlineRegion : offlineRegions) {
                    offlineRegion.getMetadata();
                }
            }

            @Override
            public void onError(String error) {
                Log.e("ここ", "Error: " + error);
            }

        });


    }

}