package seng202.team6.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import seng202.team6.dao.GeoLocationDAO;
import seng202.team6.dao.VineyardDAO;
import seng202.team6.model.GeoLocation;
import seng202.team6.model.Vineyard;
import seng202.team6.util.GeolocationResolver;
import seng202.team6.util.ProcessCSV;
import seng202.team6.util.Timer;

public class VineyardDefaultsService {
  private final Logger log = LogManager.getLogger(getClass());
  private final GeolocationResolver geolocationResolver;
  private final GeoLocationDAO geoLocationDAO;
  private final VineyardDAO vineyardDAO;
  private final boolean resolveMissingAddresses;

  public VineyardDefaultsService(GeoLocationDAO geoLocationDAO, VineyardDAO vineyardDAO,
      boolean resolveMissingAddresses) {
    this.geoLocationDAO = geoLocationDAO;
    this.vineyardDAO = vineyardDAO;
    this.resolveMissingAddresses = resolveMissingAddresses;
    // only initialise the resolver if we are using it
    // otherwise dotenv will try and look for the ORS API KEY
    // todo - find a better fix for this
    this.geolocationResolver = resolveMissingAddresses ? new GeolocationResolver() : null;
  }

  public void init() {
    Timer timer = new Timer();
    if (vineyardDAO.vineyardsTableHasData()) {
      log.info("Skip loading default vineyards as the VINEYARD table is not empty in {}ms",
          timer.stop());
      return;
    }

    List<Vineyard> vineyards = loadDefaultVineyards();
    if (resolveMissingAddresses) {
      Set<String> addresses = vineyards.stream().map(Vineyard::getAddress)
          .collect(Collectors.toSet());
      Set<String> addressesInDatabase = geoLocationDAO.getExistingLocationNames(addresses);
      List<String> missingAddresses = findMissingAddresses(addresses, addressesInDatabase);
      if (!missingAddresses.isEmpty()) {
        Map<String, GeoLocation> missingAddressesGeolocations = geolocationResolver.resolveAll(
            missingAddresses);
        geoLocationDAO.addAll(missingAddressesGeolocations);
      }
    }
    vineyardDAO.addAll(vineyards);
  }

  private List<Vineyard> loadDefaultVineyards() {
    List<Vineyard> vineyards = new ArrayList<>();
    List<String[]> rows = ProcessCSV.getCSVRows(
        getClass().getResourceAsStream("/data/nz_vineyards.csv"));
    for (int i = 1; i < rows.size(); i++) {
      String[] row = rows.get(i);
      String name = row[0];
      String address = row[1];
      String region = row[2];
      String website = row[3];
      String description = row[4];
      String logoUrl = row[5];
      Vineyard vineyard = new Vineyard(-1, name, address, region, website, description, logoUrl,
          null);
      vineyards.add(vineyard);
    }
    return vineyards;
  }

  private List<String> findMissingAddresses(Set<String> addresses, Set<String> addressesInDatabase) {
    List<String> missingAddresses = new ArrayList<>();
    for (String address : addresses) {
      if (!addressesInDatabase.contains(address)) {
        missingAddresses.add(address);
      }
    }
    return missingAddresses;
  }
}
