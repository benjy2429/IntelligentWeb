package projectFiles;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RiotException;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import fi.foyt.foursquare.api.entities.CompleteVenue;
import twitter4j.User;

/**
 * This class provides a connection to the RDF file for reading and writing data
 * @author Luke Heavens & Ben Carr
 */

public class RDFConnector {
	//Logger
	private static final Logger LOGGER = Logger.getLogger(RDFConnector.class.getName());
	//File Properties
	private static final String RDF_FILE_NAME = "/TripleStore.rdf";
	private static final String ONTOLOGY_FILE_NAME = "/Ontology.rdfs";
	private String rdfFilePath;
	private OntModel ontology;
	private Model rdfModel;
	//RDF Namespaces
	private static final String SCHEMA_NS = "http://schema.org/";
	private static final String BCLH_NS = "http://tomcat.dcs.shef.ac.uk:41032/aca11bmc/";
	private static final String TWITTER_USER_URI = "https://twitter.com/account/redirect_by_id/";
	private static final String USER_KEYWORD_URI = "http://tomcat.dcs.shef.ac.uk:41032/aca11bmc/UserKeyword#";
	private static final String KEYWORD_URI = "http://tomcat.dcs.shef.ac.uk:41032/aca11bmc/Keyword#";
	private static final String FOURSQUARE_LOCATION_URI = "http://foursquare.com/v/";
	

	/**
	 * Constructor attempts to load or recreate the RDF file into memory
	 * @param fileLocation - The directory of the RDF file as a string 
	 * @throws FileNotFoundException
	 */
	public RDFConnector(String fileLocation) throws FileNotFoundException{
		rdfFilePath = fileLocation + RDF_FILE_NAME;
	
		// Read the ontology file into memory
		ontology = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM);
		ontology.read(new FileInputStream(fileLocation + ONTOLOGY_FILE_NAME), Lang.RDFXML.getName());
	
