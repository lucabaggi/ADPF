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


package it.polimi.geinterface.network;

import it.polimi.geinterface.GroupEntityManager;
import it.polimi.geinterface.DAO.Entity;
import it.polimi.geinterface.security.SecurityManager;
import it.polimi.proximityapi.interfaces.ActionOutcomeCallback;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.android.service.MqttTraceHandler;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import android.content.Context;
import android.util.Log;

public class MQTTPahoClient {

	private static final String TAG = "PahoClient";

	private Context appCtx;

	/**
	 * Client provided by the Paho library
	 */
	private MqttAndroidClient networkClient;

	private MqttConnectOptions netConnOptions;

	private MessageCallback messageArrivedCallback;

	private ConnectionStateCallback connStateCallback;

	private byte[] willMsg;

	private String HOST;

	private SecurityManager securityManager;

	private Entity selfEntity;


	private MqttCallback MQTTCallback= new MqttCallback() {

		@Override
		public void messageArrived(String topic, MqttMessage msg) throws Exception {

			if(messageArrivedCallback== null)
				return;

			String m = new String(msg.getPayload());
			messageArrivedCallback.onMessageReceived(m);		//Notify GroupEntityManager about the message arrived
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken token) {}

		@Override
		public void connectionLost(Throwable cause) {


			Log.e(TAG, "Conn lost! - " + (cause == null ? "" : cause.getLocalizedMessage()));

			//If unexpected disconnection
			if(cause != null && connStateCallback != null)
				connStateCallback.onDisconnected();		//notify it to the client app
		}
	};

	/**
	 * Value that sets the qos to use in subscriptions and message send
	 */
	private int qos = 2;



	/*
	 * 
	 * 
	 * 
	 * CODE START
	 * 
	 * 
	 * 
	 */

	public MQTTPahoClient(Context ctx, Entity selfEntity, SecurityManager securityManager,
			ConnectionStateCallback connCallback) {

		//Save instance variables
		this.selfEntity = selfEntity;
		this.connStateCallback = connCallback;
		this.securityManager = securityManager;
		appCtx = ctx;

		netConnOptions = new MqttConnectOptions();

		//build will msg (if necessary)
		if(securityManager.check_group_changes_enabled()){
			willMsg = MessageUtils.buildCheckOutMessage(selfEntity, true).getBytes();
			netConnOptions.setWill(MessageTopic.BROADCAST.name(),
					willMsg, 2, true);
		}

		netConnOptions.setCleanSession(true);
		netConnOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
		netConnOptions.setKeepAliveInterval(15);

		//SSL setup (if needed)
		if(securityManager.check_use_SSL())
			netConnOptions.setSocketFactory(securityManager.getSSLSocketFactory());
	}


	/**
	 * Method that performs a connection attempt to the passed host. The {@link ActionOutcomeCallback#onCompleted()} method
	 * is called on connection succesfully performed.
	 */
	public void connect(String host, final ActionOutcomeCallback onConnected){
		try {
			if(networkClient != null && networkClient.isConnected())
				return;
			
			networkClient = new MqttAndroidClient(appCtx, host,  selfEntity.getEntityID());

			this.HOST = host;

			networkClient.setCallback(MQTTCallback);

			//DEBUG CODE
			networkClient.setTraceEnabled(false);
			networkClient.setTraceCallback(new MqttTraceHandler() {

				@Override
				public void traceException(String source, String message, Exception e) {
					Log.e(TAG, message + "\n " + ((e == null) ? "" : e.getLocalizedMessage()));
				}

				@Override
				public void traceError(String source, String message) {
					Log.e(TAG,source + " - " + message );				
				}

				@Override
				public void traceDebug(String source, String message) {
					Log.d(TAG,source + " - " + message );				

				}
			});
			//END DEBUG CODE


			Log.i(TAG, "New connection attempt to " + host);

			//perform the connection attempt
			networkClient.connect(netConnOptions,null ,new IMqttActionListener() {

				@Override
				public void onSuccess(IMqttToken token) {
					Log.i(TAG, "Connected to router");
					onConnected.onCompleted();
				}

				@Override
				public void onFailure(IMqttToken token, Throwable arg1) {
					Log.e(TAG, "Connection Failure");
					if(connStateCallback != null)
						connStateCallback.onConnectionFailed();
				}
			});		

		} catch (MqttException e) {
			Log.e(TAG, "Client connection error");
			e.printStackTrace();
		}

	}


