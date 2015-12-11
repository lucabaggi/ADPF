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

package it.polimi.proximityapi.DAO;

import it.polimi.geinterface.GroupEntityManager;
import it.polimi.geinterface.DAO.POI;
import it.polimi.proximityapi.TechnologyManager;
import it.polimi.proximityapi.interfaces.ProximityListener;
import it.polimi.proximityapi.proximityproviders.BaseProvider;

import org.altbeacon.beacon.Region;

import android.location.Location;

/**
 * Class defining ProximityData DAO object containing POI data used by {@link TechnologyManager} and {@link BaseProvider}
 */
public class ProximityData {

	/**
	 * Object identifier
	 */
	private String ID;
	
	/**
	 * Relative {@link POI} geolocation
	 */
	private double latitude, longitude;
	
	/**
	 * Radius of the geofence: -1 means "not set"
	 */
	private float radius = -1;
	
	/**
	 * BLE {@link Region} to monitor if {@link #use_ble} (allows to define UUID, major, minor, etc.)
	 */
	private Region BLERegion;
	
	/**
	 * Variable indicating whether the BLE capabilities are required or not for the related {@link POI}
	 */
	private boolean use_ble; 
	
	/**
	 * Variable indicating whether {@link POI} entry/exit events (implemented in {@link #proxListener}) are required or not
	 */
	private boolean need_geofence;
	
	/**
	 * Variable indicating whether currently the selfEntity is inside or outside the related geofence
	 */
	private boolean within_geofence;

	/**
	 * {@link ProximityListener} callback implmented by {@link GroupEntityManager} 
	 * in order to handle proximity and {@link POI} events
	 */
	private ProximityListener proxListener;
	
	public ProximityData(String id) {
		this.ID = id;

		//set variables to default values
		latitude = longitude = -1;
		BLERegion = null;
		need_geofence = false;
		radius = -1;
		proxListener = null;
		use_ble = false;
		within_geofence = false;
	}
	
	
	/**
	 * @return - a {@link Location} object corresponding to related {@link POI}'s center
	 */
	public Location getLocation(){
		Location ret = new Location("");
		ret.setLatitude(latitude);
		ret.setLongitude(longitude);
		return ret;
	}
	

	// ------------------ GETTERS AND SETTERS ------------------------------------

	public double getLatitude() {
		return latitude;
	}


	public ProximityData setLatitude(double latitude) {
		this.latitude = latitude;
		return this;
	}


	public double getLongitude() {
		return longitude;
	}


	public ProximityData setLongitude(double longitude) {
		this.longitude = longitude;
		return this;
	}


	public float getRadius() {
		return radius;
	}


	public ProximityData setRadius(float radius) {
		this.radius = radius;
		return this;
	}


	public Region getBLERegion() {
		return BLERegion;
	}


	public ProximityData setBLERegion(Region bLERegion) {
		BLERegion = bLERegion;
		return this;
	}


	public boolean isUse_ble() {
		return use_ble;
	}
	

	public ProximityData setUse_ble(boolean use_ble) {
		this.use_ble = use_ble;
		return this;
	}

	public boolean isNeed_geofence() {
		return need_geofence;
	}


	public ProximityData setNeed_geofence(boolean need_geofence) {
		this.need_geofence = need_geofence;
		return this;
	}


	public ProximityListener getProxListener() {
		return proxListener;
	}


	public ProximityData setProxListener(ProximityListener proxListener) {
		this.proxListener = proxListener;
		return this;
	}

	public boolean isWithin_geofence() {
		return within_geofence;
	}

	
	public ProximityData setWithin_geofence(boolean within_geofence) {
		this.within_geofence = within_geofence;
		return this;
	}

	public String getID() {
		return ID;
	}
	
	/**
	 * Method that returns the current {@link ProximityData} object informations as a the corresponding {@link POI} object
	 * @return
	 */
	public POI asPOI(){
		POI ret = new POI();
		String UUID = this.BLERegion.getId1() +
				(BLERegion.getId1() == null ? "" : ":"+BLERegion.getId1()) +
				(BLERegion.getId2() == null ? "" : ":"+BLERegion.getId2());
		ret.setBeaconUuid(UUID);
		ret.setName(ID);
		ret.setLatitude(latitude);
		ret.setLongitude(longitude);
		ret.setRadius(radius);
		return ret;
	}
	
}
