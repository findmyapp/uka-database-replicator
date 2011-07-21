package no.uka.findmyapp;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import com.google.gson.Gson;

public class DBReplicator {
	
    private Properties configFile;	
	private String slaveUsername;
	private String slavePassword;
	private String slaveDatabaseHost;
	private String slaveDatabaseDBName;
	private String masterUsername; 
	private String masterPassword;
	private String masterDatabaseHost; 
	private String masterDatabaseDBName;
	private boolean debugmode; 
	private boolean useACNProxy; 
	
	private Connection connection1 = null;
	private Connection connection2 = null;
	private ResultSet rs2;
	private java.sql.Timestamp timestamp;
	
	private DBEvent event;
	private boolean doDelete;
	
	private JSONFetcher fetcher;
	
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
	private void loadProperties() throws IOException{
		
		System.out.println("-> Loading properties");
		configFile.load(this.getClass().getClassLoader().getResourceAsStream("dbreplicator.properties"));
		
		slaveUsername = configFile.getProperty("slaveUsername");
		slavePassword = configFile.getProperty("slavePassword");
		slaveDatabaseHost = configFile.getProperty("slaveDatabaseHost");
		slaveDatabaseDBName = configFile.getProperty("slaveDatabaseDBName");
		masterUsername = configFile.getProperty("masterUsername"); 
		masterPassword = configFile.getProperty("masterPassword");
		masterDatabaseHost = configFile.getProperty("masterDatabaseHost"); 
		masterDatabaseDBName = configFile.getProperty("masterDatabaseDBName");
		
		if(configFile.getProperty("debugmode").equals("true") ){
			debugmode = true;
		}
		else debugmode = false;
		
		if(configFile.getProperty("useACNProxy").equals("true")){
			useACNProxy = true;
		}
		else useACNProxy = false;
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
			connection1 = DriverManager.getConnection("jdbc:mysql://"+slaveDatabaseHost+"/"+slaveDatabaseDBName, slaveUsername, slavePassword);

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
		connection2 = DriverManager.getConnection("jdbc:postgresql://"+masterDatabaseHost+"/"+masterDatabaseDBName,masterUsername, masterPassword );

	}
	
	/**
	 * Mandatory method to close result sets and database connection
	 * @throws SQLException 
	 */
	private void cleanUp() throws SQLException{
		
		System.out.println("-> Cleaning up connections and result sets");
		rs2.close();
			
		connection1.close();
		connection2.close();
	}
	
