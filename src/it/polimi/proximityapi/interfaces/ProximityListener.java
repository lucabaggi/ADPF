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

package it.polimi.proximityapi.interfaces;

import it.polimi.geinterface.DAO.POI;
import it.polimi.proximityapi.DAO.ProximityData;
import it.polimi.proximityapi.DAO.ProximityResult;


/**
 * This interface defines callback methods to handle new proximity informations coming from 
 * proximity providers
 * 
 */
public interface ProximityListener {
	
	
	/**
	 * Method called whenever a proximity update from providers is available
	 * 
	 */
	public void onProximityChanged(ProximityResult result);
	
	/**
	 * Event indicating the {@link POI} entry event. The {@link POI} is defined by the {@link ProximityData} object containing this 
	 * {@link ProximityListener} object
	 */
	public void onEnterGeofenceArea(ProximityResult result);
	
	
	/**
	 * Event indicating the {@link POI} exit event. The {@link POI} is defined by the {@link ProximityData} object containing this 
	 * {@link ProximityListener} object
	 */
	public void onExitGeofenceArea(ProximityResult result);
	
	

}
