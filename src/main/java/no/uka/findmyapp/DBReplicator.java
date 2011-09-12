package no.uka.findmyapp;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.google.gson.Gson;
public class DBReplicator {
	
	private final String JSON_OBJECT_SEPARATOR_REGEX = "(?<=[}]),\\s*";
	
    private final Properties configFile;	
	private String slaveUsername;
	private String slavePassword;
	private String slaveDatabaseHost;
	private String slaveDatabaseDBName;
	private String masterUsername; 
	private String masterPassword;
	private String masterDatabaseHost; 
	private String masterDatabaseDBName;
	private boolean debugmode; 
	private boolean useProxy;
	private String proxyHost = "";
	private String proxyPort = "";
	private String showingJSONURL = "";
	private String eventsJSONURL = "";
	
	private Connection connectionMaster = null;
	private Connection connectionSlave = null;
	private ResultSet rs2;
	private final java.sql.Timestamp timestamp;
	
	private final boolean doDelete;
	
	private JSONFetcher fetcher;
	boolean useDB = false;// Using JSON instead of DB
	
	// Public constructor
	public DBReplicator(){
		this.timestamp = new java.sql.Timestamp(System.currentTimeMillis());
		this.doDelete = true;
		this.configFile = new Properties();
	}
	
	/**
	 * Mandatory method to load properties from dbreplicator.properties file
	 * @throws IOException
	 */
	private void loadProperties(String configLocation) throws IOException{
		
		System.out.println("-> Loading properties from " + configLocation);
		File file = new File(configLocation);
		if(file.exists()) {
			System.out.println("Config file exists!!!");
		} else {
			System.out.println("No config file exists!!!");
		}
		//configFile.load(this.getClass().getClassLoader().getResourceAsStream(configLocation));
		
		configFile.load(new FileReader(file));
		
		slaveUsername = configFile.getProperty("slaveUsername");
		slavePassword = configFile.getProperty("slavePassword");
		slaveDatabaseHost = configFile.getProperty("slaveDatabaseHost");
		slaveDatabaseDBName = configFile.getProperty("slaveDatabaseDBName");
		masterUsername = configFile.getProperty("masterUsername"); 
		masterPassword = configFile.getProperty("masterPassword");
		masterDatabaseHost = configFile.getProperty("masterDatabaseHost"); 
		masterDatabaseDBName = configFile.getProperty("masterDatabaseDBName");
		
		showingJSONURL = configFile.getProperty("showingJSONURL");
		eventsJSONURL = configFile.getProperty("eventsJSONURL");
		
		if(configFile.getProperty("debugmode").equals("true") ){
			debugmode = true;
		} else {
			debugmode = false;
		}
		
		if(configFile.getProperty("useProxy").equals("true")){
			useProxy = true;
		} else {
			useProxy = false;
		}
		proxyHost = configFile.getProperty("proxyHost"); 
		proxyPort = configFile.getProperty("proxyport");
	}
	/**
	 * Method to connect to master database using PostgreSQL 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws SQLException 
	 */
	private void connectToMasterDataSource() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
	

