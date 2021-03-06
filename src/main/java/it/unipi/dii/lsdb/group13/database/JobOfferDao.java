package it.unipi.dii.lsdb.group13.database;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Sorts;

import it.unipi.dii.lsdb.group13.entities.JobOffer;

import org.apache.log4j.Logger;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.TransactionWork;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.eq;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Projections.computed;

import java.util.regex.Pattern;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Filters.gte;
import static org.neo4j.driver.Values.parameters;

public class JobOfferDao {

	Logger logger;

	public JobOfferDao() {
	    	 logger = Logger.getLogger(JobOfferDao.class.getName());
	    }

    public boolean createNewJobOffer(JobOffer jobOffer){
        boolean ret = true;
        Document job;
        try {
            MongoDBManager mongoDB = MongoDBManager.getInstance();
            Document locationDoc = new Document("city", jobOffer.getLocation().getCity())
                    .append("state", jobOffer.getLocation().getState());
            if(jobOffer.getSalary() != null){
                Document salaryDoc = new Document("from", jobOffer.getSalary().getFrom())
                        .append("to", jobOffer.getSalary().getTo()).append("time_unit",jobOffer.getSalary().getTimeUnit());
                job = new Document("_id", jobOffer.getId()).append("job_title", jobOffer.getTitle())
                        .append("company_name", jobOffer.getCompanyName()).append("location", locationDoc)
                        .append("salary", salaryDoc).append("post_date",jobOffer.getPostDate())
                        .append("job_type",jobOffer.getJobType()).append("job_description",jobOffer.getDescription());
            }else {
                job = new Document("_id", jobOffer.getId()).append("job_title", jobOffer.getTitle())
                        .append("company_name", jobOffer.getCompanyName()).append("location", locationDoc)
                        .append("post_date",jobOffer.getPostDate()).append("job_type",jobOffer.getJobType())
                        .append("job_description",jobOffer.getDescription());
            }
            mongoDB.getJobOffersCollection().insertOne(job);
        } catch (Exception e) {
        	logger.error(e.getMessage());
            ret = false;
        }
        return ret;
    }

