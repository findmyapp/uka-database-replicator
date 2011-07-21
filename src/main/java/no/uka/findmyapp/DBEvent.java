package no.uka.findmyapp;

import java.sql.Timestamp;

public class DBEvent
{
  private int showingId;
  private Timestamp showing_time;
  private Timestamp sale_from;
  private Timestamp sale_to;
  private boolean free;
  private int entrance_id;
  private boolean available_for_purchase;
  private Timestamp netsale_from;
  private Timestamp netsale_to;
  private Timestamp publish_time;
  private String place;
  private boolean canceled;
  private int event_id;
  private int billig_id;
  private String billig_name;
  private String title;
  private String lead;
  private String text;
  private String event_type;
  private int age_limit;
  private String spotify_string;
  private String thumbnailURL;
  private String imageURL;
  private int lowest_price;
  private String place_string;

  public int getShowingId()
  {
    return this.showingId;
  }

  public void setShowingId(int showingId)
  {
    this.showingId = showingId;
  }

  public Timestamp getShowing_time()
  {
    return this.showing_time;
  }

  public void setShowing_time(Timestamp showing_time)
  {
    this.showing_time = showing_time;
  }

  public Timestamp getSale_from()
  {
    if (this.sale_from != null) {
      return this.sale_from;
    }
    return new Timestamp(0L);
  }

  public void setSale_from(Timestamp sale_from)
  {
    this.sale_from = sale_from;
  }

  public Timestamp getSale_to()
  {
    if (this.sale_to != null) {
      return this.sale_to;
    }
    return new Timestamp(0L);
  }

  public void setSale_to(Timestamp sale_to)
  {
    this.sale_to = sale_to;
  }

  public boolean isFree()
  {
    return this.free;
  }

  public void setFree(boolean free)
  {
    this.free = free;
  }

  public int getEntrance_id()
  {
    return this.entrance_id;
  }

  public void setEntrance_id(int entrance_id)
  {
    this.entrance_id = entrance_id;
  }

  public boolean isAvailable_for_purchase()
  {
    return this.available_for_purchase;
  }

  public void setAvailable_for_purchase(boolean available_for_purchase)
  {
    this.available_for_purchase = available_for_purchase;
  }

  public Timestamp getNetsale_from()
  {
    if (this.netsale_from != null) {
      return this.netsale_from;
    }
    return new Timestamp(0L);
  }

  public void setNetsale_from(Timestamp netsale_from)
  {
    this.netsale_from = netsale_from;
  }

  public Timestamp getNetsale_to()
  {
    if (this.netsale_to != null) {
      return this.netsale_to;
    }
    return new Timestamp(0L);
  }

  public void setNetsale_to(Timestamp netsale_to)
  {
    this.netsale_to = netsale_to;
  }

  public Timestamp getPublish_time()
  {
    if (this.publish_time != null) {
      return this.publish_time;
    }
    return new Timestamp(0L);
  }

  public void setPublish_time(Timestamp publish_time)
  {
    this.publish_time = publish_time;
  }

  public String getPlace()
  {
    if (this.place != null) {
      return this.place;
    }
    return "";
  }

  public void setPlace(String place)
  {
    this.place = place;
  }

  public boolean isCanceled()
  {
    return this.canceled;
  }

  public void setCanceled(boolean canceled)
  {
    this.canceled = canceled;
  }

  public int getEvent_id()
  {
    return this.event_id;
  }

  public void setEvent_id(int event_id)
  {
    this.event_id = event_id;
  }

  public int getBillig_id()
  {
    return this.billig_id;
  }

  public void setBillig_id(int billig_id)
  {
    this.billig_id = billig_id;
  }

  public String getBillig_name()
  {
    if (this.billig_name != null) {
      return this.billig_name;
    }
    return "";
  }

  public void setBillig_name(String billig_name)
  {
    this.billig_name = billig_name;
  }

  public String getTitle()
  {
    if (this.title != null) {
      return this.title;
    }
    return "";
  }

  public void setTitle(String title)
  {
    this.title = title;
  }

  public String getLead()
  {
    if (this.lead != null) {
      return this.lead;
    }
    return "";
  }

  public void setLead(String lead)
  {
    this.lead = lead;
  }

  public String getText()
  {
    if (this.text != null) {
      return this.text;
    }
    return "";
  }

  public void setText(String text)
  {
    this.text = text;
  }

  public String getEvent_type()
  {
    if (this.event_type != null) {
      return this.event_type;
    }
    return "";
  }

  public void setEvent_type(String event_type)
  {
    this.event_type = event_type;
  }

  public int getAge_limit()
  {
    return this.age_limit;
  }

  public void setAge_limit(int age_limit)
  {
    this.age_limit = age_limit;
  }

  public String getSpotify_string()
  {
    if (this.spotify_string != null) {
      return this.spotify_string;
    }
    return "";
  }

  public void setSpotify_string(String spotify_string)
  {
    this.spotify_string = spotify_string;
  }

  public void setThumbnailURL(String thumbnailURL)
  {
    this.thumbnailURL = thumbnailURL;
  }

  public String getThumbnailURL()
  {
    if (this.thumbnailURL != null) {
      return this.thumbnailURL;
    }
    return "";
  }

  public void setImageURL(String imageURL)
  {
    this.imageURL = imageURL;
  }

  public String getImageURL()
  {
    if (this.imageURL != null) {
      return this.imageURL;
    }
    return "";
  }
  public int getLowest_price() {
    return this.lowest_price;
  }
  public void setLowest_price(int lowest_price) {
    this.lowest_price = lowest_price;
  }

public String getPlace_string() {
	if(place_string != null){
		return place_string;
	}
	else return "";
}

public void setPlaceString(String placeString) {
	this.place_string = placeString;
}
}