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

package it.polimi.proximityapi.DAO;

import it.polimi.proximityapi.proximityproviders.BLEProximityProvider;

import java.util.Collection;

import org.altbeacon.beacon.Beacon;

/**
 * This class defines DAO objects containing results "produced" by proximity providers the framework uses.
 */
public class ProximityResult {

	/**
	 * It contains {@link ProximityData} ID related to this result
	 */
	public String proximityDataID;

	/**
	 * <code>true</code> if this result has been built by the {@link BLEProximityProvider}. Otherwise false
	 */
	public boolean isBle;
	
	/**
	 * <code>true</code> if it is a ProximityUpdate, <code>false</code> altrimenti
	 */
	public boolean isProximityUpdate;
	
	/**
	 * This {@link Collection} contanis data about BLE {@link Beacon} detected
	 * <b>N.B.</b> <code>NOT null</code> only if {@link #isBle} = <code>true</code> and {@link #isProximityUpdate} = <code>true</code>
	 */
	public Collection<Beacon> visibleBeacons;

	public ProximityResult setBle(boolean isBle) {
		this.isBle = isBle;
		return this;
	}
	
	public ProximityResult setProximityUpdate(boolean isProximityUpdate) {
		this.isProximityUpdate = isProximityUpdate;
		return this;
	}

	public ProximityResult setVisibleBeacons(Collection<Beacon> visibleBeacons) {
		this.visibleBeacons = visibleBeacons;
		return this;

	}
	
	public ProximityResult setProxDataID(String id) {
		this.proximityDataID = id;
		return this;
	}
	
	

	
	
	
	
	
	
}
