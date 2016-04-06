package gupuru.mapboxsample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
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

import java.util.ArrayList;
import java.util.List;

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
                public void onMapReady(@NonNull final MapboxMap mapboxMap) {
                    mapBoxMap = mapboxMap;
                    mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(33.583549, 130.393819))
                                    .zoom(14)
                                    .bearing(0)
                                    .tilt(0)
                                    .build()));


                    MarkerOptions markerOptions = new MarkerOptions()
                            .title("ふえええ")
                            .snippet("もう疲れたよ〜")
                            .position(new LatLng(33.583549, 130.393819));

                    Marker marker = new Marker(markerOptions);
                    marker.setId(100);

                    mapboxMap.addMarker(markerOptions);

                    mapBoxMap.setOnInfoWindowClickListener(new MapboxMap.OnInfoWindowClickListener() {
                        @Override
                        public boolean onInfoWindowClick(@NonNull Marker marker) {
                            Toast.makeText(getApplicationContext(), "Window OnClick: " + marker.getTitle(), Toast.LENGTH_LONG).show();
                            return false;
                        }
                    });

                    mapboxMap.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
                        @Override
                        public boolean onMarkerClick(@NonNull Marker marker) {
                            Toast.makeText(getApplicationContext(), "Marker OnClick: " + marker.getTitle(), Toast.LENGTH_LONG).show();
                            return false;
                        }
                    });


                    List<LatLng> line = new ArrayList<>();
                    line.add(new LatLng(33.586319, 130.421886));
                    line.add(new LatLng(33.590018, 130.374715));

//                    mapboxMap.addPolyline(new PolylineOptions().addAll(line).width(1).color(Color.BLUE));
                    mapboxMap.addPolyline(new PolylineOptions().addAll(line));


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
    
}