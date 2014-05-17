package projectFiles;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.jena.riot.Lang;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.VCARD;

import exceptions.DatastoreException;
import fi.foyt.foursquare.api.entities.CompleteVenue;
import twitter4j.User;

public class RDFConnector {
	//Logger
	private static final Logger LOGGER = Logger.getLogger(DatabaseConnector.class.getName());
	//File Properties
	private static final String RDF_FILE_NAME = "/TripleStore.rdf";
	private static final String ONTOLOGY_FILE_NAME = "/Ontology.rdfs";
	private String rdfFilePath;
	private OntModel ontology;
	private Model rdfModel;
	
	private static final String SCHEMA_NS = "http://schema.org/";
	private static final String BCLH_NS = "http://tomcat.dcs.shef.ac.uk:41032/aca11bmc/";
	private static final String TWITTER_USER_URI = "https://twitter.com/?profile_id=";
	private static final String KEYWORD_URI = "http://tomcat.dcs.shef.ac.uk:41032/aca11bmc/Keyword#";
	private static final String FOURSQUARE_LOCATION_URI = "http://foursquare.com/v/";
	

	public RDFConnector(String fileLocation){
		rdfFilePath = fileLocation + RDF_FILE_NAME;
		
		ontology = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM);
		try {
			ontology.read(new FileInputStream(fileLocation + ONTOLOGY_FILE_NAME), Lang.RDFXML.getName());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		rdfModel = ModelFactory.createRDFSModel(ontology, ModelFactory.createDefaultModel());
        try {
        	rdfModel.read(new FileInputStream(rdfFilePath), Lang.RDFXML.getName());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean establishConnection() {
		try {
			//TODO check that model is accessible
			System.out.println("connection established");
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			//TODO handle correctly
			return false;
		}
	}

	public void putRDF(Model newModel) throws FileNotFoundException {
		rdfModel.add(newModel);
		rdfModel.write(new FileOutputStream(rdfFilePath), Lang.RDFXML.getName());
		rdfModel.write(System.out);
	}
	
	public void test() throws FileNotFoundException{
        // some definitions
        String uri    = "http://somewhere/meadowhall";
        String name    = "Meadowhall";
        String city = "Sheffield";

        // create an empty model
		Model model = ModelFactory.createRDFSModel(ontology, ModelFactory.createDefaultModel());

        // create the resource
        //   and add the properties cascading style
        Resource helloword 
          = model.createResource(uri)
                 .addProperty(ResourceFactory.createProperty(SCHEMA_NS + "name"), name)
        		 .addProperty(ResourceFactory.createProperty(BCLH_NS + "city"), city);        
        
        putRDF(model);
        
        /*
        String queryString = 
        		"PREFIX vcard: <http://www.w3.org/2001/vcard-rdf/3.0#> " +
        		"SELECT ?fullname " +
				"WHERE {" +
        		"	?person vcard:Given \"Joe\" . " +
        		"	?person vcard:Given ?given . " +
				"	?person vcard:Family ?family . " +
				"	?personURI vcard:N ?person . " +
				"	?personURI vcard:FN ?fullname . " +
				"}";
        
        Query query = QueryFactory.create(queryString);

	    // Execute the query and obtain results
	     QueryExecution qe = QueryExecutionFactory.create(query, getExistingRDF());
	     ResultSet results = qe.execSelect();
	
	     // Output query results	
	     ResultSetFormatter.out(System.out, results, query);
	
	     // Important - free up resources used running the query
	     qe.close();
	     */
	}
	
	

	
	public void addUsers(User user) {
		Model model = ModelFactory.createRDFSModel(ontology, ModelFactory.createDefaultModel());	
		String userUri = TWITTER_USER_URI + Long.toString(user.getId());

        model.createResource(userUri)
        	.addProperty(ResourceFactory.createProperty(BCLH_NS + "userId"), Long.toString(user.getId()))
        	.addProperty(ResourceFactory.createProperty(SCHEMA_NS + "name"), user.getName())
            .addProperty(ResourceFactory.createProperty(SCHEMA_NS + "alternateName"), user.getScreenName())
            .addProperty(ResourceFactory.createProperty(BCLH_NS + "hometown"), user.getLocation())
            .addProperty(ResourceFactory.createProperty(BCLH_NS + "profileImgUrl"), user.getProfileImageURL())
            .addProperty(ResourceFactory.createProperty(BCLH_NS + "bigProfileImgUrl"), user.getBiggerProfileImageURL())
            .addProperty(ResourceFactory.createProperty(BCLH_NS + "bannerImgUrl"), user.getProfileBannerRetinaURL())
            .addProperty(ResourceFactory.createProperty(SCHEMA_NS + "description"), user.getDescription());
        
        putRDF(model);	//TODO exception handling
	}
	

	public void addContact(long userId1, long userId2) {
		Model model = ModelFactory.createRDFSModel(ontology, ModelFactory.createDefaultModel());	
		String user1Uri = TWITTER_USER_URI + String.valueOf(userId1);
		String user2Uri = TWITTER_USER_URI + String.valueOf(userId2);

        model.createResource(user1Uri)
        	.addProperty(ResourceFactory.createProperty(SCHEMA_NS + "knows"), user2Uri);
        
        putRDF(model);			
	}
	

	public int addWord(String term) {
		// TODO Not needed anymore? Keywords are per user now
		return 0;
	}

	
	public int getWordId(String term) {
		// TODO Auto-generated method stub
		return 0;
	}

	
	public void addUserTermPair(Long userId, String word, Integer wordCount) { // TODO Changed from wordId(int) to word(String). Needs changing everywhere
		Model model = ModelFactory.createRDFSModel(ontology, ModelFactory.createDefaultModel());	
		String userUri = TWITTER_USER_URI + Long.toString(userId);
		String wordUri = KEYWORD_URI + word;

        model.createResource(userUri)
        	.addProperty(ResourceFactory.createProperty(BCLH_NS + "said"),
        			model.createResource(wordUri)
        				.addProperty(ResourceFactory.createProperty(SCHEMA_NS + "name"), word)
        				.addProperty(ResourceFactory.createProperty(BCLH_NS + "count"), Integer.toString(wordCount))
        );
        
        putRDF(model);
		
	}

	
	public void addVenues(CompleteVenue venue) {
		Model model = ModelFactory.createRDFSModel(ontology, ModelFactory.createDefaultModel());	
		String venueUri = FOURSQUARE_LOCATION_URI + venue.getId();
		String photoUrl = "";
		try {
			photoUrl = venue.getPhotos().getGroups()[1].getItems()[0].getUrl();
		} catch (Exception e) {
			// No venue photos available. Defaulting to empty string
		}

        model.createResource(venueUri)
        	.addProperty(ResourceFactory.createProperty(BCLH_NS + "venueId"), venue.getId())
        	.addProperty(ResourceFactory.createProperty(SCHEMA_NS + "name"), venue.getName())
            .addProperty(ResourceFactory.createProperty(SCHEMA_NS + "photo"), photoUrl)
            .addProperty(ResourceFactory.createProperty(BCLH_NS + "address"), venue.getLocation().getAddress())
            .addProperty(ResourceFactory.createProperty(BCLH_NS + "city"), venue.getLocation().getCity())
            .addProperty(ResourceFactory.createProperty(SCHEMA_NS + "url"), venue.getUrl())
            .addProperty(ResourceFactory.createProperty(SCHEMA_NS + "description"), venue.getDescription());
        
        putRDF(model);	
		
	}

	
	public void addUserVenue(long userId, String venueId) {
		Model model = ModelFactory.createRDFSModel(ontology, ModelFactory.createDefaultModel());	
		String userUri = TWITTER_USER_URI + Long.toString(userId);
		String venueUri = FOURSQUARE_LOCATION_URI + venueId;

        model.createResource(userUri)
        	.addProperty(ResourceFactory.createProperty(BCLH_NS + "visited"), venueUri);
        
        putRDF(model);	
		
	}
	
	
	public HashMap<String, String> showUser(String username) { //TODO NEEDS USERID IN SCHEMA!!
		
		HashMap<String,String> userResult = new HashMap<String,String>();
		
        String queryString = 
			"PREFIX schema: <" + SCHEMA_NS + "> " +
			"PREFIX bclh: <" + BCLH_NS + "> " +
			"SELECT ?userId ?name ?screenName ?hometown ?profileImgUrl ?bigProfileImgUrl ?bannerImgUrl ?description" +
			"WHERE {" +
			"	?user schema:alternateName \"" + username + "\" ; " +
			"		  bclh:userId ?userId ; " +
			"		  schema:name ?name ; " +
			"		  schema:alternateName ?screenName . " +
			"	OPTIONAL {" +
			"		?user bclh:hometown ?hometown ; " +
			"			  bclh:profileImgUrl ?profileImgUrl ; " +
			"			  bclh:bigProfileImgUrl ?bigProfileImgUrl ; " +
			"			  bclh:bannerImgUrl ?bannerImgUrl ; " +
			"			  schema:description ?description . " +
			"	}" +
			"}" +
			"LIMIT 1"; //TODO add LIKE filter 
        
        Query query = QueryFactory.create(queryString);

	    QueryExecution qe = QueryExecutionFactory.create(query, rdfModel);
	    ResultSet results = qe.execSelect();
	    QuerySolution userGraph = results.next();
	    
	    userResult.put("userId", userGraph.getLiteral("userId").getString());
		userResult.put("fullName", userGraph.getLiteral("name").getString());
		userResult.put("screenName", userGraph.getLiteral("screenName").getString());
		if (userGraph.contains("hometown")) { userResult.put("hometown", userGraph.getLiteral("hometown").getString()); }
		if (userGraph.contains("profileImgUrl")) { userResult.put("profileImgUrl", userGraph.getLiteral("profileImgUrl").getString()); }
		if (userGraph.contains("bigProfileImgUrl")) { userResult.put("bigProfileImgUrl", userGraph.getLiteral("bigProfileImgUrl").getString()); }
		if (userGraph.contains("bannerImgUrl")) { userResult.put("bannerImgUrl", userGraph.getLiteral("bannerImgUrl").getString()); }
		if (userGraph.contains("description")) { userResult.put("description", userGraph.getLiteral("description").getString()); }
		
	    ResultSetFormatter.out(System.out, results, query); //TODO Remove - Debugging purposes

	    qe.close();

		return userResult;
	}

	
	public List<HashMap<String, String>> getRetweetersOfUser(long userId) {
		// TODO Auto-generated method stub
		LinkedList<HashMap<String,String>> retweeters = new LinkedList<HashMap<String,String>>();
		
        String queryString = 
			"PREFIX schema: <" + SCHEMA_NS + "> " +
			"PREFIX bclh: <" + BCLH_NS + "> " +
			"SELECT ?name ?screenName ?profileImgUrl" +
			"WHERE {" +
			"	?userA bclh:userId \"" + Long.toString(userId) + "\" ; " +
			"		   schema:knows ?userB ; " + 
			"	?userB schema:name ?name ; " +
			"		   schema:alternateName ?screenName . " +
			"	OPTIONAL {" +
			"		?userB bclh:profileImgUrl ?profileImgUrl . " +
			"	}" +
			"}";

        Query query = QueryFactory.create(queryString);

	    QueryExecution qe = QueryExecutionFactory.create(query, rdfModel);
	    ResultSet results = qe.execSelect();
	    
	    while (results.hasNext()) {
		    QuerySolution userGraph = results.next();
		    	    
		    HashMap<String,String> userHashMap = new HashMap<String,String>();
		    userHashMap.put("name", userGraph.getLiteral("name").getString());
		    userHashMap.put("screenName", userGraph.getLiteral("screenName").getString());
			if (userGraph.contains("profileImgUrl")) { userHashMap.put("profileImgUrl", userGraph.getLiteral("profileImgUrl").getString()); }
			retweeters.add(userHashMap);
	    }

	    qe.close();

		return retweeters;
	}

	
	public List<HashMap<String, String>> getUserRetweets(long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<HashMap<String, String>> getUserLocations(long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<HashMap<String, String>> getUserKeywords(long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public HashMap<String, String> showVenue(String venueName) {
		HashMap<String,String> venue = new HashMap<String,String>();	
        String queryString = 
			"PREFIX schema: <" + SCHEMA_NS + "> " +
			"PREFIX bclh: <" + BCLH_NS + "> " +
			"SELECT ?venueId ?name ?photo ?address ?city ?url ?description" +
			"WHERE {" +
			"	?venue schema:name ?name ; " +
			"		   bclh:venueId ?venueId . " +
			"	FILTER (REGEX(?name, \"" + venueName + "\", \"i\")) " +
			"	OPTIONAL {" +
			"		?venue schema:photo ?photo ; " +
			"			  bclh:address ?address ; " +
			"			  bclh:city ?city ; " +
			"			  schema:url ?url ; " +
			"			  schema:description ?description . " +
			"	}" +
			"}" +
			"LIMIT 1";
        
        Query query = QueryFactory.create(queryString);

	    QueryExecution qe = QueryExecutionFactory.create(query, rdfModel);
	    ResultSet results = qe.execSelect();
	    
	    if (results.hasNext()) { 
	    	QuerySolution venueGraph = results.next();
	    
	    	venue.put("venueId", venueGraph.getLiteral("venueId").getString());
		    venue.put("name", venueGraph.getLiteral("name").getString());
			if (venueGraph.contains("photo")) { venue.put("photo", venueGraph.getLiteral("photo").getString()); }
			if (venueGraph.contains("address")) { venue.put("address", venueGraph.getLiteral("address").getString()); }
			if (venueGraph.contains("city")) { venue.put("city", venueGraph.getLiteral("city").getString()); }
			if (venueGraph.contains("url")) { venue.put("url", venueGraph.getLiteral("url").getString()); }
			if (venueGraph.contains("description")) { venue.put("description", venueGraph.getLiteral("description").getString()); }
	    }
	    
	    qe.close();
	    
		return venue;
	}

	
	public List<HashMap<String, String>> getVenueVisitors(String venueId) {
		// TODO Auto-generated method stub
		return null;
	}

	public void closeConnection() {
		// TODO Auto-generated method stub
		
	}
}
