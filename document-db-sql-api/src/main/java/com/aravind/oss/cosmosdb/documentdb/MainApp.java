package com.aravind.oss.cosmosdb.documentdb;

import com.aravind.oss.cosmosdb.IdAndLink;
import com.microsoft.azure.documentdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Examples are inspired from https://docs.microsoft.com/en-us/azure/cosmos-db/sql-api-dotnet-samples
 */
public class MainApp {
    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    private static DocumentClient client;
    private static ConnectionPolicy conPolicy = ConnectionPolicy.GetDefault();

    public static void main(String[] args) {
        log.info("Program arguments: \n\t URI: {} \n\t Primary Key: {}", args[0], args[1]);
        String serviceEndpoint = args[0];
        String masterKey = args[1];

        client = new DocumentClient(serviceEndpoint, masterKey, conPolicy, ConsistencyLevel.Session);

        try {
            DatabaseAccount databaseAccount = client.getDatabaseAccount();

            log.info("Address Link: {}", databaseAccount.getAddressesLink());
            log.info("Database Link {}: ", databaseAccount.getDatabasesLink());
            log.info("Media Link {}: ", databaseAccount.getMediaLink());

            for (IdAndLink db : getAllDatabases()) {
                log.info("Database id: {}, Database link: {}", db.id, db.link);
                for (IdAndLink coll : getAllContainers(db.link)) {
                    log.info("\n\t Collection id: {}, Collection link: {}", coll.id, coll.link);
                }
            }
        } catch (DocumentClientException e) {
            log.error("Error while opening connection.", e);
        } finally {
            client.close();
        }
    }

    /**
     * The <code>readDatabases</code> method can be replaced by the following code
     * <p>
     * client.queryDatabases("SELECT * FROM root r", null);
     */
    private static List<IdAndLink> getAllDatabases() {
        FeedResponse<Database> resp = client.readDatabases(null);

        List<IdAndLink> dbs = new ArrayList<>();
        for (Database db : resp.getQueryIterable()) {
            dbs.add(new IdAndLink(db.getId(), db.getSelfLink()));
        }
        return dbs;
    }

    private static List<IdAndLink> getAllContainers(String dbSelfLink) {
        FeedResponse<DocumentCollection> resp = client.queryCollections(dbSelfLink, "SELECT * FROM root r", null);
        List<IdAndLink> colls = new ArrayList<>();
        for (DocumentCollection coll : resp.getQueryIterable()) {
            colls.add(new IdAndLink(coll.getId(), coll.getSelfLink()));
        }
        return colls;
    }
}
