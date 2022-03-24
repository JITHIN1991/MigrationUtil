package com.knowesis.SCBMigrationUtility;

import java.util.HashMap;

public interface PandaCache {

	public void set( String id, int expiry, String data);
	
	public void set( String id, String data);

	public void delete( String id);

	public Object get( String id);

	public void incr( String key, int factor);

	public void descr( String key, int factor);

	public HashMap<String, String> getView( String key);
	
	public Object getClient();

}
