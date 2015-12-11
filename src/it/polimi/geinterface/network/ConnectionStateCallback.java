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

import it.polimi.geinterface.GroupEntityManager;

/**
 * 
 * Interface defining callback methods that notifies about connection events
 */
public interface ConnectionStateCallback {

	/**
	 * This method is called when a connection attempt to the network broker is successful.
	 * (after a {@link GroupEntityManager#connect(String)} 
	 */
	public void onConnected();
	
	/**
	 * 
	 *	This method is called whenever an unexpected disconnection from the network broker happened (without
	 * a previous call of {@link GroupEntityManager#stop()}
	 */
	public void onDisconnected();
	
	/**
	 * Method called when a connection attempt performed with a call to {@link GroupEntityManager#connect(String)} method is
	 * unsuccessful.
	 */
	public void onConnectionFailed();
}
