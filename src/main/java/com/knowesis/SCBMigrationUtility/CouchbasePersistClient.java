package com.knowesis.SCBMigrationUtility;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.CouchbaseConnectionFactoryBuilder;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.Stale;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;

public class CouchbasePersistClient implements PandaCache{
	private   CouchbaseClient couchClient = null;
	private static String designDoc = "pandalytics";
	Logger logger = Logger.getLogger( CouchbasePersistClient.class.getName() );

	public CouchbasePersistClient(){
		if( couchClient == null ) {
			LinkedList< URI > uris = new LinkedList< URI >();
			//uris.add( URI.create( "http://119.81.94.148:8091/pools" ) );
			uris.add( URI.create( "http://127.0.0.1:8091/pools" ) );
			initCacheClient( uris );
		}
	}

	public CouchbasePersistClient( LinkedList< URI > uris, String bucket ){
//		if( couchClient == null )
			initCacheClient( uris, bucket );
	}

	private void initCacheClient( LinkedList< URI > uris ) {
		try {
			CouchbaseConnectionFactoryBuilder cfb = new CouchbaseConnectionFactoryBuilder();
			cfb.setOpTimeout( 10000 );
			//	cfb.setTimeoutExceptionThreshold( 240000 );
			couchClient = new CouchbaseClient( cfb.buildCouchbaseConnection( uris, "default", "" ) );
			System.out.println( "************************************* Initialised CouchClient ********** " + couchClient.toString() );
		}catch( IOException e ) {
			System.err.println( "IOException connecting to Panda Cache: " + e.getMessage() );
			System.exit( 1 );
		}
	}
	

	private void initCacheClient( LinkedList< URI > uris, String bucket ) {
		try {
			CouchbaseConnectionFactoryBuilder cfb = new CouchbaseConnectionFactoryBuilder();
			cfb.setOpTimeout( 10000 );
			//	cfb.setTimeoutExceptionThreshold( 240000 );
			couchClient = new CouchbaseClient( cfb.buildCouchbaseConnection( uris, bucket, "" ) );
			System.out.println( "************************************* Initialised CouchClient ********** " + couchClient.toString() );
		}catch( IOException e ) {
			System.err.println( "IOException connecting to Panda Cache: " + e.getMessage() );
			System.exit( 1 );
		}
	}

	public CouchbaseClient getClient() {
		return couchClient;
	}

	public HashMap<String, String> getView( String viewName ) {
		HashMap<String, String > documents = new HashMap<String, String>();
		View view = couchClient.getView(designDoc, viewName );
		Query query = new Query();
		query.setIncludeDocs( false );
		query.setStale( Stale.FALSE );
		ViewResponse viewresponse = null;
		int maxRetries = 5;
		int currentRetryCount = 0;
		while( true ) {
			try {
				currentRetryCount ++;
				viewresponse = couchClient.query( view, query );
				break;
			}
			catch( Exception tex ) {
				if( currentRetryCount < maxRetries ) {
					logger.warning (   "TimeoutException occured in : " + view.getViewName() +  " view. Current retry count: " + currentRetryCount );
					logger.warning(   "Retry in progress.." );
					try {
						Thread.sleep( 10000 );
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else {
					tex.printStackTrace();
//					throw tex;
				}
			}
		}
		Iterator< ViewRow > itr = viewresponse.iterator();
		while( itr.hasNext() ) {
			ViewRow record = itr.next();
			String doc = ( String ) get( record.getKey() );
			documents.put( record.getKey(), doc );
		}
		return documents;
	}
	
	public void set( String id, int expiry, String data ) {
		try {
			couchClient.set( id, expiry, data );
		} catch( Exception e ) {
			logger.warning( "Node Down? While deleting the document for id: " + id );
			logger.warning( "Exception : " + e.getMessage() );
			System.out.println( "Node Down? While deleting the document for id: " + id );
			System.out.println( "Exception : " + e.getMessage() ); 
		}
	}
	
	public void set( String id, String data) {
		try {
			couchClient.set( id, 0, data );
		} catch( Exception e ) {
			logger.warning( "Node Down? While deleting the document for id: " + id );
			logger.warning( "Exception : " + e.getMessage() );
			System.out.println( "Node Down? While deleting the document for id: " + id );
			System.out.println( "Exception : " + e.getMessage() ); 
		}
		
	}

	public void delete( String id ) {
		try {
			couchClient.delete( id );
		} catch( Exception e ) {
			logger.warning( "Node Down? While deleting the document for id: " + id );
			logger.warning( "Exception : " + e.getMessage() );
			System.out.println( "Node Down? While deleting the document for id: " + id );
			System.out.println( "Exception : " + e.getMessage() ); 
		}
	}

	
	public Object get( String id ) {
		Object result = null;
 		try {
		    result = getClient().get( id );
		} catch( Exception e ) {
			System.out.println( "Node Down? While getting the document for id: " + id );
			System.out.println( "Will try to get from Replica : " + e.toString() ); 
			try {
			    result = couchClient.getFromReplica( id );
			}  catch( Exception eRep ) {
				logger.warning( "Node Down? While deleting the document for id: " + id );
				logger.warning( "Getting from Replica Failed. May be no replica available : " + eRep.toString() );
				System.out.println( "Node Down? While deleting the document for id: " + id );
				System.out.println( "Getting from Replica Failed. May be no replica available : " + eRep.toString() ); 
				return null;
			}
		}
		return result;
	}

	

	public void incr( String key, int factor) {
		couchClient.incr( key, factor );
		
	}

	public void descr( String key, int factor) {
		couchClient.decr( key, factor );
		
	}

}