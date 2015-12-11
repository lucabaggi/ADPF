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

import it.polimi.geinterface.SubscriptionCallback.EntityEvent;
import it.polimi.geinterface.SubscriptionCallback.ErrorEvent;
import it.polimi.geinterface.SubscriptionCallback.GroupEvent;
import it.polimi.geinterface.DAO.Entity;
import it.polimi.geinterface.DAO.Entity.Builder;
import it.polimi.geinterface.DAO.Entity.Type;
import it.polimi.geinterface.DAO.Group;
import it.polimi.geinterface.DAO.JsonStrings;
import it.polimi.geinterface.DAO.POI;
import it.polimi.geinterface.DAO.Subscription;
import it.polimi.geinterface.concurrency.Scheduler;
import it.polimi.geinterface.filter.PropertiesFilter;
import it.polimi.geinterface.network.ConnectionStateCallback;
import it.polimi.geinterface.network.MQTTPahoClient;
import it.polimi.geinterface.network.MessageCallback;
import it.polimi.geinterface.network.MessageTopic;
import it.polimi.geinterface.network.MessageType;
import it.polimi.geinterface.network.MessageUtils;
import it.polimi.geinterface.security.SecurityManager;
import it.polimi.geinterface.service.ProximityService;
import it.polimi.logging.LogMessageUtils;
import it.polimi.logging.LoggerService;
import it.polimi.logging.LoggerService.LogMod;
import it.polimi.proximityapi.TechnologyManager;
import it.polimi.proximityapi.DAO.ProximityData;
import it.polimi.proximityapi.DAO.ProximityResult;
import it.polimi.proximityapi.interfaces.ActionOutcomeCallback;
import it.polimi.proximityapi.interfaces.ProximityListener;
import it.polimi.proximityapi.jsonLogic.POIJsonParser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;
import org.json.simple.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.util.Log;


public class GroupEntityManager implements ClientProximityAPI {


	/*
	 * INTENT KEYS
	 * 
	 */
	/**
	 * Key for the {@link Parcelable} object selfEntity set by client app
	 */
	public static final String SELF_ENTITY_INTENT_KEY = "SELF_ENTITY_INTENT_KEY";

	/**
	 * Key for the {@link POI} object related to the event of {@link POI} entry/exit
	 */
	public static final String POI_OBJ_INTENT_KEY = "POI_OBJ_INTENT_KEY";

	/**
	 * Key for the boolean value used to determine if the event is a {@link POI} entry or exit
	 */
	public static final String ENTERED_INTENT_KEY = "ENTERED_INTENT_KEY";



	private static GroupEntityManager _instance;

	private static final String TAG = "GroupEntityManager";

	private Context appCtx;
	private TechnologyManager techManager;

	/**
	 * {@link Entity} representing the device on which che framework is running.
	 */
	private Entity selfEntity;

	private ConnectionStateCallback connStateCallback;

	/**
	 * {@link ArrayList} containing beacons detected in the last BLE scan
	 */
	protected ArrayList<Entity> lastSeenBeacons;

	protected MQTTPahoClient networkClient;

	protected Scheduler scheduler;

	/**
	 * This is the {@link Timer} used to wait for a time equal to {@link #CHECK_IN_AFTER_PROP_UPDATE_DELAY}
	 * the {@link MessageType#PROPERTIES_UPDATE} corresponding to a {@link MessageType#CHECK_OUT} false. When
	 * {@link Timer} fires, a {@link GroupEvent#ENTITY_CHECK_OUT} is fired.
	 */
	protected Timer checkInTimer;
	/**
	 * {@link HashMap} where {@link TimerTask} waiting for a {@link MessageType#PROPERTIES_UPDATE} are stored
	 */
	protected Map<String, TimerTask> waitingForCheckInTasks;

	/**
	 * Delay to wait before firing a {@link GroupEvent#ENTITY_CHECK_OUT}, after a {@link MessageType#CHECK_OUT}
	 * with valid equal to <code>false</code> is received
	 *  
	 */
	protected final long CHECK_IN_AFTER_PROP_UPDATE_DELAY = 30000;


	private SubscriptionCallback subscriptionCallback;

	/**
	 * {@link Subscription} list for proximity events
	 */
	private ArrayList<Subscription> proximitySubscriptionList;

	/**
	 * {@link Subscription} list for group events
	 */
	private ArrayList<Subscription> groupSubscriptionList;

	/**
	 * {@link Subscription} list for geofence events
	 */
	private ArrayList<Subscription> geofenceSubscriptionList;


	/**
	 * {@link ArrayList} of {@link ProximityData} created starting from {@link POI}
	 */
	private ArrayList<ProximityData> proximityDataList;

	/**
	 * {@link ArrayList} of {@link POI}s that have to be monitored
	 */
	public ArrayList<POI> poiList;


	/**
	 * {@link ArrayList} of {@link Entity} containing the set of {@link Entity} responding to a
	 * {@link MessageType#SYNC_REQ} message
	 */
	protected ArrayList<Entity> syncRespEntityList;

	/**
	 * Object used in syncResp to handle the {@link Timer} timeout
	 */
	private Object syncObj = new Object();

	private MessageHandler msgHandler;

	private SecurityManager securityManager;

	/**
	 * Maximum number of geofence subscriptions.
	 */
	private int GEOFENCE_SUBSCRIPTION_LIMIT = 5;

	/**
	 * Number of BLE scans to wait before sending a {@link MessageType#PROX_BEACONS} message
	 */
	private final static int DEFAULT_BLE_SCAN_COUNTER = 6;

	/**
	 * Counter incremented at each BLE scan. When it reaches the default value
	 * ( {@link GroupEntityManager#DEFAULT_BLE_SCAN_COUNTER} ), a {@link MessageType#PROX_BEACONS}
	 * message is sent.
	 */
	private int bleScanCounter = 0;


	public static GroupEntityManager getInstance(){
		return _instance;
	}

	public static void init(Context ctx, Entity self, SecurityManager secureMgr,
			final ConnectionStateCallback connCallback){
		_instance = new GroupEntityManager(ctx, self, secureMgr, connCallback);
		Log.d(TAG, "NEW GROUPENTITYMANAGER INSTANCE");
	}