		// Create a new RDFS model in memory using the ontology
		rdfModel = ModelFactory.createRDFSModel(ontology, ModelFactory.createDefaultModel());
		rdfModel.setNsPrefixes(ontology);
        try {
        	// Attempt to read the existing RDF file
        	rdfModel.read(new FileInputStream(rdfFilePath), Lang.RDFXML.getName());
        } catch (RiotException ex) {
        	// If the file does not exist/is corrupt, create a new file from the ontology
        	LOGGER.log(Level.WARNING, "File is new or corrupt, creating a new model: " + ex.getMessage());
        	rdfModel.write(new FileOutputStream(rdfFilePath), Lang.RDFXML.getName());
        }       
	}

	
	/**
	 * Checks whether the RDF model is loaded into memory
	 * @return true if model is in memory, false otherwise
	 */
	public boolean establishConnection() {
		if (rdfModel != null) {
			System.out.println("connection established");
			return true;
		} else {
			return false;
		}
	}

	
	/**
	 * Combines a newly created model with the model in memory
	 * @param newModel - The newly created model
	 */
	public void putRDF(Model newModel) {
		rdfModel.add(newModel);
	}
	

	/**
	 * Creates a new user RDF resource from a Twitter user object
	 * @param user - The Twitter user object
	 */
	public void addUsers(User user) {
		Model model = ModelFactory.createRDFSModel(ontology, ModelFactory.createDefaultModel());
		// Set the URI to the users Twitter profile page
		String userUri = TWITTER_USER_URI + Long.toString(user.getId());

        Resource userResource = model.createResource(userUri);
		userResource.addProperty(ResourceFactory.createProperty(BCLH_NS + "userId"), Long.toString(user.getId()));
    	userResource.addProperty(ResourceFactory.createProperty(SCHEMA_NS + "name"), user.getName());
        userResource.addProperty(ResourceFactory.createProperty(SCHEMA_NS + "alternateName"), user.getScreenName());
        // Optional properties
        if (user.getLocation() != null) { userResource.addProperty(ResourceFactory.createProperty(BCLH_NS + "hometown"), user.getLocation()); }
        if (user.getProfileImageURL() != null) { userResource.addProperty(ResourceFactory.createProperty(BCLH_NS + "profileImgUrl"), user.getProfileImageURL()); }
        if (user.getBiggerProfileImageURL() != null) { userResource.addProperty(ResourceFactory.createProperty(BCLH_NS + "bigProfileImgUrl"), user.getBiggerProfileImageURL()); }      
        if (user.getProfileBannerRetinaURL() != null) { userResource.addProperty(ResourceFactory.createProperty(BCLH_NS + "bannerImgUrl"), user.getProfileBannerRetinaURL()); }
        if (user.getDescription() != null) { userResource.addProperty(ResourceFactory.createProperty(SCHEMA_NS + "description"), user.getDescription()); }
        
        putRDF(model);
	}
	

	/**
	 * Add a relation between two RDF users
	 * @param userId1 - The userId of the first user
	 * @param userId2 - The userId of the second user
	 */
	public void addContact(long userId1, long userId2) {
		Model model = ModelFactory.createRDFSModel(ontology, ModelFactory.createDefaultModel());
		// Set the URI to the users Twitter profile pages
		String user1Uri = TWITTER_USER_URI + String.valueOf(userId1);
		String user2Uri = TWITTER_USER_URI + String.valueOf(userId2);

		// Create a knows property between the two users
        model.createResource(user1Uri)
        	.addProperty(ResourceFactory.createProperty(SCHEMA_NS + "knows"), user2Uri);
        
        putRDF(model);			
	}

	
	/**
	 * Add a relation between a user and a keyword
	 * @param user - The Twitter user object
	 * @param term - The keyword as a string
	 * @param wordCount - The word count of the term from the user
	 */
	public void addUserTermPair(User user, String term, Integer wordCount) { 
		Model model = ModelFactory.createRDFSModel(ontology, ModelFactory.createDefaultModel());
		// Set the URI to the users Twitter profile page
		String userUri = TWITTER_USER_URI + Long.toString(user.getId());
		String userKeywordUri = "";
		String keywordUri = "";
		try {
			// Set the keyword URIs
			userKeywordUri = USER_KEYWORD_URI + Long.toString(user.getId()) + URLEncoder.encode(term, "UTF-8");
			keywordUri = KEYWORD_URI + URLEncoder.encode(term, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		// Create the resources and properties
        model.createResource(userUri) //User Node
	    	.addProperty(ResourceFactory.createProperty(BCLH_NS + "userId"), Long.toString(user.getId()))
	    	.addProperty(ResourceFactory.createProperty(SCHEMA_NS + "name"), user.getName())
	        .addProperty(ResourceFactory.createProperty(SCHEMA_NS + "alternateName"), user.getScreenName())
	        .addProperty(ResourceFactory.createProperty(BCLH_NS + "hometown"), user.getLocation())
	        .addProperty(ResourceFactory.createProperty(BCLH_NS + "profileImgUrl"), user.getProfileImageURL())
	        .addProperty(ResourceFactory.createProperty(BCLH_NS + "bigProfileImgUrl"), user.getBiggerProfileImageURL())
	        .addProperty(ResourceFactory.createProperty(BCLH_NS + "bannerImgUrl"), user.getProfileBannerRetinaURL())
	        .addProperty(ResourceFactory.createProperty(SCHEMA_NS + "description"), user.getDescription())
	        .addProperty(ResourceFactory.createProperty(BCLH_NS + "noTimesSaid"), 
	        		model.createResource(userKeywordUri) //UserKeyword Node (count)
	        			.addProperty(ResourceFactory.createProperty(BCLH_NS + "count"), wordCount.toString())
	        			.addProperty(ResourceFactory.createProperty(BCLH_NS + "wordSaid"),
	        					model.createResource(keywordUri) //Keyword Node (term name)
	        						.addProperty(ResourceFactory.createProperty(SCHEMA_NS + "name"), term)
	        			)
	        );

        putRDF(model);
	}


	/**
	 * Create a new venue RDF resource
	 * @param venue - The Foursquare venue object
	 */
	public void addVenues(CompleteVenue venue) {
		Model model = ModelFactory.createRDFSModel(ontology, ModelFactory.createDefaultModel());
		// Set the URI to the venues Foursquare profile page
		String venueUri = FOURSQUARE_LOCATION_URI + venue.getId();
		String photoUrl = "";
		try {
			// Attempt to get the latest venue photo
			photoUrl = venue.getPhotos().getGroups()[1].getItems()[0].getUrl();
		} catch (Exception e) {
			// No venue photos available. Defaulting to empty string
		}

		// Create the resource and properties
        Resource venueResource = model.createResource(venueUri);
        venueResource.addProperty(ResourceFactory.createProperty(BCLH_NS + "venueId"), venue.getId());
    	venueResource.addProperty(ResourceFactory.createProperty(SCHEMA_NS + "name"), venue.getName());
    	// Optional properties
        if (!photoUrl.equals("")) { venueResource.addProperty(ResourceFactory.createProperty(SCHEMA_NS + "photo"), photoUrl); }
        if (venue.getLocation().getAddress() != null) { venueResource.addProperty(ResourceFactory.createProperty(BCLH_NS + "address"), venue.getLocation().getAddress()); }
        if (venue.getLocation().getCity() != null) { venueResource.addProperty(ResourceFactory.createProperty(BCLH_NS + "city"), venue.getLocation().getCity()); }
        if (venue.getUrl() != null) { venueResource.addProperty(ResourceFactory.createProperty(SCHEMA_NS + "url"), venue.getUrl()); }
        if (venue.getDescription() != null) { venueResource.addProperty(ResourceFactory.createProperty(SCHEMA_NS + "description"), venue.getDescription()); }
        
        putRDF(model);		
	}

	
	/**
	 * Adds a relation between a user and a venue, if a user has visited that venue
	 * @param userId - The userId of the user
	 * @param venueId - The venueId of the venue
	 */
	public void addUserVenue(long userId, String venueId) {
		Model model = ModelFactory.createRDFSModel(ontology, ModelFactory.createDefaultModel());	
		// Set the URIs to the Twitter and Foursquare profile pages
		String userUri = TWITTER_USER_URI + Long.toString(userId);
		String venueUri = FOURSQUARE_LOCATION_URI + venueId;

		// Add a visited property between the user and venue
        model.createResource(userUri)
        	.addProperty(ResourceFactory.createProperty(BCLH_NS + "visited"), venueUri);
        
        putRDF(model);	
	}
	
	
	/**
	 * Retrieve a user's data from the RDF model
	 * @param username - The user's username as a string
	 * @return A hashmap of the properties and their values
	 */
	public HashMap<String, String> showUser(String username) {
		// Create a new hashmap
		HashMap<String,String> userResult = new HashMap<String,String>();
		
		// SPARQL query retrieving a single user resource from a username
        String queryString = 
			"PREFIX schema: <" + SCHEMA_NS + "> " +
			"PREFIX bclh: <" + BCLH_NS + "> " +
			"SELECT ?userId ?name ?screenName ?hometown ?profileImgUrl ?bigProfileImgUrl ?bannerImgUrl ?description " +
			"WHERE {" +
			"	?user schema:alternateName ?screenName ; " +
			"		  a schema:Person ; " +
			"		  bclh:userId ?userId ; " +
			"		  schema:name ?name . " +
			"	FILTER (REGEX(?screenName, \"" + username + "\", \"i\")) " +
			"	OPTIONAL { ?user bclh:hometown ?hometown } " +
			"	OPTIONAL { ?user bclh:profileImgUrl ?profileImgUrl } " +
			"	OPTIONAL { ?user bclh:bigProfileImgUrl ?bigProfileImgUrl } " +
			"	OPTIONAL { ?user bclh:bannerImgUrl ?bannerImgUrl } " +
			"	OPTIONAL { ?user schema:description ?description } " +
			"}" +
			"LIMIT 1";
        
        Query query = QueryFactory.create(queryString);

        // Perform the query on the model
	    QueryExecution qe = QueryExecutionFactory.create(query, rdfModel);
	    ResultSet results = qe.execSelect();
	    
	    if (results.hasNext()) {
	    	// Get the first result
		    QuerySolution userGraph = results.next();
		    
		    // Add properties to the hashmap
		    userResult.put("userId", userGraph.getLiteral("userId").getString());
			userResult.put("fullName", userGraph.getLiteral("name").getString());
			userResult.put("screenName", userGraph.getLiteral("screenName").getString());
			// Optional properties
			if (userGraph.contains("hometown")) { userResult.put("hometown", userGraph.getLiteral("hometown").getString()); }
			if (userGraph.contains("profileImgUrl")) { userResult.put("profileImgUrl", userGraph.getLiteral("profileImgUrl").getString()); }
			if (userGraph.contains("bigProfileImgUrl")) { userResult.put("bigProfileImgUrl", userGraph.getLiteral("bigProfileImgUrl").getString()); }
			if (userGraph.contains("bannerImgUrl")) { userResult.put("bannerImgUrl", userGraph.getLiteral("bannerImgUrl").getString()); }
			if (userGraph.contains("description")) { userResult.put("description", userGraph.getLiteral("description").getString()); }
	    }
	    
	    qe.close();

		return userResult;
	}


	/**
	 * Retrieve user details for users that have retweeted a given user
	 * @param userId - The userId of the user
	 * @return A list of user hashmaps containing the properties and their values 
	 */
	public List<HashMap<String, String>> getRetweetersOfUser(long userId) {
		// Create a new list of hashmaps
		LinkedList<HashMap<String,String>> retweeters = new LinkedList<HashMap<String,String>>();
		
		// SPARQL query retrieving the retweeter user resources from a userId
        String queryString = 
			"PREFIX schema: <" + SCHEMA_NS + "> " +
			"PREFIX bclh: <" + BCLH_NS + "> " +
			"SELECT ?userId ?name ?screenName ?profileImgUrl " +
			"WHERE {" +
			"	?userA bclh:userId \"" + Long.toString(userId) + "\" ; " +
			"		   a schema:Person ; " + 
			"		   schema:knows ?userBUri . " + 
			"   BIND (URI(?userBUri) AS ?userB) . " +
			"	?userB a schema:Person ; " +
			"		   bclh:userId ?userId ; " + 
			"		   schema:name ?name ; " + 
			"		   schema:alternateName ?screenName . " +
			"	OPTIONAL { ?userB bclh:profileImgUrl ?profileImgUrl } " +
			"}";
        
        Query query = QueryFactory.create(queryString);

        // Perform the query on the model
	    QueryExecution qe = QueryExecutionFactory.create(query, rdfModel);
	    ResultSet results = qe.execSelect();
	    
	    // Iterate through the results
	    while (results.hasNext()) {
		    QuerySolution userGraph = results.next();
		    	
		    // Add the users data to the hashmap
		    HashMap<String,String> userHashMap = new HashMap<String,String>();
		    userHashMap.put("userId", userGraph.getLiteral("userId").getString());
		    userHashMap.put("fullName", userGraph.getLiteral("name").getString());
		    userHashMap.put("screenName", userGraph.getLiteral("screenName").getString());
			if (userGraph.contains("profileImgUrl")) { userHashMap.put("profileImgUrl", userGraph.getLiteral("profileImgUrl").getString()); }
			// Add the user to the list
			retweeters.add(userHashMap);
	    }

	    qe.close();

		return retweeters;
	}

	
	/**
	 * Retrieves user details for users that have been retweeted by a given user
	 * @param userId - The userId of a user
	 * @return A list of user hashmaps containing the properties and their values
	 */
	public List<HashMap<String, String>> getUserRetweets(long userId) {
		// Create the list of hashmaps
		LinkedList<HashMap<String,String>> retweeters = new LinkedList<HashMap<String,String>>();
		String userUri = TWITTER_USER_URI + Long.toString(userId);
		
		// SPARQL query retrieving the users retweeted resources from a userId
        String queryString = 
			"PREFIX schema: <" + SCHEMA_NS + "> " +
			"PREFIX bclh: <" + BCLH_NS + "> " +
			"SELECT ?userId ?name ?screenName ?profileImgUrl " +
			"WHERE {" +
			"	?userA schema:knows \"" + userUri + "\" ; " + 
			"		   a schema:Person ; " +
			"		   bclh:userId ?userId ; " +
			"		   schema:name ?name ; " +
			"		   schema:alternateName ?screenName . " +
			"	OPTIONAL { ?userA bclh:profileImgUrl ?profileImgUrl } " +
			"}";
        
        Query query = QueryFactory.create(queryString);

        // Perform the query on the model
	    QueryExecution qe = QueryExecutionFactory.create(query, rdfModel);
	    ResultSet results = qe.execSelect();
	    
	    // Iterate through the results
	    while (results.hasNext()) {
		    QuerySolution userGraph = results.next();
		    	    
		    // Add the user information to the hashmap
		    HashMap<String,String> userHashMap = new HashMap<String,String>();
		    userHashMap.put("userId", userGraph.getLiteral("userId").getString());
		    userHashMap.put("fullName", userGraph.getLiteral("name").getString());
		    userHashMap.put("screenName", userGraph.getLiteral("screenName").getString());
			if (userGraph.contains("profileImgUrl")) { userHashMap.put("profileImgUrl", userGraph.getLiteral("profileImgUrl").getString()); }
			// Add the user to the list
			retweeters.add(userHashMap);
	    }

	    qe.close();

		return retweeters;
	}

	
	/**
	 * Retrieve the locations that a user has visited
	 * @param userId - The user's userId
	 * @return A list of hashmaps containing properties and their values
	 */
	public List<HashMap<String, String>> getUserLocations(long userId) {
		// Create the list of hashmaps
		LinkedList<HashMap<String,String>> locations = new LinkedList<HashMap<String,String>>();		
	
		// SPARQL query retrieving the venue resources from a userId
        String queryString = 
        	"PREFIX schema: <" + SCHEMA_NS + "> " +
   			"PREFIX bclh: <" + BCLH_NS + "> " +
   			"SELECT ?venueId ?name ?photo ?address ?city ?url ?description " +
   			"WHERE {" +
   			"	?user bclh:userId \"" + Long.toString(userId) + "\" ; " +
   			"		  a schema:Person ; " +
   			"		  bclh:visited ?venueUri . " + 
   			"   BIND (URI(?venueUri) AS ?venue) . " +
   			"	?venue a schema:Place ; " +
   			"		   schema:name ?name ; " +
   			"		   bclh:venueId ?venueId . " +
   			"	OPTIONAL { ?venue schema:photo ?photo } " +
   			"	OPTIONAL { ?venue bclh:address ?address } " +
   			"	OPTIONAL { ?venue bclh:city ?city } " +
   			"	OPTIONAL { ?venue schema:url ?url } " +
   			"	OPTIONAL { ?venue schema:description ?description } " +
   			"}";    
	       
	    Query query = QueryFactory.create(queryString);

	    // Perform the query on the model
	    QueryExecution qe = QueryExecutionFactory.create(query, rdfModel);
	    ResultSet results = qe.execSelect();
	    
	    // Iterate through the results
	    while (results.hasNext()) {
		    QuerySolution venueGraph = results.next();
		    	    
		    // Add the venue data to the hashmap
		    HashMap<String,String> locationHashMap = new HashMap<String,String>();
		    locationHashMap.put("name", venueGraph.getLiteral("name").getString());
		    locationHashMap.put("venueId", venueGraph.getLiteral("venueId").getString());
		    // Optional properties
		    if (venueGraph.contains("photo")) { locationHashMap.put("imageUrl", venueGraph.getLiteral("photo").getString()); }
		    if (venueGraph.contains("address")) { locationHashMap.put("address", venueGraph.getLiteral("address").getString()); }
		    if (venueGraph.contains("city")) { locationHashMap.put("city", venueGraph.getLiteral("city").getString()); }
		    if (venueGraph.contains("url")) { locationHashMap.put("websiteUrl", venueGraph.getLiteral("url").getString()); }
		    if (venueGraph.contains("description")) { locationHashMap.put("description", venueGraph.getLiteral("description").getString()); }
		    // Add the venue to the list
			locations.add(locationHashMap);
	    }

	    qe.close();

	    return locations;
	}

	
	/**
	 * Retrieve the keywords said by a user
	 * @param userId - The userId of a user
	 * @return A list of hashmaps containing the properties and their values
	 */
	public List<HashMap<String, String>> getUserKeywords(long userId) {
		// Create the list of hashmaps
		LinkedList<HashMap<String,String>> keywords = new LinkedList<HashMap<String,String>>();
		
		// SPARQL query retrieving 10 keyword resources from a userId ordered by count
        String queryString = 
			"PREFIX schema: <" + SCHEMA_NS + "> " +
			"PREFIX bclh: <" + BCLH_NS + "> " +
			"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
			"SELECT ?count ?word " +
			"WHERE {" +
			"	?user a schema:Person ; " + 
			"		  bclh:userId \"" + Long.toString(userId) + "\" ; " +
			"		  bclh:noTimesSaid ?userKeywordUri . " +
			"   BIND (URI(?userKeywordUri) AS ?userKeyword) . " +
			"	?userKeyword a bclh:UserKeyword ; " +
			"				 bclh:count ?count ; " +
			"				 bclh:wordSaid ?keywordUri . " +
			"   BIND (URI(?keywordUri) AS ?keyword) . " +
			"	?keyword a bclh:Keyword ; " +
			"			 schema:name ?word . " +
			"}" +
        	"ORDER BY DESC(xsd:integer(?count)) " +
			"LIMIT 10";

        Query query = QueryFactory.create(queryString);

        // Perform the query on the model 
	    QueryExecution qe = QueryExecutionFactory.create(query, rdfModel);
	    ResultSet results = qe.execSelect();
	    
	    // Iterate through the results
	    while (results.hasNext()) { 
	    	QuerySolution keywordGraph = results.next();

	    	// Add the word and count to the hashmap
			HashMap<String,String> keywordHashMap = new HashMap<String,String>();
			keywordHashMap.put("word", keywordGraph.getLiteral("word").getString());
			keywordHashMap.put("count", keywordGraph.getLiteral("count").getString());
			// Add the keyword to the list
			keywords.add(keywordHashMap);
	    }
	    
	    qe.close();
	    
		return keywords;

	}

	
	/**
	 * Retrieve the venue RDF resource from a venue name 
	 * @param venueName - The name of a venue as a string
	 * @return A hashmap of the venue's properties and values
	 */
	public HashMap<String, String> showVenue(String venueName) {
		// Create a new hashmap
		HashMap<String,String> venue = new HashMap<String,String>();
		// Escape any SPARQL syntax characters
		String escapedVenueName = venueName.replace("(","\\\\(").replace(")","\\\\)").replace(".","\\\\.").replace(";","\\\\;").replace("*","\\\\*");
		
		// SPARQL query retrieving a single venue resource from a venue name
        String queryString = 
			"PREFIX schema: <" + SCHEMA_NS + "> " +
			"PREFIX bclh: <" + BCLH_NS + "> " +
			"SELECT ?venueId ?name ?photo ?address ?city ?url ?description " +
			"WHERE {" +
			"	?venue a schema:Place ; " + 
			"		   schema:name ?name ; " +
			"		   bclh:venueId ?venueId . " +
			"	FILTER (REGEX(str(?name), \"" + escapedVenueName + "\", \"i\")) " + 
			"	OPTIONAL { ?venue schema:photo ?photo } " +
			"	OPTIONAL { ?venue bclh:address ?address } " +
			"	OPTIONAL { ?venue bclh:city ?city } " +
			"	OPTIONAL { ?venue schema:url ?url } " +
			"	OPTIONAL { ?venue schema:description ?description } " +
			"}" +
			"LIMIT 1";
        
        Query query = QueryFactory.create(queryString);
        
        // Perform the query on the model
	    QueryExecution qe = QueryExecutionFactory.create(query, rdfModel);
	    ResultSet results = qe.execSelect();

	    if (results.hasNext()) { 
	    	QuerySolution venueGraph = results.next();
	    
	    	// Add the venue data  to the hashmap
	    	venue.put("venueId", venueGraph.getLiteral("venueId").getString());
		    venue.put("name", venueGraph.getLiteral("name").getString());
		    // Optional properties
			if (venueGraph.contains("photo")) { venue.put("imageUrl", venueGraph.getLiteral("photo").getString()); }
			if (venueGraph.contains("address")) { venue.put("address", venueGraph.getLiteral("address").getString()); }
			if (venueGraph.contains("city")) { venue.put("city", venueGraph.getLiteral("city").getString()); }
			if (venueGraph.contains("url")) { venue.put("url", venueGraph.getLiteral("url").getString()); }
			if (venueGraph.contains("description")) { venue.put("description", venueGraph.getLiteral("description").getString()); }
	    }
	    
	    qe.close();
	    
		return venue;
	}

	
	/**
	 * Retrieve the users who have visited a venue
	 * @param venueId - The venueId of the venue
	 * @return A list of hashmaps containing the properties and values
	 */
	public List<HashMap<String, String>> getVenueVisitors(String venueId) {
		// Create the list of hashmaps
		LinkedList<HashMap<String,String>> users = new LinkedList<HashMap<String,String>>();
		String venueUri = FOURSQUARE_LOCATION_URI + venueId;
		
		// SPARQL query retrieving the visiting user resources from a venueId
        String queryString = 
			"PREFIX schema: <" + SCHEMA_NS + "> " +
			"PREFIX bclh: <" + BCLH_NS + "> " +
			"SELECT ?userId ?name ?screenName ?hometown ?profileImgUrl ?description " +
			"WHERE {" +
			"	?user bclh:visited \"" + venueUri + "\" ; " +
			"		  a schema:Person ; " +
			"		  bclh:userId ?userId ; " +
			"		  schema:name ?name ; " +
			"		  schema:alternateName ?screenName . " +
			"	OPTIONAL { ?user bclh:hometown ?hometown } " +
			"	OPTIONAL { ?user bclh:profileImgUrl ?profileImgUrl } " +
			"	OPTIONAL { ?user schema:description ?description } " +
			"}";
        
        Query query = QueryFactory.create(queryString);

        // Perform the query on the model
	    QueryExecution qe = QueryExecutionFactory.create(query, rdfModel);
	    ResultSet results = qe.execSelect();
	    
	    // Iterate through the results
	    while (results.hasNext()) { 
	    	QuerySolution userGraph = results.next();
	    
	    	// Add the user data to the hashmap
	    	HashMap<String,String> userHashMap = new HashMap<String,String>();
	    	userHashMap.put("userId", userGraph.getLiteral("userId").getString());
	    	userHashMap.put("fullName", userGraph.getLiteral("name").getString());
	    	userHashMap.put("screenName", userGraph.getLiteral("screenName").getString());
	    	// Optional properties
			if (userGraph.contains("hometown")) { userHashMap.put("hometown", userGraph.getLiteral("hometown").getString()); }
			if (userGraph.contains("profileImgUrl")) { userHashMap.put("profileImgUrl", userGraph.getLiteral("profileImgUrl").getString()); }
			if (userGraph.contains("description")) { userHashMap.put("description", userGraph.getLiteral("description").getString()); }
			// Add the hashmap to the list
			users.add(userHashMap);
	    }
	    
	    qe.close();
		
		return users;
	}

	
	/**
	 * Write the updated model in memory to the external RDF file for storage
	 */
	public void closeConnection() {
		System.out.println("Writing to file");
		try {
			rdfModel.write(new FileOutputStream(rdfFilePath), Lang.RDFXML.getName());
		} catch (FileNotFoundException ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
		}
		System.out.println("File written");
	}
}
