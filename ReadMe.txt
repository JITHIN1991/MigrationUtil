MigratinUtil converts the Documwent Structure based on the ActionType

=>COmmand Structure

 java -jar SCBMigrationUtil-jar-with-dependencies.jar <CouchBase URL> <bucketName-From> <bucketName-To> <actionType> <fileToProcess_IfAny>

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
		
