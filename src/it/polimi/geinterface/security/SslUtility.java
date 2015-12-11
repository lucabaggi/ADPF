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

import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.HashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import android.content.Context;

public class SslUtility {

	private static SslUtility		mInstance = null;
	private Context					mContext = null;
	private HashMap<Integer, SSLSocketFactory> mSocketFactoryMap = new HashMap<Integer, SSLSocketFactory>();

	public SslUtility(Context context) {
		mContext = context;
	}

	public static SslUtility getInstance( Context c) {
		if ( null == mInstance ) {
			mInstance = new SslUtility( c );
			//throw new RuntimeException("first call must be to SslUtility.newInstance(Context) ");
		}
		return mInstance;
	}


	public SSLSocketFactory getSocketFactory(int certificateId, String certificatePassword ) {

		SSLSocketFactory result = mSocketFactoryMap.get(certificateId);  	// check to see if already created

		if ( ( null == result) && ( null != mContext ) ) {					// not cached so need to load server certificate

			try {
				KeyStore keystoreTrust = KeyStore.getInstance("BKS");		// Bouncy Castle

				keystoreTrust.load(mContext.getResources().openRawResource(certificateId),
						certificatePassword.toCharArray());

				TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

				trustManagerFactory.init(keystoreTrust);

				SSLContext sslContext = SSLContext.getInstance("TLS");

				sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());

				result = sslContext.getSocketFactory();

				mSocketFactoryMap.put( certificateId, result);	// cache for reuse
			}
			catch ( Exception ex ) {
				// log exception
			}
		}

		return result;
	}

}