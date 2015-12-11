/*
 * Copyright 2015 Luca Baggi, Marco Mezzanotte
 * 
 * This file is part of ADPF.
 *
 *  ADPF is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  ADPF is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with ADPF.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.polimi.proximityapi;


import it.polimi.geinterface.GroupEntityManager;
import it.polimi.geinterface.DAO.JsonStrings;
import it.polimi.geinterface.DAO.POI;
import it.polimi.proximityapi.DAO.ProximityData;
import it.polimi.proximityapi.DAO.ProximityResult;
import it.polimi.proximityapi.interfaces.ActionOutcomeCallback;
import it.polimi.proximityapi.interfaces.PassiveTechnologyListener;
import it.polimi.proximityapi.interfaces.ProximityListener;
import it.polimi.proximityapi.interfaces.ProximityProvider;
import it.polimi.proximityapi.proximityproviders.BLEProximityProvider;
import it.polimi.proximityapi.proximityproviders.BaseProvider;
import it.polimi.proximityapi.proximityproviders.GeofenceProvider;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.widget.Toast;

public class TechnologyManager extends WakefulBroadcastReceiver implements PassiveTechnologyListener{


	private static final String TAG = "TechnologyManager";

	/**
	 * This value represents the defualt radius of the {@link POI} geofence in case it is not set by the client app
	 */
	public static final float BT_START_GEOFENCE_RADIUS = 50;

	private Context appCtx;

	private BLEProximityProvider bleProvider;

	private GeofenceProvider geofenceProvider;

	private Handler mHandler;

	/**
	 * worker thread used to perform long-running tasks
	 */
	HandlerThread workerThread;


	private boolean STARTED = false;

	/**
	 * List of {@link ProximityData} objects submitted by the {@link GroupEntityManager}
	 */
	private ArrayList<ProximityData> proximityData;

	/**
	 * List of proximity providers used by the framework (used to easily extend it in the future)
	 */
	private ArrayList<BaseProvider> proximityProviders;


	public TechnologyManager(Context ctx) {		

		appCtx = ctx;

		//register "this" as BroadcastReceiver for geofence events
		ctx.registerReceiver(this, new IntentFilter(appCtx.getPackageName() + "." + GeofenceProvider.POI_EVENT_BROADCAST_ACTION));


		bleProvider = new BLEProximityProvider(appCtx);
		bleProvider.setTechListener(this);
		geofenceProvider = new GeofenceProvider(ctx);


		proximityProviders = new ArrayList<BaseProvider>();

		proximityProviders.add(bleProvider);
		proximityProviders.add(geofenceProvider);


		proximityData = new ArrayList<ProximityData>();


		//Initialize the worker thread
		workerThread = new HandlerThread("MyLocMgrWorker");
		workerThread.start();
		mHandler = new Handler(workerThread.getLooper());

		//Call init method of each provider used
		for(BaseProvider p : proximityProviders)
			p.init();
	}


	public void stop() {

		if(!STARTED) return;

		//stop all providers used 
		for(ProximityProvider p : proximityProviders)
			p.stop();

		STARTED = false;
		Log.d(TAG, "Stopped");
		appCtx.unregisterReceiver(this);
	}



	/**
	 * Method used to start proximity providers
	 */
	public void startProximiyUpdates() {

		mHandler.post(new Runnable() {

			@Override
			public void run() {
				for(BaseProvider p : proximityProviders)
					p.start();

				STARTED = true;
				Log.i(TAG, "Started");
			}
		});
	}



	/**
	 * This method allows {@link GroupEntityManager} to submit {@link ProximityData} objects as "proximity updates requests"
	 */
	public void registerProximityData(final ProximityData pNew, final ActionOutcomeCallback callback){

		mHandler.post(new Runnable() {

			@Override
			public void run() {

				/*
				 * 
				 * TEST CODE
				 * 


				new Timer().schedule(new TimerTask() {

					@Override
					public void run() {
						Log.w(TAG, "Timer fired!");
						Intent fakeIntent = new Intent(appCtx.getPackageName() + "." + GeofenceProvider.POI_EVENT_BROADCAST_ACTION);
						fakeIntent.putExtra(LocationManager.KEY_PROXIMITY_ENTERING, true);
						fakeIntent.putExtra(JsonStrings.NAME, pNew.getID());
						appCtx.sendBroadcast(fakeIntent);
					}
				}, 15000);

				/*
				new Timer().schedule(new TimerTask() {

					@Override
					public void run() {
						Log.w(TAG, "Timer fired!");
						//fakeProvider.fakeLocationPush(45.347956, 10.880966);
						Intent fakeIntent = new Intent(appCtx.getPackageName() + "." + GeofenceProvider.POI_EVENT_BROADCAST_ACTION);
						fakeIntent.putExtra(LocationManager.KEY_PROXIMITY_ENTERING, false);
						fakeIntent.putExtra(JsonStrings.NAME, pNew.getID());
						appCtx.sendBroadcast(fakeIntent);
					}
				}, 25000);

				 */

				synchronized (TechnologyManager.this) {

					proximityData.add(pNew);		//Save the passed proximitydata

					if(!STARTED)
						Log.w(TAG, TAG + " not yet started!!!");

					//Notify all providers but the BLEProvider (notified upon POI entry)
					for(ProximityProvider p:proximityProviders)
						if(!p.getClass().getName().equalsIgnoreCase(BLEProximityProvider.class.getName()))
							p.onNewProximityRequest(pNew);


					//call the completion callback
					if(callback != null)
						callback.onCompleted();
				}
			}
		});

	}

	/**
	 * Method that removes a {@link ProximityData} previously submitted
	 */
	public void removeProximityData(final ProximityData p, final ActionOutcomeCallback callback){
		if(!proximityData.contains(p)) return;

		mHandler.post(new Runnable() {

			@Override
			public void run() {

				synchronized (TechnologyManager.this) {
					proximityData.remove(p);

					for(ProximityProvider provider : proximityProviders)
						provider.onRemoveProximityRequest(p);

					int BLE_needed = p.isUse_ble() ? 0 : 1;
					if(p.isUse_ble())
						for(ProximityData p1 : proximityData)
							if(p1.isUse_ble()){
								BLE_needed++;
								break;
							}

					if(BLE_needed == 0)		//if no more BLE request have been submitted, stop the provider
						bleProvider.stop();

					//controllo se ci sono altri ProximityData che hanno bisongo della posizione
					Log.w(TAG,"Rimosso proximity data: '" + p.getID() + "'");
					if(proximityData.size() > 0) return;

					// Chiamo la callback 
					if(callback != null)
						callback.onCompleted();
				}

			}
		});
	}



	/*
	 * 
	 * -----------			METHODS CALLED BY PROVIDERS	------------------
	 * 
	 * 
	 */

	@Override
	public synchronized void onProximityChanged(Class<? extends BaseProvider> providerClass,
			ProximityResult result) {


		for(ProximityData p : proximityData){

			//Find the proximity data relative to this update
			if(!p.getID().equalsIgnoreCase(result.proximityDataID))
				continue;

			p.getProxListener().onProximityChanged(result);
		}
	}


	/*
	 * 
	 * -----------			METODI DI DEBUG			------------------
	 * 
	 */
	/*


	 * STRATEGIA DI LOCATION
	 * 
	 * Queste soglie rappresentano la distanza (in metri) dal POI PIU' VICINO che portano a cambiare la
	 * strategia di update della location


	private int TRESHOLD_1 = 1000;			//1km
	private int TRESHOLD_2 = 15 * 1000;		//15 km

	 *//**
	 * Questo metodo contiene la logica di decisione della strategia di update della posizione
	 * da passare al {@link GoogleAPILocationProvider}.
	 * @author marco
	 *//*

	private void updateLocationStrategy(){

		if(proximityData.size() == 0 || true)
			return;

		//determino la distanza dal proximityData più vicino

		//final Location last = googleProvider.getLastLocation();
		final Location last;
		if(USING_MOCK_LOCATION_DATA)
			last = fakeProvider.getLastLocation();
		else
			last = nativeProvider.getLastLocation();

		if(last == null) return;

		Collections.sort(proximityData, new Comparator<ProximityData>() {

			@Override
			public int compare(ProximityData lhs, ProximityData rhs) {
				return Double.compare(getDistance(lhs.getLocation(), last), getDistance(rhs.getLocation(), last));
			}
		});

		double dist = getDistance(proximityData.get(0).getLocation(),last);

		//Se sono all'interno del geofence
		if(dist < proximityData.get(0).getRadius())
			nativeProvider.updateLocationStrategy(NativeProximityProvider.MEDIUM_ACCURACY);
		else
			if(dist <= TRESHOLD_1)
				//googleProvider.setLocationStrategy(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
				nativeProvider.updateLocationStrategy(NativeProximityProvider.HIGH_ACCURACY);
			else
				if(dist > TRESHOLD_1 && dist <= TRESHOLD_2)
					//googleProvider.setLocationStrategy(LocationRequest.PRIORITY_LOW_POWER);
					nativeProvider.updateLocationStrategy(NativeProximityProvider.MEDIUM_ACCURACY);
				else
					//googleProvider.setLocationStrategy(LocationRequest.PRIORITY_NO_POWER);
					nativeProvider.updateLocationStrategy(NativeProximityProvider.LOW_POWER);

	}
	  */

	public static float getDistance(Location l1, Location l2){
		//return getDistance(l1.getLatitude(), l1.getLongitude(),l2.getLatitude(),l1.getLongitude());
		float[] c = new float[2];
		Location.distanceBetween(l1.getLatitude(),l1.getLongitude(), l2.getLatitude(), l2.getLongitude(), c);
		return c[0];
	}

	public static float getDistance(double lat1, double lng1, double lat2, double lng2) {
		float[] c = new float[2];
		Location.distanceBetween(lat1,lng1,lat2,lng2, c);
		return c[0];
	}


	/**
	 * Implementation of BroadcastReceiver methods
	 */

	@Override
	public void onReceive(Context context, Intent intent) {

		//Retrieve POI data from intent
		String proxDataName = intent.getStringExtra(JsonStrings.NAME);
		boolean enterEvent = intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false);
		String log = (enterEvent ? "Entry in " : "Exit from ") + " geofence " + proxDataName;
		Log.i(TAG,log);


		for(ProximityData p : proximityData)
			if(p.getID().equals(proxDataName)){

				if(p.isNeed_geofence()){
					//callback for geofence events
					if(enterEvent){
						p.setWithin_geofence(true);
						p.getProxListener().onEnterGeofenceArea(null);
						bleProvider.onNewProximityRequest(p);
					}else{
						p.setWithin_geofence(false);
						p.getProxListener().onExitGeofenceArea(null);
						bleProvider.onRemoveProximityRequest(p); 
					}
				}
			}
	}
}
