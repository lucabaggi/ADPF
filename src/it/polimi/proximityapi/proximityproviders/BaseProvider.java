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

package it.polimi.proximityapi.proximityproviders;

import it.polimi.proximityapi.TechnologyManager;
import it.polimi.proximityapi.interfaces.PassiveTechnologyListener;
import it.polimi.proximityapi.interfaces.ProximityProvider;


public abstract class BaseProvider implements ProximityProvider {

	/**
	 * Instance variable set by {@link TechnologyManager} upon a new provider is created
	 * in order to make it able to call {@link PassiveTechnologyListener} callbacks and notify
	 * new proximity results
	 */
	protected PassiveTechnologyListener techListener;

	
	public void setTechListener(PassiveTechnologyListener techListener) {
		this.techListener = techListener;
	}
	
	
}
