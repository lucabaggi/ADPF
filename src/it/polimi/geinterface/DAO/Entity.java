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


package it.polimi.geinterface.DAO;



import it.polimi.geinterface.DistanceRange;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * Class that represents the Entity abstraction (every actor of the network, both devices and beacons)
 *
 */
public class Entity implements Parcelable{

	private static final String TAG ="Entity";

	private String entityID;
	private Type entityType;
	private DistanceRange distanceRange;

	private JSONObject properties;

	private Entity(Type t) {
		properties = new JSONObject();
		properties.put(JsonStrings.PROPERTIES, new JSONObject());
		this.entityType = t;
	}
	
	public Entity(Parcel parcel){
		this.entityID = parcel.readString();
		this.entityType = Type.valueOf(parcel.readString());
		this.distanceRange = DistanceRange.valueOf(parcel.readString());
		
		JSONParser parser = new JSONParser();
		try {
			this.properties = (JSONObject) parser.parse(parcel.readString());
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	public String getEntityID() {
		return entityID;
	}

	private void setEntityID(String entityID) {
		this.entityID = entityID;
	}

	/**
	 * returns the properties of the Entity
	 *
	 */
	public JSONObject getProperties(){
		return properties;

	}

	public void setProperties(JSONObject properties){
		this.properties = properties;
	}

	public Type getEntityType() {
		return entityType;
	}

	public DistanceRange getDistanceRange() {
		return distanceRange;
	}

	public void setDistanceRange(DistanceRange distanceRange) {
		this.distanceRange = distanceRange;
	}


	/**
	 * 
	 * @return a {@link JSONObject} describing the {@link Entity}
	 */
	public JSONObject getJsonDescriptor(){

		String jsonDescription = "{\"" + JsonStrings.ENTITY + "\":"
				+ "{\"" + JsonStrings.ENTITY_ID + "\":\"" + this.getEntityID() + "\","
				+ "\"" + JsonStrings.ENTITY_TYPE + "\":\"" + this.getEntityType().name() + "\","
				+ "\"" + JsonStrings.DISTANCE_RANGE + "\":\"" + this.getDistanceRange().name() + "\","
				+ "\"" + JsonStrings.PROPERTIES + "\":" + this.getProperties() + "}}";

		JSONParser parser = new JSONParser();
		try {
			return ((JSONObject)parser.parse(jsonDescription));
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public boolean equals(Object o) {
		if(o == null || !(o instanceof Entity))
			return false;

		if(o == this)
			return true;

		return this.getEntityID().equalsIgnoreCase(((Entity)o).getEntityID());
	}




	/**
	 * Builder used to create an {@link Entity}
	 *
	 */
	public static class Builder{
		Entity ret;

		public Builder(String id, Type t) {
			ret = new Entity(t);
			ret.setEntityID(id.toLowerCase());
			ret.setDistanceRange(DistanceRange.UNKNOWN);
		}

		public Builder addProperties(JSONObject properties) {
			if(properties.keySet().contains(JsonStrings.PROPERTIES)
					&& properties.keySet().size() == 1
					&& (properties.get(JsonStrings.PROPERTIES) instanceof JSONObject))
				ret.setProperties(properties);
			else
				Log.e(TAG, "Properties not valid");
			return this;
		}

		public Builder setDistance(DistanceRange distanceRange){
			ret.setDistanceRange(distanceRange);
			return this;
		}

		public Entity build(){
			return ret;
		}

	}
	

	public static JSONObject wrapJSONProperties(JSONObject obj){
		JSONObject ret = new JSONObject();
		ret.put(JsonStrings.PROPERTIES, obj);
		return ret;
	}
	
	
	public static String getBeaconNameFromUuid(String beaconId){
		
		switch (beaconId.toUpperCase()) {
		
		case "B9407F30-F5F8-466E-AFF9-25556B57FE6D:42730:37336":
			return "Ice Beacon";
			
		case "B9407F30-F5F8-466E-AFF9-25556B57FE6D:34061:44153":
			return "Blueberry Beacon";
			
		case "B9407F30-F5F8-466E-AFF9-25556B57FE6D:48147:52400":
			return "Mint Beacon";
			
		default:
			return beaconId;
		}
	}
	

	/**
	 * {@link Enum} che definisce i tipi di {@link Entity} possibili/gestibili
	 * @author marco
	 *
	 */
	public static enum Type{
		DEVICE,
		BLE_BEACON,
		ALL
	}

	
	/*
	 * 
	 * 
	 * METHODS FOR PARCELABLE INTERFACE
	 * 
	 * 
	 * 
	 */

	
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.entityID);
		dest.writeString((this.entityType == null) ? "" : this.entityType.name());
		dest.writeString((this.distanceRange == null) ? "" : this.distanceRange.name());
		dest.writeString(this.properties.toString());
	}
	
	public final static Parcelable.Creator<Entity> CREATOR = new Parcelable.Creator<Entity>() {

		@Override
		public Entity createFromParcel(Parcel source) {
			return new Entity(source);
		}

		@Override
		public Entity[] newArray(int size) {
			return new Entity[size];
		}
	};
}







