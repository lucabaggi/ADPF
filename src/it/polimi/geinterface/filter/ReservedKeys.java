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
	 * {@link Enum} containing list of reserved keys for filters
	 *
	 */
	public enum ReservedKeys{
		/**
		 * key for the {@link FilterType}
		 */
		$op,
		/**
		 * key for the left operand of a filter
		 */
		$key,
		/**
		 * key for the right operand of a filter
		 */
		$val,
		/**
		 * key for the left filter of a nested filter
		 */
		$filter_1,
		/**
		 * key for the right filter of a nested filter
		 */
		$filter_2
	}