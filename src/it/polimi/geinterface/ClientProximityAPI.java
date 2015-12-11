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

import it.polimi.geinterface.SubscriptionCallback.EntityEvent;
import it.polimi.geinterface.SubscriptionCallback.GroupEvent;
import it.polimi.geinterface.DAO.Entity;
import it.polimi.geinterface.DAO.Group;
import it.polimi.geinterface.DAO.Subscription;
import it.polimi.proximityapi.interfaces.ActionOutcomeCallback;

import java.util.ArrayList;

public interface ClientProximityAPI {

	/*
	 * 
	 * 
	 * ASYNCHRONOUS METHODS
	 * 
	 * 
	 */

	/**
	 * It permits to subscribe to proximity events: every time an {@link Entity} belonging to {@link Group} 
	 * <code>g2</code>is in proximity with respect {@link Entity} e1, 
	 * a {@link EntityEvent#ENTITY_PROXIMITY_UPDATE} is fired.
	 * 
	 * @param e1 - {@link Entity} with respect to the distance has to be computed.
	 * @param g2 - {@link Group} representing the set of {@link Entity} in which the subscriber is interested
	 * to compute the distance from <code>e1</code>
	 * 
	 * @return - it returns a {@link Subscription} object 
	 * 
	 */
	public Subscription subscribeEntityDistanceTracking(Entity e1, Group g2);			


	/**
	 * It permits to subscribe to geofence events: every time an {@link Entity} belonging to <code>g1</code> enters
	 * or exits the geofence of radius <code>distance</code> built around {@link Entity} <code>e1</code>,
	 * a {@link EntityEvent#ENTITY_GEOFENCE_ENTRY} or a {@link EntityEvent#ENTITY_GEOFENCE_EXIT} is fired.
	 * 
	 * @param e1 - {@link Entity} representing the centre of the geofence that has to be created.
	 * @param g1 - {@link Group} representing the set of {@link Entity} in which the subscriber is interested.
	 * @param distance -  {@link DistanceRange} representing the geofence radius.
	 * 
	 * @return - it returns a {@link Subscription} object.
	 */
	public Subscription subscribeEntityGeoFenceTracking(Entity e1, Group g1,
			DistanceRange distance);


	/**
	 * It permits to subscribe to group events about the {@link Group} <code>g</code>. Events related to 
	 * this subscription are: {@link GroupEvent#ENTITY_CHECK_IN}, {@link GroupEvent#ENTITY_CHECK_OUT},
	 * {@link GroupEvent#ENTITY_GROUP_JOIN}, {@link GroupEvent#ENTITY_GROUP_LEAVE}.
	 * 
	 * @param g - {@link Group} in which the subscriber is interested to receive notifications.
	 * 
	 * @return - it returns a {@link Subscription} object.
	 */
	public Subscription subscribeGroupChanges(Group g);





	/*
	 * 
	 * 
	 * SYNCHRONOUS METHODS
	 * 
	 * 
	 */



	/**
	 * It permits to remove the {@link Subscription} passed as parameter.
	 * 
	 * @param s - the {@link Subscription} to be removed.
	 * 
	 * @return - <code>true</code> if the {@link Subscription} is removed, <code>false</code> otherwise.
	 */
	public boolean unsubscribe(Subscription s);



	/**
	 * 
	 * It permits to collect all entities belonging to {@link Group} <code>g</code> which are at a distance
	 * less or equal than <code>distance</code> with respect to selfEntity.
	 * 
	 * @param distance - maximum {@link DistanceRange} an {@link Entity} has to be distant from selfEntity
	 * to be inserted into <code>result</code>.
	 * @param g - {@link Group} the {@link Entity} has to belong to in order to be inserted into <code>result</code>.
	 * @param callback - {@link ActionOutcomeCallback} that has to be performed after <code>millisec</code> ms.
	 * @param millisec - Time in milliseconds selfEntity has to wait to receive responses by all the entities 
	 * in proximity.
	 * @param result - {@link ArrayList} containing entities in proximity.
	 */
	public void getAllEntitiesInProximity(DistanceRange distance, Group g, ActionOutcomeCallback callback, long millisec, ArrayList<Entity> result);



}

