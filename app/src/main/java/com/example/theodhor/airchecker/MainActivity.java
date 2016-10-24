package com.example.theodhor.airchecker;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.example.theodhor.airchecker.Connection.BusProvider;
import com.example.theodhor.airchecker.Connection.Communicator;
import com.example.theodhor.airchecker.Connection.ResponseEvent;
import com.example.theodhor.airchecker.Models.Data;
import com.example.theodhor.airchecker.Models.Value;
import com.example.theodhor.airchecker.Utilities.Constants;
import com.example.theodhor.airchecker.Utilities.GPSTracker;
import com.example.theodhor.airchecker.gcm.RegistrationIntentService;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends ActionBarActivity implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, SearchView.OnQueryTextListener,SwipeRefreshLayout.OnRefreshListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private TextView weatherText, locationText, temperatureText, aqiNumber, apiTime, informationText;
    private ImageView weatherImage, cityCoverPhoto;
    private Data data;
    Integer currentStationIndex;
    private RecyclerView resumeRcv;
    private RecyclerAdapter adapter;
    private LinearLayoutManager linearLayoutManager;
    private List<Parameter> parameterList;
    private ArrayList<String> stationNames;
    private ArrayList<LatLng> stations;
    private GoogleMap mMap;
    private GPSTracker gps;
    private Spinner stationSpinner;
    private ArrayAdapter arrayAdapter;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    PendingResult<LocationSettingsResult> result;
    final static int REQUEST_LOCATION = 199;
    private View aqiView;
    private GradientDrawable th;
    private ProgressDialog progressDialog;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ScrollView scrollView;
    private static final String TAG = "MainActivity";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final int MY_PERMISSIONS_REQUEST_ACCCES_LOCATION = 1;
    private String[] perms = {"android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"};



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle( this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        assert drawer != null;
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        assert navigationView != null;
        navigationView.setNavigationItemSelectedListener(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi( Places.GEO_DATA_API )
                .addApi( Places.PLACE_DETECTION_API )
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();
        mGoogleApiClient.connect();


        stations = new ArrayList<>();
        stations.add(Constants.id_1);
        stations.add(Constants.id_2);
        stations.add(Constants.id_4);
        stations.add(Constants.id_6);
        stations.add(Constants.id_7);
        stations.add(Constants.id_8);

        stationNames = new ArrayList<>();
        stationNames.add("Bregu i Diellit");
        stationNames.add("Ulpiana");
        stationNames.add("Kodra e Trimave");
        stationNames.add("Dodona");
        stationNames.add("Dardania");
        stationNames.add("Qendra");

        weatherText = (TextView)findViewById(R.id.weatherText);
        locationText = (TextView)findViewById(R.id.locationWeatherText);
        temperatureText = (TextView)findViewById(R.id.currentTemperature);
        weatherImage = (ImageView)findViewById(R.id.weatherImage);
        cityCoverPhoto = (ImageView)findViewById(R.id.cityCoverPhoto);
        resumeRcv = (RecyclerView)findViewById(R.id.resumeRcv);
        linearLayoutManager = new LinearLayoutManager(getApplicationContext());

        stationSpinner = (Spinner)findViewById(R.id.placesSpinner);
        arrayAdapter = new ArrayAdapter(this,R.layout.spinner_item,stationNames);
        stationSpinner.setAdapter(arrayAdapter);
        progressDialog = ProgressDialog.show(this, "", "Loading", true,true);
        stationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                progressDialog.show();
                locationText.setText(stationNames.get(position));
                getStationData(position);
                mMap.clear();
                LatLng coordinate = stations.get(position);
                mMap.addMarker(new MarkerOptions().position(coordinate).title(stationNames.get(position)));
                CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 15);
                mMap.animateCamera(yourLocation);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });



        aqiNumber = (TextView)findViewById(R.id.aqiNumber);
        aqiView = (View)findViewById(R.id.aqiView);
        informationText = (TextView)findViewById(R.id.informationText);
        th = ((GradientDrawable)aqiView.getBackground());
        scrollView = (ScrollView)findViewById(R.id.scrollView);

        swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipeRefresh);
        assert swipeRefreshLayout != null;
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Communicator().getData();
            }
        });
        swipeRefreshLayout.setRefreshing(true);
        scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {

            @Override
            public void onScrollChanged() {
                int scrollY = scrollView.getScrollY();
                if (scrollY == 0) swipeRefreshLayout.setEnabled(true);
                else swipeRefreshLayout.setEnabled(false);

            }
        });
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        progressDialog.show();

        gps = new GPSTracker(this);
        float[] distances = new float[stations.size()];
        LatLng currentLatLng = new LatLng(gps.getLatitude(),gps.getLongitude());
        currentStationIndex = 0;

        for (int i=0;i<stations.size();i++){
            distances[i] = distance(currentLatLng,stations.get(i));
        }

        float minDistance = distances[0];
        for(int j=0;j<distances.length;j++){
            if(distances[j]<minDistance){
                minDistance = distances[j];
                currentStationIndex = j;
            }
        }

        getStationData(currentStationIndex);
        stationSpinner.setSelection(currentStationIndex);
    }

    /*private void askPermissionIfNeeded(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.A) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCCES_LOCATION);
            }
        }
    }*/

    private void getStationData(Integer currentStationIndex){
        switch (currentStationIndex){
            case 0:
                new Communicator().getData(1);
                break;
            case 1:
                new Communicator().getData(2);
                break;
            case 2:
                new Communicator().getData(4);
                break;
            case 3:
                new Communicator().getData(6);
                break;
            case 4:
                new Communicator().getData(7);
                break;
            case 5:
                new Communicator().getData(8);
                break;
        }
    }

    private static float distance(LatLng current, LatLng last){
        if(last==null)
            return 0;
        Location cL = new Location("");
        cL.setLatitude(current.latitude);
        cL.setLongitude(current.longitude);

        Location lL = new Location("");
        lL.setLatitude(last.latitude);
        lL.setLongitude(last.longitude);
        return lL.distanceTo(cL);
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(30 * 1000);
        mLocationRequest.setFastestInterval(5 * 1000);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            status.startResolutionForResult(MainActivity.this, REQUEST_LOCATION);
                        } catch (IntentSender.SendIntentException e) {

                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        BusProvider.getInstance().unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BusProvider.getInstance().register(this);
        if(checkPlayServices()){
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        /*MenuItem searchItem = menu.findItem(R.id.search_menu);
        SearchView mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //fabAddPatient.hide();
            }
        });*/
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /*if (id == R.id.search_menu) {
            return true;
        }else if(id == R.id.refresh){
            refresh();
        }*/

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        Intent a = null;
        int id = item.getItemId();
        if(id == R.id.temperatura){
            a = new Intent(this,GraphActivity.class);
            a.putExtra("sensor_id",1);
        }else if(id == R.id.radiacioni){
            a = new Intent(this,GraphActivity.class);
            a.putExtra("sensor_id",2);
        }else if(id == R.id.puhuri){
            a = new Intent(this,GraphActivity.class);
            a.putExtra("sensor_id",3);
        }else if(id == R.id.zhurma){
            a = new Intent(this,GraphActivity.class);
            a.putExtra("sensor_id",4);
        }else if(id == R.id.monoksidiKarbonit){
            a = new Intent(this,GraphActivity.class);
            a.putExtra("sensor_id",5);
        }else if(id == R.id.amonium){
            a = new Intent(this,GraphActivity.class);
            a.putExtra("sensor_id",6);
        }else if(id == R.id.sulfuriHidrogjenit){
            a = new Intent(this,GraphActivity.class);
            a.putExtra("sensor_id",7);
        }else if(id == R.id.dioksidiSulfurit){
            a = new Intent(this,GraphActivity.class);
            a.putExtra("sensor_id",8);
        }else if(id == R.id.dioksidiAzotit){
            a = new Intent(this,GraphActivity.class);
            a.putExtra("sensor_id",9);
        }else if(id == R.id.dioksidiKarbonit){
            a = new Intent(this,GraphActivity.class);
            a.putExtra("sensor_id",10);
        }else if(id == R.id.info){
            showInformationDialog();
        }else if(id == R.id.subscribe){
            a = new Intent(this, SubscribeActivity.class);
            startActivity(a);
            return true;
        }
        if(a != null){
            if(data != null){
                a.putExtra("data", data);
                startActivity(a);
            }else{
                new AlertDialog.Builder(this)
                        .setTitle("Kujdes!")
                        .setMessage("Aktivizoni internetin per te marre te dhenat")
                        .setNegativeButton("Mbylle", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // continue with delete
                            }
                        })
                        .show();
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showInformationDialog(){
        final Dialog informationDialog = new Dialog(this);
        informationDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        informationDialog.setContentView(R.layout.information_dialog);
        informationDialog.show();
    }

    @Subscribe
    public void onResponseEvent(ResponseEvent responseEvent){
        swipeRefreshLayout.setRefreshing(false);
        data = responseEvent.getDataList();
        setAqi(data.getAqi());
        Log.e("This",""+data.getTemperatureData().getValues().size());
        showData(responseEvent.getDataList());
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    private void showData(Data data){
        parameterList = new ArrayList<>();
        parameterList.clear();
        Picasso.with(this).load(data.getPanoramaUrl()).into(cityCoverPhoto);
        weatherText.setText(data.getWeatherSummary());
        //locationText.setText(data.getCity()+"\n"+data.getAddress());
        temperatureText.setText(Math.round(data.getWeatherTemperature()) + data.getTemperatureData().getUnit());
        weatherImage.setImageResource(getResourceId(data.getWeatherIcon().replaceAll("-", "_"), "drawable", getPackageName()));
        informationText.setText(data.getDescription());
        Log.e("This", "" + data.getPanoramaUrl());
        parameterList.add(new Parameter("Radiacioni", String.format("%.02f",           data.getRadiationData().getValues().get(data.getRadiationData().getValues().size() - 1).getValue()),          data.getRadiationData().getUnit(),       2, data, data.getRadiationData().getLimitValue()));
        parameterList.add(new Parameter("Pluhuri",String.format("%.02f",               data.getDustData().getValues().get(data.getDustData().getValues().size()-1).getValue()),                      data.getDustData().getUnit(),            3, data, data.getDustData().getLimitValue().floatValue()));
        parameterList.add(new Parameter("Zhurma",String.format("%.02f",                data.getNoiseData().getValues().get(data.getNoiseData().getValues().size()-1).getValue()),                    data.getNoiseData().getUnit(),           4, data, data.getNoiseData().getLimitValue().floatValue()));
        parameterList.add(new Parameter("Amoniumi",String.format("%.02f",              data.getAmoniumData().getValues().get(data.getAmoniumData().getValues().size()-1).getValue()),                data.getAmoniumData().getUnit(),         5, data, data.getAmoniumData().getLimitValue().floatValue()));
        parameterList.add(new Parameter("Dioksidi i Azotit",String.format("%.02f",     data.getNitrogenDioxideData().getValues().get(data.getNitrogenDioxideData().getValues().size()-1).getValue()),data.getNitrogenDioxideData().getUnit(), 6, data, data.getNitrogenDioxideData().getLimitValue().floatValue()));
        parameterList.add(new Parameter("Dioksidi i Karbonit",String.format("%.02f",   data.getCarbonDioxideData().getValues().get(data.getCarbonDioxideData().getValues().size() - 1).getValue()),  data.getCarbonDioxideData().getUnit(),   7, data, data.getCarbonDioxideData().getLimitValue().floatValue()));
        parameterList.add(new Parameter("Dioksidi i Sulfurit",String.format("%.02f",   data.getSulfurDioxideData().getValues().get(data.getSulfurDioxideData().getValues().size()-1).getValue()),    data.getSulfurDioxideData().getUnit(),   8, data, data.getSulfurDioxideData().getLimitValue().floatValue()));
        parameterList.add(new Parameter("Monoksidi i Karbonit",String.format("%.02f",  data.getCarbonMonoxideData().getValues().get(data.getCarbonMonoxideData().getValues().size()-1).getValue()),  data.getCarbonMonoxideData().getUnit(),  9, data, data.getCarbonMonoxideData().getLimitValue().floatValue()));
        parameterList.add(new Parameter("Sulfuri i Hidrogjenit",String.format("%.02f", data.getHydrogenSulfideData().getValues().get(data.getHydrogenSulfideData().getValues().size()-1).getValue()),data.getHydrogenSulfideData().getUnit(), 10, data, data.getHydrogenSulfideData().getLimitValue().floatValue()));
        adapter = new RecyclerAdapter(parameterList);
        resumeRcv.setLayoutManager(linearLayoutManager);
        resumeRcv.setAdapter(adapter);
        resumeRcv.setNestedScrollingEnabled(false);
        CameraUpdate location = CameraUpdateFactory.newLatLngZoom(new LatLng(data.getLatitude(), data.getLongitude()), 15);
        /*LatLng latLng = new LatLng(data.getLatitude(), data.getLongitude());
        mMap.addMarker(new MarkerOptions().position(latLng).title(locationText.getText().toString()));
        mMap.animateCamera(location);*/
        progressDialog.dismiss();
    }

    private void setAqi(Integer aqi){
        aqiView = (View)findViewById(R.id.aqiView);
        th = ((GradientDrawable)aqiView.getBackground());
        if(aqi>0&&aqi<=50){
            th.setColor(Color.parseColor("#fd0001"));
        }
        if(aqi>50 && aqi <=100){
            th.setColor(Color.parseColor("#fd0001"));
            Log.e("Kjo","True");
        }
        if(aqi>100 && aqi <=150){
            th.setColor(Color.parseColor("#fd0001"));
        }
        if(aqi>150 && aqi <=200){
            th.setColor(Color.parseColor("#fd0001"));
        }
        if(aqi>200 && aqi <=300){
            th.setColor(Color.parseColor("#fd0001"));
        }
        if(aqi>300){
            th.setColor(Color.parseColor("#fd0001"));
        }
        aqiNumber.setText(aqi.toString());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng coordinate = stations.get(currentStationIndex);
        CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 10);
        mMap.animateCamera(yourLocation);
    }


    public int getResourceId(String pVariableName, String pResourcename, String pPackageName){
        try {
            return getResources().getIdentifier(pVariableName, pResourcename, pPackageName);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private void refresh(){
        getStationData(currentStationIndex);
        setAqi(data.getAqi());
        progressDialog.setMessage("Duke u rifreksuar...");
        progressDialog.show();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public void onRefresh() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }


}