		Class.forName("org.postgresql.Driver").newInstance();
		connectionMaster = DriverManager.getConnection("jdbc:postgresql://"+masterDatabaseHost+"/"+masterDatabaseDBName,masterUsername, masterPassword );

	}
	
	/**
	 * Method to connect to slave database using MySQL jdbc driver
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws SQLException 
	 */
	private void connectToSlaveDataSource() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
	

			Class.forName ("com.mysql.jdbc.Driver").newInstance ();
			connectionSlave = DriverManager.getConnection("jdbc:mysql://"+slaveDatabaseHost+"/"+slaveDatabaseDBName, slaveUsername, slavePassword);

	}
	
	/**
	 * Mandatory method to close result sets and database connection
	 * @throws SQLException 
	 */
	private void cleanUp() throws SQLException{
		
		System.out.println("-> Cleaning up connections and result sets");
		
		if(rs2 != null) {
			rs2.close();
		}
		
		if(connectionMaster != null) {
			connectionMaster.close();
		}
		if(connectionSlave != null) {
			connectionSlave.close();
		}
	}
	
	/**
	 * Get array of all events from DB.
	 * 
	 * @param resultSet
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 * @throws MalformedURLException 
	 */
	private DBEvent[] populateEventsFromDb(Connection connection) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, MalformedURLException, IllegalArgumentException, IOException {
		if(connection == null) {
			return null;
		}
		System.out.println("-> Retrieving data from master database");
    	Statement stmt = connection.createStatement();
    	ResultSet resultSet = stmt.executeQuery("SELECT * FROM uka.events_showing_event_published");
    	if(resultSet == null) {
    		return null;
    	}
		List<DBEvent> eventList = new ArrayList<DBEvent>();
		int counter = 0;
		System.out.println("Retrieving rows from DB:");
		while(resultSet.next()) {
			System.out.print(++counter + ",");
			DBEvent event = new DBEvent();
			// This must match database view
			event.setShowingId(resultSet.getInt(1));
			event.setShowing_time(resultSet.getTimestamp(2));
			event.setSale_from(resultSet.getTimestamp(3)); 
			event.setSale_to(resultSet.getTimestamp(4));
			event.setFree(resultSet.getBoolean(5));
			event.setEntrance_id(resultSet.getInt(6));
			event.setAvailable_for_purchase(resultSet.getBoolean(7));
			event.setNetsale_from(resultSet.getTimestamp(8));
			event.setNetsale_to(resultSet.getTimestamp(9));
			event.setPublish_time(resultSet.getTimestamp(10));
			event.setPlace(resultSet.getString(11));
			event.setCanceled(resultSet.getBoolean(12));
			event.setEvent_id(resultSet.getInt(13));
			event.setBillig_id(resultSet.getInt(14));
			event.setBillig_name(resultSet.getString(15));
			event.setTitle(resultSet.getString(16));
			event.setLead(resultSet.getString(17));
			event.setText(resultSet.getString(18));
			event.setEvent_type(resultSet.getString(19));
			event.setAge_limit(resultSet.getInt(20));
			event.setSpotify_string(resultSet.getString(21));
			
			DBEvent jsonShowing = getShowingFromJson(event.getShowingId(), fetcher);
			DBEvent jsonEvent = getEventFromJson(event.getEvent_id(), fetcher);
			event = joinDbEvent(event, jsonShowing, jsonEvent);
			
			eventList.add(event);
		}
		return eventList.toArray(new DBEvent[eventList.size()]);
	}
	
	/**
	 * Get array of all events from JSON.
	 * 
	 * @return
	 * @throws MalformedURLException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	private DBEvent[] populateEventsFromJson(JSONFetcher fetcher) throws MalformedURLException, IOException {
		// Workaround - need to fetch a webpage before fetching JSON feed from uka.no
		// TODO: sort this out if possible
		fetcher.getWebFile("http://www.uka.no");
		// Get all showings from JSON
		String allShowingsString = cleanJsonString(fetcher.getWebFile(showingJSONURL));
		if(allShowingsString == null || allShowingsString.length() < 6) {
			return null;
		}
		List<DBEvent> eventList = new ArrayList<DBEvent>();
		// Split into separate JSON objects
		String[] showingStrings = allShowingsString.split(JSON_OBJECT_SEPARATOR_REGEX);
		System.out.println("Found " + showingStrings.length + " JSON event objects");
		for(String showingString : showingStrings) {
			// Map JSON object
			JSONShowing jsonShowingObject = new Gson().fromJson(showingString, JSONShowing.class);
			// Create event object from JSON object
			DBEvent showing = jsonToShowing(jsonShowingObject);
			// Get event info from JSON
			DBEvent event = getEventFromJson(showing.getEvent_id(), fetcher);
			// Augment event info
			showing = joinJsonEvent(showing, event);
			eventList.add(showing);
		}
		return eventList.toArray(new DBEvent[eventList.size()]);
	}
	
	/**
	 * Get showing info with given id from JSON.
	 * 
	 * @param id
	 * @param fetcher
	 * @return
	 * @throws MalformedURLException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	private DBEvent getShowingFromJson(int id, JSONFetcher fetcher) throws MalformedURLException, IllegalArgumentException, IOException {
		// Get object from JSON
		String showing = cleanJsonString(fetcher.getWebFile(showingJSONURL+id));
		if(showing == null || showing.length() < 6) {
			return null;
		}
		// Map JSON object
		JSONShowing jsonObject = new Gson().fromJson(showing, JSONShowing.class);
		// Create event object from JSON object
		return jsonToShowing(jsonObject);
	}
	
	/**
	 * Get event info with given id from JSON.
	 * 
	 * @param id
	 * @param fetcher
	 * @return
	 * @throws MalformedURLException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	private DBEvent getEventFromJson(int id, JSONFetcher fetcher) throws MalformedURLException, IllegalArgumentException, IOException {
		// Get object from JSON
		String event = cleanJsonString(fetcher.getWebFile(eventsJSONURL+id));
		if(event == null || event.length() < 6) {
			return null;
		}
		// Map JSON object
		JSONEvent jsonObject = new Gson().fromJson(event, JSONEvent.class);
		// Create event object from JSON object
		return jsonToEvent(jsonObject);
	}
	
	/**
	 * Join JSON showing event object info with event object info.
	 * 
	 * @param primary Object containing showing info
	 * @param secondary Object containing event info
	 * @return Showing object augmented with event info.
	 */
	private DBEvent joinJsonEvent(DBEvent primary, DBEvent secondary) {
		if(secondary != null) {
			primary.setTitle(secondary.getTitle());
			primary.setLead(secondary.getLead());
			primary.setText(secondary.getText());
			primary.setEvent_type(secondary.getEvent_type());
			primary.setAge_limit(secondary.getAge_limit());
			primary.setSpotify_string(secondary.getSpotify_string());
			primary.setImageURL(secondary.getImageURL());
			primary.setThumbnailURL(secondary.getThumbnailURL());
		}
		return primary;
	}
	
	/**
	 * Join DB showing event object info with event object info.
	 * 
	 * @param primary Object containing showing info
	 * @param secondary Object containing event info
	 * @return Showing object augmented with event info.
	 */
	private DBEvent joinDbEvent(DBEvent primary, DBEvent showing, DBEvent event) {
		if(showing != null) {
			primary.setLowest_price(showing.getLowest_price());
			primary.setPlaceString(showing.getPlace_string());
		}
		if(event != null) {
			primary.setImageURL(event.getImageURL());
			primary.setThumbnailURL(event.getThumbnailURL());
		}
		return primary;
	}
	
	/**
	 * Clean JSON string to valid format.
	 * 
	 * @param jsonObject
	 * @return
	 */
	private String cleanJsonString(String jsonObject) {
		if(jsonObject == null) {
			return null;
		}
		return jsonObject.substring(jsonObject.indexOf("[")+1, jsonObject.lastIndexOf("]")).trim();
	}
	
	/**
	 * Use date from JSON to create event object.
	 * 
	 * @param jsonShowing JSON object
	 * @return Event object
	 */
	private DBEvent jsonToShowing(JSONShowing jsonShowing) {
		if(jsonShowing == null) {
			return null;
		}
		DBEvent event = new DBEvent();
		event.setShowingId(jsonShowing.getId());
		event.setShowing_time(Timestamp.valueOf(jsonShowing.getShowing_time().replace("T", " ")));
		event.setSale_from(Timestamp.valueOf(jsonShowing.getSale_from().replace("T", " "))); 
		event.setFree(jsonShowing.isFree());
		event.setPlace(jsonShowing.getPlace());
		event.setCanceled(jsonShowing.isCanceled());
		event.setEvent_id(jsonShowing.getEvent_id());
		event.setLowest_price(jsonShowing.getPrices_from());
		event.setPlaceString(jsonShowing.getPlace_string());
		return event;
	}
	
	/**
	 * Use date from JSON to create event object.
	 * 
	 * @param jsonEvent JSON object
	 * @return Event object
	 */
	private DBEvent jsonToEvent(JSONEvent jsonEvent) {
		if(jsonEvent == null) {
			return null;
		}
		DBEvent event = new DBEvent();
		event.setTitle(jsonEvent.getTitle());
		event.setLead(jsonEvent.getLead());
		event.setText("");	// TODO Use when available in JSON
		event.setEvent_type(jsonEvent.getEvent_type());
		event.setAge_limit(jsonEvent.getAge_limit());
		event.setSpotify_string(jsonEvent.getSpotify_query());
		event.setImageURL(jsonEvent.getImage_url());
		// Thumbnail is not provided in JSON feed at this time
		event.setThumbnailURL("");
		return event;
	}
	
	/**
	 * Insert event data into DB.
	 * 
	 * @param event
	 * @throws SQLException
	 */
	private void insertEventData(DBEvent event, Connection connection) throws SQLException {
		// Insert data from Event bean into slave database using UPDATE INTO				
		String SQL = "INSERT INTO findmydb.UKA_EVENTS (id,showing_time,publish_time,place,billig_id,billig_name,netsale_from,netsale_to,sale_from,sale_to,free,available_for_purchase,canceled,entrance_id,event_id,title,lead,text,event_type,image,thumbnail,age_limit,spotify_string,update_date,lowest_price,place_string) VALUES ("+event.getShowingId()+", '"+event.getShowing_time()+"', '"+event.getPublish_time()+"', '"+event.getPlace()+"' , "+event.getBillig_id()+" ,' "+event.getBillig_name()+"', '"+event.getNetsale_from()+"','"+event.getNetsale_to()+"','"+event.getSale_from()+"','"+event.getSale_to()+"',"+event.isFree()+","+event.isAvailable_for_purchase()+","+event.isCanceled()+","+event.getEntrance_id()+","+event.getEvent_id()+",'"+event.getTitle()+"','"+event.getLead()+"','"+event.getText()+"','"+event.getEvent_type()+"','"+event.getImageURL()+"','"+event.getThumbnailURL()+"',"+event.getAge_limit()+",'"+event.getSpotify_string()+"','"+timestamp+"', "+event.getLowest_price()+",'"+event.getPlace_string()+"') ON DUPLICATE KEY UPDATE showing_time='"+event.getShowing_time()+"',publish_time='"+event.getPublish_time()+"',place='"+event.getPlace()+"',billig_id="+event.getBillig_id()+", billig_name='"+event.getBillig_name()+"', netsale_from='"+event.getNetsale_from()+"', netsale_to='"+event.getNetsale_to()+"',sale_from='"+event.getSale_from()+"',sale_to='"+event.getSale_to()+"', free="+event.isFree()+", available_for_purchase="+event.isAvailable_for_purchase()+", canceled="+event.isCanceled()+", entrance_id="+event.getEntrance_id()+", event_id="+event.getEvent_id()+",title='"+event.getTitle()+"', lead='"+event.getLead()+"', text='"+event.getText()+"', event_type='"+event.getEvent_type()+"', image='"+event.getImageURL()+"',thumbnail='"+event.getThumbnailURL()+"', age_limit="+event.getAge_limit()+", spotify_string='"+event.getSpotify_string()+"', update_date='"+timestamp+"', lowest_price="+event.getLowest_price()+", place_string='"+event.getPlace_string()+"'" ;
		if(!debugmode){
			System.out.println("\n-> Insering new items into slave database");
			Statement stmt = connection.createStatement();
			stmt.executeUpdate(SQL);
		}
		else{
			System.out.println("-->SQL:\n"+SQL);
		}
	}
	
	/**
	 * Remove old event data from DB.
	 * 
	 * @param connection
	 * @throws SQLException
	 */
	private void deleteOldEntries(Connection connection) throws SQLException {
		String SQL = "DELETE FROM findmydb.UKA_EVENTS WHERE update_date < '" + timestamp+"'";
		if(!debugmode){
			Statement stmt = connection.createStatement();
			System.out.println("\n-> Delete old items from slave database");
			int result = stmt.executeUpdate(SQL);
			System.out.println("-> Deleted "+result+" items");
		}
		else{
			System.out.println("-->SQL:\n"+SQL);
		}
	}
	
	/**
	 * Method to trigger database replication from master database to slave database
	 * @return Exit code
	 */
	public int replicate(String configLocation){
		
		try{
			this.loadProperties(configLocation);
		}
		catch(IOException e){
			System.out.println("Unable to read configuration file");
			e.printStackTrace();
			return 501;
		}

		// Populate Event bean to hold info
		if(useProxy){
			fetcher = new JSONFetcher(proxyHost, proxyPort);
		}
		else{
			fetcher = new JSONFetcher();
		}
			
		DBEvent[] events = null;
		if(useDB) {
			try {
				System.out.println("-> Connecting to master database");
				this.connectToMasterDataSource();
			} catch (InstantiationException e) {
				System.out.println("Unable to load PSQL driver for master database");
				e.printStackTrace();
				return 202;
			} catch (IllegalAccessException e) {
				System.out.println("Unable to load PSQL driver for master database");
				e.printStackTrace();
				return 203;
			} catch (ClassNotFoundException e) {
				System.out.println("Unable to load PSQL driver for master database");
				e.printStackTrace();
				return 204;
			} catch (SQLException e) {
				System.out.println("Unable to connect to master database");
				e.printStackTrace();
				return 201;
			}
			if (connectionMaster == null) {
					System.out.println("No connection to master database, exiting");
			    return 206;
			}
				try {
					events = populateEventsFromDb(connectionMaster);
				} catch (MalformedURLException e) {
					System.out.println("Error connecting to URL");
						e.printStackTrace();
					return 303;
				} catch (IllegalArgumentException e) {
					System.out.println("Error reading from JSON feed");
					e.printStackTrace();
					return 302;
				} catch (InstantiationException e) {
					System.out.println("Unable to load PSQL driver for master database");
					e.printStackTrace();
					return 202;
				} catch (IllegalAccessException e) {
					System.out.println("Unable to execute SQL query on master database");
					e.printStackTrace();
					return 301;
				} catch (ClassNotFoundException e) {
					System.out.println("Unable to execute SQL query on master database");
					e.printStackTrace();
					return 301;
				} catch (SQLException e) {
					System.out.println("Unable to execute SQL query on master database");
					e.printStackTrace();
					return 301;
				} catch (IOException e) {
					System.out.println("Error reading from JSON feed");
					e.printStackTrace();
					return 302;
				}
		} else {
			try {
				events = populateEventsFromJson(fetcher);
			} catch (MalformedURLException e) {
				System.out.println("Error connecting to URL");
				e.printStackTrace();
				return 303;
			} catch (IOException e) {
				System.out.println("Error reading from JSON feed");
				e.printStackTrace();
				return 302;
			}
		}
			
		try{
			System.out.println("-> Connecting to slave database");
			this.connectToSlaveDataSource();
		} catch(SQLException e){
			System.out.println("Unable to connect to slave database");
			e.printStackTrace();
			return 101;
		} catch (InstantiationException e) {
			System.out.println("Unable to load MySQL driver for slave database");
			e.printStackTrace();
			return 102;
		} catch (IllegalAccessException e) {
			System.out.println("Unable to load MySQL driver for slave database");
			e.printStackTrace();
			return 103;
		} catch (ClassNotFoundException e) {
			System.out.println("Unable to load MySQL driver for slave database");
			e.printStackTrace();
			return 104;
		}
		
		if(connectionSlave == null) {
			System.out.println("No connection to slave database, exiting");
			return 106;
		}
		if(events != null) {
			for(DBEvent event : events) {
				try {
					insertEventData(event, connectionSlave);
				} catch (SQLException e) {
					System.out.println("Unable to execute SQL update query on slave database");
					e.printStackTrace();
					return 105;
				}
			}
		} else {
			System.out.println("-> No new events found!");
		}
		
		
		// Remove all rows that are older than current timestamp in slave database
		if(doDelete) {
			if(connectionSlave == null) {
				System.out.println("No connection to slave database for delete operation, exiting");
				return 402;
			}
			try{
				deleteOldEntries(connectionSlave);
			}
			catch(SQLException e){
				System.out.println("Unable to execute DELETE FROM statement on slave database");
				e.printStackTrace();
				return 401;
			}
		}
		// Close result sets and DB connections
		try{
			this.cleanUp();
		}
		catch(SQLException e){
			System.out.println("Error closing result set or SQL database connection");
			return 601;
		}
			
		return 0;
	}
	
	/**
	 * The main method of the DBReplicator
	 * This method triggers a full database replication between master and slave
	 * @param args No command line arguments are required
	 */
	public static void main(String[] args){
		
		System.out.println("\n\n----->Batch run started at time "+ new java.util.Date(System.currentTimeMillis()  ) +" <-----\n");
		
		System.out.println(
		 " \n\n Exit status codes:\n"+
		 "-------------------------------------------\n"+
		 "*   0 - Replication successfully executed\n"+
		 "* 101 - Unable to establish connection to slave database\n"+
		 "* 102 - Unable to load MySQL driver for slave database connection (InstantiationException)\n"+
		 "* 103 - Unable to load MySQL driver for slave database connection (IllegalAccessException)\n"+
		 "* 104 - Unable to load MySQL driver for slave database connection (ClassNotFoundException)\n"+
		 "* 105 - Unable to execute UPDATE query into slave database\n"+
		 "* 106 - No open connection to slave database to execute UPDATE query\n"+
		 "* 201 - Unable to establish connection to master database\n"+
		 "* 202 - Unable to load PSQL driver for master database connection (InstantiationException)\n"+
		 "* 203 - Unable to load PSQL driver for master database connection (IllegalAccessException)\n"+
		 "* 204 - Unable to load PSQL driver for master database connection (ClassNotFoundException)\n"+
		 "* 205 - Error executing SELECT statement on master database (SQLException)\n"+
		 "* 206 - No open connection to master database to execute SELECT query\n"+
		 "* 301 - Error while reading from ResultSet from master database query\n"+
		 "* 302 - Error while reading from JSON-feed @uka.no\n"+
		 "* 401 - Error executing DELETE statement on slave database (SQLException)\n"+
		 "* 402 - No open connection to slave database to execute DELETE statement\n"+
		 "* 501 - Unable to read configuration file (FATAL)\n"+
		 "* 601 - Error while closing Result Sets or database connections - but replication should be OK\n\n"
		);
		
		String configLocation = "";
		
		if(args.length > 0 && args != null){
			if(args[0] instanceof String){
				configLocation = args[0];
			}
			else{
				System.out.println("Usage:\n >java -jar dbreplicator <configFileName>");
				System.out.println("NB! Config file must be in same path as jar file");
				System.exit(0);				
			}
		}
		else{
			System.out.println("Usage:\n >java -jar dbreplicator <configFileLocation>");
			System.out.println("NB! Config file must be in same path as jar file");
			System.exit(0);
		}
		
		DBReplicator dbr = new DBReplicator();
		int status = dbr.replicate(configLocation);
		System.out.println("\n\n----->Batch run completed<-----\n");
		System.out.println("Exit status:  "+status);
	}
}

