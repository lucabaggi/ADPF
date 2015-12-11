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

import it.polimi.proximityapi.TechnologyManager;
import it.polimi.proximityapi.DAO.ProximityResult;
import it.polimi.proximityapi.proximityproviders.BaseProvider;


/**
 * 
 * This interface defines methods "seen" by {@link BaseProvider} objects and used to send proximity updates to 
 * {@link TechnologyManager}
 * 
 */
public interface PassiveTechnologyListener {
	
	/**
	 * 
	 * Callback executed when an object at Technology level wants to send a proximity fresh update to upper levels
	 * 
	 * @param providerClass - is the {@link Class} object of the proximity provider "producing" the fresh proximity data
	 * @param result - contains proximity information and additional provider-specific data (for example the type of the update)
	 */
	public void onProximityChanged(Class<? extends BaseProvider> providerClass, ProximityResult result);
	
}
