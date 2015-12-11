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

/**
 * It contains ranges of distance used by the framework.
 */
public enum DistanceRange{

	/**
	 * It corresponds to the range 0m - 0.5m
	 */
	IMMEDIATE,		
	
	/**
	 * It corresponds to the range 0.5m - 2m
	 */
	NEXT_TO,			
	
	/**
	 * It corresponds to the range 2m - 4m
	 */
	NEAR,		
	
	/**
	 * It corresponds to the range 4m - 8m
	 */
	FAR,			
	
	/**
	 * It corresponds to a distance greater than 8m
	 */
	REMOTE,			
	

	/**
	 * Two devices are seeing the same BLE Beacon
	 */
	SAME_BEACON, 
	
	/**
	 * Two devices are connected to the same network
	 */
	SAME_WIFI,		
	
	
	/**
	 * No distance informations available
	 */
	UNKNOWN

}
