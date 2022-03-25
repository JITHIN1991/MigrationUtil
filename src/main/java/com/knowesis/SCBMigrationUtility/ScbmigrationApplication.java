package com.knowesis.SCBMigrationUtility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.protocol.views.AbstractView;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.Stale;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ScbmigrationApplication {

//	static Logger logger = Logger.getLogger("MyLog");
	
	static FileHandler fh;

//	private static CouchbaseClient client;
	
	private static PandaCache fromCache = null;
	private static PandaCache toCache = null;
	
	ObjectMapper mapper = new ObjectMapper();
	
	static String viewName = null;
	
	static Iterator<ViewRow> data = null;

	public static void main(String[] args) {
		String systemCurDir = System.getProperty("user.dir");
		try {
			fh = new FileHandler(systemCurDir + "/SCBLog.log");
		} catch (SecurityException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
//		logger.addHandler(fh);
		SimpleFormatter formatter = new SimpleFormatter();
		fh.setFormatter(formatter);

//		logger.info("Started from main");
		System.out.println("Started from main");
		String url = null;
		String actionType = null;
		String filePath = null;
		String bucketFrom = null;
		String bucketTo = null;
		
		if(args.length<4) {
			System.out.println( "Check the command => java -jar SCBMigrationUtil-jar-with-dependencies.jar <CouchBase URL> <bucketName-From> <bucketName-To> <ActionType> <EventTemplateDataFile>" );
			System.exit( 0 );
		}
		
		url = args[0];
		bucketFrom = args[1];
		bucketTo = args[2];
		actionType = args[3];
		
		//VALIDATION STARTS HERE		
		if(url==null || url.equals("")) {
			System.out.println("CouchBase URL is empty...!");
			System.exit(0);			
		}
		if(bucketFrom==null || bucketFrom.equals("")) {
			System.out.println("BucketFrom is empty...!");
			System.exit(0);			
		}
		if(bucketFrom==null || bucketFrom.equals("")) {
			System.out.println("BucketFrom is empty...!");
			System.exit(0);			
		}
		if(bucketTo==null || bucketTo.equals("")) {
			System.out.println("BucketTo is empty...!");
			System.exit(0);			
		}
		if(actionType==null || actionType.equals("")) {
			System.out.println("BucketTo is empty...!");
			System.exit(0);			
		}
		
		if (actionType.equals("EVENT")||actionType.equals("ALL")) {		
			if(args.length < 5) {
				System.out.println("EVENT TEMPLATE - FILE MISSING...!");
				System.exit(0);
			}
			filePath = args[4];
		}
		//VALIDATION ENDS HERE
		
		
		System.out.println("FILE PATH >>>>>>>>>>>>> "+filePath);
		try {
			initialize(url,bucketFrom,bucketTo, actionType, filePath);
		} catch (Exception e) {
//			logger.info("Error from main : " + e.getMessage());
			System.out.println("Error from main : " + e.getMessage());
			e.printStackTrace();
		}
//		System.exit(0);
	}

	/**
	 * Function to initialize couchbase
	 * 
	 * @param url
	 * @throws Exception
	 */
	public static void initialize(String url,String bucketFrom, String bucketTo,String actionType, String filePath) throws Exception {
		System.out.println("Entered - initialize");
		try {
			LinkedList<URI> uris = new LinkedList<URI>();
			System.out.println("URL :" + url);
			uris.add(new URI(url));
//			client = couchbaseClient(uris);
			fromCache = new CouchbasePersistClient(uris, bucketFrom);
			toCache = new CouchbasePersistClient(uris, bucketTo);
			updateUtility(url, actionType, filePath);
		} catch (Exception e) {
//			logger.info("Error from initialize : " + e.getMessage());
			System.out.println("Error from initialize : " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}

	public static void updateUtility(String url, String actionType, String filePath) throws Exception {
//		logger.info("Entered - updateUtility");
		System.out.println("Entered - updateUtility");
		switch (actionType) {
		case "OFFER":
			updateOffer();
			break;
		case "EVENT":
			updateEvent(filePath);
			break;
		case "BU":
			updateBusinessUnit(url);
			break;
		case "ALL":
			updateOffer();
			updateEvent(filePath);
			updateBusinessUnit(url);
			break;
		default:
			System.out.println("Invalid action type !");
			break;
		}
//		logger.info("-----------------Completed-------------------");
		System.out.println("-----------------Completed-------------------");
	}

	private static Iterator<ViewRow> fetchData(String viewName) throws InterruptedException {
//		logger.info("Entered - fetchData");
		System.out.println("Entered - fetchData");
//		View view = client.getView("pandalytics", viewName);// USE CLIEN
//		Query query = new Query();
		
		CouchbaseClient fromClient = (CouchbaseClient) fromCache.getClient();
		View view = fromClient.getView("pandalytics", viewName );
//		View view = ((CouchbaseClient) cache.getClient()).getView("pandalytics", viewName );
		Query query = new Query();
		query.setIncludeDocs( false );
		query.setStale( Stale.FALSE );

		
		
		ViewResponse viewresponse = null;
		viewresponse = fromClient.query((AbstractView) view, query);
		int maxRetries = 5;
		int currentRetryCount = 0;
		/**
		 * Fetching data from view. Catched timeout exception. Maximum 5 retry is
		 * configured
		 */
		while (true) {
			try {
				currentRetryCount++;
				viewresponse = fromClient.query((AbstractView) view, query);
			} catch (Exception ex) {
				if (currentRetryCount < maxRetries) {
//					logger.info("TimeoutException occured in : " + view.getViewName() + " view. Current retry count: "
//							+ currentRetryCount);
					System.out.println("TimeoutException occured in : " + view.getViewName() + " view. Current retry count: "+ currentRetryCount);
//					logger.info("Retry in progress..");
					System.out.println("Retry in progress..");
					Thread.sleep(20000L);
					continue;
				}
				ex.printStackTrace();
				throw ex;
			}
			break;
		}
//		logger.info(viewName + " : Total Data = " + viewresponse.getTotalRows());
		System.out.println(viewName + " : Total Data = " + viewresponse.getTotalRows());
		Iterator<ViewRow> itr = viewresponse.iterator();
		return itr;
	}

	/**
	 * This is to update the offer by reading from view
	 * 
	 * @param item
	 * @param status
	 * @throws InterruptedException
	 */
	private static void updateOffer() throws InterruptedException {
//		logger.info("Entered - updateOffer");
		System.out.println("Entered - updateOffer");
		viewName = "view_allOffers";
		CouchbaseClient fromClient = (CouchbaseClient) fromCache.getClient();
		CouchbaseClient toClient = (CouchbaseClient) toCache.getClient();
		
		data = fetchData(viewName);
		if (data.hasNext()) {
			JsonParser parser = new JsonParser();
			/**
			 * Iterating the response to process each doc Parsing the doc as a jsonobject
			 * Checking the current values, if it's not as required then set the new values
			 * Set the whole doc in the couchbase
			 */
			while (data.hasNext()) {
				ViewRow record = data.next();
				String doc = (String) fromClient.get(record.getKey());
//				String doc = record.getDocument().toString();
				System.out.println(">>>>>>>>>>>Processing DOC : "+record.getKey());
				JsonObject jobj = (JsonObject) parser.parse(doc);

				String docId = record.getKey();

				JsonObject jsonObject = null;
				if (jobj.get("isCountAsContact") != null) {

					if (!jobj.get("isCountAsContact").isJsonObject()) {
						jsonObject = new JsonObject();
						String isCountAsContact = jobj.get("isCountAsContact").getAsString().toUpperCase();

						if (isCountAsContact.equalsIgnoreCase("Y") || isCountAsContact.equalsIgnoreCase("\"Y\"")) {
							jsonObject.addProperty("expression", "true");
							jsonObject.addProperty("type", "boolean");
							jobj.add("isCountAsContact", jsonObject);
						} else if (isCountAsContact.equalsIgnoreCase("N")
								|| isCountAsContact.equalsIgnoreCase("\"N\"")) {
							jsonObject.addProperty("expression", "false");
							jsonObject.addProperty("type", "boolean");
							jobj.add("isCountAsContact", jsonObject);
						} else {
//							logger.info("Not found the isCountAsContact value as Y or N, No Operation perforemed for "
//									+ docId);
							System.out.println("Not found the isCountAsContact value as Y or N, No Operation perforemed for "+ docId);
						}
					}
				}
				if (jobj.get("offerPreferredChannelType") != null) {
					jobj.addProperty("offerPreferredChannelType",
							jobj.get("offerPreferredChannelType").getAsString().toLowerCase());
				}
				if (jobj.get("offerPreferredLanguageType") != null) {
					jobj.addProperty("offerPreferredLanguageType",
							jobj.get("offerPreferredLanguageType").getAsString().toLowerCase());
				}
//				logger.info("Converted the structure for the offer : " + docId);
				System.out.println("Converted the structure for the offer : " + docId);
				toClient.set(record.getKey(), 0, jobj.toString());
			}
		}
//		logger.info("---Offer end---");
		System.out.println("---Offer end---");
	}

	private static void updateEvent(String filePath) throws InterruptedException, FileNotFoundException {
//		logger.info("Entered - updateEvent");
		System.out.println("Entered - updateEvent");
		CouchbaseClient toClient = (CouchbaseClient) toCache.getClient();

		System.out.println("FILEPATH >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>:"+filePath);
		Scanner read = new Scanner(new File(filePath));
//		Scanner read = new Scanner(new File("/home/remya/Downloads/EventTemplateRecord-1.txt"));
		read.useDelimiter("\\|");
		String event_id, event_name, description, status, event_type, payloads, indicator_logic, event_logic,
				function_expression, expression_type, bricks_logic, createdAt, updatedAt;
		JsonObject emptyObject = new JsonObject();
		JsonArray emptyArray = new JsonArray();
		while (read.hasNext()) {
			event_id = read.next();
			event_name = read.next();
			description = read.next();
			status = read.next();
			event_type = read.next();
			payloads = read.next();
			indicator_logic = read.next();
			event_logic = read.next();
			function_expression = read.next();
			expression_type = read.next();
			bricks_logic = read.next();
			createdAt = read.next();
			updatedAt = read.next();

			JsonObject eventObject = new JsonObject();
			eventObject.addProperty("id", event_id);
			eventObject.addProperty("docType", "EventDefinition");
			eventObject.addProperty("name", event_name);
			eventObject.addProperty("category", "");
			eventObject.addProperty("status", status);
			eventObject.addProperty("description", description);
			eventObject.addProperty("asEventTemplate", true);
			eventObject.addProperty("eventEndTime", "");
			eventObject.addProperty("eventStartTime", "");
			eventObject.addProperty("recurringCount", 0);
			eventObject.addProperty("enableRecuringEvent", "false");
			eventObject.addProperty("taggedForAll", "true");
			eventObject.addProperty("enableIntermediateEvent", false);
			eventObject.addProperty("type", "Behavioural");

			JsonObject eventSiftExpression = new JsonObject();

			JsonObject fromFile = new JsonObject();
			fromFile = new Gson().fromJson(bricks_logic, JsonObject.class);

			JsonObject data = new JsonObject();
			data.add("bricksExpression", fromFile.get("expressionString"));
			data.add("expressionType", fromFile.get("expressionType"));
			data.add("bricksUsedInExpression", fromFile.getAsJsonArray("bricksUsed"));
			data.add("expressionReturnType", fromFile.get("returnType"));
			data.add("bricksLogic", fromFile.getAsJsonObject("bricksLogic"));
			data.addProperty("bricksJavaExpression", "");

			eventSiftExpression.addProperty("type", "Bricks Editor");
			eventSiftExpression.add("data", data);

			eventObject.add("eventSiftExpression", eventSiftExpression);
			eventObject.add("eventLocations", null);
			eventObject.addProperty("targetSystemID", "");
			eventObject.add("eventpayloadsummary", emptyArray);
			eventObject.addProperty("campaigns", "");
			eventObject.addProperty("interactionPoint", "");
			eventObject.add("scheduledReminderCondition", emptyArray);
			eventObject.add("activityTriggeredReminderCondition", emptyArray);
			eventObject.addProperty("globalIndicatorsToBeSend", "true");
			eventObject.add("eventPayloadExpressions", emptyArray);
			eventObject.addProperty("enableWhitelist", "NO_WHITELIST");
			eventObject.addProperty("uploadedWhitelistId", "");
			eventObject.add("eventWhitelistExpression", null);
			eventObject.add("eventExtentedAtrributes", emptyArray);
			eventObject.addProperty("businessUnit", "");
			eventObject.add("updateHistory", emptyArray);
			eventObject.add("versionHistory", emptyArray);
			eventObject.addProperty("version", "0");
			eventObject.addProperty("remarks", "");
			eventObject.addProperty("requestStructureId", "");
			eventObject.addProperty("draft", "false");
			eventObject.addProperty("isDraft", "false");
			eventObject.addProperty("approvalWorkflowEnabled", "false");
			eventObject.addProperty("approverGroups", "");
			eventObject.addProperty("approvalRemarks", "");

			String json = null;
			json = new Gson().toJson(eventObject);

			if (json != null)
				toClient.set(event_id, 0, json);
//			logger.info("Ended - updateEvent");
			System.out.println("Ended - updateEvent");
		}
		read.close();
	}

	private static void updateBusinessUnit(String url) throws InterruptedException, FileNotFoundException {
//		logger.info("Entered - updateBU");
		System.out.println("Entered - updateBU");
		// GET all documents from couch
		CouchbaseClient fromClient = (CouchbaseClient) fromCache.getClient();

		CouchbaseClient toClient = (CouchbaseClient) toCache.getClient();

		
		Iterator<String> views = getAllViews(url);
		Map<String, List<String>> issuesReported = new HashMap<>();
		while (views.hasNext()) {
			String view = views.next();
			List<String> docsWithIssues = new ArrayList<>();
			try {
				// getAll document of this view
				Iterator<ViewRow> documents = fetchData(view);
				// check whether business unit exists for each document
				if (documents.hasNext()) {
					JsonParser parser = new JsonParser();
					while (documents.hasNext()) {
						ViewRow record = documents.next();
						try {
							String doc = (String) fromClient.get(record.getKey());
							if (doc == null) {
								continue;
							}
							JsonObject jobj = (JsonObject) parser.parse(doc);
							jobj.addProperty("businessUnit", "SCB");
							toClient.set(record.getKey(), 0, jobj.toString());
							System.out.println("Updated BU for the doc :"+record.getKey());
						} catch (Throwable e) {
							// TODO: handle exception
							docsWithIssues.add(record.getKey());
//							logger.info("Error occurred for the doc" + record.getKey());
							System.out.println("Error occurred for the doc" + record.getKey());
						}
					}
				}
//				logger.info("---All documents are set with BUs--");
				System.out.println("---All documents are set with BUs--");
				issuesReported.put(view, docsWithIssues);
			} catch (Exception e) {
				// TODO: handle exception
//				logger.info("Error occured for the view>>>>>>>>>>>>>>>>>>>>>>>>>:" + view);
				System.out.println("Error occured for the view>>>>>>>>>>>>>>>>>>>>>>>>>:" + view);
				System.out.println(e.getMessage());
			}
		}
//		logger.info(
//				"##################################################ISSUES REPORTED FOR THESE ITEMS####################################################");
		System.out.println("##################################################ISSUES REPORTED FOR THESE ITEMS####################################################");
		for (String viewWithIssueDocs : issuesReported.keySet()) {
//			logger.info("<<<<<UNDER " + viewWithIssueDocs + " VIEW>>>>>>");
			System.out.println("<<<<<UNDER " + viewWithIssueDocs + " VIEW>>>>>>");
			List<String> docs = issuesReported.get(viewWithIssueDocs);
			for (String doc : docs) {
//				logger.info(doc);
				System.out.println(doc);
			}
		}
		
//		logger.info("Completed BU update");
		System.out.println("Completed BU update");
	}

	public static Iterator<String> getAllViews(String url) {
		RestTemplate template = new RestTemplate();
		ResponseEntity<String> res = template.getForEntity(url + "/config/ddocs", String.class);
		System.out.println(res.getBody());
		ObjectMapper mapper = new ObjectMapper();

		JsonNode resNode = null;
		try {
			resNode = mapper.readTree(res.getBody());
			System.out.println("Mapped to:" + resNode);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (resNode.get("rows").isArray()) {
//			logger.info("IS AN ARRAY");
			System.out.println("IS AN ARRAY");
			for (JsonNode node : resNode.get("rows")) {
//				logger.info("INSIDE LOOP");
				System.out.println("INSIDE LOOP");
//				logger.info(node.at("/doc/meta/id").toString());
				System.out.println(node.at("/doc/meta/id").toString());
				if (node.at("/doc/meta/id").asText().contentEquals("_design/pandalytics")) {
//					logger.info("DESIGN PANDALYTICS");
					System.out.println("DESIGN PANDALYTICS");
					node.path("/doc/json/views").forEach(System.out::println);
					Iterator<String> it = node.at("/doc/json/views").fieldNames();
					while (it.hasNext()) {
						String fieldName = it.next();
//						logger.info(fieldName);
						System.out.println(fieldName);
					}
					return node.at("/doc/json/views").fieldNames();
				}
			}
		}
		return null;
	}

//	public static CouchbaseClient couchbaseClient(List<URI> uris) {
//		try {
//			CouchbaseClient cl = new CouchbaseClient(uris, "config", "");
//			return cl;
//		} catch (IOException e) {
//			logger.info("Error from couchbaseClient : " + e.getMessage());
//			e.printStackTrace();
//		}
//		return null;
//	}

}
