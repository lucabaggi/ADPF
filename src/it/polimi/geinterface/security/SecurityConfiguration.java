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


package it.polimi.geinterface.security;

import javax.net.SocketFactory;


/**
 * Class used to define security configuration abstractions
 *
 */
public class SecurityConfiguration {

	
	protected boolean use_SSL;
	
	protected boolean group_changes_enabled;
	
	protected boolean proximity_changes_enabled;
	
	protected SocketFactory SSL_socket_factory;
	
	
	protected SecurityConfiguration(){
		use_SSL = false;
		group_changes_enabled = true;
		proximity_changes_enabled = true;
		
	}
	
	
	protected void setGhostMode(){
		this.group_changes_enabled = false;
		this.proximity_changes_enabled = false;
	}
	
	protected void restoreSettings(boolean old_group_settings, boolean old_proximity_settings){
		this.group_changes_enabled = old_group_settings;
		this.proximity_changes_enabled = old_proximity_settings;
	}

}
