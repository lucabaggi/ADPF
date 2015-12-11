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

import android.os.Parcel;
import android.os.Parcelable;


/**
 * 
 * Class representing a POI (Point Of Interest)
 *
 */
public class POI implements Parcelable{
	
	private String name;
	private double latitude, longitude;
	private float radius;
	private String beaconUuid;
	
	public POI() {}
	
	public POI(Parcel parcel){
		this.name = parcel.readString();
		this.latitude = parcel.readDouble();
		this.longitude = parcel.readDouble();
		this.radius = parcel.readFloat();
		this.beaconUuid = parcel.readString();
	}
	
	public String getName(){
		return this.name;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public double getLatitude(){
		return this.latitude;
	}
	
	public void setLatitude(double latitude){
		this.latitude = latitude;
	}
	
	public double getLongitude(){
		return this.longitude;
	}
	
	public void setLongitude(double longitude){
		this.longitude = longitude;
	}
	
	public float getRadius(){
		return this.radius;
	}

	public void setRadius(float radius){
		this.radius = radius;
	}

	public String getBeaconUuid() {
		return beaconUuid;
	}

	public void setBeaconUuid(String beaconUuid) {
		this.beaconUuid = beaconUuid;
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
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.name);
		dest.writeDouble(this.latitude);
		dest.writeDouble(this.longitude);
		dest.writeFloat(this.radius);
		dest.writeString(this.beaconUuid);
	}
	
	
	public final static Parcelable.Creator<POI> CREATOR = new Parcelable.Creator<POI>() {

		@Override
		public POI createFromParcel(Parcel source) {
			return new POI(source);
		}

		@Override
		public POI[] newArray(int size) {
			return new POI[size];
		}
	};
}
