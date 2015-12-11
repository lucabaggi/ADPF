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

package it.polimi.logging;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import it.polimi.geinterface.DAO.Entity.Type;
import it.polimi.geinterface.DAO.JsonStrings;
import it.polimi.geinterface.network.MessageType;

/**
 * 
 * Classe che contiene i metodi di utility per i messaggi di logging
 * @author luca
 *
 */
public class LogMessageUtils {

	/*
	 * 
	 * 
	 * 
	 * METODI DI ESTRAZIONE CAMPI
	 * 
	 * 
	 * 
	 * 
	 */


	public static String getSenderID(String msgJson){
		JSONParser parser = new JSONParser();
		JSONObject jsonMsg;
		String senderID = "";
		try {
			jsonMsg = (JSONObject) parser.parse(msgJson);
			senderID = (String)jsonMsg.get(JsonStrings.SENDER);
			return senderID;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}


	public static String getLogId(String message){
		JSONParser parser = new JSONParser();
		JSONObject jsonMsg;
		String logId = "";
		try {
			jsonMsg = (JSONObject) parser.parse(message);
			logId = (String)jsonMsg.get(JsonStrings.LOG_ID);
			return logId;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
	
	public static String getTopicReply(String message){
		JSONParser parser = new JSONParser();
		JSONObject jsonMsg;
		String topic = "";
		try {
			jsonMsg = (JSONObject) parser.parse(message);
			topic = (String)jsonMsg.get(JsonStrings.TOPIC_REPLY);
			return topic;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}


	public static LogType getLogType(String message){
		JSONParser parser = new JSONParser();
		JSONObject jsonMsg;
		String logType = "";
		try {
			jsonMsg = (JSONObject) parser.parse(message);
			logType = (String)jsonMsg.get(JsonStrings.LOG_TYPE);
			return LogType.valueOf(logType);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}



	public static Type getEntityType(String message){
		JSONParser parser = new JSONParser();
		JSONObject jsonMsg;
		String type = "";
		try {
			jsonMsg = (JSONObject) parser.parse(message);
			type = (String)jsonMsg.get(JsonStrings.ENTITY_TYPE);
			return Type.valueOf(type);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}


	public static String getEventType(String message){
		JSONParser parser = new JSONParser();
		JSONObject jsonMsg;
		String type = "";
		try {
			jsonMsg = (JSONObject) parser.parse(message);
			type = (String)jsonMsg.get(JsonStrings.EVENT_TYPE);
			return type;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}


	public static long getTimestamp(String message){
		JSONParser parser = new JSONParser();
		JSONObject jsonMsg;
		long timestamp;
		try {
			jsonMsg = (JSONObject) parser.parse(message);
			timestamp = (long)jsonMsg.get(JsonStrings.TIMESTAMP);
			return timestamp;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return -1;
	}


	public static boolean getValidBitFromMessage(String message){

		JSONObject jsonMsg;
		JSONParser parser = new JSONParser();
		String type = getEventType(message);

		if(type.equals(MessageType.CHECK_OUT.ordinal())){
			try {
				jsonMsg = (JSONObject) parser.parse(message);
				return (Boolean) jsonMsg.get(JsonStrings.VALID);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		return false;
	}
	
	
	public static JSONObject getStatus(String message){
		
		JSONObject jsonMsg;
		JSONParser parser = new JSONParser();
		
		try {
			jsonMsg = (JSONObject) parser.parse(message);
			JSONObject status = (JSONObject) jsonMsg.get(JsonStrings.STATUS);
			return status;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return null;
	}



	/*
	 * 
	 * 
	 * 
	 * METODI DI BUILD DEI LOGMESSAGE
	 * 
	 * 
	 * 
	 * 
	 */


	/**
	 * 
	 * Costruisce un messaggio di log per rappresentare i messaggi inviati sulla rete da un device
	 * 
	 * @param id
	 * @param sender
	 * @param type
	 * @param logType
	 * @param eventType - rappresenta l'evento che verrà eventualmente scatenato, quindi il {@link MessageType}
	 * @param valid - ha senso solo per i messaggi di tipo {@link MessageType#CHECK_OUT}
	 * @param timestamp
	 * @return
	 */
	public static String buildMessageSentLog(String id, String sender, Type type,
			MessageType eventType, String topic_reply, boolean valid, long timestamp){

		JSONObject msg = new JSONObject();
		msg.put(JsonStrings.LOG_ID, id);
		msg.put(JsonStrings.SENDER, sender);
		msg.put(JsonStrings.ENTITY_TYPE, type.name());
		msg.put(JsonStrings.LOG_TYPE, LogType.SENT_MSG.name());
		msg.put(JsonStrings.EVENT_TYPE, eventType.name());

		if(eventType.equals(MessageType.SYNC_REQ))
			msg.put(JsonStrings.TOPIC_REPLY, topic_reply);

		if(eventType.equals(MessageType.CHECK_OUT))
			msg.put(JsonStrings.VALID, valid);
		
		msg.put(JsonStrings.TIMESTAMP, timestamp);

		return msg.toJSONString();

	}

	/**
	 * 
	 * Costruisce un messaggio di log rappresentante un messaggio ricevuto da un device
	 * 
	 * @param id
	 * @param sender
	 * @param type
	 * @param logType
	 * @param eventType
	 * @param timestamp
	 * @return
	 */
	public static String buildMessageReceivedLog(String id, String sender, Type type,
			MessageType eventType, String topic_reply, JSONObject status, long timestamp){

		JSONObject msg = new JSONObject();
		msg.put(JsonStrings.LOG_ID, id);
		msg.put(JsonStrings.SENDER, sender);
		msg.put(JsonStrings.ENTITY_TYPE, type.name());
		msg.put(JsonStrings.LOG_TYPE, LogType.REC_MSG.name());
		msg.put(JsonStrings.EVENT_TYPE, eventType.name());
		
		if(eventType.equals(MessageType.SYNC_RESP))
			msg.put(JsonStrings.TOPIC_REPLY, topic_reply);
		
		msg.put(JsonStrings.STATUS, status);
		msg.put(JsonStrings.TIMESTAMP, timestamp);

		return msg.toJSONString();

	}


	/**
	 * 
	 * Costruisce un messaggio di log rappresentante un evento scatenato in seguito ad un 
	 * messaggio ricevuto
	 * 
	 * @param id
	 * @param sender
	 * @param type
	 * @param logType
	 * @param eventType
	 * @param status
	 * @param timestamp
	 * @return
	 */
	public static String buildEventLog(String id, String sender, Type type,
			String eventType, JSONObject status, long timestamp){

		JSONObject msg = new JSONObject();
		msg.put(JsonStrings.LOG_ID, id);
		msg.put(JsonStrings.SENDER, sender);
		msg.put(JsonStrings.ENTITY_TYPE, type.name());
		msg.put(JsonStrings.LOG_TYPE, LogType.EVENT.name());
		msg.put(JsonStrings.EVENT_TYPE, eventType);
		msg.put(JsonStrings.STATUS, status);
		msg.put(JsonStrings.TIMESTAMP, timestamp);

		return msg.toJSONString();

	}


	public static JSONObject buildStatus(int proxSubCount, int groupSubCount, int geoSubCount){
		JSONObject status = new JSONObject();
		status.put(JsonStrings.PROX_SUB_COUNT, proxSubCount);
		status.put(JsonStrings.GROUP_SUB_COUNT, groupSubCount);
		status.put(JsonStrings.GEO_SUB_COUNT, geoSubCount);
		return status;
	}




}
