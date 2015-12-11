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

import it.polimi.geinterface.DistanceRange;
import it.polimi.geinterface.DAO.Entity;
import it.polimi.geinterface.DAO.Entity.Builder;
import it.polimi.geinterface.DAO.Entity.Type;
import it.polimi.geinterface.DAO.Group;
import it.polimi.geinterface.DAO.JsonStrings;

import java.util.ArrayList;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


/**
 * Utility class containing useful methods for message building and data to retrieve.
 */
public class MessageUtils {

	/**
	 * Method retrieving message type form a {@link String} representing the message received.
	 */
	public static MessageType getMsgType(String msgJson){

		JSONParser parser = new JSONParser();
		JSONObject jsonMsg;
		String msgType = "";
		try {
			jsonMsg = (JSONObject) parser.parse(msgJson);
			JSONObject message = (JSONObject) jsonMsg.get(JsonStrings.MESSAGE);
			msgType = ((String) message.get(JsonStrings.MSG_TYPE)).toUpperCase().trim();
		} catch (ParseException e) {
			e.printStackTrace();
		}

		for(MessageType type : MessageType.values())
			if(type.name().equals(msgType))
				return type;

		return null;	
	}


	/**
	 * Method that return the sender {@link Entity} identifier from a message 
	 */
	public static String getSenderID(String msgJson){
		JSONParser parser = new JSONParser();
		JSONObject jsonMsg;
		String senderID = "";
		try {
			jsonMsg = (JSONObject) parser.parse(msgJson);
			JSONObject message = (JSONObject) jsonMsg.get(JsonStrings.MESSAGE);
			senderID = (String)message.get(JsonStrings.SENDER);
			return senderID;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}


	/**
	 * Method that returns the proper {@link MessageTopic} where send a message having {@link MessageType} passeds
	 */
	public static MessageTopic getMsgTopic(MessageType type){

		if(type.equals(MessageType.PROXIMITY_UPDATE))
			return MessageTopic.PROXIMITY;

		if(type.equals(MessageType.PROPERTIES_UPDATE))
			return MessageTopic.GROUP;

		return MessageTopic.BROADCAST;
	}

	/**
	 * Method that returns an {@link Entity} from a message, dealing woth different {@link MessageType}.
	 * 
	 * @param position - possible values are 1 or 2, returning the first or the second {@link Entity} contained in the message.
	 * 				position = 2 makes sense only in case of {@link MessageType#PROXIMITY_UPDATE}. In case of other {@link MessageType} only
	 * 				position = 1 can be passed.
	 */
	public static Entity getEntityFromMessage(int position, String message){

		JSONObject jsonMsg;
		JSONParser parser = new JSONParser();
		MessageType type = getMsgType(message);

		if(position == 1){
			switch (type) {
			case PROXIMITY_UPDATE:
				try {
					jsonMsg = (JSONObject) parser.parse(message);
					JSONObject msg = (JSONObject) jsonMsg.get(JsonStrings.MESSAGE);
					JSONObject jsonEntity = (JSONObject) msg.get(JsonStrings.ENTITY_1);
					return createEntity(type, jsonEntity);
				} catch (ParseException e) {
					e.printStackTrace();
				}				
				break;
			case PROPERTIES_UPDATE:
			case PROX_BEACONS:
			case SYNC_RESP:
			case CHECK_IN:
			case CHECK_OUT:
				try {
					jsonMsg = (JSONObject) parser.parse(message);
					JSONObject msg = (JSONObject) jsonMsg.get(JsonStrings.MESSAGE);
					JSONObject jsonEntity = (JSONObject) msg.get(JsonStrings.ENTITY);
					return createEntity(type, jsonEntity);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			default: 
				return null;
			}
			return null;
		}

		if(position == 2){
			if(type.ordinal() == MessageType.PROXIMITY_UPDATE.ordinal()){
				try {
					jsonMsg = (JSONObject) parser.parse(message);
					JSONObject msg = (JSONObject) jsonMsg.get(JsonStrings.MESSAGE);
					JSONObject jsonEntity = (JSONObject) msg.get(JsonStrings.ENTITY_2);
					return createEntity(type, jsonEntity);
				} catch (ParseException e) {
					e.printStackTrace();
				}	
			}
			return null;
		}

		return null;
	}

	/**
	 * Method returning a list of {@link Entity} objects retrieved from a message having type {@link MessageType#PROX_BEACONS} or 
	 * {@link MessageType#SYNC_RESP}
	 */
	public static ArrayList<Entity> getBeaconsFromMsg(String message){

		JSONObject jsonMsg;
		JSONParser parser = new JSONParser();
		MessageType type = getMsgType(message);

		if(type.ordinal() == MessageType.PROX_BEACONS.ordinal() ||
				type.ordinal() == MessageType.SYNC_REQ.ordinal())
			try {
				jsonMsg = (JSONObject) parser.parse(message);
				JSONObject msg = (JSONObject) jsonMsg.get(JsonStrings.MESSAGE);
				JSONArray beaconsJsonArray = (JSONArray) msg.get(JsonStrings.BEACONS);
				ArrayList<Entity> beacons = new ArrayList<>();
				for(Object o : beaconsJsonArray){
					JSONObject beaconJsonObject = (JSONObject) o;
					String beaconId = (String)beaconJsonObject.get(JsonStrings.BEACON_ID);
					String jsonDistance = (String) beaconJsonObject.get(JsonStrings.DISTANCE_RANGE);
					DistanceRange distance = null;
					for(DistanceRange d : DistanceRange.values())
						if(d.name().equals(jsonDistance))
							distance = d;
					Entity beacon = new Entity.Builder(beaconId, Type.BLE_BEACON)
					.setDistance(distance)
					.build();
					beacons.add(beacon);
				}
				return beacons;
			} catch (ParseException e) {
				e.printStackTrace();
			}	
		return null;
	}

	/**
	 * Method returning the {@link DistanceRange} reported in a message having type {@link MessageType#PROPERTIES_UPDATE}
	 */
	public static DistanceRange getDistanceRangeFromMessage(String message){

		JSONObject jsonMsg;
		JSONParser parser = new JSONParser();
		MessageType type = getMsgType(message);

		if(type.ordinal() == MessageType.PROXIMITY_UPDATE.ordinal() ||
				type.ordinal() == MessageType.SYNC_RESP.ordinal() ||
				type.ordinal() == MessageType.SYNC_REQ.ordinal()){
			try {
				jsonMsg = (JSONObject) parser.parse(message);
				JSONObject msg = (JSONObject) jsonMsg.get(JsonStrings.MESSAGE);
				String jsonDistance = (String) msg.get(JsonStrings.DISTANCE_RANGE);
				for(DistanceRange d : DistanceRange.values())
					if(d.name().equals(jsonDistance))
						return d;
			} catch (ParseException e) {
				e.printStackTrace();
			}	
		}

		return null;
	}

	/**
	 * Method returning a {@link Set} of oldProperties passing as parameter a message of type {@link MessageType#PROPERTIES_UPDATE}
	 */
	public static JSONObject getOldPropertiesFromMessage(String message){

		JSONObject jsonMsg;
		JSONParser parser = new JSONParser();
		MessageType type = getMsgType(message);

		if(type.ordinal() == MessageType.PROPERTIES_UPDATE.ordinal()){
			try {
				jsonMsg = (JSONObject) parser.parse(message);
				JSONObject msg = (JSONObject) jsonMsg.get(JsonStrings.MESSAGE);
				JSONObject jsonEntity = (JSONObject) msg.get(JsonStrings.ENTITY);
				JSONObject jsonOldProp = (JSONObject) jsonEntity.get(JsonStrings.OLD_PROPERTIES);
				/*
				Set<String> oldProperties = new HashSet<String>();
				for(Object o : jsonOldProp)
					if(!o.toString().equals(""))
						oldProperties.add(o.toString());
				 */
				return jsonOldProp;
			} catch (ParseException e) {
				e.printStackTrace();
			}	
		}

		return null;
	}

	/**
	 * Method returning the topic to use to send a {@link MessageType#SYNC_RESP} in reply to a corresponding request
	 */
	public static String getRequestTopicFromMessage(String message){

		JSONObject jsonMsg;
		JSONParser parser = new JSONParser();
		MessageType type = getMsgType(message);

		if(type.ordinal() == MessageType.SYNC_REQ.ordinal() ||
				type.ordinal() == MessageType.SYNC_RESP.ordinal()){
			try {
				jsonMsg = (JSONObject) parser.parse(message);
				JSONObject msg = (JSONObject) jsonMsg.get(JsonStrings.MESSAGE);
				return (String) msg.get(JsonStrings.TOPIC_REPLY);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	/**
	 * Method returning the "valid bit" value of a {@link MessageType#CHECK_OUT}
	 */
	public static boolean getValidBitFromMessage(String message){

		JSONObject jsonMsg;
		JSONParser parser = new JSONParser();
		MessageType type = getMsgType(message);

		if(type.ordinal() == MessageType.CHECK_OUT.ordinal()){
			try {
				jsonMsg = (JSONObject) parser.parse(message);
				JSONObject msg = (JSONObject) jsonMsg.get(JsonStrings.MESSAGE);
				return (Boolean) msg.get(JsonStrings.VALID);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		return false;
	}

	/**
	 * Method returning the {@link Group} "serialized" in a {@link MessageType#SYNC_REQ}
	 */
	public static JSONObject getGroupFromMessage(String msg){

		JSONObject jsonMsg;
		JSONParser parser = new JSONParser();
		MessageType type = getMsgType(msg);

		if(type.ordinal() == MessageType.SYNC_REQ.ordinal()){
			try {
				jsonMsg = (JSONObject) parser.parse(msg);
				JSONObject msgObject = (JSONObject) jsonMsg.get(JsonStrings.MESSAGE);
				return (JSONObject) msgObject.get(JsonStrings.GROUP);
			} catch (ParseException e) {
				e.printStackTrace();
			}		
		}

		return null;
	}

	public static String getLogIdFromMessage(String msg){
		JSONObject jsonMsg;
		JSONParser parser = new JSONParser();

		try {
			jsonMsg = (JSONObject) parser.parse(msg);
			JSONObject msgObject = (JSONObject) jsonMsg.get(JsonStrings.MESSAGE);
			return (String) msgObject.get(JsonStrings.LOG_ID);
		} catch (ParseException e) {
			e.printStackTrace();
		}		

		return null;
	}


	/*
	 * 
	 * 			MESSAGE BUILDING UTILITY METHODS
	 * 
	 */


	/**
	 * 
	 * Method that builds the message sent to notify a proximity update between two {@link Entity}, containing
	 * the corresponding {@link DistanceRange}
	 */
	public static String buildProximityMessage(String senderID, MessageType type, Entity e1, 
			Entity e2, DistanceRange distance, String logId){

		JSONObject mesg = new JSONObject();
		JSONObject proxMsg = new JSONObject();
		proxMsg.put(JsonStrings.SENDER, senderID);
		proxMsg.put(JsonStrings.MSG_TYPE, type.name());
		proxMsg.put(JsonStrings.ENTITY_1, (JSONObject)e1.getJsonDescriptor().get(JsonStrings.ENTITY));
		proxMsg.put(JsonStrings.ENTITY_2, (JSONObject)e2.getJsonDescriptor().get(JsonStrings.ENTITY));
		proxMsg.put(JsonStrings.DISTANCE_RANGE, distance.name());
		proxMsg.put(JsonStrings.LOG_ID, logId);
		mesg.put(JsonStrings.MESSAGE, proxMsg);

		return mesg.toJSONString();
	}


	/**
	 * Method that builds join/leave messages relative to a {@link Group},
	 * sent by an {@link Entity} (a {@link MessageType#PROPERTIES_UPDATE})
	 */
	public static String buildGroupMessage(String senderID, Entity e, JSONObject oldProperties, String logId){

		String groupMsg = "{\"" + JsonStrings.MESSAGE +"\":"
				+ "{\"" + JsonStrings.SENDER + "\":\"" +  senderID +"\","
				+ "\"" + JsonStrings.MSG_TYPE + "\":\"" + MessageType.PROPERTIES_UPDATE.name() + "\","
				+ "\"" + JsonStrings.LOG_ID + "\":\"" + logId + "\","
				+ "\"" + JsonStrings.ENTITY + "\":{"
				+ "\"" + JsonStrings.ENTITY_ID + "\":\"" + e.getEntityID() + "\","
				+ "\"" + JsonStrings.ENTITY_TYPE + "\":\"" + e.getEntityType().name() + "\","
				+ "\"" + JsonStrings.DISTANCE_RANGE + "\":\"" + e.getDistanceRange().name() + "\","
				+ "\"" + JsonStrings.CURRENT_PROPERTIES + "\":" + e.getProperties() + ",";

		groupMsg += "\"" + JsonStrings.OLD_PROPERTIES + "\":" + oldProperties + "}}}";

		return groupMsg;
	}

	/**
	 * Method that builds a {@link MessageType#CHECK_IN} message relative to the passed {@link Entity}
	 */
	public static String buildCheckInMessage(Entity e, String logId){
		JSONObject mesg = new JSONObject();
		JSONObject checkInMsg = new JSONObject();
		checkInMsg.put(JsonStrings.SENDER, e.getEntityID());
		checkInMsg.put(JsonStrings.MSG_TYPE, MessageType.CHECK_IN.name());
		checkInMsg.put(JsonStrings.ENTITY, ((JSONObject)e
				.getJsonDescriptor()
				.get(JsonStrings.ENTITY)));
		checkInMsg.put(JsonStrings.LOG_ID, logId);
		mesg.put(JsonStrings.MESSAGE, checkInMsg);

		return mesg.toJSONString();
	}

	/**
	 * Method that builds a {@link MessageType#CHECK_OUT} message relative to the passed {@link Entity}
	 */
	public static String buildCheckOutMessage(Entity e, boolean isValid){
		JSONObject mesg = new JSONObject();
		JSONObject checkOutMsg = new JSONObject();
		checkOutMsg.put(JsonStrings.SENDER, e.getEntityID());
		checkOutMsg.put(JsonStrings.MSG_TYPE, MessageType.CHECK_OUT.name());
		checkOutMsg.put(JsonStrings.ENTITY, ((JSONObject)e
				.getJsonDescriptor()
				.get(JsonStrings.ENTITY)));
		checkOutMsg.put(JsonStrings.VALID, isValid);
		mesg.put(JsonStrings.MESSAGE, checkOutMsg);

		return mesg.toJSONString();
	}

	/**
	 * Method that build a {@link MessageType#SYNC_REQ}, passed a {@link String} representing the topic reply where
	 * other entities have to send the corresponding {@link MessageType#SYNC_RESP} message
	 */
	public static String buildSyncReqMessage(String senderID, String topicReply, 
			ArrayList<Entity> beacons, Group group, DistanceRange distance, String logId){
		JSONObject mesg = new JSONObject();
		JSONObject syncReqMsg = new JSONObject();
		syncReqMsg.put(JsonStrings.SENDER, senderID);
		syncReqMsg.put(JsonStrings.MSG_TYPE, MessageType.SYNC_REQ.name());
		syncReqMsg.put(JsonStrings.TOPIC_REPLY, topicReply);
		syncReqMsg.put(JsonStrings.BEACONS, ((JSONArray)buildBeaconsJsonArray(beacons)));

		JSONObject jsonGroup = new JSONObject();
		jsonGroup.put(JsonStrings.FILTER, (group.getFilter() != null) ? group.getFilter().toString() : "");
		jsonGroup.put(JsonStrings.ENTITY_ID, group.getEntity_id());
		jsonGroup.put(JsonStrings.ENTITY_TYPE, group.getType().name());
		jsonGroup.put(JsonStrings.GROUP_DESCRIPTOR, group.toString());

		syncReqMsg.put(JsonStrings.GROUP, jsonGroup);

		syncReqMsg.put(JsonStrings.DISTANCE_RANGE, distance.name());	
		syncReqMsg.put(JsonStrings.LOG_ID, logId);
		mesg.put(JsonStrings.MESSAGE, syncReqMsg);

		return mesg.toJSONString();
	}

	/**
	 * Method that builds the {@link MessageType#SYNC_RESP} message corresponding to a {@link MessageType#SYNC_REQ}
	 */
	public static String buildSyncRespMessage(Entity e, String topicReply){
		JSONObject msg = new JSONObject();
		JSONObject syncRespMsg = new JSONObject();
		syncRespMsg.put(JsonStrings.SENDER, e.getEntityID());
		syncRespMsg.put(JsonStrings.MSG_TYPE, MessageType.SYNC_RESP.name());
		
		/*
		 * Only for logging
		 */
		syncRespMsg.put(JsonStrings.TOPIC_REPLY, topicReply);
		
	
		syncRespMsg.put(JsonStrings.ENTITY,((JSONObject)e.getJsonDescriptor().get(JsonStrings.ENTITY)));
		msg.put(JsonStrings.MESSAGE, syncRespMsg);

		return msg.toJSONString();
	}

	/**
	 * Method building the {@link MessageType#PROX_BEACONS} message containing data about {@link Entity} of type {@link Type#BLE_BEACON}
	 * seen by the sender in last BLE scanning cycles
	 */
	public static String buildProxBeaconsMessage(ArrayList<Entity> beacons, Entity e, String logId){
		JSONObject msg = new JSONObject();
		JSONObject proxMsg = new JSONObject();
		proxMsg.put(JsonStrings.SENDER, e.getEntityID());
		proxMsg.put(JsonStrings.MSG_TYPE, MessageType.PROX_BEACONS.name());
		proxMsg.put(JsonStrings.ENTITY, ((JSONObject)e.getJsonDescriptor().get(JsonStrings.ENTITY)));
		proxMsg.put(JsonStrings.BEACONS, ((JSONArray)buildBeaconsJsonArray(beacons)));
		proxMsg.put(JsonStrings.LOG_ID, logId);
		msg.put(JsonStrings.MESSAGE, proxMsg);
		return msg.toJSONString();
	}



	/*
	 * 
	 * HELPER METHODS
	 * 
	 * 
	 */

	private static Entity createEntity(MessageType type, JSONObject jsonEntity){

		Builder builder;

		String entityId = (String) jsonEntity.get(JsonStrings.ENTITY_ID);
		String jsonEntityType = (String) jsonEntity.get(JsonStrings.ENTITY_TYPE);
		JSONObject jsonProperties;
		if(type.ordinal() == MessageType.PROPERTIES_UPDATE.ordinal())
			jsonProperties = (JSONObject) jsonEntity.get(JsonStrings.CURRENT_PROPERTIES);
		else {
			jsonProperties = (JSONObject) jsonEntity.get(JsonStrings.PROPERTIES);		
		}

		Type entityType = null;
		for(Type t : Type.values())
			if(jsonEntityType.equalsIgnoreCase(t.name()))
				entityType = t;

		String jsonDistance = (String) jsonEntity.get(JsonStrings.DISTANCE_RANGE);
		DistanceRange distanceRange = null;
		for(DistanceRange d : DistanceRange.values())
			if(jsonDistance.equalsIgnoreCase(d.name()))
				distanceRange = d;


		if(entityType != null){
			builder = new Builder(entityId, entityType)
			.setDistance(distanceRange)
			.addProperties(jsonProperties);


			return builder.build();
		}

		return null;				
	}

	/**
	 * Helper method building a {@link JSONArray} containing beacons data to include in some framework messages
	 */
	private static JSONArray buildBeaconsJsonArray(ArrayList<Entity> beacons){
		JSONArray beaconsArray = new JSONArray();
		for(Entity b : beacons){
			JSONObject beacon = new JSONObject();
			String beaconId = b.getEntityID();
			beacon.put(JsonStrings.BEACON_ID, beaconId);
			beacon.put(JsonStrings.DISTANCE_RANGE, 
					b.getDistanceRange().name());
			beaconsArray.add(beacon);
		}
		return beaconsArray;
	}
	
	
	/*
	 * 
	 * 
	 * LOGGING HELPER METHODS
	 * 
	 * 
	 * 
	 */
	
	
	public static String addLogField(String message, String logId){
		JSONParser parser = new JSONParser();
		try {
			JSONObject msg = (JSONObject) parser.parse(message);
			JSONObject m = (JSONObject) msg.get(JsonStrings.MESSAGE);
			MessageType msgType = MessageType.valueOf((String)m.get(JsonStrings.MSG_TYPE));
			if(msgType.equals(MessageType.CHECK_OUT)){
				m.put(JsonStrings.LOG_ID, logId);
				JSONObject newJsonMsg = new JSONObject();
				newJsonMsg.put(JsonStrings.MESSAGE, m);
				return newJsonMsg.toJSONString();
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return message;
	}
}