	/**
	 * Method that publishes a message on the given topic
	 */
	public void publishMessage(String topic, String msg){

		if(networkClient == null){
			Log.e(TAG, "MQTT CLIENT NOT CONNECTED");
			return;
		}

		if(!networkClient.isConnected())
			return;

		try {
			networkClient.publish(topic, msg.getBytes(), qos, false);
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}


	public void susbcribe(String topic){

		if(networkClient == null){
			Log.e(TAG, "MQTT CLIENT NOT CONNECTED");
			return;
		}

		try {
			networkClient.subscribe(topic, qos);
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

	public void unsusbcribe(String topic){
		if(networkClient == null){
			Log.e(TAG, "MQTT CLIENT NOT CONNECTED");
			return;
		}

		try {
			networkClient.unsubscribe(topic);
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Method that performs the will message update process in an almost transparent way for the {@link GroupEntityManager}
	 * @param oldEntity - the "old" selfEntity, before the update of the propertiess
	 * @param newEntity - the "new" selfEntity with the updated properties
	 * @param onConnected
	 */
	public void onSelfEntityUpdate(Entity oldEntity, final Entity newEntity, Boolean forGhostMode, 
			final ActionOutcomeCallback onConnected){

		if(networkClient == null){
			Log.e(TAG, "MQTT CLIENT NOT CONNECTED");
			return;
		}

		//Check wether the will msg has to be set or not
		if(securityManager.check_group_changes_enabled()){
			willMsg = MessageUtils.buildCheckOutMessage(newEntity, true).getBytes();
			Log.w(TAG, new String(willMsg));

			netConnOptions.setWill(MessageTopic.BROADCAST.name(),
					willMsg,
					2, true);
		}
		else {
			//if not a special topic is used to set the will msg in order to avoid that someone receives it as a mistake
			netConnOptions.setWill("$", "".getBytes(), 0, false);
		}

		try {
			if(securityManager.check_group_changes_enabled() && !forGhostMode){
				Log.i(TAG, "publishing FALSE checkout message");


				networkClient.publish(MessageTopic.BROADCAST.name(),
						MessageUtils.buildCheckOutMessage(oldEntity, false).getBytes(),
						2, false);
			}

			//perform the disconnect and reconnect process
			networkClient.disconnect(5000,null, new IMqttActionListener() {

				@Override
				public void onSuccess(IMqttToken arg0) {

					try {
						Log.i(TAG, "Disconnection success");
						networkClient = new MqttAndroidClient(appCtx, HOST, newEntity.getEntityID());
						networkClient.setCallback(MQTTCallback);
						networkClient.connect(netConnOptions, null, new IMqttActionListener() {

							@Override
							public void onSuccess(IMqttToken arg0) {
								onConnected.onCompleted();
							}

							@Override
							public void onFailure(IMqttToken arg0, Throwable arg1) {
								Log.e(TAG, "Disconnect Failure!");
								if(connStateCallback != null)
									connStateCallback.onConnectionFailed();
							}
						});

					} catch (MqttException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void onFailure(IMqttToken arg0, Throwable arg1) {
					Log.e(TAG, "Failed disconnection");
				}
			});

		} catch (MqttException e) {
			e.printStackTrace();
		}

	}

	public void disconnect(){

		if(networkClient == null || !networkClient.isConnected())
			return;
		try {
			if(securityManager.check_group_changes_enabled())
				networkClient.publish(MessageTopic.BROADCAST.name(), willMsg,2,false);

			//switch ad active per fare il flush sul server
			//LoggerService.changeMode(appCtx, LogMod.active);

			networkClient.disconnect();
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

	public void setMessageArrivedCallback(MessageCallback messageArrivedCallback) {
		this.messageArrivedCallback = messageArrivedCallback;
	}

}
