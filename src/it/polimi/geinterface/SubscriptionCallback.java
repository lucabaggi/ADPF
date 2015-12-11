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


package it.polimi.geinterface;

import it.polimi.geinterface.DAO.Entity;
import it.polimi.geinterface.DAO.Group;
import it.polimi.geinterface.DAO.Subscription;

/**
 * Interface that has to be implemented in order to react to {@link EntityEvent} and {@link GroupEvent} events
 *
 */
public interface SubscriptionCallback{


	/**
	 * Method that has to be implemented to say to the framework what to do when a {@link EntityEvent}
	 * is fired
	 * @param s - the {@link Subscription} related
	 * @param event - the {@link EntityEvent} related
	 * @param e1 - first {@link Entity} of the event
	 * @param e2 - second {@link Entity} of the event
	 * @param distance - {@link DistanceRange} between the two entities
	 */
	public void handleEvent(Subscription s, EntityEvent event, Entity e1, Entity e2, DistanceRange distance);
	
	/**
	 * Method that has to be implemented to say to the framework what to do when a {@link GroupEvent}
	 * is fired
	 * @param s - the {@link Subscription} related
	 * @param event - the {@link GroupEvent} related
	 * @param e - the {@link Entity} firing the event
	 * @param g - the {@link Group} involved in the event
	 */
	public void handleGroupEvent(Subscription s, GroupEvent event, Entity e, Group g);
	
	/**
	 * Method used to handle a {@link ErrorEvent}
	 * @param s - the {@link Subscription} related
	 * @param event - the {@link ErrorEvent} related
	 */
	public void handleSubscriptionError(Subscription s, ErrorEvent event);
	
	
	/**
	 * {@link Enum} used to represent proximity and geofence events
	 *
	 */
	public static enum EntityEvent{
		
		/**
		 * Proximity events between two {@link Entity}
		 */
		ENTITY_PROXIMITY_UPDATE,
		
		/**
		 * Geofence entry event
		 */
		ENTITY_GEOFENCE_ENTRY,
		
		/**
		 * Geofence exit event
		 */
		ENTITY_GEOFENCE_EXIT,
	}
	
	/**
	 * {@link Enum} used to represent group events
	 *
	 */
	public static enum GroupEvent{
		
		/**
		 * Event representing the entry of an {@link Entity} in the network
		 */
		ENTITY_CHECK_IN,
		
		/**
		 * Event representing the exit of an {@link Entity} from the network
		 */
		ENTITY_CHECK_OUT,
		
		/**
		 * Group join event
		 */
		ENTITY_GROUP_JOIN,
		
		/**
		 * Group leave event
		 */
		ENTITY_GROUP_LEAVE
	}
	
	
	/**
	 * {@link Enum} used to represent error events due to {@link Subscription} not allowed
	 *
	 */
	public static enum ErrorEvent{
		
		/**
		 * Error event fired when a geofence {@link Subscription} has reached the maximum number of
		 * {@link Entity} that can be stored for it
		 */
		ERROR_SUBSCRIPTION_ENTITY_SIZE_EXCEEDED("Raggiunto il limite massimo di Entities gestibili da questa subscription"),
		
		/**
		 * Error event fired when the maximum number of geofence {@link Subscription} is reached
		 */
		ERROR_SUBSCRIPTION_NUMBER_LIMIT_EXCEEDED("Raggiunto limite massimo di subscription di tipo geofence registrabili"),
		

		/**
		 * Error event fired when a proximity or geofence {@link Subscription} receives as {@link Entity} 
		 * parameter something which is not selfEntity or a BLE Beacon.
		 */
		ERROR_SUBSCRIPTION_NOT_VALID("Entity passata come parametro non e' di tipo BLE_BEACON o selfEntity");
		
		
		private String message;
		
		private ErrorEvent(String msg){
			message = msg;
		}
		
		public String getMessage(){
			return message;
		}
	}

}
