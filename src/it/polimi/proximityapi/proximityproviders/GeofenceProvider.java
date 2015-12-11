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

import java.util.HashMap;
import java.util.Random;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import it.polimi.geinterface.DAO.JsonStrings;
import it.polimi.proximityapi.DAO.ProximityData;

public class GeofenceProvider extends BaseProvider {


	public final static String POI_EVENT_BROADCAST_ACTION = "POI_EVENT_BROADCAST_ACTION";

	private static final String TAG = "GeofenceProvider";

	private LocationManager locManager;

	private Context appCtx;

	private HashMap<ProximityData, PendingIntent> poiIntentMap;


	public GeofenceProvider(Context c) {
		appCtx = c;
		poiIntentMap = new HashMap<ProximityData, PendingIntent>();
	}

	@Override
	public void init() {

		locManager = (LocationManager)appCtx.getSystemService(appCtx.LOCATION_SERVICE);

	}

	@Override
	public void destroy() {
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
		//delete all registered requests to system LocationManager
		for(ProximityData p : poiIntentMap.keySet())
			locManager.removeProximityAlert(poiIntentMap.get(p));
	}


	@Override
	public void onNewProximityRequest(ProximityData data) {

		//Create the intent cotaining POI-specific data used by TechnologyManager to 
		//call proper callbacks
		Intent poiEventIntent = new Intent(appCtx.getPackageName() + "." + POI_EVENT_BROADCAST_ACTION);
		
		poiEventIntent.putExtra(JsonStrings.NAME, data.getID());	//metto l'id del proximityData relativo
		PendingIntent pi = PendingIntent.getBroadcast(appCtx, new Random().nextInt(Integer.MAX_VALUE), poiEventIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		poiIntentMap.put(data, pi);
		locManager.addProximityAlert(data.getLatitude(), data.getLongitude(), data.getRadius(), -1,pi);
	}

	@Override
	public void onRemoveProximityRequest(ProximityData data) {
		if(poiIntentMap.containsKey(data)){
			locManager.removeProximityAlert(poiIntentMap.get(data));
			poiIntentMap.remove(poiIntentMap.get(data));
		}
	}

}
