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

package it.polimi.proximityapi.proximityproviders;

import it.polimi.proximityapi.TechnologyManager;
import it.polimi.proximityapi.DAO.ProximityData;
import it.polimi.proximityapi.DAO.ProximityResult;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BleNotAvailableException;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.RemoteException;
import android.util.Log;

public class BLEProximityProvider extends BaseProvider implements BeaconConsumer {

	private static String TAG = "BLEProximityProvider";

	private Context appCtx;

	/**
	 * This map contains the mapping between {@link Region} unique IDs and {@link ProximityData} defining them,
	 * in order to properly create {@link ProximityResult} objects.
	 */
	private HashMap<String, ProximityData> regionProximityIDMap;

	private BeaconManager mBeaconManager;

	/**
	 * Android system Bluetooth "manager"
	 */
	private BluetoothAdapter bluetoothAdapter;

	private boolean BEACON_SERVICE_CONNECTED = false;

	private BackgroundPowerSaver powerSaver;


	public BLEProximityProvider(Context ctx) {

		powerSaver = new BackgroundPowerSaver(ctx);
		
		this.regionProximityIDMap = new HashMap<String, ProximityData>();
		this.appCtx = ctx;

		if(ctx == null)
			Log.w(TAG, "Context passed is null!");
	}



	/*
	 * 
	 * ----------------		ProximityProvider methods impl	---------------
	 * 
	 */
	@Override
	public void init() {
		
		//AltBeacon library setup
		mBeaconManager = BeaconManager.getInstanceForApplication(appCtx);
		mBeaconManager.setDebug(false);

		//Instruct the library to scan for iBeacon compliant packets
		mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
		mBeaconManager.bind(this);	
		mBeaconManager.setBackgroundMode(false);

		mBeaconManager.setForegroundBetweenScanPeriod(7000l);
		mBeaconManager.setForegroundScanPeriod(2500l);
	}

	@Override
	public void destroy() {
		mBeaconManager.unbind(this);
	}