	/**
	 * 
	 * @param ctx - {@link Context} of the application using the framework.
	 * @param self - {@link Entity} representing selfEntity
	 * @param secureMgr - {@link SecurityManager} used to set security policies.
	 * @param connCallback - {@link ConnectionStateCallback} used to set callback functions for 
	 * network events (disconnection, connection failed, successful connection)
	 */
	private GroupEntityManager(Context ctx, Entity self, SecurityManager secureMgr,
			final ConnectionStateCallback connCallback){
		_instance = this;
		appCtx = ctx;
		selfEntity = self;

		LoggerService.changeMode(ctx, LogMod.silent);

		Log.e(TAG, ctx.getPackageName());
		if(secureMgr == null)
			//set default security configuration
			securityManager = new SecurityManager.Builder(ctx).build();
		else
			securityManager = secureMgr;

		techManager = new TechnologyManager(appCtx);

		techManager.startProximiyUpdates();

		proximityDataList = new ArrayList<ProximityData>();
		proximitySubscriptionList = new ArrayList<Subscription>();
		groupSubscriptionList = new ArrayList<Subscription>();
		geofenceSubscriptionList = new ArrayList<Subscription>();

		lastSeenBeacons = new ArrayList<Entity>();	

		waitingForCheckInTasks = Collections.synchronizedMap(new HashMap<String, TimerTask>());

		msgHandler = new MessageHandler();

		checkInTimer = new Timer(true);

		this.connStateCallback = connCallback;

		networkClient = new MQTTPahoClient(appCtx,self, securityManager,connCallback);

		networkClient.setMessageArrivedCallback(new MessageCallback() {

			@Override
			public void onMessageReceived(String m) {

				//timestamp used for logging
				long timestamp = Calendar.getInstance().getTimeInMillis() + LoggerService.NTP_DELAY;

				String senderID = MessageUtils.getSenderID(m);
				MessageType type = MessageUtils.getMsgType(m);

				//skip messages from myself
				if(senderID.equalsIgnoreCase(selfEntity.getEntityID()))
					return;

				Log.i(TAG, "Message received from " + senderID );

				/*
				 * 
				 * The following line of codes are used only for logging 
				 * 
				 */
				String log, topicReply,logId; 
				JSONObject status = LogMessageUtils.buildStatus(proximitySubscriptionList.size(),
						groupSubscriptionList.size(), geofenceSubscriptionList.size());

				if(type.equals(MessageType.SYNC_RESP))
					topicReply = MessageUtils.getRequestTopicFromMessage(m);
				else {
					topicReply = null;
				}

				if(type.equals(MessageType.CHECK_OUT)){
					if(MessageUtils.getValidBitFromMessage(m))
						logId = selfEntity.getEntityID() + timestamp;
					else {
						logId = MessageUtils.getSenderID(m);
					}
					log = LogMessageUtils.buildMessageReceivedLog(logId, selfEntity.getEntityID(), 
							Type.DEVICE, type, topicReply, status, timestamp);
					m = MessageUtils.addLogField(m, logId);
				}
				else{
					log = LogMessageUtils.buildMessageReceivedLog(MessageUtils.getLogIdFromMessage(m),
							selfEntity.getEntityID(), Type.DEVICE, type, topicReply, status, timestamp);
				}

				if(!senderID.equals(selfEntity.getEntityID()))
					LoggerService.writeToFile(appCtx, log);
				/*
				 * 
				 * End of logging code
				 * 
				 */



				msgHandler.messageConsumer(m);
			}
		});

		scheduler = new Scheduler();
		scheduler.resume();
	}


	/**
	 * Method that has to be called in order to connect to the network.
	 * 
	 * @param host_url - IP address of the PubSub broker
	 */
	public void connect(String host_url){

		networkClient.connect(host_url, new ActionOutcomeCallback() {

			@Override
			public void onCompleted() {

				if(securityManager.check_group_changes_enabled()){
					long timestamp = Calendar.getInstance().getTimeInMillis() + LoggerService.NTP_DELAY;
					String logId = selfEntity.getEntityID() + timestamp;

					networkClient.publishMessage(MessageTopic.BROADCAST.name(), 
							MessageUtils.buildCheckInMessage(selfEntity, logId));

					//logging
					String log = LogMessageUtils.buildMessageSentLog(logId, selfEntity.getEntityID(), 
							Type.DEVICE, MessageType.CHECK_IN, null, false, timestamp);
					LoggerService.writeToFile(appCtx, log);
				}

				networkClient.susbcribe(MessageTopic.BROADCAST.name());
				networkClient.susbcribe(MessageTopic.PROXIMITY.name());
				networkClient.susbcribe(MessageTopic.GROUP.name());
				networkClient.susbcribe(selfEntity.getEntityID());

				if(connStateCallback != null)
					connStateCallback.onConnected();
			}
		});

	}

	/**
	 * Method that has to be called in order to add {@link POI}.
	 * 
	 * @param jsonStr - {@link String} (in JSON format) containing the list of {@link POI}
	 */
	public void addPOI(String jsonStr){

		ArrayList<POI> newPois = POIJsonParser.parsePOIFile(jsonStr);

		if(poiList == null)
			poiList = newPois;
		else
			poiList.addAll(newPois);

		Log.d(TAG, "Registered POI number: "  + poiList.size());
		createProximityData(newPois);
	}


	/**
	 * Method that create a {@link ProximityData} for each  {@link POI} added.
	 * 
	 * @param toAdd - list of {@link POI} from which {@link ProximityData} have to be created
	 */
	private void createProximityData(ArrayList<POI> toAdd) {

		ProximityData help;

		for(final POI p : toAdd){
			help = new ProximityData(p.getName());
			help.setBLERegion(new Region(p.getName(),Identifier.parse(p.getBeaconUuid()), null, null))
			.setUse_ble(true)
			.setLatitude(p.getLatitude())
			.setLongitude(p.getLongitude())
			.setRadius(p.getRadius())
			.setNeed_geofence(true)
			.setProxListener(new ProximityListener() {

				@Override
				public void onProximityChanged(ProximityResult result) {

					if(!result.isBle || result.visibleBeacons == null
							|| result.visibleBeacons.size() == 0)
						return;


					ArrayList<Beacon> beacons = new ArrayList<Beacon>(result.visibleBeacons);
					ArrayList<Entity> beaconEntities = new ArrayList<Entity>();

					Entity temp;

					Log.i(TAG, "Beacon found with BLE scan: " + beacons.size());

					for(Beacon b: beacons){
						temp = new Entity.Builder(b.getId1() + ":" + b.getId2() + ":" + b.getId3(),
								Entity.Type.BLE_BEACON)
						.setDistance(getDistanceRangeFromBeacon(b.getDistance()))
						.build();

						beaconEntities.add(temp);

						//Check if there is some proximity subscription matching with the beacon
						evaluateProximity(selfEntity, temp, getDistanceRangeFromBeacon(b.getDistance()), "");
					}

					//Check if one of the beacons matches a geofence subscription
					evaluateGeofence(selfEntity, beaconEntities, "");

					if(securityManager.check_proximity_changes_enabled()){

						bleScanCounter++;

						if(bleScanCounter == DEFAULT_BLE_SCAN_COUNTER){
							long timestamp = Calendar.getInstance().getTimeInMillis() + LoggerService.NTP_DELAY;
							String logId = selfEntity.getEntityID() + timestamp;

							networkClient.publishMessage(MessageTopic.PROXIMITY.name(), 
									MessageUtils.buildProxBeaconsMessage(beaconEntities, selfEntity, logId));

							bleScanCounter = 0;

							//logging
							String log = LogMessageUtils.buildMessageSentLog(logId, selfEntity.getEntityID(), 
									Type.DEVICE, MessageType.PROX_BEACONS, null, false, timestamp);
							LoggerService.writeToFile(appCtx, log);
						}
					}

					//Storing of the last detected beacons
					saveLastSeenBeacons(beacons);
				}

				@Override
				public void onExitGeofenceArea(ProximityResult result) {

					Intent poiExitIntent = new Intent(getPOIEventBroadcastAction());
					poiExitIntent.putExtra(SELF_ENTITY_INTENT_KEY, selfEntity);
					poiExitIntent.putExtra(POI_OBJ_INTENT_KEY, p);
					poiExitIntent.putExtra(ENTERED_INTENT_KEY, false);
					appCtx.sendBroadcast(poiExitIntent);		
				}

				@Override
				public void onEnterGeofenceArea(ProximityResult result) {

					Intent poiEntryIntent = new Intent(getPOIEventBroadcastAction());
					poiEntryIntent.putExtra(SELF_ENTITY_INTENT_KEY, selfEntity);
					poiEntryIntent.putExtra(POI_OBJ_INTENT_KEY, p);
					poiEntryIntent.putExtra(ENTERED_INTENT_KEY, true);
					appCtx.sendBroadcast(poiEntryIntent);		
				}
			});

			techManager.registerProximityData(help, new ActionOutcomeCallback() {

				@Override
				public void onCompleted() {
					Log.d(TAG, "Proximity data registered!");
				}
			});

			proximityDataList.add(help);
		}
	}


