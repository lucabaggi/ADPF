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

package it.polimi.geinterface.service;

import it.polimi.geinterface.GroupEntityManager;
import it.polimi.geinterface.SubscriptionCallback;
import it.polimi.geinterface.DAO.Entity;
import it.polimi.geinterface.network.ConnectionStateCallback;
import it.polimi.geinterface.security.SecurityManager;

import java.util.Random;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;


public class ProximityService extends Service {

	Notification notification;
	private ProximityServiceBinder mBinder = new ProximityServiceBinder();
	private int notificationID;
	private BroadcastReceiver stopReceiver;

	@Override
	public IBinder onBind(Intent intent) {
		Log.i("Serviceeee", "onBind");
		stopReceiver = new StopReceiver();
		registerReceiver(stopReceiver, new IntentFilter("STOP_PROXIMITY_SERVICE"));
		showNotification();
		return mBinder;
	}

	/*
	 * The onStartCommand is implmented in order to keep the service running even after the client app unbind
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.i("ProximityService", "onStart");
		return START_NOT_STICKY;
	}


	/**
	 * This class implements the binder related to {@link ProximityService} and used by the client app to
	 * retrieve the {@link GroupEntityManager} instance and interact with it
	 */
	public class ProximityServiceBinder extends Binder{

		/**
		 * This method returns a {@link GroupEntityManager} instance and creates it (if needed)
		 * @param self - the selfEntity created by the client app "characterizing" this device {@link Entity}
		 * @param secureMgr - the {@link SecurityManager} instance created by the client app
		 * @param connCallback - the callback to handle network events
		 * @param call - the callback to handle framework events related to submitted subscriptions 
		 */
		public GroupEntityManager getGEManager(Entity self, SecurityManager secureMgr,
				final ConnectionStateCallback connCallback, SubscriptionCallback call){


			if(GroupEntityManager.getInstance() == null)
				GroupEntityManager.init(ProximityService.this.getApplicationContext(), self, secureMgr, connCallback);

			GroupEntityManager geManager = GroupEntityManager.getInstance();
			/*
			 * callback instances are set also in case of "rebind"
			 */
			geManager.setConnStateCallback(connCallback);
			geManager.setSubscriptionCallback(call);
			Log.d("Service", "Subscription callback set.");
			return geManager;
		}


		public GroupEntityManager getGeManager(final ConnectionStateCallback connCallback, SubscriptionCallback call){
			
			if(GroupEntityManager.getInstance() == null)
				return null;

			GroupEntityManager geManager = GroupEntityManager.getInstance();
			/*
			 * callback instances are set also in case of "rebind"
			 */
			geManager.setConnStateCallback(connCallback);
			geManager.setSubscriptionCallback(call);
			return geManager;
		}



	}

	@Override
	public boolean onUnbind(Intent intent) {
		super.onUnbind(intent);
		return true;		
	}


	@Override
	public void onDestroy() {
		GroupEntityManager geManager = GroupEntityManager.getInstance();
		//LoggerService.changeMode(getApplicationContext(), LogMod.active);

		if(geManager != null)
			geManager.stop();		//stop the geManager releasing all resources used

		hideNotification();
		Log.i("ProximityService", "ServiceStopped");

		unregisterReceiver(stopReceiver);
		super.onDestroy();
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
	}


	private void showNotification() {
		if(notification != null) return;

		Notification notification = new Notification.Builder(this).setAutoCancel(false).setContentText("ProximityService running...")
				.setContentTitle("ProximityService running...").setSmallIcon(R.drawable.ic_dialog_map)
				.setContentIntent(PendingIntent.getBroadcast(this, 123123123, new Intent("STOP_PROXIMITY_SERVICE"), PendingIntent.FLAG_UPDATE_CURRENT))
				.build();

		// Send the notification.
		notificationID = new Random().nextInt(100) + 10000;
		((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(notificationID, notification);
	}

	private void hideNotification() {
		((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(notificationID);
	}


	public class StopReceiver extends WakefulBroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("ProximityService", "stop received");
			stopSelf();
		}

	}

}