	@Override
	public void start() {
		try {
			if(!mBeaconManager.checkAvailability()){
				Log.w(TAG, "BLE available but not ENABLED!");
			}
		} catch (BleNotAvailableException e) {
			Log.e(TAG, "BLE not supported by the device");
			return;
		}


		new Thread(new Runnable() {

			@Override
			public void run() {

				//Wait for beacon service connection
				while(!BEACON_SERVICE_CONNECTED){
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				//Once the service connected, start the monitoring of all submitted regions
				Iterator<Map.Entry<String, ProximityData>> set =  regionProximityIDMap.entrySet().iterator();

				if(set.hasNext())
					turnOnBluetooth(); 	//turn on the bluetooth (if needed)

				while(set.hasNext()){
					try {
						mBeaconManager.startMonitoringBeaconsInRegion(set.next().getValue().getBLERegion());
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}

				Log.d(TAG, "BLE start() completed!");
			}
		}).start();

	}

	@Override
	public void stop() {

		try {
			for(Region r : mBeaconManager.getMonitoredRegions())
				mBeaconManager.stopMonitoringBeaconsInRegion(r);
			for(Region r : mBeaconManager.getRangedRegions())
				mBeaconManager.stopRangingBeaconsInRegion(r);
		} catch (RemoteException e) {
			e.printStackTrace();
		}


		Log.d(TAG, "BLE Provider stop() completed!");

		mBeaconManager.unbind(this);
	}

	@Override
	public synchronized void onNewProximityRequest(ProximityData data) {

		if(!BEACON_SERVICE_CONNECTED)
			start();

		if(!mBeaconManager.isBound(this))
			mBeaconManager.bind(this);

		if(data.isUse_ble()){
			if(regionProximityIDMap.containsKey(data.getBLERegion().getUniqueId())){
				Log.w(TAG, "Region already monitored");
				return;
			}

			if(!isBluetoothOn())
				turnOnBluetooth();

			//Store the mapping between regionID-ProximityDataID to build proximityResult correctly
			regionProximityIDMap.put(data.getBLERegion().getUniqueId(), data);

			//Start region monitoring
			try {
				mBeaconManager.startMonitoringBeaconsInRegion(data.getBLERegion());
				Log.d(TAG, "Start monitoring for " + data.getID());
			} catch (RemoteException e) {
				e.printStackTrace();
			}

		}
	}



	@Override
	public synchronized void onRemoveProximityRequest(ProximityData data) {

		if(data.getBLERegion() == null || !data.isUse_ble() ||
				!regionProximityIDMap.containsKey(data.getBLERegion().getUniqueId())){
			Log.w(TAG, "Region not monitored");
			return;
		}

		regionProximityIDMap.remove(data.getBLERegion().getUniqueId());

		try {
			mBeaconManager.stopMonitoringBeaconsInRegion(data.getBLERegion());
			mBeaconManager.stopRangingBeaconsInRegion(data.getBLERegion());
		} catch (RemoteException e) {
			e.printStackTrace();
		}

	}


	/*
	 * 
	 * ----------------  BeaconConsumer	methods	---------------
	 * 
	 */
	@Override
	public Context getApplicationContext() {
		return appCtx.getApplicationContext();
	}

	@Override
	public void onBeaconServiceConnect() {

		Log.e(TAG, "BeaconService connected!");
		BEACON_SERVICE_CONNECTED = true;
		mBeaconManager.setBackgroundMode(false);
		mBeaconManager.setBackgroundScanPeriod(10000);
		mBeaconManager.setBackgroundBetweenScanPeriod(3000); 

		mBeaconManager.setMonitorNotifier(new MonitorNotifier() {

			@Override
			public void didExitRegion(Region reg) {


				/*
				 * Stop beacons ranging until a new didEnterRegion event is fired (otherwise unuseful)
				 */
				try {
					mBeaconManager.stopRangingBeaconsInRegion(reg);
					Log.d(TAG, "Beacon ranging stopped for " + reg.getUniqueId() + ": OUT OF THE REGION!");
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void didEnterRegion(Region reg) {
				//Upon entry a Region to range, start its ranging

				try {
					mBeaconManager.startRangingBeaconsInRegion(reg);
					Log.d(TAG, "Start beacon ranging for region " + reg.getUniqueId());
				} catch (RemoteException e) {
					e.printStackTrace();
				}

			}

			@Override
			public void didDetermineStateForRegion(int arg0, Region arg1) {
			}
		});


		mBeaconManager.setRangeNotifier(new RangeNotifier() {

			@Override
			public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region reg) {

				
				synchronized (BLEProximityProvider.this) {

					//if no requests submitted for dected beacons
					if(!regionProximityIDMap.containsKey(reg.getUniqueId())) return;

					ProximityResult res = new ProximityResult().setBle(true)
							.setProxDataID(regionProximityIDMap.get(reg.getUniqueId()).getID())
							.setVisibleBeacons(beacons).setProximityUpdate(true);

					techListener.onProximityChanged(BLEProximityProvider.this.getClass(),res);
				}
			}
		});

	}



	@Override
	public void unbindService(ServiceConnection arg0) {
		BEACON_SERVICE_CONNECTED = false;
		appCtx.unbindService(arg0);

	}

	@Override
	public boolean bindService(Intent arg0, ServiceConnection arg1, int arg2) {
		return appCtx.bindService(arg0, arg1, arg2);
	}


	/*
	 * ------------------	UTILITY METHODS	------------------
	 *
	 */



	private void turnOnBluetooth(){
		if(bluetoothAdapter == null)
			bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(!bluetoothAdapter.isEnabled())
			bluetoothAdapter.enable();
	}

	private boolean isBluetoothOn(){
		if(bluetoothAdapter == null)
			bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		return bluetoothAdapter.isEnabled();
	}


}