	/**
	 * Method used to store last detected beacons. The maximum number of beacons that can be stored is 3: the
	 * nearest two, and the furthest one.
	 * @param visibleBeacons - List of {@link Beacon} detected in the last BLE scan.
	 */
	protected void saveLastSeenBeacons(final ArrayList<Beacon> visibleBeacons) {

		if(visibleBeacons.size() == 0){
			synchronized (lastSeenBeacons) {
				lastSeenBeacons.clear();
			}
			return;
		}


		scheduler.schedule(new Runnable() {

			@Override
			public void run() {

				if(visibleBeacons.size() < 4)
					synchronized (lastSeenBeacons) {
						lastSeenBeacons.clear();
						for(Beacon e: visibleBeacons){
							Entity be = new Entity.Builder(e.getId1() + ":" + e.getId2() + ":"
									+ e.getId3(), Type.BLE_BEACON)
							.setDistance(GroupEntityManager.getDistanceRangeFromBeacon(e.getDistance()))
							.build();
							lastSeenBeacons.add(be);
						}
					}

				else{

					//sort by distance
					Collections.sort(visibleBeacons, new Comparator<Beacon>() {
						@Override
						public int compare(Beacon lhs, Beacon rhs) {
							return Double.compare(lhs.getDistance(), rhs.getDistance());
						}
					});

					Beacon b1 = visibleBeacons.get(0);
					Beacon b2 = visibleBeacons.get(1);
					Beacon b3 = visibleBeacons.get(visibleBeacons.size() - 1);
					Entity e1 = new Entity.Builder(b1.getId1() + ":" + b1.getId2() + ":"
							+ b1.getId3(), Type.BLE_BEACON)
					.setDistance(GroupEntityManager.getDistanceRangeFromBeacon(b1.getDistance()))
					.build();
					Entity e2 = new Entity.Builder(b2.getId1() + ":" + b2.getId2() + ":"
							+ b2.getId3(), Type.BLE_BEACON)
					.setDistance(GroupEntityManager.getDistanceRangeFromBeacon(b2.getDistance()))
					.build();
					Entity e3 = new Entity.Builder(b3.getId1() + ":" + b3.getId2() + ":"
							+ b3.getId3(), Type.BLE_BEACON)
					.setDistance(GroupEntityManager.getDistanceRangeFromBeacon(b3.getDistance()))
					.build();
					synchronized (lastSeenBeacons) {
						lastSeenBeacons.add(e1);
						lastSeenBeacons.add(e2);
						lastSeenBeacons.add(e3);
					}

				}

			}
		});

	}


	/*
	 * 
	 * PUBLISH MESSAGES
	 * 
	 */


	/**
	 * Method that sends a {@link MessageType#PROXIMITY_UPDATE} on the network
	 * @param e1 
	 * @param e2
	 * @param distance - {@link DistanceRange} between <code>e1</code> and <code>e2</code>
	 */
	private void sendProximityUpdateMessage(Entity e1, Entity e2, DistanceRange distance){
		long timestamp = Calendar.getInstance().getTimeInMillis() + LoggerService.NTP_DELAY;
		String logId = selfEntity.getEntityID() + timestamp;

		/**
		 * The message is sent on <code>e2</code> topic
		 */
		networkClient.publishMessage(e2.getEntityID(),
				MessageUtils.buildProximityMessage(selfEntity.getEntityID(),MessageType.PROXIMITY_UPDATE,
						e1, e2,distance, logId));

		//logging
		String log = LogMessageUtils.buildMessageSentLog(logId, selfEntity.getEntityID(), 
				Type.DEVICE, MessageType.PROXIMITY_UPDATE, null, false, timestamp);
		LoggerService.writeToFile(appCtx, log);
	}

	/*
	 * 
	 * END - PUBLISH MESSAGES
	 * 
	 */



	/**
	 * Method that has to be called in order to change selfEntity properties ( {@link Entity#getProperties()} ).
	 * <b>IT IS NOT ALLOWED TO MODIFY ENTITY_ID</b>
	 * @param newSelfEntity - {@link Entity} with new properties
	 * @throws Exception - Exception raised when entityId is modified
	 */
	public void updateSelfEntity(final Entity newSelfEntity, final boolean fromGhost_toVisible) throws Exception{
		if(!newSelfEntity.getEntityID().equals(selfEntity.getEntityID())){
			throw new Exception("Self Entity ID modified!");
		}

		Log.w(TAG, "waiting scheduler pause");
		scheduler.pause();
		Log.w(TAG, "Modifying entity");
		final Entity oldEntity = selfEntity;

		networkClient.onSelfEntityUpdate(oldEntity, newSelfEntity, fromGhost_toVisible, 
				new ActionOutcomeCallback() {

			@Override
			public void onCompleted() {

				if(securityManager.check_group_changes_enabled() && !fromGhost_toVisible){
					long timestamp = Calendar.getInstance().getTimeInMillis() + LoggerService.NTP_DELAY;
					String logId = selfEntity.getEntityID();

					networkClient.publishMessage(MessageUtils.getMsgTopic(MessageType.PROPERTIES_UPDATE).name(),
							MessageUtils.buildGroupMessage(newSelfEntity.getEntityID(),
									newSelfEntity,
									oldEntity.getProperties(), logId));

					//logging
					String log = LogMessageUtils.buildMessageSentLog(logId, selfEntity.getEntityID(), 
							Type.DEVICE, MessageType.PROPERTIES_UPDATE, null, false, timestamp);
					LoggerService.writeToFile(appCtx, log);
				}

				networkClient.susbcribe(MessageTopic.BROADCAST.name());
				networkClient.susbcribe(MessageTopic.PROXIMITY.name());
				networkClient.susbcribe(MessageTopic.GROUP.name());
				networkClient.susbcribe(selfEntity.getEntityID());

			}
		});

		selfEntity = newSelfEntity;
		scheduler.resume();
	}


