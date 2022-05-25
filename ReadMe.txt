

MigratinUtil converts the Documwent Structure based on the ActionType


----------------------------------------------------COmmand Structure----------------------------------------------------------



        java -jar SCBMigrationUtil-jar-with-dependencies.jar <CouchBase URL> <bucketName-From> <bucketName-To> <actionType> <fileToProcess_IfAny>
  

	-> CouchBase URL       :  Couchbase connection url

	-> bucketName-From     : Bucketname from which the documents to be fetched
	 
	-> bucketName-To       : Bucket Name to which the documents to be updated
	
	-> actionType          : action that needs to be performed
	
	-> fileToProcess_IfAny : File path if any data to be processed .Applicatble for BU update,Event template migration,and bulk delete
	
	
	
  
  
------------------------------------------------------------------------------------------------------------------------------  
  
  
  
1) For Offer Migration 

	java -jar SCBMigrationUtil-jar-with-dependencies.jar <CouchBase URL> <bucketName-From> <bucketName-To> OFFER
	
	For Example: 
		java -jar SCBMigrationUtil-jar-with-dependencies.jar  http://localhost:8091/pools configCopy configCopy OFFER


2) For Event Migration
	
	java -jar SCBMigrationUtil-jar-with-dependencies.jar <CouchBase URL> <bucketName-From> <bucketName-To> EVENT <filePath>
	
	Here the file should containe eventTemplate details separated with the delimiter "|"

	For Example: 
		java -jar SCBMigrationUtil-jar-with-dependencies.jar  http://localhost:8091/pools configCopy configCopy EVENT ./eventTemplate.txt
	
3) For BU Migration
	
	java -jar SCBMigrationUtil-jar-with-dependencies.jar <CouchBase URL> <bucketName-From> <bucketName-To> BU <fiePath>
	
	Here the file should contain the viewnames with newLine separator(ie one view name in one line)

	For Example:
		java -jar SCBMigrationUtil-jar-with-dependencies.jar  http://localhost:8091/pools configCopy configCopy BU ./views.txt
		

4) For BULK DELETE 
	
	java -jar SCBMigrationUtil-jar-with-dependencies.jar <CouchBase URL> <bucketName-From> <bucketName-To> DELETE <fiePath>
	
	Here the file should contain the docIds with newLine separator(ie one docId in one line)

	For Example:
	
		java -jar SCBMigrationUtil-jar-with-dependencies.jar  http://localhost:8091/pools configCopy configCopy DELETE ./sampleDocIds.txt


		
