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

import it.polimi.geinterface.DAO.Entity.Type;
import it.polimi.geinterface.filter.PropertiesFilter;

import java.util.StringTokenizer;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * 
 * Class representing a group of {@link Entity}, used to define the filtering for a {@link Subscription}
 *
 */
public class Group implements Parcelable{

	private PropertiesFilter filter;

	private String entity_id;
	private Type type;
	
	private String groupDesc = "";


	private Group(){}
	
	private Group(Parcel source){
		try {
			String filterTemp =source.readString();
			this.filter = filterTemp.equalsIgnoreCase("") ? null : PropertiesFilter.parseFromString(filterTemp);
			this.entity_id = source.readString();
			this.type = Type.valueOf(source.readString());
			this.groupDesc = source.readString();
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			
		}
	}

	/**
	 * Method that checks if an {@link Entity} belongs to the {@link Group}
	 * @param e - the {@link Entity} that is evaluated
	 * @return <code>true</code> if the {@link Entity} belongs to the {@link Group}, otherwise <code>false</code>
	 */ 
	public boolean evaluate(Entity e){

		if(!this.entity_id.equals("")){
			String entityId = this.entity_id;

			if(e.getEntityType().equals(Entity.Type.BLE_BEACON)){

				StringTokenizer receivedTokenizer = new StringTokenizer(e.getEntityID(),":");
				String receivedUUID = receivedTokenizer.nextToken();
				StringTokenizer filterTokenizer = new StringTokenizer(entityId,":");
				String filterUUID = filterTokenizer.nextToken();

				if(!filterUUID.equalsIgnoreCase(receivedUUID))
					return false;

				if(!filterTokenizer.hasMoreElements())	
					return true;

				//Extraction of the Major
				String receivedMajor = receivedTokenizer.nextToken();
				String filterMajor = filterTokenizer.nextToken();
				if(!filterMajor.equalsIgnoreCase(receivedMajor))
					return false;

				if(!filterTokenizer.hasMoreElements())		
					return true;

				//Extraction of the Minor
				String receivedMinor = receivedTokenizer.nextToken();
				String filterMinor = filterTokenizer.nextToken();
				if(!filterMinor.equalsIgnoreCase(receivedMinor))
					return false;

				return true;

			}

			else
				if(!entityId.equalsIgnoreCase(e.getEntityID()))
					return false;
		}

		//Type check
		if(!this.type.equals(Type.ALL)){
			if(!this.type.equals(e.getEntityType()))
				return false;
		}

		
		//Properties check
		if(this.filter != null)
			return PropertiesFilter.evalFilter(filter, e.getProperties());
		
		return true;
		
		
	}

	@Override
	public String toString() {
		return groupDesc;
	}

	
	public PropertiesFilter getFilter() {
		return filter;
	}

	private void setFilter(PropertiesFilter filter) {
		this.filter = filter;
	}

	public String getEntity_id() {
		return entity_id;
	}

	private void setEntity_id(String entity_id) {
		this.entity_id = entity_id;
	}

	public Type getType() {
		return type;
	}

	private void setType(Type type) {
		this.type = type;
	}
	
	private void setGroupDesc(String groupDesc) {
		this.groupDesc = groupDesc;
	}


	/**
	 * 
	 * Builder used to create a {@link Group}
	 *
	 */
	public static class Builder{

		Group ret;

		/**
		 * 
		 * @param description - description used for logging
		 */
		public Builder(String description) {
			ret = new Group();
			ret.setType(Type.ALL);
			ret.setEntity_id("");
			ret.setFilter(null);
			ret.setGroupDesc(description);
		}


		/**
		 * Setting of an EntityId as filter for the group: in this case, only one {@link Entity} can belong
		 * to the group. If this filter is not required, <code>filterID</code> parameter has to be
		 * passed with value <code>""</code>
		 * @param filterID - the entityId required
		 */
		public Builder setEntityID(String filterID){
			ret.setEntity_id(filterID);
			return this;
		}

		
		public Builder setFilter(PropertiesFilter filter){
			ret.setFilter(filter);
			return this;
		}
		

		/**
		 * Setting of the {@link Type} the group has to filter for. If this filter is not required, the type
		 * has to be passed with value {@link Type#ALL}
		 * @param filterType - the type required
		 */
		public Builder setEntityType(Type filterType){
			ret.setType(filterType);
			return this;
		}

		public Group build(){
			return ret;
		}
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
		dest.writeString((filter == null) ? "" : filter.toString());
		dest.writeString(entity_id);
		dest.writeString((this.type == null) ? "" : this.type.name());
		dest.writeString(groupDesc);
	}
	
	public final static Parcelable.Creator<Group> CREATOR = new Parcelable.Creator<Group>() {

		@Override
		public Group createFromParcel(Parcel source) {
			return new Group(source);
		}

		@Override
		public Group[] newArray(int size) {
			return new Group[size];
		}
	};

}