/**
 * Private inner class to hold information from the Event JSON-feed at uka.no
 * @author thomas.langerud
 *
 */
class JSONEvent {
	    private String spotify_query;
	    private String event_type;
	    private String lead;
	    private String title;
	    private String detail_url;
	    private int age_limit;
	    private int id;
	    private String image_url;
	    private int[] recommended_events;
	    
	    public String getSpotify_query(){ return this.spotify_query; }
	    public String getEvent_type(){ return this.event_type; }
	    public String getLead(){ return this.lead; }
	    public String getTitle(){ return this.title; }
	    public String getDetail_url(){ return this.detail_url; }
	    public int getAge_limit(){ return this.age_limit; }
	    public int getId(){ return this.id; }
	    public String getImage_url() { return this.image_url; }
	    public int[] getRecommended_events() { return this.recommended_events; }

		public void setSpotify_query(String spotify_query) { this.spotify_query = spotify_query; }		
		public void setEvent_type(String event_type) { this.event_type = event_type; }
		public void setLead(String lead) { this.lead = lead; }
		public void setTitle(String title) { this.title = title; }
		public void setDetail_url(String detail_url) { this.detail_url = detail_url; }
		public void setAge_limit(int age_limit) { this.age_limit = age_limit; }
		public void setId(int id) { this.id = id; }
		public void setImage_url(String image_url) { this.image_url = image_url; }
		public void setRecommended_events(int[] recommended_events){ this.recommended_events = recommended_events; }
		
