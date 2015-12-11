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

package it.polimi.proximityapi.jsonLogic;

import it.polimi.geinterface.DAO.JsonStrings;
import it.polimi.geinterface.DAO.POI;
import it.polimi.proximityapi.TechnologyManager;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Parser taking as input the {@link String} representation of a {@link JSONArray} object containing {@link POI} definitions.
 * It returns and {@link ArrayList} of {@link POI} objects with corresponding data
 */
public class POIJsonParser {
		
	public static ArrayList<POI> parsePOIFile(String jsonString)
	{
		ArrayList<POI> poiList = new ArrayList<>();
		JSONParser parser = new JSONParser();
		
		JSONArray poiArray;
		try {
			poiArray = (JSONArray) parser.parse(jsonString);
			for(int i = 0; i < poiArray.size(); i++){
				JSONObject jsonPOI = (JSONObject) poiArray.get(i);
				POI poi = new POI();
				poi.setName((String)jsonPOI.get(JsonStrings.NAME));
				poi.setLatitude(Double.parseDouble((String)jsonPOI.get(JsonStrings.LATITUDE)));
				poi.setLongitude(Double.parseDouble((String)jsonPOI.get(JsonStrings.LONGITUDE)));
				if(!jsonPOI.get(JsonStrings.RADIUS).equals(""))
					poi.setRadius(Float.parseFloat((String)jsonPOI.get(JsonStrings.RADIUS)));
				else {
					//Default radius set when it is not set in the corresponding JSON object
					poi.setRadius(TechnologyManager.BT_START_GEOFENCE_RADIUS);
				}
				poi.setBeaconUuid((String)jsonPOI.get(JsonStrings.BEACON_UUID));
				poiList.add(poi);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return poiList;
	}

	
}