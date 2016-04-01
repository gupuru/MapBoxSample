package gupuru.mapboxsample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    public final static String JSON_CHARSET = "UTF-8";
    public final static String JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME";

    private MapView mapView;
    private MapboxMap mapBoxMap;
    private Button downloadBtn;

    private OfflineManager mOfflineManager;
    private OfflineRegion mOfflineRegion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = (MapView) findViewById(R.id.mapview);
        if (mapView != null) {
            mapView.setStyle(Style.MAPBOX_STREETS);
            mapView.onCreate(savedInstanceState);
        }

        downloadBtn = (Button) findViewById(R.id.download);
        downloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadMap();
            }
        });

        mOfflineManager = OfflineManager.getInstance(this);
        mOfflineManager.setAccessToken("pk.eyJ1Ijoic2VmdXJpa29oZWkiLCJhIjoiY2loenV6bjVxMDRrcXVra290ZW83ZjA0NSJ9.xhF8kOKgGhuE0TCSg90VwA");
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

    private void downloadMap() {
        // Definition
        String styleURL = mapBoxMap.getStyleUrl();
        LatLngBounds bounds = mapBoxMap.getProjection().getVisibleRegion().latLngBounds;
        double minZoom = mapBoxMap.getCameraPosition().zoom;
        double maxZoom = mapBoxMap.getMaxZoom();
        float pixelRatio = this.getResources().getDisplayMetrics().density;
        OfflineTilePyramidRegionDefinition definition = new OfflineTilePyramidRegionDefinition(
                styleURL, bounds, minZoom, maxZoom, pixelRatio);

        // Sample way of encoding metadata from a JSONObject
        byte[] metadata;
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(JSON_FIELD_REGION_NAME, "tokyo");
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
                    launchDownload();
                }

                @Override
                public void onError(String error) {
                    Log.d("ここ", "エラー");
                }
            });
        }
    }

    private void launchDownload() {
        // Set an observer
        mOfflineRegion.setObserver(new OfflineRegion.OfflineRegionObserver() {
            @Override
            public void onStatusChanged(OfflineRegionStatus status) {
                // Compute a percentage
                double percentage = status.getRequiredResourceCount() >= 0 ?
                        (100.0 * status.getCompletedResourceCount() / status.getRequiredResourceCount()) :
                        0.0;

                if (status.isComplete()) {
                    // Download complete
                    Log.d("ここ", "ダウンロード完了");
                    mapView.set
                    return;
                } else if (status.isRequiredResourceCountPrecise()) {
                    // Switch to determinate state
                }

                // Debug
                Log.d("ここ", String.format("%s/%s resources; %s bytes downloaded.",
                        String.valueOf(status.getCompletedResourceCount()),
                        String.valueOf(status.getRequiredResourceCount()),
                        String.valueOf(status.getCompletedResourceSize())));
            }

            @Override
            public void onError(OfflineRegionError error) {
                Log.e("ここ", "onError reason: " + error.getReason());
                Log.e("ここ", "onError message: " + error.getMessage());
            }

            @Override
            public void mapboxTileCountLimitExceeded(long limit) {
                Log.e("ここ", "Mapbox tile count limit exceeded: " + limit);
            }
        });

        // Change the region state
        mOfflineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE);
    }


}
