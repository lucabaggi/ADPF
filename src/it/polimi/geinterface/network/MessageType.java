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


package it.polimi.geinterface.network;

import it.polimi.geinterface.DAO.Entity;
import it.polimi.geinterface.DAO.Entity.Type;

/**
 * This class defines constants representing possible messages types
 */
public enum MessageType{
	/**
	 * Message received on {@link MessageTopic#BROADCAST} sent upon a new {@link Entity} (usually of type {@link Type#DEVICE}) entered the network where this device is
	 * currently in
	 */
	CHECK_IN,
	
	/**
	 * Message received on {@link MessageTopic#BROADCAST} sent upon a new {@link Entity} (usually of type {@link Type#DEVICE}) exited the network where this device is
	 * currently in.
	 */
	CHECK_OUT,
	
	/**
	 * Message received on {@link MessageTopic#GROUP} that indicates a property update "process" completed 
	 * successfully by another {@link Entity} in the network
	 */
	PROPERTIES_UPDATE,
	
	/**
	 * Message received on topic {@link MessageTopic#PROXIMITY} that indicates another {@link Entity} changed its proximity relative to 
	 * another {@link Entity}
	 */
	PROXIMITY_UPDATE,
	
	/**
	 * Message sent on topic {@link MessageTopic#PROXIMITY} by an {@link Entity} in order to notify other devices about
	 * {@link Entity} of type {@link Type#BLE_BEACON} in its surroundings
	 * 
	 */
	PROX_BEACONS,
	
	/**
	 * Message sent on topic {@link MessageTopic#BROADCAST} containing a requesto to get informations on other {@link Entity} receiving it.
	 */
	
	SYNC_REQ,
	
	/**
	 * Message sent as response to {@link #SYNC_REQ}, containing the proximity of this device relative to the sender
	 */
	SYNC_RESP
}