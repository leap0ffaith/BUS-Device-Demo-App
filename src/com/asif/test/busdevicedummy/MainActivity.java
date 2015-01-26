package com.asif.test.busdevicedummy;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity {

	LocationManager mLocationManager ;
	private Location networkLocation = null;
    private Location gpsLocation = null;
	protected boolean sendingRunning = false;
	private LocationListener gpsLocationListener;
	private LocationListener networkLocationListener;
    private static final int HALF_MINUTE = 1000 * 30;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		if(!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
		{
			displayLocationAccessDialog();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		startLocationListeners();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		unRegisterListeners();
	}
	
	private void displayLocationAccessDialog(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setMessage(R.string.gps_network_not_enabled);
        dialog.setPositiveButton(R.string.enable, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });
        dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
            }
        });
        dialog.show();
    }
	
	private void startLocationListeners() 
	{
		gpsLocationListener = new LocationListener() {
			
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
			}
			
			@Override
			public void onProviderEnabled(String provider) {
			}
			
			@Override
			public void onProviderDisabled(String provider) {
			}
			
			@Override
			public void onLocationChanged(Location location) {
				Log.d("GPS_DATA", "Lat/Lon: "+ location.getLatitude()+" , " + location.getLongitude() + ". Accuracy: "+ location.getAccuracy() + ". Taken " + (System.currentTimeMillis() - location.getTime()) + " milliSec ago.");
				if(isBetterLocation(location, gpsLocation))
				{
					gpsLocation = location;
					onLocationUpdate();
					
				}
			}
		};
		
		networkLocationListener = new LocationListener() {
			
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
			}
			
			@Override
			public void onProviderEnabled(String provider) {
			}
			
			@Override
			public void onProviderDisabled(String provider) {
			}
			
			@Override
			public void onLocationChanged(Location location) {
				Log.d("NETWORK_DATA", "Lat/Lon: "+ location.getLatitude()+" , " + location.getLongitude() + ". Accuracy: "+ location.getAccuracy() + ". Taken " + (System.currentTimeMillis() - location.getTime()) + " milliSec ago.");
				if(isBetterLocation(location, gpsLocation))
				{
					networkLocation = location;
					onLocationUpdate();
				}
			}
		};
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, gpsLocationListener);
		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000, 0, networkLocationListener);
	}
	
	private void unRegisterListeners() {
		mLocationManager.removeUpdates(gpsLocationListener);
		mLocationManager.removeUpdates(networkLocationListener);
	}

	protected void onLocationUpdate() 
	{
		Location bestLocation = selectBestLocationForSending(); 
		TextView deviceStatusTV = (TextView) findViewById(R.id.status_text_view);
		TextView latLongTV = (TextView) findViewById(R.id.lat_lon_text_view);
		TextView speedTV = (TextView) findViewById(R.id.speed_text_view);
		deviceStatusTV.setText(R.string.running);
		latLongTV.setText("provider: "+ bestLocation.getProvider() + " Lat, long: " + bestLocation.getLatitude() +","+bestLocation.getLongitude());
		speedTV.setText("speed: "+bestLocation.getSpeed()+" accuracy: "+bestLocation.getAccuracy());
		sendLocation(bestLocation);
	}
	
	private void sendLocation(Location selectBestLocationForSending) {
		//TODO 
	}

	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
	    if (currentBestLocation == null) {
	        // A new location is always better than no location
	        return true;
	    }

	    // Check whether the new location fix is newer or older
	    long timeDelta = location.getTime() - currentBestLocation.getTime();
	    boolean isSignificantlyNewer = timeDelta > HALF_MINUTE;
	    boolean isSignificantlyOlder = timeDelta < -HALF_MINUTE;
	    boolean isNewer = timeDelta > 0;

	    // If it's been more than two minutes since the current location, use the new location
	    // because the user has likely moved
	    if (isSignificantlyNewer) {
	        return true;
	    // If the new location is more than two minutes older, it must be worse
	    } else if (isSignificantlyOlder) {
	        return false;
	    }

	    // Check whether the new location fix is more or less accurate
	    int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
	    boolean isLessAccurate = accuracyDelta > 0;
	    boolean isMoreAccurate = accuracyDelta < 0;
	    boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	    // Check if the old and new location are from the same provider
	    boolean isFromSameProvider = isSameProvider(location.getProvider(),
	            currentBestLocation.getProvider());

	    // Determine location quality using a combination of timeliness and accuracy
	    if (isMoreAccurate) {
	        return true;
	    } else if (isNewer && !isLessAccurate) {
	        return true;
	    } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
	        return true;
	    }
	    return false;
	}
	
	private boolean isSameProvider(String provider1, String provider2) {
	    if (provider1 == null) {
	      return provider2 == null;
	    }
	    return provider1.equals(provider2);
	}
	
	private Location selectBestLocationForSending() {
		if(gpsLocation != null && System.currentTimeMillis() - gpsLocation.getTime() <= 5000 && gpsLocation.getAccuracy() <= 30)
			return gpsLocation;
		if(networkLocation != null && System.currentTimeMillis() - networkLocation.getTime() <= 5000 && networkLocation.getAccuracy() <= 30)
			return networkLocation;
		if(isBetterLocation(gpsLocation, networkLocation))
			return gpsLocation;
		else
			return networkLocation;
	}
}
