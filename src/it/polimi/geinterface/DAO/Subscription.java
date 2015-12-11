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
import it.polimi.geinterface.GroupEntityManager;
import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 
 * Class used to represent subscriptions performed through the {@link GroupEntityManager}
 *
 */
public class Subscription implements Parcelable{
	
	private Group g1,g2;
	private DistanceRange distanceForGeofence;	
	
	/**
	 * {@link Entity} used for geofence subscriptions
	 */
	private Entity e1;
	
	/**
	 * {@link ArrayList} of {@link Entity} used to store entities entered in the geofence of a geofence subscription
	 */
	private ArrayList<String> detectedEntities = new ArrayList<String>();
	
	
	
	public Subscription(Entity e1, Group g2, DistanceRange distanceForGeofence) {
		this.e1 = e1;
		this.g2 = g2;
		this.distanceForGeofence = distanceForGeofence;
	}
	
	public Subscription(Group g1, Group g2, DistanceRange distanceForGeofence) {
		this.g1 = g1;
		this.g2 = g2;
		this.distanceForGeofence = distanceForGeofence;
	}
	
	public Subscription(Parcel parcel){
		
		this.distanceForGeofence = DistanceRange.valueOf(parcel.readString());
		
		this.g1 = parcel.readParcelable(Group.class.getClassLoader());
		this.g2 = parcel.readParcelable(Group.class.getClassLoader());
		
	}

	public Group getG1() {
		return g1;
	}

	public Group getG2() {
		return g2;
	}

	public DistanceRange getDistance() {
		return distanceForGeofence;
	}

	
	@Override
	public int describeContents() {
		return 0;
	}
	
	
	public Entity getE1() {
		return e1;
	}

	/**
	 * Method that adds, if possible, a new {@link Entity} to the list of entities entered in a geofence
	 * @return - <code>true</code> if it is addded, <code>false</code> if the limit number is reached
	 */
	public boolean addDetectedEntity(String entityID){
		if(detectedEntities.size() >= 50){
			return false;
		}
		detectedEntities.add(entityID);
		return true;
	}
	
	
	public String getAlreadyDetected(String s){
		int index = detectedEntities.lastIndexOf(s);
		if(index >= 0)
			return detectedEntities.get(index);
		else
			return null;
	}
	
	
	public void removeAlreadyDetected(String id){
		detectedEntities.remove(id);
	}

	@Override
	public String toString() {
		String ret = getG1().toString();
		ret += getG2() == null ? "" : " - " + getG2();
		return ret + " dist: " + distanceForGeofence.name();
	}
	
	
	/*
	 * 
	 * 
	 * 
	 * METHODS FOR PARCELABLE INTERFACE 
	 * 
	 * 
	 * 
	 * 
	 */

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		
		dest.writeString((distanceForGeofence == null) ? "" : distanceForGeofence.name());
		dest.writeParcelable(g1, flags);
		dest.writeParcelable(g2, flags);

	}
	
	
	public final static Parcelable.Creator<Subscription> CREATOR = new Parcelable.Creator<Subscription>() {

		@Override
		public Subscription createFromParcel(Parcel source) {
			return new Subscription(source);
		}

		@Override
		public Subscription[] newArray(int size) {
			return new Subscription[size];
		}
	};

}
