package com.example.integrasigooglemap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap gMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng origin; // Start location
    private LatLng destination; // Destination location
    private Marker originMarker;
    private Marker destinationMarker;
    private Button btnMapMode;
    private boolean isSatelliteMode = false;
    private boolean is3DMode = false;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize FusedLocationClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check and request location permission
        checkLocationPermission();

        // Initialize the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.id_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize UI elements
        AutoCompleteTextView searchPlace1 = findViewById(R.id.search_place_1);
        AutoCompleteTextView searchPlace2 = findViewById(R.id.search_place_2);
        Button btnStartDirections = findViewById(R.id.btn_start_directions);
        ProgressBar progressBar = findViewById(R.id.progress_bar);
        btnMapMode = findViewById(R.id.btn_map_mode); // Map mode button

        // Search Place 1 (Origin)
        searchPlace1.setOnEditorActionListener((v, actionId, event) -> {
            String query = searchPlace1.getText().toString().trim();
            if (query.equalsIgnoreCase("here")) {
                // Set current location as origin
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        origin = new LatLng(location.getLatitude(), location.getLongitude());
                        setMarker(origin, "Your Location", true);
                    } else {
                        Toast.makeText(this, "Unable to find your current location", Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (!query.isEmpty()) {
                searchPlace(query, true);
            }
            return false;
        });

        // Search Place 2 (Destination)
        searchPlace2.setOnEditorActionListener((v, actionId, event) -> {
            String query = searchPlace2.getText().toString().trim();
            if (!query.isEmpty()) {
                searchPlace(query, false);
            }
            return false;
        });

        // Start Directions Button
        btnStartDirections.setOnClickListener(v -> {
            if (origin != null && destination != null) {
                calculateRoute(progressBar);
            } else {
                Toast.makeText(this, "Please set both start and destination locations!", Toast.LENGTH_SHORT).show();
            }
        });

        // Toggle map mode button functionality
        btnMapMode.setOnClickListener(v -> {
            if (is3DMode) {
                // Set to Normal mode
                gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                btnMapMode.setText("Satelit");
                is3DMode = false;
                isSatelliteMode = false;
            } else if (isSatelliteMode) {
                // Set to 3D mode if supported
                gMap.setBuildingsEnabled(true); // Enable 3D mode
                btnMapMode.setText("Normal");
                is3DMode = true;
                isSatelliteMode = false;
            } else {
                // Set to Satellite mode
                gMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                btnMapMode.setText("3D");
                isSatelliteMode = true;
            }
        });
    }


    private void searchPlace(String query, boolean isStartLocation) {
        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng location = new LatLng(address.getLatitude(), address.getLongitude());
                setMarker(location, query, isStartLocation);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error finding location", Toast.LENGTH_SHORT).show();
        }
    }

    private void setMarker(LatLng location, String title, boolean isStartLocation) {
        String coordinates = String.format("Lat: %.5f, Lng: %.5f", location.latitude, location.longitude);

        if (isStartLocation) {
            if (originMarker != null) {
                originMarker.remove();
            }
            origin = location;
            originMarker = gMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title(title)
                    .snippet(coordinates)); // Use snippet for coordinates
        } else {
            if (destinationMarker != null) {
                destinationMarker.remove();
            }
            destination = location;
            destinationMarker = gMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title(title)
                    .snippet(coordinates)); // Use snippet for coordinates
        }

        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;

        // Enable zoom controls
        gMap.getUiSettings().setZoomControlsEnabled(true);

        // Enable current location display
        enableUserLocation();

        // Move camera to default location (e.g., Jakarta)
        LatLng defaultLocation = new LatLng(-6.200000, 106.816666);
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation();
            } else {
                Toast.makeText(this, "Location permission is required to use this feature", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            gMap.setMyLocationEnabled(true);
        }
    }

    private void calculateRoute(ProgressBar progressBar) {
        String url = getDirectionsUrl(origin, destination);
        new FetchDirectionsTask(progressBar).execute(url);
    }

    private String getDirectionsUrl(LatLng origin, LatLng destination) {
        return "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&mode=driving" +
                "&key=YOUR_API_KEY"; // Replace YOUR_API_KEY with your actual API key
    }

    private class FetchDirectionsTask extends AsyncTask<String, Void, String> {
        private final ProgressBar progressBar;

        public FetchDirectionsTask(ProgressBar progressBar) {
            this.progressBar = progressBar;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            String data = "";
            try {
                URL url = new URL(params[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder buffer = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }

                data = buffer.toString();
                reader.close();
                inputStream.close();
                connection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressBar.setVisibility(View.GONE);

            try {
                JSONObject jsonObject = new JSONObject(result);
                JSONArray routesArray = jsonObject.getJSONArray("routes");
                if (routesArray.length() > 0) {
                    JSONObject route = routesArray.getJSONObject(0);
                    JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                    String points = overviewPolyline.getString("points");
                    List<LatLng> routePoints = decodePolyline(points);

                    PolylineOptions polylineOptions = new PolylineOptions()
                            .addAll(routePoints)
                            .width(10)
                            .color(getResources().getColor(android.R.color.holo_blue_dark));

                    gMap.addPolyline(polylineOptions);
                } else {
                    Toast.makeText(MainActivity.this, "No route found", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Error parsing route data", Toast.LENGTH_SHORT).show();
            }
        }

        // Method to decode the polyline into LatLng points
        private List<LatLng> decodePolyline(String encoded) {
            List<LatLng> polyline = new ArrayList<>();
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;

            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dLat = (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
                lat += dLat;

                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dLng = (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
                lng += dLng;

                polyline.add(new LatLng((lat / 1E5), (lng / 1E5)));
            }

            return polyline;
        }
    }
}