	/**
	 * Method to trigger database replication from master database to slave database
	 * @return
	 */
	public int replicate(){
		
		try{
			this.loadProperties();
		}
		catch(IOException e){
			System.out.println("Unable to read configuration file");
			e.printStackTrace();
			return 501;
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
		
		
		// Set up connection to master database
		try{
			System.out.println("-> Connecting to master database");
			this.connectToMasterDataSource();
		}
		catch(SQLException e){
			System.out.println("Unable to connect to master database");
			e.printStackTrace();
			return 201;
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
		}
		
		// Get all events from UKA master database view
		// View is uka.events_showing_event_published
		if (connection2 != null){
		    try{
		    	System.out.println("-> Retrieving data from master database");
		    	Statement stmt = connection2.createStatement();
		    	rs2 = stmt.executeQuery("SELECT * FROM uka.events_showing_event_published");

		    }
		    catch(java.sql.SQLException e){
		    	System.out.println("Unable to execute SQL query on master database");
		    	e.printStackTrace();
		    	return 205;
		    }
		    finally{
		    }
		}
		else{
			System.out.println("No connection to master database, exiting");
		    return 206;
		}		

		// Populate Event bean to hold info
		try{
			if(useACNProxy){
				fetcher = new JSONFetcher("oscpx1118.accenture.com","3128");
			}
			else{
				fetcher = new JSONFetcher();
			}
			
			// Workaround - need to fetch a webpage before fetching JSON feed from uka.no
			// TODO: sort this out if possible
			String temp = fetcher.getWebFile("http://www.uka.no");
			temp = null;
			
			int counter = 0;
			
			System.out.println("Inserting row:\n");
			while(rs2.next()){				
				event = new DBEvent();
				
				System.out.print(++counter + ",");
				
				// This must match database view
				event.setShowingId(rs2.getInt(1));
				event.setShowing_time(rs2.getTimestamp(2));
				event.setSale_from(rs2.getTimestamp(3)); 
				event.setSale_to(rs2.getTimestamp(4));
				event.setFree(rs2.getBoolean(5));
				event.setEntrance_id(rs2.getInt(6));
				event.setAvailable_for_purchase(rs2.getBoolean(7));
				event.setNetsale_from(rs2.getTimestamp(8));
				event.setNetsale_to(rs2.getTimestamp(9));
				event.setPublish_time(rs2.getTimestamp(10));
				event.setPlace(rs2.getString(11));
				event.setCanceled(rs2.getBoolean(12));
				event.setEvent_id(rs2.getInt(13));
				event.setBillig_id(rs2.getInt(14));
				event.setBillig_name(rs2.getString(15));
				event.setTitle(rs2.getString(16));
				event.setLead(rs2.getString(17));
				event.setText(rs2.getString(18));
				event.setEvent_type(rs2.getString(19));
				event.setAge_limit(rs2.getInt(20));
				event.setSpotify_string(rs2.getString(21));
			
				
				/* JSON to Java using Google GSON
				 * ref: http://stackoverflow.com/questions/1688099/converting-json-to-java/1688182#1688182
				 */
				String eventJSONcontent = fetcher.getWebFile("http://uka.no/program/json/event/"+event.getEvent_id());
				String croppedEventJSONcontent = eventJSONcontent.substring(eventJSONcontent.indexOf("[")+1, eventJSONcontent.indexOf("]"));
				
				JSONEvent eventDataArray = new Gson().fromJson(croppedEventJSONcontent, JSONEvent.class);
				
				if(debugmode){
					System.out.println("eventDataArray:\n"+eventDataArray);
				}
				
				String showingJSONcontent = fetcher.getWebFile("http://uka.no/program/json/showing/"+event.getShowingId());
				String croppedShowingJSONcontent = showingJSONcontent.substring(showingJSONcontent.indexOf("[")+1, showingJSONcontent.indexOf("]"));
				
				JSONShowing showingDataArray = new Gson().fromJson(croppedShowingJSONcontent, JSONShowing.class);
				
				if(debugmode){
					System.out.println("showingDataArray:\n"+showingDataArray);
				}
				
				// TODO: Add additional fields from JSON feed
				event.setThumbnailURL("");
				event.setImageURL("");
				
				if(showingDataArray != null){
					event.setLowest_price(showingDataArray.getPrices_from());
				}
				
				// Insert data from Event bean into slave database using UPDATE INTO				
				if(connection1 != null){
					try{
						String SQL = "INSERT INTO findmydb.UKA_EVENTS VALUES ("+event.getShowingId()+", '"+event.getShowing_time()+"', '"+event.getPublish_time()+"', '"+event.getPlace()+"' , "+event.getBillig_id()+" ,' "+event.getBillig_name()+"', '"+event.getNetsale_from()+"','"+event.getNetsale_to()+"','"+event.getSale_from()+"','"+event.getSale_to()+"',"+event.isFree()+","+event.isAvailable_for_purchase()+","+event.isCanceled()+","+event.getEntrance_id()+","+event.getEvent_id()+",'"+event.getTitle()+"','"+event.getLead()+"','"+event.getText()+"','"+event.getEvent_type()+"','"+event.getImageURL()+"','"+event.getThumbnailURL()+"',"+event.getAge_limit()+",'"+event.getSpotify_string()+"','"+timestamp+"', "+event.getLowest_price()+") ON DUPLICATE KEY UPDATE showing_time='"+event.getShowing_time()+"',publish_time='"+event.getPublish_time()+"',place='"+event.getPlace()+"',billig_id="+event.getBillig_id()+", billig_name='"+event.getBillig_name()+"', netsale_from='"+event.getNetsale_from()+"', netsale_to='"+event.getNetsale_to()+"',sale_from='"+event.getSale_from()+"',sale_to='"+event.getSale_to()+"', free="+event.isFree()+", available_for_purchase="+event.isAvailable_for_purchase()+", canceled="+event.isCanceled()+", entrance_id="+event.getEntrance_id()+", event_id="+event.getEvent_id()+",title='"+event.getTitle()+"', lead='"+event.getLead()+"', text='"+event.getText()+"', event_type='"+event.getEvent_type()+"', image='"+event.getImageURL()+"',thumbnail='"+event.getThumbnailURL()+"', age_limit="+event.getAge_limit()+", spotify_string='"+event.getSpotify_string()+"', update_date='"+timestamp+"', lowest_price="+event.getLowest_price()+"" ;
						if(!debugmode){
							Statement stmt = connection1.createStatement();
							int result = stmt.executeUpdate(SQL);
						}
						else{
							System.out.println("-->SQL:\n"+SQL);
						}
					}
					catch(Exception e){
						System.out.println("Unable to execute SQL update query on slave database");
						e.printStackTrace();
						return 105;
					}
				}
				else{
					System.out.println("No connection to slave database, exiting");
					return 106;
				}					
			}
			
		}
		catch(SQLException e){
			System.out.println("Error reading from ResultSet");
			e.printStackTrace();
			
			//Make sure no delete is executed if data has not been fetched correctly 
			doDelete = false;
			
			return 301;
		}
		catch(IOException e){
			System.out.println("Error reading from JSON feed");
			e.printStackTrace();
			return 302; 
		}
		
		// Remove all rows that are older than current timestamp in slave database
		if(doDelete){
			if(connection1 != null){
				try{
					String SQL = "DELETE FROM findmydb.UKA_EVENTS WHERE update_date < '" + timestamp+"'";
					if(!debugmode){
						Statement stmt = connection1.createStatement();
						System.out.println("\n-> Delete old items from slave database");
						int result = stmt.executeUpdate(SQL);
						System.out.println("-> Deleted "+result+" items");
					}
					else{
						System.out.println("-->SQL:\n"+SQL);
					}
				}
				catch(SQLException e){
					System.out.println("Unable to execute DELETE FROM statement on slave database");
					e.printStackTrace();
					return 401;
				}
			}
			else{
				System.out.println("No connection to slave database for delete operation, exiting");
				return 402;
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
		
		System.out.println(
		 " \n\n Exit status codes:\n"+
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
		
		DBReplicator dbr = new DBReplicator();
		int status = dbr.replicate();
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
	    
	    public String getSpotify_query(){ return this.spotify_query; }
	    public String getEvent_type(){ return this.event_type; }
	    public String getLead(){ return this.lead; }
	    public String getTitle(){ return this.title; }
	    public String getDetail_url(){ return this.detail_url; }
	    public int getAge_limit(){ return this.age_limit; }
	    public int getId(){ return this.id; }

		public void setSpotify_query(String spotify_query) { this.spotify_query = spotify_query; }		
		public void setEvent_type(String event_type) { this.event_type = event_type; }
		public void setLead(String lead) { this.lead = lead; }
		public void setTitle(String title) { this.title = title; }
		public void setDetail_url(String detail_url) { this.detail_url = detail_url; }
		public void setAge_limit(int age_limit) { this.age_limit = age_limit; }
		public void setId(int id) { this.id = id; }
		
		public String toString() {
	        return String.format("spotify_query:%s,event_type:%s,lead:%s,title:%s,detail_url:%s,age_limit:%d,id:%d", this.spotify_query, this.event_type, this.lead, this.title,this.detail_url,this.age_limit,this.id);
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
		
		public String toString(){
			return String.format("canceled:%s, place:%s, buy_url:%s,entrance_ticket:%s,showing_time:%s,event_id:%d,prices_from:%d,id:%d,free:%s,sales_from:%s", this.isCanceled(), this.place, this.buy_url, this.entrance_ticket, this.showing_time, this.event_id, this.prices_from, this.id, this.free, this.sale_from );
		}
		
}