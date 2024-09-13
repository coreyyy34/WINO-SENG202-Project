package seng202.team0.database;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Wine represents the wine record in the database
 */
public class Wine {

  /**
   * Title
   */
  private final StringProperty title;
  /**
   * Variety
   */
  private final StringProperty variety;
  /**
   * Country
   */
  private final StringProperty country;
  /**
   * Region
   */
  private final StringProperty region;
  /**
   * Winery
   * <p>
   * Represented as a name to ease binding
   * </p>
   */
  private final StringProperty winery;
  /**
   * Description of the wine
   */
  private final StringProperty description;
  /**
   * Normalized score from 0-100
   */
  private final IntegerProperty scorePercent;
  /**
   * Alcohol by volume as a percentage if known, else 0
   */
  private final FloatProperty abv;
  /**
   * Price of the wine in NZD if known, else 0
   */
  private final FloatProperty price;


  /**
   * Constructor
   *
   * @param title        title
   * @param variety      variety
   * @param country      country
   * @param winery       winery
   * @param description  description of wine
   * @param scorePercent score from 0-100
   * @param abv          alcohol by volume
   * @param price        NZD price
   */
  public Wine(
      String title,
      String variety,
      String country,
      String region,
      String winery,
      String description,
      Integer scorePercent,
      Float abv,
      Float price
  ) {
    this.title = new SimpleStringProperty(this, "title", title);
    this.variety = new SimpleStringProperty(this, "variety", variety);
    this.country = new SimpleStringProperty(this, "country", country);
    this.region = new SimpleStringProperty(this, "region", region);
    this.winery = new SimpleStringProperty(this, "winery", winery);
    this.description = new SimpleStringProperty(this, "description", description);
    this.scorePercent = new SimpleIntegerProperty(this, "scorePercent", scorePercent);
    this.abv = new SimpleFloatProperty(this, "abv", abv);
    this.price = new SimpleFloatProperty(this, "price", price);
  }

  /**
   * Default constructor
   */
  public Wine() {
    this.title = new SimpleStringProperty(this, "title");
    this.variety = new SimpleStringProperty(this, "variety");
    this.country = new SimpleStringProperty(this, "country");
    this.region = new SimpleStringProperty(this, "region");
    this.winery = new SimpleStringProperty(this, "winery");
    this.description = new SimpleStringProperty(this, "description");
    this.scorePercent = new SimpleIntegerProperty(this, "scorePercent");
    this.abv = new SimpleFloatProperty(this, "abv");
    this.price = new SimpleFloatProperty(this, "price");
  }


  /**
   * Gets the title
   *
   * @return title
   */
  public String getTitle() {
    return title.get();
  }

  /**
   * Sets the title
   *
   * @param title title
   */
  public void setTitle(String title) {
    this.title.set(title);
  }

  /**
   * Gets the title property
   *
   * @return title property
   */
  public StringProperty titleProperty() {
    return title;
  }

  /**
   * Gets the variety
   *
   * @return variety
   */
  public String getVariety() {
    return variety.get();
  }

  /**
   * Sets the variety
   *
   * @param variety variety
   */
  public void setVariety(String variety) {
    this.variety.set(variety);
  }

  /**
   * Gets the variety property
   *
   * @return variety property
   */
  public StringProperty varietyProperty() {
    return variety;
  }

  /**
   * Gets the country
   *
   * @return country
   */
  public String getCountry() {
    return country.get();
  }

  /**
   * Sets the country
   *
   * @param country country
   */
  public void setCountry(String country) {
    this.country.set(country);
  }

  /**
   * Gets the country property
   *
   * @return country property
   */
  public StringProperty countryProperty() {
    return country;
  }

  /**
   * Gets the Region
   *
   * @return region
   */
  public String getRegion() {
    return region.get();
  }

  /**
   * Sets the region
   * @param region region
   */
  public void setRegion(String region) {
    this.region.set(region);
  }

  /**
   * Gets the region property
   * @return region property
   */
  public StringProperty regionProperty() {
    return region;
  }

  /**
   * Gets the winery
   *
   * @return winery
   */
  public String getWinery() {
    return winery.get();
  }

  /**
   * Sets the winery
   *
   * @param winery winery
   */
  public void setWinery(String winery) {
    this.winery.set(winery);
  }

  /**
   * Gets the winery property
   *
   * @return winery property
   */
  public StringProperty wineryProperty() {
    return winery;
  }

  /**
   * Gets the description
   *
   * @return description
   */
  public String getDescription() {
    return description.get();
  }

  /**
   * Sets the description
   *
   * @param description description
   */
  public void setDescription(String description) {
    this.description.set(description);
  }

  /**
   * Gets the description property
   *
   * @return description property
   */
  public StringProperty descriptionProperty() {
    return description;
  }

  /**
   * Gets the score percentage
   *
   * @return score percentage
   */
  public int getScorePercent() {
    return scorePercent.get();
  }

  /**
   * Sets the score percentage
   *
   * @param scorePercent score percentage
   */
  public void setScorePercent(int scorePercent) {
    this.scorePercent.set(scorePercent);
  }

  /**
   * Gets the score percentage
   *
   * @return score percentage
   */
  public IntegerProperty scorePercentProperty() {
    return scorePercent;
  }

  /**
   * Gets the alcohol by volume
   *
   * @return abv
   */
  public float getAbv() {
    return abv.get();
  }

  /**
   * Gets the alcohol by volume
   *
   * @param abv alcohol by volume
   */
  public void setAbv(float abv) {
    this.abv.set(abv);
  }

  /**
   * Gets the alcohol by volume property
   *
   * @return alcohol by volume
   */
  public FloatProperty abvProperty() {
    return abv;
  }

  /**
   * Gets the price
   *
   * @return price
   */
  public float getPrice() {
    return price.get();
  }

  /**
   * Sets the price
   *
   * @param price price
   */
  public void setPrice(float price) {
    this.price.set(price);
  }

  /**
   * Gets the price property
   *
   * @return price property
   */
  public FloatProperty priceProperty() {
    return price;
  }
}