	/**
	 * Method that permits the start of {@link POI} monitoring added with the {@link #addPOI(String)} method.
	 */
	public void startPOIMonitoring(){
		if(poiList == null || poiList.size() == 0){
			Log.e(TAG, "POI Monitoring FAIL: no POI added!");
			return;
		}
		techManager.startProximiyUpdates();
	}

	@Override
	public Subscription subscribeEntityDistanceTracking(Entity e1, Group g2) {

		//group corresponding to entity e1 passed as parameter
		Group g1 = new Group.Builder(e1.getEntityID()).setEntityID(e1.getEntityID()).build();

		Subscription ret = new Subscription(g1, g2, DistanceRange.UNKNOWN);

		//if e1 is not selfEntity or a beacon, an ErrorEvent is notified
		if(!e1.getEntityType().equals(Type.BLE_BEACON) && !selfEntity.equals(e1)){
			getSubscriptionCallback().handleSubscriptionError(ret, ErrorEvent.ERROR_SUBSCRIPTION_NOT_VALID);
			return null;
		}

		scheduler.pause();
		proximitySubscriptionList.add(ret);
		scheduler.resume();
		Log.d(TAG, "Proximity subs size: " + proximitySubscriptionList.size());
		return ret;
	}

	@Override
	public Subscription subscribeEntityGeoFenceTracking(Entity e1, Group g2,
			DistanceRange distance) {
		Subscription ret = new Subscription(e1, g2, distance);

		//boolean value used to determine if an EventError has to be notified, after the Scheduler is resumed
		boolean handleError = false;	

		//if e1 is not selfEntity or a beacon, an ErrorEvent is notified
		if(!e1.getEntityType().equals(Type.BLE_BEACON) && !selfEntity.equals(e1)){
			getSubscriptionCallback().handleSubscriptionError(ret, ErrorEvent.ERROR_SUBSCRIPTION_NOT_VALID);
			return null;
		}

		scheduler.pause();

		/*
		 * if the limit on geofence subscription is reached, an ErrorEvent has to be fired on Scheduler resume,
		 * otherwise the subscription is added to the geofenceSubscriptionList
		 */	
		if(geofenceSubscriptionList.size() >= GEOFENCE_SUBSCRIPTION_LIMIT )
			handleError = true;
		else
			geofenceSubscriptionList.add(ret);
		scheduler.resume();

		if(handleError){
			getSubscriptionCallback().handleSubscriptionError(ret,ErrorEvent.ERROR_SUBSCRIPTION_NUMBER_LIMIT_EXCEEDED);
			return null;
		}
		return ret;

	}

	@Override
	public Subscription subscribeGroupChanges(Group g) {
		//Group g2 set to null as is not needed in group subscriptions
		Subscription ret = new Subscription(g, null, DistanceRange.UNKNOWN);
		scheduler.pause();
		groupSubscriptionList.add(ret);
		scheduler.resume();

		return ret;
	}

	@Override
	public boolean unsubscribe(final Subscription sub){

		scheduler.pause();

		boolean found = false;

		for(Subscription s : groupSubscriptionList)
			if(s.equals(sub)){
				groupSubscriptionList.remove(s);
				found = true;
				break;
			}

		if(!found)
			for(Subscription s : proximitySubscriptionList)
				if(s.equals(sub)){
					proximitySubscriptionList.remove(s);
					found = true;
					break;
				}

		if(!found)
			for(Subscription s : geofenceSubscriptionList)
				if(s.equals(sub)){
					geofenceSubscriptionList.remove(s);
					found = true;
					break;
				}

		if(found)
			Log.i(TAG, "Subscription removed correctly!");
		else
			Log.w(TAG, "Subscription does not exists!");

		scheduler.resume();
		return found;
	}


