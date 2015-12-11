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

import android.content.Context;

/**
 * Class used to configure security policies, and to implement Ghost Mode
 *
 */
public class SecurityManager implements GhostMode {

	private SecurityConfiguration securityConfig;

	/**
	 * Attribute used to store group security settings before enabling Ghost Mode: in this way it is possible
	 * to restore previous settings when Ghost Mode is disabled
	 */
	private boolean old_group_settings;
	
	/**
	 * Attribute used to store proximity security settings before enabling Ghost Mode: in this way it is possible
	 * to restore previous settings when Ghost Mode is disabled
	 */
	private boolean old_proximity_settings;

	private SecurityManager() {
		this.securityConfig = new SecurityConfiguration();	
	}


	/**
	 * 
	 * Builder used to create the {@link SecurityManager}
	 *
	 */
	public static class Builder{

		private SecurityManager ret;
		private Context context;

		public Builder(Context ctx) {
			ret = new SecurityManager();
			context = ctx;
		}

		/**
		 * It permits to set the use of SSL for MQTT
		 * @param use_SSL - <code>true</code> if SSL protocol is required, <code>false</code> otherwise. 
		 * @param bks_trust_store_file - if <code>use_SSL = true</code> the Android resource_id containing
		 * the trusted-keystore (in BKS format) for server authentication is needed
		 * @param keyStorePass - if <code>use_SSL = true</code> the password for the keystore (passed as resource)
		 * is needed
		 */
		public Builder useSSL(boolean use_SSL, int bks_trust_store_file_id, String keyStorePass){
			ret.securityConfig.use_SSL = use_SSL;

			if(use_SSL){
				context.getResources().getResourceEntryName(bks_trust_store_file_id);
				ret.securityConfig.SSL_socket_factory = 
						SslUtility.getInstance(context).getSocketFactory(bks_trust_store_file_id,keyStorePass);
			}

			return this;
		}

		public Builder groupAdvertiseEnabled(boolean enabled){
			ret.securityConfig.group_changes_enabled = enabled;
			return this;
		}

		public Builder proximityAdvertiseEnabled(boolean enabled){
			ret.securityConfig.proximity_changes_enabled = enabled;
			return this;
		}

		public SecurityManager build(){
			ret.old_group_settings = ret.securityConfig.group_changes_enabled;
			ret.old_proximity_settings = ret.securityConfig.proximity_changes_enabled;
			return ret;
		}
	}


	@Override
	public void enableGhostMode() {
		this.old_group_settings = securityConfig.group_changes_enabled;
		this.old_proximity_settings = securityConfig.proximity_changes_enabled;

		securityConfig.setGhostMode();
	}

	@Override
	public void disableGhostMode() {
		securityConfig.restoreSettings(old_group_settings, old_proximity_settings);
	}


	public boolean check_old_group_settings(){
		return this.old_group_settings;
	}


	public boolean check_use_SSL() {
		return securityConfig.use_SSL;
	}


	public boolean check_group_changes_enabled() {
		return securityConfig.group_changes_enabled;
	}



	public boolean check_proximity_changes_enabled() {
		return securityConfig.proximity_changes_enabled;
	}


	public SocketFactory getSSLSocketFactory(){
		return securityConfig.SSL_socket_factory;
	}


}
