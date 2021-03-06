package it.unipi.dii.lsdb.group13.database;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.UpdateResult;
import it.unipi.dii.lsdb.group13.entities.JobOffer;
import it.unipi.dii.lsdb.group13.entities.JobSeeker;
import it.unipi.dii.lsdb.group13.Session;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;
import static org.neo4j.driver.Values.parameters;

public class JobSeekerDao {

	Logger logger;

	public JobSeekerDao() {
	    	 logger = Logger.getLogger(JobSeekerDao.class.getName());
	    }

    public String signUp(JobSeeker jobSeeker) {
        try {
            MongoDBManager mongoDB = MongoDBManager.getInstance();
            Document doc = new Document("_id",jobSeeker.getUsername()).append("password",jobSeeker.getPassword())
                    .append("first_name",jobSeeker.getFirstName()).append("last_name",jobSeeker.getLastName())
                    .append("birthdate",jobSeeker.getBirthdate()).append("gender",jobSeeker.getGender()).append("email",jobSeeker.getEmail())
                    .append("location", new Document("city",jobSeeker.getLocation().getCity()).append("state",jobSeeker.getLocation().getState()));
            mongoDB.getJobSeekersCollection().insertOne(doc);
            return "true";
        }
        catch(Exception e) {
        	logger.error(e.getMessage());
            return e.getMessage();
        }
    }

    public void addJobSeekerToNeo4j(String username){
        Neo4jManager neo4j = Neo4jManager.getInstance();
        try (org.neo4j.driver.Session session = neo4j.getDriver().session()) {
                session.writeTransaction((TransactionWork<Void>) tx -> {
                    tx.run("MERGE (js:JobSeeker {username: $username})",
                            parameters("username", username));
                    return null;
                });
                logger.info("User added to neo4j");
        }catch (Exception e){
            logger.error("User " + username + " not added to neo4j");
            logger.error(e.getMessage());
        }
    }

    public boolean deleteAccount(String username) {
        boolean ret = true;
        try {
            MongoDBManager mongoDB = MongoDBManager.getInstance();
            mongoDB.getJobSeekersCollection().deleteOne(eq("_id",username));
            deleteFromNeo4j(username);
        }catch (Exception e){
            logger.error(e.getMessage());
            ret = false;
        }
        return ret;
    }