	@Override
	public void getAllEntitiesInProximity(final DistanceRange distance, Group g,
			final ActionOutcomeCallback callback, long millisec, final ArrayList<Entity> result) {

		//In this case, it means that a syncRequest is already running
		if(syncRespEntityList != null)
			return;		

		syncRespEntityList = result;

		//Creation of a unique topic where receive syncResp messages
		final String newTopic = new Random().nextInt(10000) +"-reqTopic";

		networkClient.susbcribe(newTopic);

		ArrayList<Entity> lastBeacons;

		synchronized (lastSeenBeacons) {
			lastBeacons = new ArrayList<>(lastSeenBeacons);
		}

		long timestamp = Calendar.getInstance().getTimeInMillis() + LoggerService.NTP_DELAY;
		String logId = selfEntity.getEntityID() + timestamp;

		networkClient.publishMessage(MessageTopic.BROADCAST.name(),
				MessageUtils.buildSyncReqMessage(selfEntity.getEntityID(),
						newTopic, lastBeacons, g, distance, logId));

		//logging
		String log = LogMessageUtils.buildMessageSentLog(logId, selfEntity.getEntityID(), 
				Type.DEVICE, MessageType.SYNC_REQ, newTopic, false, timestamp);
		LoggerService.writeToFile(appCtx, log);


		if(checkInTimer == null)
			checkInTimer = new Timer(true);

		checkInTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				networkClient.unsusbcribe(newTopic);

				synchronized (syncObj) {
					syncRespEntityList = null;
				}

				ArrayList<Entity> lastBeacons;

				synchronized (lastSeenBeacons) {
					lastBeacons = new ArrayList<>(lastSeenBeacons);
				}

				//Beacons in proximity added to result list
				ArrayList<Entity> beacons = lastBeacons;
				for(Entity e: beacons)
					if(e.getDistanceRange().ordinal() <= distance.ordinal())
						result.add(e);

				callback.onCompleted();
			}
		}, millisec);
	}

	/**
	 * Method used to stop the {@link GroupEntityManager}
	 */
	public void stop(){
		techManager.stop();
		scheduler.stop();

		networkClient.disconnect();
		_instance = null;
	}

	public void setConnStateCallback(ConnectionStateCallback connStateCallback) {
		this.connStateCallback = connStateCallback;
	}

	/**
	 * This method permits the {@link GroupEntityManager} to call {@link Subscription} callback functions
	 * on the app also in case of {@link ProximityService} "rebind"
	 * @param sub
	 */
	public void setSubscriptionCallback(SubscriptionCallback sub){
		scheduler.pause();
		this.subscriptionCallback = sub;
		scheduler.resume();
	}

	public SubscriptionCallback getSubscriptionCallback(){
		return this.subscriptionCallback;
	}
	/**
	 * Method that computes the {@link DistanceRange} corresponding to the distance (in meters) 
	 * received from BLE scan
	 * 
	 * @param distance - distance in meters obtained from a BLE scan
	 * @return the corresponding {@link DistanceRange}
	 */
	public static DistanceRange getDistanceRangeFromBeacon(double distance){
		if(distance < 0.6)
			return DistanceRange.IMMEDIATE;

		if(distance < 2.1)
			return DistanceRange.NEXT_TO;

		if(distance < 4.1)
			return DistanceRange.NEAR;

		return DistanceRange.FAR;
	}


	/**
	 * Class used to handle received messages
	 *
	 */
	private class MessageHandler {

		public void messageConsumer(String message){

			MessageType msgType = MessageUtils.getMsgType(message);

			//used for logging
			String logId; 

			Log.d(TAG, "Message received: "+  message);
			switch (msgType) {
			case PROXIMITY_UPDATE:
				Entity e1 = MessageUtils.getEntityFromMessage(1, message);
				Entity e2 = MessageUtils.getEntityFromMessage(2, message);
				DistanceRange distance = MessageUtils.getDistanceRangeFromMessage(message);
				logId = MessageUtils.getLogIdFromMessage(message);

				evaluateProximity(e1, e2, distance, logId);

				evaluateGeofenceD2D(e1, e2, distance, logId);

				break;

			case PROX_BEACONS:
				Entity e = MessageUtils.getEntityFromMessage(1, message);
				ArrayList<Entity> beacons = MessageUtils.getBeaconsFromMsg(message);
				logId = MessageUtils.getLogIdFromMessage(message);

				evaluateGeofence(e, beacons, logId);

				if(beacons.size() == 0)
					return;

				for(Entity b : beacons)
					evaluateProximity(e, b, b.getDistanceRange(), logId);

				synchronized (lastSeenBeacons) {
					//storing in beacons the intersection between lastSeenBeacons and beacons
					beacons.retainAll(lastSeenBeacons);
				}

				evaluateProximityD2D(beacons, e, logId);
				break;

			case PROPERTIES_UPDATE:

				long timestamp = Calendar.getInstance().getTimeInMillis() + LoggerService.NTP_DELAY;
				logId = MessageUtils.getLogIdFromMessage(message) + timestamp;
				JSONObject status = LogMessageUtils.buildStatus(proximitySubscriptionList.size(), 
						groupSubscriptionList.size(), geofenceSubscriptionList.size());

				/*
				 * logging
				 */
				String log = LogMessageUtils.buildMessageReceivedLog(logId, selfEntity.getEntityID(), 
						Type.DEVICE, MessageType.PROPERTIES_UPDATE, null, status, timestamp);
				LoggerService.writeToFile(appCtx, log);

				final Entity updatingEntity = MessageUtils.getEntityFromMessage(1, message);
				JSONObject oldProperties = MessageUtils.getOldPropertiesFromMessage(message);			


				scheduler.schedule(new Runnable() {

					@Override
					public void run() {
						TimerTask toCancel = waitingForCheckInTasks.remove(updatingEntity.getEntityID());

						if(toCancel != null){
							toCancel.cancel();
							Log.i(TAG, "TimerTask for " + updatingEntity.getEntityID() + " stopped");
						}
					}
				});

				evaluateGroup(updatingEntity, oldProperties, logId);
				break;


			case SYNC_REQ:

				if(!securityManager.check_proximity_changes_enabled())
					return;

				String topic = MessageUtils.getRequestTopicFromMessage(message);

				ArrayList<Entity> bcns = MessageUtils.getBeaconsFromMsg(message);
				String senderID = MessageUtils.getSenderID(message);

				//skip message from myself
				if(selfEntity.getEntityID().equalsIgnoreCase(senderID))
					return;	

				DistanceRange distanceRange = MessageUtils.getDistanceRangeFromMessage(message);
				JSONObject group = MessageUtils.getGroupFromMessage(message);
				handleSyncReq(topic, bcns, group, distanceRange);

				break;

			case SYNC_RESP:
				Entity respondingEntity = MessageUtils.getEntityFromMessage(1, message);

				handleSyncResp(respondingEntity);

				break;

			case CHECK_IN:
				Entity checkInEntity = MessageUtils.getEntityFromMessage(1, message);
				logId = MessageUtils.getLogIdFromMessage(message);
				Log.d(TAG, message);

				/*
				 * if checkInEntity matches a proximity subscription, an ENTITY_PROXIMITY_UPDATE
				 * with distance SAME_WIFI is fired
				 */		
				evaluateProximity(selfEntity, checkInEntity, DistanceRange.SAME_WIFI, logId);

				evaluateCheckIn(checkInEntity, logId);
				break;

			case CHECK_OUT:
				Entity checkOutEntity = MessageUtils.getEntityFromMessage(1, message);
				logId = MessageUtils.getLogIdFromMessage(message);

				evaluateCheckOut(checkOutEntity, MessageUtils.getValidBitFromMessage(message), logId);
				break;

			default:
				break;
			}
		}

	}






	/*
	 * 
	 * 
	 * 
	 * 
	 * 
	 * METHODS USED TO DETERMINE EVENTS FIRING
	 * 
	 * 
	 * 
	 * 
	 * 
	 */

	/**
	 * Method that evaluate if one or more proximity subscriptions are matched and relative 
	 * proximity events have to be fired
	 * @param e1 - first {@link Entity} involved in proximity evaluation
	 * @param e2 - second {@link Entity} involved in proximity evaluation
	 * @param distance - {@link DistanceRange} between the two {@link Entity}
	 */
	private void evaluateProximity(final Entity e1, final Entity e2, final DistanceRange distance, final String logId){

		if(proximitySubscriptionList.size() == 0
				|| e1.getEntityID().equalsIgnoreCase(e2.getEntityID()))		
			return;

		scheduler.schedule(new Runnable() {

			@Override
			public void run() {

				for(Subscription s : proximitySubscriptionList){

					//if e1 belongs to g1 and e2 belongs to g2, or vice versa
					if((s.getG1().evaluate(e1) && s.getG2().evaluate(e2))
							|| 
							(s.getG1().evaluate(e2) && s.getG2().evaluate(e1))){

						//timestamp used for logging
						long timestamp = Calendar.getInstance().getTimeInMillis() + LoggerService.NTP_DELAY;

						getSubscriptionCallback().handleEvent(s,EntityEvent.ENTITY_PROXIMITY_UPDATE, e1, e2, distance);

						//Logging
						if(!logId.equals("")){
							JSONObject status = LogMessageUtils.buildStatus(proximitySubscriptionList.size(),
									groupSubscriptionList.size(), geofenceSubscriptionList.size());
							String log = LogMessageUtils.buildEventLog(logId, selfEntity.getEntityID(), 
									Type.DEVICE, EntityEvent.ENTITY_PROXIMITY_UPDATE.name(), status, timestamp);
							LoggerService.writeToFile(appCtx, log);
						}
					}
				}
			}
		});
	}


	/**
	 * Method that computes the distance between selfEntity and another device, receiving the intersection 
	 * between beacons contained in device's {@link MessageType#PROX_BEACONS} message and selfEntity 
	 * lastSeenBeacons
	 * @param beacons - intersection between selfEntity lastSeenBeacons and beacons contained in the 
	 * {@link MessageType#PROX_BEACONS} received
	 * @param sender - {@link Entity} which sent the {@link MessageType#PROX_BEACONS} message
	 * @param logId - Parameter used only for logging
	 */
	private void evaluateProximityD2D(final ArrayList<Entity> beacons, final Entity sender, final String logId){

		if(beacons.isEmpty() ||  
				sender.getEntityID().equalsIgnoreCase(selfEntity.getEntityID()))
			return;

		scheduler.schedule(new Runnable() {

			@Override
			public void run() {
				DistanceRange bestDistance = DistanceRange.SAME_BEACON;

				ArrayList<Entity> queue;

				synchronized (lastSeenBeacons) {
					queue = new ArrayList<>(lastSeenBeacons);
				}

				for(Entity common : beacons)
					for(Entity mine : queue)
						if(common.equals(mine))
							switch(common.getDistanceRange()){
							case IMMEDIATE:
							case NEXT_TO:
								bestDistance = (mine.getDistanceRange().ordinal() < bestDistance.ordinal())
								? ((mine.getDistanceRange().ordinal() < common.getDistanceRange().ordinal()) 
										? common.getDistanceRange() : mine.getDistanceRange()) : bestDistance;
										break;

							default:
								bestDistance = (mine.getDistanceRange().ordinal() <= DistanceRange.NEXT_TO.ordinal()
								&& common.getDistanceRange().ordinal() < bestDistance.ordinal())
								? common.getDistanceRange() : bestDistance;
								break;
							}

				/*
				 * if security configuration allows sending of proximity messages, a PROXIMITY_UPDATE
				 * message is sent; otherwise, a call to evaluateProximity is performed in order to
				 * verify if a proximity event has to be fired (in fact it is not allowed to send
				 * PROX_BEACONS messages, so it is impossible to receive PROXIMITY_UPDATE messages from
				 * other devices).
				 * 
				 */
				if(securityManager.check_proximity_changes_enabled())
					sendProximityUpdateMessage(selfEntity, sender, bestDistance);
				else
					evaluateProximity(selfEntity, sender, bestDistance, logId);	


				evaluateGeofenceD2D(selfEntity, sender, bestDistance, logId);
			} 
		});
	}


	/**
	 * Method that checks if there is some geofence subscription matching with one or more
	 * beacons contained in the {@link MessageType#PROX_BEACONS} message received.
	 * @param e - {@link Entity} which has received the {@link MessageType#PROX_BEACONS} message
	 * @param beacons - List of {@link Entity} of type {@link Type#BLE_BEACON} contained in the message
	 */
	private void evaluateGeofence(final Entity e, final ArrayList<Entity> beacons, final String logId) {

		if(geofenceSubscriptionList.size() == 0)
			return;

		scheduler.schedule(new Runnable() {

			@Override
			public void run() {

				for(Subscription s : geofenceSubscriptionList){
					Group g = s.getG2();
					Entity eSub = s.getE1();

					if(e.equals(eSub)){

						for(Entity beacon: beacons)
							if(g.evaluate(beacon))
								evaluateGeofenceEventFiring(s,beacon, beacon.getDistanceRange(), logId);
					}else{
						if(g.evaluate(e)){
							if(beacons.size() == 0)		
								//if no beacons are detected in BLE scan, is verified if a geofence exit happened
								evaluateGeofenceEventFiring(s, e, DistanceRange.SAME_WIFI, logId);
							else
								for(Entity beacon: beacons)
									if(beacon.equals(eSub))
										evaluateGeofenceEventFiring(s,e,beacon.getDistanceRange(), logId);
						}
					}
				}				
			}
		});

	}

	
	/**
	 * Method that checks if some geofence subscription is matched by two entities of type {@link Type#DEVICE}
	 * @param e1 - first {@link Entity} to be checked
	 * @param e2 - second {@link Entity} to be checked
	 * @param distance - {@link DistanceRange} the two entities are one respect the other
	 * @param logId - used only for logging
	 */
	private void evaluateGeofenceD2D(final Entity e1, final Entity e2, final DistanceRange distance, final String logId) {

		if(geofenceSubscriptionList.size() == 0) {
			Log.i(TAG, "Nessuna geofenceSubscription presente");
			return;
		}

		scheduler.schedule(new Runnable() {

			@Override
			public void run() {

				for(Subscription s : geofenceSubscriptionList){
					Entity subEntity = s.getE1();
					Group subGroup = s.getG2();

					if(e1.equals(subEntity)){
						Log.i(TAG, e1.getEntityID() + " geofence ENTITY matching");
						if(subGroup.evaluate(e2)){
							Log.i(TAG, e1.getEntityID() + " geofence ENTITY matching AND " +
									e2.getEntityID() + " gefence GROUP matching");
							evaluateGeofenceEventFiring(s, e2, distance, logId);
						}
					}else{
						if(e2.equals(subEntity)){
							Log.i(TAG, e2.getEntityID() + " geofence ENTITY matching");
							if(subGroup.evaluate(e1)){
								Log.i(TAG, e2.getEntityID() + " geofence ENTITY matching AND " +
										e1.getEntityID() + " gefence GROUP matching");
								evaluateGeofenceEventFiring(s, e1, distance, logId);
							}
						}
					}
				}
			}
		});

	}


	/**
	 * Method called by {@link #evaluateGeofence(Entity, ArrayList, String)} and 
	 * {@link #evaluateGeofenceD2D(Entity, Entity, DistanceRange, String)} in order to verify if a matched
	 * geofence subscription has to fire a geofence entry/exit event.
	 * 
	 * @param s - the matching {@link Subscription}
	 * @param e2 - the {@link Entity} that could have caused the event
	 * @param distanceRange - the {@link DistanceRange} <code>e2</code> is from geofence center
	 * @param logId - used only for logging
	 */
	protected void evaluateGeofenceEventFiring(Subscription s, Entity e2, DistanceRange distanceRange, String logId) {
		String ret = s.getAlreadyDetected(e2.getEntityID());

		if(ret != null){
			if(distanceRange.ordinal() > s.getDistance().ordinal()){

				long timestamp = Calendar.getInstance().getTimeInMillis() + LoggerService.NTP_DELAY;

				getSubscriptionCallback().handleEvent(s,EntityEvent.ENTITY_GEOFENCE_EXIT, s.getE1(), e2, distanceRange);
				s.removeAlreadyDetected(ret);

				//logging
				if(!logId.equals("")){
					JSONObject status = LogMessageUtils.buildStatus(proximitySubscriptionList.size(), 
							groupSubscriptionList.size(), geofenceSubscriptionList.size());
					String log = LogMessageUtils.buildEventLog(logId, selfEntity.getEntityID(), 
							Type.DEVICE, EntityEvent.ENTITY_GEOFENCE_EXIT.name(), status, timestamp);
					LoggerService.writeToFile(appCtx, log);
				}
			}
		}else{
			
			if(distanceRange.ordinal() <= s.getDistance().ordinal()){
				
				if(!s.addDetectedEntity(e2.getEntityID())){
					getSubscriptionCallback().handleSubscriptionError(s,ErrorEvent.ERROR_SUBSCRIPTION_ENTITY_SIZE_EXCEEDED);
					return;
				}

				long timestamp = Calendar.getInstance().getTimeInMillis() + LoggerService.NTP_DELAY;

				getSubscriptionCallback().handleEvent(s,EntityEvent.ENTITY_GEOFENCE_ENTRY, s.getE1(), e2, distanceRange);

				//logging
				if(!logId.equals("")){
					JSONObject status = LogMessageUtils.buildStatus(proximitySubscriptionList.size(), 
							groupSubscriptionList.size(), geofenceSubscriptionList.size());
					String log = LogMessageUtils.buildEventLog(logId, selfEntity.getEntityID(), 
							Type.DEVICE, EntityEvent.ENTITY_GEOFENCE_ENTRY.name(), status, timestamp);
					LoggerService.writeToFile(appCtx, log);
				}
			}
		}
	}


	/**
	 * Method that checks if some group event has to be fired as a result of a PROPERTIES_UPDATE message
	 * @param e - the {@link Entity} that updated properties
	 * @param oldProperties - properties of {@link Entity} <code>e</code> before updating 
	 * @param logId - used only for logging
	 */
	private void evaluateGroup(final Entity e, final JSONObject oldProperties, final String logId){

		if(groupSubscriptionList.size() == 0)
			return;

		scheduler.schedule(new Runnable() {

			@Override
			public void run() {

				Builder builder = new Builder(e.getEntityID(), e.getEntityType());
				builder.addProperties(oldProperties);
				Entity oldEntity = builder.build();

				for(Subscription s : groupSubscriptionList){

					/*
					 * if new entity matches the subscription group AND old entity doesn't match it, it is 
					 * a ENTITY_GROUP_JOIN
					 */
					if(!s.getG1().evaluate(oldEntity) && s.getG1().evaluate(e)){
						long timestamp = Calendar.getInstance().getTimeInMillis() + LoggerService.NTP_DELAY;

						getSubscriptionCallback().handleGroupEvent(s,GroupEvent.ENTITY_GROUP_JOIN, e, s.getG1());

						//logging
						JSONObject status = LogMessageUtils.buildStatus(proximitySubscriptionList.size(), 
								groupSubscriptionList.size(), geofenceSubscriptionList.size());
						String log = LogMessageUtils.buildEventLog(logId, selfEntity.getEntityID(), 
								Type.DEVICE, GroupEvent.ENTITY_GROUP_JOIN.name(), status, timestamp);
						LoggerService.writeToFile(appCtx, log);
					}

					/*
					 * if old entity matches the subscription group AND new entity doesn't match it, it is 
					 * a ENTITY_GROUP_LEAVE
					 */
					if(!s.getG1().evaluate(e) && s.getG1().evaluate(oldEntity)){
						long timestamp = Calendar.getInstance().getTimeInMillis() + LoggerService.NTP_DELAY;

						getSubscriptionCallback().handleGroupEvent(s,GroupEvent.ENTITY_GROUP_LEAVE, e, s.getG1());

						JSONObject status = LogMessageUtils.buildStatus(proximitySubscriptionList.size(), 
								groupSubscriptionList.size(), geofenceSubscriptionList.size());
						String log = LogMessageUtils.buildEventLog(logId, selfEntity.getEntityID(), 
								Type.DEVICE, GroupEvent.ENTITY_GROUP_LEAVE.name(), status, timestamp);
						LoggerService.writeToFile(appCtx, log);
					}

				}

			}
		});

	}

	/**
	 * Method used to evaluate a {@link MessageType#CHECK_OUT} message received. If its valid bit is set to
	 * <code>true</code>, method checks for a possible {@link GroupEvent#ENTITY_CHECK_OUT} event; if it is 
	 * <code>false</code>, it sets the {@link Timer} to wait for the corresponding 
	 * {@link MessageType#PROPERTIES_UPDATE} message.
	 * @param e - {@link Entity} that sent the {@link MessageType#CHECK_OUT} message
	 * @param valid_bit - valid boolean field of the message
	 * @param logId - used only for logging
	 */
	private void evaluateCheckOut(final Entity e, final boolean valid_bit, final String logId){

		if(groupSubscriptionList.size() == 0)
			return;

		scheduler.schedule(new Runnable() {

			@Override
			public void run() {

				for(Subscription s : groupSubscriptionList){
 
					if(s.getG1().evaluate(e)){
	
						if(!valid_bit){

							if(waitingForCheckInTasks.containsKey(e.getEntityID()))
								return;

							TimerTask task = new TimerTask() {

								@Override
								public void run() {

									Log.w(TAG, "Timeout CheckIn per " + e.getEntityID());
									waitingForCheckInTasks.remove(e.getEntityID());
									evaluateCheckOut(e, true, "");	
								}
							};

							waitingForCheckInTasks.put(e.getEntityID(), task);

							checkInTimer.schedule(task, CHECK_IN_AFTER_PROP_UPDATE_DELAY);
							Log.d(TAG, "Timeout checkIn registrato per " + e.getEntityID());
							return;
						}else{
							long timestamp = Calendar.getInstance().getTimeInMillis() + LoggerService.NTP_DELAY;

							getSubscriptionCallback().handleGroupEvent(s,GroupEvent.ENTITY_CHECK_OUT, e, s.getG1());

							//logging
							if(!logId.equals("")){
								JSONObject status = LogMessageUtils.buildStatus(proximitySubscriptionList.size(), 
										groupSubscriptionList.size(), geofenceSubscriptionList.size());
								String log = LogMessageUtils.buildEventLog(logId, selfEntity.getEntityID(), 
										Type.DEVICE, GroupEvent.ENTITY_CHECK_OUT.name(), status, timestamp);
								LoggerService.writeToFile(appCtx, log);
							}
						}
					}
				}
			}
		});

	}

	/**
	 * Method used to evaluate if a {@link MessageType#CHECK_IN} message is firing a proximity or a group
	 * event
	 * @param e - {@link Entity} that sent the message
	 * @param logId - used only for logging
	 */
	private void evaluateCheckIn(final Entity e, final String logId){

		if(groupSubscriptionList.size() == 0 ||
				e.getEntityID().equalsIgnoreCase(selfEntity.getEntityID()))
			return;

		scheduler.schedule(new Runnable() {

			@Override
			public void run() {

				for(Subscription s : groupSubscriptionList){

					if(s.getG1().evaluate(e)){
						long timestamp = Calendar.getInstance().getTimeInMillis() + LoggerService.NTP_DELAY;

						getSubscriptionCallback().handleGroupEvent(s,GroupEvent.ENTITY_CHECK_IN, e, s.getG1());

						//logging
						JSONObject status = LogMessageUtils.buildStatus(proximitySubscriptionList.size(), 
								groupSubscriptionList.size(), geofenceSubscriptionList.size());
						String log = LogMessageUtils.buildEventLog(logId, selfEntity.getEntityID(), 
								Type.DEVICE, GroupEvent.ENTITY_CHECK_IN.name(), status, timestamp);
						LoggerService.writeToFile(appCtx, log);
					}

				}
			}
		});
	}

	
	/**
	 * Method used to handle an {@link Entity} responding to a {@link MessageType#SYNC_REQ} message
	 * @param sender - the responding {@link Entity}
	 */
	private void handleSyncResp(final Entity sender){

		synchronized (syncObj) {
			if(syncRespEntityList == null)
				return;

			syncRespEntityList.add(sender);		
		}
	}

	
	/**
	 * Method used to handle a {@link MessageType#SYNC_REQ} message: if the <code>group</code> is matched
	 * and the computed {@link DistanceRange} is less or equal than <code>distanceRange</code>, a 
	 * {@link MessageType#SYNC_RESP} message is sent on the topic <code>topic</code>
	 * @param topic - the topic where the {@link MessageType#SYNC_RESP} has to be sent
	 * @param beacons - beacons contained in the {@link MessageType#SYNC_REQ} message
	 * @param group - the {@link Group} that has to be matched
	 * @param distanceRange - the {@link DistanceRange} that has to be matched
	 */
	private void handleSyncReq(final String topic, final ArrayList<Entity> beacons,
			final JSONObject group, final DistanceRange distanceRange) {

		scheduler.schedule(new Runnable() {

			@Override
			public void run() {

				PropertiesFilter filter = null;

				DistanceRange maxDistance = distanceRange;

				if(!((String)group.get(JsonStrings.FILTER)).equals(""))
					try {
						filter = PropertiesFilter.parseFromString((String)group.get(JsonStrings.FILTER));
					} catch (Exception e) {
						e.printStackTrace();
					}
				String entityId = (String) group.get(JsonStrings.ENTITY_ID);
				Type entityType = Type.valueOf((String)group.get(JsonStrings.ENTITY_TYPE));
				String groupDesc = (String)group.get(JsonStrings.GROUP_DESCRIPTOR);

				Group g = new Group.Builder(groupDesc)
				.setEntityID(entityId)
				.setEntityType(entityType)
				.setFilter(filter)
				.build();

				if(!g.evaluate(selfEntity))
					return;

				DistanceRange bestDistance = DistanceRange.SAME_WIFI;
				ArrayList<Entity> queue;

				synchronized (lastSeenBeacons) {
					queue = new ArrayList<>(lastSeenBeacons);
					//intersection to obtain common beacons
					beacons.retainAll(lastSeenBeacons);		
				}


				Builder builder = new Builder(selfEntity.getEntityID(),Type.DEVICE);
				JSONObject selfProps = selfEntity.getProperties();
				builder.addProperties(selfProps);

				if(beacons.size() == 0){
					if(DistanceRange.SAME_WIFI.ordinal() <= maxDistance.ordinal()){
						builder.setDistance(DistanceRange.SAME_WIFI);
						networkClient.publishMessage(topic, MessageUtils.buildSyncRespMessage(builder.build(), topic));
					}
					return;
				}

				for(Entity common : beacons)
					for(Entity mine : queue)
						if(common.equals(mine))
							switch(common.getDistanceRange()){
							case IMMEDIATE:
							case NEXT_TO:
								bestDistance = (mine.getDistanceRange().ordinal() < bestDistance.ordinal())
								? ((mine.getDistanceRange().ordinal() < common.getDistanceRange().ordinal()) 
										? common.getDistanceRange() : mine.getDistanceRange()) : bestDistance;
										break;

							default:
								bestDistance = (mine.getDistanceRange().ordinal() <= DistanceRange.NEXT_TO.ordinal()
								&& common.getDistanceRange().ordinal() < bestDistance.ordinal())
								? common.getDistanceRange() : bestDistance;
								break;
							}

				if(bestDistance.ordinal() <= maxDistance.ordinal()){
					builder.setDistance(bestDistance);
					networkClient.publishMessage(topic, MessageUtils.buildSyncRespMessage(builder.build(), topic));
				}
			}
		});

	}


	/**
	 * Method that has to be called in order to enable Ghost Mode
	 */
	public void enableGhostMode() {
		if(securityManager.check_old_group_settings())
			networkClient.publishMessage(MessageTopic.BROADCAST.name(),
					MessageUtils.buildCheckOutMessage(selfEntity, true));

		securityManager.enableGhostMode();
		this.sendFakeUpdateProp();
	}

	/**
	 * Method that has to be called in order to disable Ghost Mode
	 */
	public void disableGhostMode() {
		if(securityManager.check_old_group_settings()){
			long timestamp = Calendar.getInstance().getTimeInMillis() + LoggerService.NTP_DELAY;
			String logId = selfEntity.getEntityID() + timestamp;

			networkClient.publishMessage(MessageTopic.BROADCAST.name(), 
					MessageUtils.buildCheckInMessage(selfEntity, logId));

			//logging
			String log = LogMessageUtils.buildMessageSentLog(logId, selfEntity.getEntityID(), 
					Type.DEVICE, MessageType.CHECK_IN, null, false, timestamp);
			LoggerService.writeToFile(appCtx, log);
		}

		securityManager.disableGhostMode();
		this.sendFakeUpdateProp();
	}


	public String getPOIEventBroadcastAction(){
		return appCtx.getPackageName() + ".POI_EVENT";
	}

	private void sendFakeUpdateProp(){
		try {
			updateSelfEntity(selfEntity, true);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
