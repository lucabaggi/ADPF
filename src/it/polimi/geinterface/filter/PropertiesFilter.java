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

import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.jayway.jsonpath.Criteria;
import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;

public class PropertiesFilter extends JSONObject{


	private PropertiesFilter(){}

	/**
	 * Class used to create a {@link PropertiesFilter} starting from other {@link PropertiesFilter} or from 
	 * primitive predicates.
	 * For primitive predicates, a path as in {@link JsonPath} is needed.
	 */
	public static class Builder{

		public static PropertiesFilter and(PropertiesFilter filter1, PropertiesFilter filter2){
			PropertiesFilter ret = new PropertiesFilter();
			ret.put(ReservedKeys.$op.name(), FilterType.AND.name());
			ret.put(ReservedKeys.$filter_1.name(), filter1);
			ret.put(ReservedKeys.$filter_2.name(), filter2);
			return ret;
		}

		public static PropertiesFilter or(PropertiesFilter filter1, PropertiesFilter filter2){
			PropertiesFilter ret = new PropertiesFilter();
			ret.put(ReservedKeys.$op.name(), FilterType.OR.name());
			ret.put(ReservedKeys.$filter_1.name(), filter1);
			ret.put(ReservedKeys.$filter_2.name(), filter2);
			return ret;
		}

		public static PropertiesFilter contains(String path, Object value){
			PropertiesFilter ret = new PropertiesFilter();
			ret.put(ReservedKeys.$op.name(), FilterType.CONTAINS.name());
			ret.put(ReservedKeys.$key.name(), path);
			ret.put(ReservedKeys.$val.name(), value);
			return ret;
		}

		public static PropertiesFilter equals(String path, Object value){
			PropertiesFilter ret = new PropertiesFilter();
			ret.put(ReservedKeys.$op.name(), FilterType.EQUALS.name());
			ret.put(ReservedKeys.$key.name(), path);
			ret.put(ReservedKeys.$val.name(), value);
			return ret;
		}

		public static PropertiesFilter gt(String path, Object value){
			PropertiesFilter ret = new PropertiesFilter();
			ret.put(ReservedKeys.$op.name(), FilterType.GT.name());
			ret.put(ReservedKeys.$key.name(), path);
			ret.put(ReservedKeys.$val.name(), value);
			return ret;
		}

		public static PropertiesFilter lt(String path, Object value){
			PropertiesFilter ret = new PropertiesFilter();
			ret.put(ReservedKeys.$op.name(), FilterType.LT.name());
			ret.put(ReservedKeys.$key.name(), path);
			ret.put(ReservedKeys.$val.name(), value);
			return ret;
		}
		
		public static PropertiesFilter exists(String key_path){
			PropertiesFilter ret = new PropertiesFilter();
			ret.put(ReservedKeys.$op.name(), FilterType.EXISTS.name());
			ret.put(ReservedKeys.$key.name(), key_path);
			ret.put(ReservedKeys.$val.name(), true);
			return ret;
		}
		
		public static PropertiesFilter not_exists(String key_path){
			PropertiesFilter ret = new PropertiesFilter();
			ret.put(ReservedKeys.$op.name(), FilterType.NOT_EXISTS.name());
			ret.put(ReservedKeys.$key.name(), key_path);
			ret.put(ReservedKeys.$val.name(), false);
			return ret;
		}
	}


	/**
	 * Method that parse a JSON {@link String} representing a filter into the corresponding {@link PropertiesFilter}
	 * @param jsonPropFilter - the JSON {@link String} representing the filter
	 * @return the corresponding {@link PropertiesFilter}
	 */
	public static PropertiesFilter parseFromString(String jsonPropFilter) throws Exception{
		JSONParser p = new JSONParser();

		try {
			JSONObject obj = (JSONObject) p.parse(jsonPropFilter);
			PropertiesFilter ret = new PropertiesFilter();
			for(Object key : obj.keySet())
				ret.put(key, obj.get(key));
			return ret;
		} catch (ParseException e) {
			e.printStackTrace();
			throw new Exception("Error parsing filter " + jsonPropFilter);
		}
	}

	protected static Filter buildJSONPathFilter(JSONObject json){

		if(json == null)
			return null;
		if(json.size() != 3){
			System.err.println("No 3 keys for object " +json);
			return null;
		}

		String operand = (String)json.get(ReservedKeys.$op.name());
		FilterType op_type = FilterType.valueOf(operand);

		if(op_type.ordinal() <= FilterType.OR.ordinal()){
			if(op_type.equals(FilterType.AND))
				return buildJSONPathFilter((JSONObject) json.get(ReservedKeys.$filter_1.name()))
						.and(buildJSONPathFilter((JSONObject) json.get(ReservedKeys.$filter_2.name())));

			return buildJSONPathFilter((JSONObject) json.get(ReservedKeys.$filter_1.name()))
					.or(buildJSONPathFilter((JSONObject) json.get(ReservedKeys.$filter_2.name())));

		}else
		{
			String key = (String)json.get(ReservedKeys.$key.name());
			String val = "" + json.get(ReservedKeys.$val.name());
			if(op_type.equals(FilterType.CONTAINS))
				return Filter.filter(
						Criteria.where((String)json.get(ReservedKeys.$key.name()))
						.contains(json.get(ReservedKeys.$val.name())));

			if(op_type.equals(FilterType.EQUALS))
				return Filter.filter(
						Criteria.where((String)json.get(ReservedKeys.$key.name()))
						.eq(json.get(ReservedKeys.$val.name())));

			if(op_type.equals(FilterType.GT))
				return Filter.filter(
						Criteria.where((String)json.get(ReservedKeys.$key.name()))
						.gt(json.get(ReservedKeys.$val.name())));

			if(op_type.equals(FilterType.LT))
				return Filter.filter(
						Criteria.where((String)json.get(ReservedKeys.$key.name()))
						.lt(json.get(ReservedKeys.$val.name())));
			
			if(op_type.equals(FilterType.EXISTS))
				return Filter.filter(
						Criteria.where((String)json.get(ReservedKeys.$key.name()))
						.exists((boolean) json.get(ReservedKeys.$val.name())));
			
			if(op_type.equals(FilterType.NOT_EXISTS))
				return Filter.filter(
						Criteria.where((String)json.get(ReservedKeys.$key.name()))
						.exists((boolean) json.get(ReservedKeys.$val.name())));		
		}

		return null;
	}


	/**
	 * Method used to evaluate a {@link PropertiesFilter} on a {@link JSONObject}
	 * @param f - the {@link PropertiesFilter} to be evaluated
	 * @param toFilter - the {@link JSONObject} that has to match the filter 
	 * @return
	 */
	public static boolean evalFilter(PropertiesFilter f, JSONObject toFilter){
		Filter filter = buildJSONPathFilter(f);
		return !((List<Map<String, Object>>)JsonPath.parse(toFilter).read("$.[?]",filter)).isEmpty();
	}
}