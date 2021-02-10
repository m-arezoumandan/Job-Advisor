package it.unipi.dii.lsdb.group13.database;

import org.neo4j.driver.*;

public class Neo4jManager implements AutoCloseable {

    private final Driver driver;
    private static Neo4jManager neo4j = null;
    //private String uri = "neo4j://localhost:7687";  // to localhost

    private Neo4jManager(){
        driver = GraphDatabase.driver("neo4j://localhost:7687",
                AuthTokens.basic("neo4j","lem13"));
        System.out.println("Neo4j Connection opened");
    }

    Driver getDriver() {
        if(neo4j == null)
            throw new RuntimeException("Connection doesn't exist.");
        else
            return neo4j.driver;
    }

    static Neo4jManager getInstance() {
        if(neo4j == null)
            neo4j = new Neo4jManager();

        return neo4j;
    }

    @Override
    public void close(){
        if (driver == null){
            System.out.println("Neo4j connection was not open");
        }else {
            System.out.println("Neo4j connection closed");
            driver.close();
        }
    }
}