		@Override
		public String toString() {
	        return String.format("spotify_query:%s,event_type:%s,lead:%s,title:%s,detail_url:%s,age_limit:%d,id:%d, image_url:%s", this.spotify_query, this.event_type, this.lead, this.title,this.detail_url,this.age_limit,this.id, this.image_url);
	    }
}

/**
 * Private inner class to hold information from the Showing JSON-feed at uka.no
 * @author thomas.langerud
 *
 */
class JSONShowing {
		private boolean canceled;
		private String place;
		private String buy_url;
		private String entrance_ticket;
		private String showing_time;
		private int event_id;
		private int prices_from;
		private int id;
		private boolean free;
		private String sale_from;
		private String place_string;
		
		public boolean isCanceled() { return canceled; }
		public String getPlace() { return place; }
		public String getBuy_url() { return buy_url; }
		public String getEntrance_ticket() { return entrance_ticket; }
		public String getShowing_time() { return showing_time; }
		public int getEvent_id() { return event_id; }
		public int getPrices_from() { return prices_from; }
		public int getId() { return id; }
		public boolean isFree() { return free; }
		public String getSale_from() { return sale_from; }
		public String getPlace_string() { return place_string; }
		
		public void setCanceled(boolean canceled) { this.canceled = canceled; }
		public void setPlace(String place) { this.place = place; }
		public void setBuy_url(String buy_url) { this.buy_url = buy_url; }
		public void setEntrance_ticket(String entrance_ticket) { this.entrance_ticket = entrance_ticket; }
		public void setShowing_time(String showing_time) { this.showing_time = showing_time; }
		public void setEvent_id(int event_id) { this.event_id = event_id; }
		public void setPrices_from(int prices_from) { this.prices_from = prices_from; }
		public void setId(int id) { this.id = id; }
		public void setFree(boolean free) { this.free = free; }
		public void setSale_from(String sale_from) { this.sale_from = sale_from; }
		public void setPlace_string(String place_string) { this.place_string = place_string; }
		
		@Override
		public String toString(){
			return String.format("canceled:%s, place:%s, buy_url:%s,entrance_ticket:%s,showing_time:%s,event_id:%d,prices_from:%d,id:%d,free:%s,sales_from:%s", this.isCanceled(), this.place, this.buy_url, this.entrance_ticket, this.showing_time, this.event_id, this.prices_from, this.id, this.free, this.sale_from );
		}
		
}