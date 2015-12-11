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


package it.polimi.geinterface.filter;

/**
 * 
 * {@link Enum} representing possible types of filter
 *
 */
public enum FilterType {
	
	/**
	 * These filters can have as operands other filters
	 */
	AND,
	OR,

	/**
	 * These filters must have as operands values defined by keys
	 */
	CONTAINS,
	EQUALS,
	EXISTS,
	NOT_EXISTS,
	LT,
	GT
}
