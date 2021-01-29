package it.unipi.dii.lsdb.group13.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoDBManager implements DatabaseManager {
     private MongoClient mongoClient;
     private MongoDatabase database;
     private static MongoDBManager dbManager = null;

     MongoDBManager(){
         String uriString= "mongodb://localhost:27017";
         mongoClient = MongoClients.create(uriString);
         database= mongoClient.getDatabase("job_advisor");
         System.out.println("Connection opened");
     }

    //@Override
    public static MongoDBManager getInstance() {
    	if(dbManager == null){
    	    dbManager = new MongoDBManager();
        }
    	return dbManager;
    }

    @Override
    public void closeDB() {
        mongoClient.close();
        System.out.println("Connection closed");
    }

    public MongoCollection getJobSeekersCollection(){System.out.println("arrived here 1"); return database.getCollection("job_seekers"); }

    public MongoCollection getCompaniesCollection() {
        return database.getCollection("companies");
    }

    public MongoCollection getJobOffersCollection() {
        return database.getCollection("job_offers");
    }

}