    public void addJobOfferToNeo4j(JobOffer jobOffer){
        Neo4jManager neo4j = Neo4jManager.getInstance();
        try (org.neo4j.driver.Session session = neo4j.getDriver().session()) {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("MATCH (c:Company) WHERE c.name = $name MERGE (c)-[:PUBLISHED]->(jo:JobOffer {id: $id , title: $title})",
                        parameters("name", jobOffer.getCompanyName(), "id", jobOffer.getId(),
                                "title" , jobOffer.getTitle()));
                return null;
            });
        	logger.info("Job Offer added to Neo4j.");
        }catch (Exception e){
            logger.error("Job offer " + jobOffer.getId() + " not added to neo4j");
            logger.error(e.getMessage());
        }
    }

    private void removeJobOfferToNeo4j(String id){
        Neo4jManager neo4j = Neo4jManager.getInstance();
        try (org.neo4j.driver.Session session = neo4j.getDriver().session()) {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("Match(j:JobOffer) WHERE j.id = $id DETACH DELETE j",
                        parameters("id", id));
                return null;
            });
            logger.info("Job deleted from neo4j");
        }catch (Exception e){
            logger.error("Job offer " + id+ " not deleted to neo4j");
            logger.error(e.getMessage());
        }
    }

    public boolean deleteJobOffer(String id){
        boolean ret = true;
        try {
            MongoDBManager mongoDB = MongoDBManager.getInstance();
            mongoDB.getJobOffersCollection().deleteOne(eq("_id",id));
            removeJobOfferToNeo4j(id);
        }catch (Exception e){
            logger.error(e.getMessage());
            ret = false;
        }
        return ret;
    }


    public List<JobOffer> getJobOffersByCity(String city){
        List<JobOffer> jobOffers = new ArrayList<>();
        MongoDBManager mongoDB = MongoDBManager.getInstance();
        MongoCollection<Document> collection= mongoDB.getJobOffersCollection();
        Bson a= project(fields(include("job_title",
                "job_type", "job_description", "company_name", "location", "post_date"), computed("lower",
                eq("$toLower", "$location.city"))));
        Bson b= match(eq("lower", city));
        Bson c= sort(Sorts.descending("post_date"));
        AggregateIterable<Document> founded = collection.aggregate(Arrays.asList(a,b,c ));
        for(Document doc: founded) {
            jobOffers.add(parseJobOffer(doc));
        }
        return jobOffers;
    }

    public List<JobOffer> getJobOffersByCompany(String company){
        List<JobOffer> jobOffers = new ArrayList<>();
        MongoDBManager mongoDB = MongoDBManager.getInstance();
        MongoCollection<Document> collection= mongoDB.getJobOffersCollection();
        Bson a= project(fields(
                include("job_title", "job_type", "job_description", "company_name", "location", "post_date"),
                computed("lower", eq("$toLower", "$company_name"))));
        Bson b= match(eq("lower", company));
        Bson c= sort(Sorts.descending("post_date"));
        AggregateIterable<Document> founded = collection.aggregate(Arrays.asList(a,b ,c ));
        		//match(eq("$sort",(eq("post_date",-1))))));
        for(Document doc: founded) {
            jobOffers.add(parseJobOffer(doc));
        }
        return jobOffers;
    }

    public List<JobOffer> getJobOffersByJobType(String jobType){
        List<JobOffer> jobOffers = new ArrayList<>();
        MongoDBManager mongoDB = MongoDBManager.getInstance();
        FindIterable<Document> founded = mongoDB.getJobOffersCollection().find(eq("job_type", jobType)).sort(eq("post_date",-1));
        for (Document doc: founded) {
            jobOffers.add(parseJobOffer(doc));
        }
        return jobOffers;
    }

    /*in the mongo shell the query is: { job_title: {$regex: 'jobTitle', $options:i} }*/
    public List<JobOffer> getJobOffersByJobTitle(String jobTitle){
        List<JobOffer> jobOffers = new ArrayList<>();
        MongoDBManager mongoDB = MongoDBManager.getInstance();
        Document regQuery = new Document();
        regQuery.append("$regex",Pattern.quote(jobTitle));
        regQuery.append("$options", "i"); /* the option i is for case insensitive*/
        FindIterable<Document> founded = mongoDB.getJobOffersCollection().find(eq("job_title", regQuery)).sort(eq("post_date",-1));
        for(Document doc: founded) {
            jobOffers.add(parseJobOffer(doc));
        }
        return jobOffers;
    }

    public List<JobOffer> getJobOffersBySalary(String timeunit, Double minimum){
        List<JobOffer> jobOffers = new ArrayList<>();
        MongoDBManager mongoDB = MongoDBManager.getInstance();
        MongoCollection collection = mongoDB.getJobOffersCollection();
        Bson m = match(eq("salary.time_unit", timeunit));
        Bson p = project(fields(include("job_title", "company_name", "job_type","location", "salary", "post_date"),
                computed("time", "$salary.time_unit"),
                computed("min",
                        eq("$convert",
                                new BsonDocument("input", new BsonDocument("$reduce",
                                        new BsonDocument("input", new BsonDocument("$split",new BsonArray(Arrays.asList(
                                                new BsonString("$salary.from"),
                                                new BsonString(",")))))
                                                .append("initialValue", new BsonString(""))
                                                .append("in", new BsonDocument("$concat", new BsonArray(Arrays.asList(
                                                        new BsonString("$$value"),
                                                        new BsonString("$$this")))))))
                                        .append("to",new BsonString("double"))
                                        .append("onError",new BsonString("0"))))));
        Bson n = match(gte("min", minimum));
        Bson o= sort(Sorts.descending("post_date"));
        AggregateIterable<Document> founded = collection.aggregate(Arrays.asList(m, p , n, o));
        for(Document doc: founded) {
            jobOffers.add(parseJobOffer(doc));

        }
        return jobOffers;
    }

    public JobOffer getById(String Id) {
        MongoDBManager mongoDB = MongoDBManager.getInstance();
        Document doc = (Document) mongoDB.getJobOffersCollection().find(eq("_id", Id)).first();
        if (doc != null) {
            return parseJobOffer(doc);
        } else {
            return null;
        }
    }

    public List<JobOffer> findPublished(String username) {
        List<JobOffer> jobOffers = new ArrayList<>();
        MongoDBManager mongoDB = MongoDBManager.getInstance();

        FindIterable<Document> founded = mongoDB.getJobOffersCollection().find(eq("company_name", username))
                .sort(new Document("post_date", -1));
        for(Document doc: founded) {
            jobOffers.add(parseJobOffer(doc));
        }
        return jobOffers;
    }

    public List<JobOffer> postsByFollowedCompanies(String username) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        List<JobOffer> jobOffers;
        try (org.neo4j.driver.Session session = neo4j.getDriver().session() ) {
            jobOffers = session.readTransaction((TransactionWork<List<JobOffer>>) tx -> {
                Result result = tx.run("MATCH (:JobSeeker {username:$username})-[:FOLLOWS]->(c)-[r:PUBLISHED]->(j)" +
                        "RETURN c.name AS company, date(r.date) as postDate,j.title AS title, j.id AS id ORDER BY postDate DESC LIMIT 20", parameters("username", username));

                List<JobOffer> offers = new ArrayList<>();
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

        return jobOffers;
    }

    public LinkedHashMap<String, Integer> rankCities(String jobType){
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        // Rank cities based on # of (job_type) jobs
        MongoDBManager mongoDB = MongoDBManager.getInstance();
        mongoDB.getJobOffersCollection();
        Bson m = match(eq("job_type",jobType));
        Bson g = group("$location", Accumulators.sum("count",1));
        Bson s = sort(Sorts.descending("count"));
        Bson l = limit(10);
        AggregateIterable<Document> aggregate = mongoDB.getJobOffersCollection().aggregate(Arrays.asList(m, g, s,l));
        MongoCursor<Document> iterator = aggregate.iterator();
        while (iterator.hasNext()) {
            Document next = iterator.next();
            Document loc = (Document) next.get("_id");
            map.put(loc.getString("state") + " - " + loc.getString("city"), next.getInteger("count"));
        }
        iterator.close();
        return map;
    }

    private JobOffer parseJobOffer(Document doc){
        return new JobOffer(doc.getString("_id"), doc.getString("job_title"), doc.getString("company_name"),
                doc.getDate("post_date"),  doc.getString("job_description"), doc.getString("job_type"),
                doc.getEmbedded(List.of("location", "state"), String.class), doc.getEmbedded(List.of("location", "city"), String.class),
                doc.getEmbedded(List.of("salary", "from"), String.class),doc.getEmbedded(List.of("salary", "to"), String.class),
                doc.getEmbedded(List.of("salary", "time_unit"), String.class));
    }

    public List<Long> statistics (){
        ArrayList<Long> stats = new ArrayList<>();
        try {
            MongoDBManager mongoDB = MongoDBManager.getInstance();
            stats.add(mongoDB.getJobSeekersCollection().countDocuments());
            stats.add(mongoDB.getCompaniesCollection().countDocuments());
            stats.add(mongoDB.getJobOffersCollection().countDocuments());
        }catch (Exception e){
        	logger.error(e.getMessage());
        }
        return stats;
    }


}