    private void deleteFromNeo4j(String username){
        Neo4jManager neo4j = Neo4jManager.getInstance();
        try (org.neo4j.driver.Session session = neo4j.getDriver().session()) {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("Match(j:JobSeeker) WHERE j.username = $username DETACH DELETE j",
                        parameters("username", username));
                return null;
            });
            logger.info("User deleted from neo4j");
        }catch (Exception e){
            logger.error("User " + username + " not added to neo4j");
            logger.error(e.getMessage());
        }
    }

    public boolean changePassword(String newpwd) {
         Session.getSingleton();
         String username= Session.getLoggedUser();
         MongoDBManager mongoDB = MongoDBManager.getInstance();
         try {
             mongoDB.getJobSeekersCollection().updateOne(eq("_id",username),set("password",newpwd));
             return true;
         }
         catch(Exception e) {
             logger.error(e.getMessage());
             return false;
         }
     }

    public boolean addSkill(String skill)
    {
        Session.getSingleton();
        String username= Session.getLoggedUser();
        MongoDBManager mongoDB = MongoDBManager.getInstance();
        try {
            mongoDB.getJobSeekersCollection().updateOne(eq("_id",username),push("skills",skill));
            return true;
        }
        catch(Exception e)
        {
            logger.error(e.getMessage());
            return false;
        }
    }

    public boolean deleteSkill(String skill)
    {
        Session.getSingleton();
        String username= Session.getLoggedUser();
        MongoDBManager mongoDB = MongoDBManager.getInstance();
        UpdateResult result;
        try {
            result = mongoDB.getJobSeekersCollection().updateOne(eq("_id",username),pull("skills",skill));
        }
        catch(Exception e)
        {
            logger.error(e.getMessage());
            return false;
        }

        if(result.getModifiedCount() == 1)
            return true;
        else
            return false;
    }

    public String searchUsername(String username) throws Exception{
        String foundedPw;
        Document doc;
        MongoDBManager mongoDB = MongoDBManager.getInstance();

        doc = (Document) mongoDB.getJobSeekersCollection().find(eq("_id", username)).first();
        if (doc == null) {
            throw new Exception("Invalid username");
        } else {
            foundedPw = (String) doc.get("password");
        }
        logger.info("check founded password: "+ foundedPw);
        return foundedPw;
    }

    public JobSeeker findUser(String username){
        MongoDBManager mongoDB = MongoDBManager.getInstance();
        Document user = (Document) mongoDB.getJobSeekersCollection().find(eq("_id", username)).first();
        // handle nullPointerException
        if (user != null) {
            Document loc = (Document) user.get("location");
            if (user.containsKey("skills")) {
                return new JobSeeker(username, user.getString("first_name"), user.getString("last_name"),
                        user.getString("gender"), user.getString("birthdate"), user.getString("email"),
                        loc.getString("state"), loc.getString("city"),
                        (List<String>)user.get("skills", List.class));
            } else {
                return new JobSeeker(username, user.getString("first_name"), user.getString("last_name"),
                        user.getString("gender"), user.getString("birthdate"), user.getString("email"),
                        loc.getString("state"), loc.getString("city"));
            }
        } else {
            return null;
        }
    }

    public List<JobSeeker> searchSkill(String skill) {
        List<JobSeeker> seekers = new ArrayList<>();
        MongoDBManager mongoDB = MongoDBManager.getInstance();
        FindIterable findIterable = mongoDB.getJobSeekersCollection().find(eq("skills", skill));
        MongoCursor<Document> cursor = findIterable.iterator();

        while ( cursor.hasNext() ) {
            Document doc = cursor.next();
            Document loc = doc.get("location", Document.class);
            seekers.add( new JobSeeker(doc.getString("_id"), doc.getString("first_name"), doc.getString("last_name"),
                    doc.getString("gender"), doc.getString("birthdate"), doc.getString("email"),
                    loc.getString("state"), loc.getString("city"), doc.getList("skills", String.class)) );
        }
        cursor.close();

        return seekers;
    }

    public List<JobSeeker> searchLocation(String city, String state) {

        List<JobSeeker> seekers = new ArrayList<>();

        MongoDBManager mongoDB = MongoDBManager.getInstance();
        MongoCursor<Document> cursor = mongoDB.getJobSeekersCollection().find(and(eq("location.city", city), eq("location.state", state))).iterator();
        while (cursor.hasNext()) {
            Document doc = cursor.next();
            Document loc = doc.get("location", Document.class);
            seekers.add(new JobSeeker(doc.getString("_id"), doc.getString("first_name"), doc.getString("last_name"),
                    doc.getString("gender"), doc.getString("birthdate"), doc.getString("email"),
                    loc.getString("state"), loc.getString("city"), doc.getList("skills", String.class)) );
        }
        cursor.close();

        return seekers;
    }

    public List<JobSeeker> searchByCity(String city) {
        List<JobSeeker> seekers = new ArrayList<>();

        MongoDBManager mongoDB = MongoDBManager.getInstance();
        MongoCursor<Document> cursor = mongoDB.getJobSeekersCollection().find(eq("location.city", city)).iterator();
        while (cursor.hasNext()) {
            Document doc = cursor.next();
            Document loc = doc.get("location", Document.class);
            seekers.add(new JobSeeker(doc.getString("_id"), doc.getString("first_name"), doc.getString("last_name"),
                    doc.getString("gender"), doc.getString("birthdate"), doc.getString("email"),
                    loc.getString("state"), loc.getString("city"), doc.getList("skills", String.class)) );
        }
        cursor.close();

        return seekers;
    }

    public List<JobSeeker> searchByState(String state) {
        List<JobSeeker> seekers = new ArrayList<>();
        MongoDBManager mongoDB = MongoDBManager.getInstance();
        MongoCursor<Document> cursor = mongoDB.getJobSeekersCollection().find(eq("location.state", state)).iterator();
        while (cursor.hasNext()) {
            Document doc = cursor.next();
            Document loc = doc.get("location", Document.class);
            seekers.add(new JobSeeker(doc.getString("_id"), doc.getString("first_name"), doc.getString("last_name"),
                    doc.getString("gender"), doc.getString("birthdate"), doc.getString("email"),
                    loc.getString("state"), loc.getString("city"), doc.getList("skills", String.class)));
        }
        cursor.close();

        return seekers;
    }

    public LinkedHashMap<String, Integer> rankSkills(){
        MongoDBManager mongoDB = MongoDBManager.getInstance();
        MongoCollection collection = mongoDB.getJobSeekersCollection();
        Bson uw = unwind("$skills");
        // trim operator to remove the whitespace characters and toLower operator to change all to lower case
        Bson sb = sortByCount(eq("$toLower", eq("$trim", eq("input", "$skills"))));
        Bson limit = limit(10);
        AggregateIterable aggregate = collection.aggregate(Arrays.asList(uw, sb, limit));
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        MongoCursor<Document> iterator = aggregate.iterator();
        while (iterator.hasNext()) {
            Document next = iterator.next();
            map.put(next.getString("_id"), next.getInteger("count"));
        }
        iterator.close();
        return map;
    }

    public boolean followCompany(String jobSeekerUsername, String companyName){
        boolean ret = true;
        Neo4jManager neo4j = Neo4jManager.getInstance();
        try (org.neo4j.driver.Session session = neo4j.getDriver().session() ) {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run( "MATCH (js:JobSeeker),(c:Company) WHERE js.username = $username AND c.name = $name" +
                                " Merge (js)-[r:FOLLOWS]->(c)",
                        parameters( "username", jobSeekerUsername, "name", companyName) );
                return null;
            });
        }catch (Exception e){
        	logger.error(e.getMessage());
            ret = false;
        }
        return ret;
    }

    public boolean unfollowCompany(String jobSeekerUsername, String companyName){
        boolean ret = true;
        Neo4jManager neo4j = Neo4jManager.getInstance();
        try (org.neo4j.driver.Session session = neo4j.getDriver().session() ) {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run( "MATCH (:JobSeeker {username: $username})-[r:FOLLOWS]-(:Company {name: $name}) DELETE r",
                        parameters( "username", jobSeekerUsername, "name", companyName) );
                return null;
            });
        }catch (Exception e){
        	logger.error(e.getMessage());
            ret = false;
        }
        return ret;
    }

    public boolean isFollowing(String jobSeekerUsername, String companyName){
        Neo4jManager neo4j = Neo4jManager.getInstance();
        boolean following = false;
        try (org.neo4j.driver.Session session = neo4j.getDriver().session() ) {
            following = session.readTransaction((TransactionWork<Boolean>) tx -> {
                Result result = tx.run( "MATCH (js:JobSeeker) , (c:Company) " +
                                "WHERE js.username = $username AND c.name=$name " +
                                "RETURN EXISTS( (js)-[:FOLLOWS]-(c) )",
                        parameters( "username", jobSeekerUsername, "name", companyName) );
                return result.single().get(0).asBoolean();
            });
        }catch (Exception e){
            logger.error(e.getMessage());
        }
        return following;
    }

    public boolean saveJobOffer(String jobSeekerUsername, String JobOfferId){
        boolean ret = true;
        Neo4jManager neo4j = Neo4jManager.getInstance();
        try (org.neo4j.driver.Session session = neo4j.getDriver().session() ) {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run( "MATCH (js:JobSeeker),(jo:JobOffer) WHERE js.username = $username AND jo.id = $id" +
                                " Merge (js)-[r:SAVED]->(jo)",
                        parameters( "username", jobSeekerUsername, "id", JobOfferId) );
                return null;
            });
        }catch (Exception e){
        	logger.error(e.getMessage());
            ret = false;
        }
        return ret;
    }

    public boolean unSaveJobOffer(String jobSeekerUsername, String JobOfferId){
        boolean ret = true;
        Neo4jManager neo4j = Neo4jManager.getInstance();
        try (org.neo4j.driver.Session session = neo4j.getDriver().session() ) {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run( "MATCH (:JobSeeker {username: $username})-[r:SAVED]-(:JobOffer {id: $id}) DELETE r",
                        parameters( "username", jobSeekerUsername, "id", JobOfferId) );
                return null;
            });
        }catch (Exception e){
        	logger.error(e.getMessage());
            ret = false;
        }
        return ret;
    }

    public boolean isSaved(String jobSeekerUsername, String JobOfferId){
        Neo4jManager neo4j = Neo4jManager.getInstance();
        boolean saved = false;
        try (org.neo4j.driver.Session session = neo4j.getDriver().session() ) {
            saved = session.readTransaction((TransactionWork<Boolean>) tx -> {
                Result result = tx.run( "MATCH (js:JobSeeker) , (jo:JobOffer) " +
                                "WHERE js.username = $username AND jo.id=$id " +
                                "RETURN EXISTS( (js)-[:SAVED]-(jo) )",
                        parameters( "username", jobSeekerUsername, "id", JobOfferId) );
                return result.single().get(0).asBoolean();
            });
        }catch (Exception e){
        	logger.error(e.getMessage());    
        	}
        return saved;
    }

    public List<JobOffer> savedOffers(String username) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        List<JobOffer> jobTitles;
        try (org.neo4j.driver.Session session = neo4j.getDriver().session() ) {
            jobTitles = session.readTransaction((TransactionWork<List<JobOffer>>) tx -> {
                Result result = tx.run( "MATCH (js:JobSeeker)-[:SAVED]->(jo) WHERE js.username = $username" +
                                " RETURN jo.title as title, jo.id AS id",
                        parameters( "username", username) );
                ArrayList<JobOffer> offers = new ArrayList<>();
                while(result.hasNext())
                {
                    Record r = result.next();
                    offers.add(new JobOffer(r.get("id").asString(), r.get("title").asString()));
                }
                return offers;
            });
        }
        return jobTitles;
    }


    public List<String> followedCompanies(String username) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        List<String> companies;
        try (org.neo4j.driver.Session session = neo4j.getDriver().session() ) {
            companies = session.readTransaction((TransactionWork<List<String>>) tx -> {
                Result result = tx.run( "MATCH (js:JobSeeker)-[:FOLLOWS]->(c) WHERE js.username = $username" +
                                " RETURN c.name as Name",
                        parameters( "username", username) );
                ArrayList<String> comps = new ArrayList<>();
                while(result.hasNext())
                {
                    Record r = result.next();
                    comps.add(r.get("Name").asString());
                }
                return comps;
            });
        }
        return companies;
    }

    public List<String> findRecommendedCompanies(String username) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        List<String> companies;
        try(org.neo4j.driver.Session session = neo4j.getDriver().session()) {
            companies = session.readTransaction( (TransactionWork<List<String>>) tx -> {

                Result result = tx.run("MATCH (u1:JobSeeker)-[:FOLLOWS]->(c:Company)<-[:FOLLOWS]-(u2:JobSeeker)" +
                                " WHERE u1.username = $username AND u1.username <> u2.username" +
                                " WITH u2 AS foundedUser, count(DISTINCT c) AS strength" +
                                " MATCH (foundedUser)-[:FOLLOWS]->(c1:Company)" +
                                " WHERE NOT EXISTS { (j:JobSeeker {username : $username})-[:FOLLOWS]-(c1) } " +
                                " RETURN c1.name AS companyName, strength ORDER BY strength DESC LIMIT 15", parameters("username", username));

                ArrayList<String> comps = new ArrayList<>();
                while(result.hasNext()) {
                    Record r = result.next();
                    comps.add(r.get("companyName").asString());
                }
                return comps;
            });
        }
        return companies;
    }

    public List<JobOffer> findRecommendedJobOffers(String username) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        List<JobOffer> recommened;
        try (org.neo4j.driver.Session session = neo4j.getDriver().session()) {
            recommened = session.readTransaction((TransactionWork<List<JobOffer>>) tx -> {

                Result result = tx.run("MATCH (u1:JobSeeker)-[:SAVED]->(j1:JobOffer)<-[:SAVED]-(u2:JobSeeker) " +
                        "WHERE u1.username = $username AND u1.username <> u2.username " +
                        "WITH u2 AS foundedUser, count(DISTINCT j1) AS strength " +
                        "MATCH (foundedUser)-[:SAVED]->(j2:JobOffer)<-[p:PUBLISHED]-(c:Company)" +
                        "WHERE NOT EXISTS { (u:JobSeeker {username : $username})-[:SAVED]-(j2)} " +
                        "RETURN foundedUser.username, j2.id as id, j2.title AS title, date(p.date) as postDate, c.name as company, strength ORDER BY strength DESC LIMIT 15", parameters("username", username));

                ArrayList<JobOffer> offers = new ArrayList<>();
                while (result.hasNext()) {
                    Record r = result.next();
                    JobOffer offer = null;
                    int year = r.get("postDate").asLocalDate().getYear();
                    int month = r.get("postDate").asLocalDate().getMonthValue();
                    int day = r.get("postDate").asLocalDate().getDayOfMonth();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");
                    try {
                        offer = new JobOffer(r.get("id").asString(), r.get("title").asString(), r.get("company").asString(), sdf.parse(year + "-" + month + "-" + day));
                    } catch (ParseException e) {
                    	logger.error(e.getMessage());
                    }
                    offers.add(offer);
                }
                return offers;
            });
        }
        return recommened;
    }
}