package edu.upc.eetac.dsa.marc.beeter.api;

public interface MediaType {//se relacionan todas las clases del .model
	public final static String BEETER_API_USER = "application/vnd.beeter.api.user+json";
	public final static String BEETER_API_USER_COLLECTION = "application/vnd.beeter.api.user.collection+json";
	public final static String BEETER_API_STING = "application/vnd.beeter.api.sting+json";
	public final static String BEETER_API_STING_COLLECTION = "application/vnd.beeter.api.sting.collection+json";
	public final static String BEETER_API_ERROR = "application/vnd.dsa.beeter.error+json";

}