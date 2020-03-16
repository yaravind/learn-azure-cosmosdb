package com.aravind.oss.cosmosdb;

import com.aravind.oss.cosmosdb.documentdb.MainApp;
import com.google.common.base.Stopwatch;
import com.microsoft.azure.documentdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/***
 * 1. We can use resource id's instead of self-links going forward. Reference: https://azure.microsoft.com/en-us/blog/azure-documentdb-bids-fond-farewell-to-self-links/
 *
 * Took 11 minutes and costed 53.33 RU's without bulk executor
 */
public class Helpers {
    private static final Logger log = LoggerFactory.getLogger(Helpers.class);

    public static String createDocumentCollectionUri(String databaseName, String collectionName) {
        return String.format("/dbs/%s/colls/%s", databaseName, collectionName);
    }

    public static String createDatabaseUri(String databaseName) {
        return String.format("/dbs/%s", databaseName);
    }

    /**
     * The <code>readDatabases</code> method can be replaced by the following code
     * <p>
     * client.queryDatabases("SELECT * FROM root r", null);
     */
    public static List<IdAndLink> getAllDatabases(DocumentClient client) {
        FeedResponse<Database> resp = client.readDatabases(null);

        List<IdAndLink> dbs = new ArrayList<>();
        for (Database db : resp.getQueryIterable()) {
            dbs.add(new IdAndLink(db.getId(), db.getSelfLink()));
        }
        return dbs;
    }

    public static Database getDatabaseByLink(DocumentClient client, String databaseId) throws DocumentClientException {
        ResourceResponse<Database> response = client.readDatabase(databaseId, null);
        return response.getResource();
    }

    public static List<IdAndLink> getAllContainers(DocumentClient client, String databaseId) {
        FeedResponse<DocumentCollection> resp = client.queryCollections(databaseId, "SELECT * FROM root r", null);
        List<IdAndLink> colls = new ArrayList<>();
        for (DocumentCollection coll : resp.getQueryIterable()) {
            colls.add(new IdAndLink(coll.getId(), coll.getSelfLink()));
        }
        return colls;
    }

    /***
     * We can also use client.readCollection(createDocumentCollectionUri(databaseName, collectionName), null)
     *
     * @param client
     * @param databaseId
     * @param containerId
     * @return
     */
    public static Resource getContainerById(DocumentClient client, String databaseId, String containerId) {
        log.info("Retrieving container: {} in database: {}", containerId, databaseId);
        try {
            FeedResponse<DocumentCollection> resp = client.queryCollections(databaseId, "SELECT * FROM root r WHERE r.id = '" + containerId + "'", null);
            if (resp.getQueryIterator().hasNext())
                return resp.getQueryIterator().next();
        } catch (IllegalStateException ex) {
            //hasNext() does a prefetch to check if there is any data and raises a runtime exception of Container doesnt exist
            if (((DocumentClientException) ex.getCause()).getStatusCode() == 404) {
                log.error("Given Database '{}' doesn't exist in Cosmos account.", databaseId, ex);
                return null;
            }
        }
        return null;
    }

    /**
     * Page size 10: Time: 30 secs, RU Charge: 3.58
     * Page size 100: Time: 11 secs, RU Charge: 35.85
     * Page size 200: Time: 9 secs, RU Charge: 71.7
     * Page size 300: Time: 8 secs, RU Charge: 71.7
     * Page size 500: Time: 8 secs, RU Charge: 179.25
     * Page size 600: Time: 7 secs, RU Charge: 71.7
     * Page size 700: Time: 7 secs, RU Charge: 35.85
     * Page size 800: Time: 7 secs, RU Charge: 71.7
     * Page size 1000: Time: 6 secs, RU Charge: 358.49
     *
     * @param client
     * @param databaseId
     * @param containerId
     */
    public static void readEntireContainer(DocumentClient client, String databaseId, String containerId, int pageSize) {
        try {
            FeedOptions ops = new FeedOptions();
            ops.setPageSize(pageSize);
            log.info("Reading the entire feed of container: {} in database: {} with page size: {}", containerId, databaseId, ops.getPageSize());

            //DocumentCollection collection = (DocumentCollection) Helpers.getContainerById(client, dbLink, collectionId);

            FeedResponse<Document> response = client.readDocuments(containerId, ops);
            Iterator<Document> iterator = response.getQueryIterator();
            int totalDocs = 0;
            Stopwatch stopwatch = Stopwatch.createStarted();
            while (iterator.hasNext()) {
                totalDocs++;
                Document next = iterator.next();
            }
            long elapsed = stopwatch.elapsed(TimeUnit.SECONDS);
            log.info("Took {} seconds to read {} docs", elapsed, totalDocs);
            log.info("Completed reading. Total documents read: {}. Total charge: {}", totalDocs, response.getRequestCharge());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Resource createContainerIfNotExists(DocumentClient client, String databaseId, String containerId) throws DocumentClientException {
        DocumentCollection container = (DocumentCollection) getContainerById(client, databaseId, containerId);
        if (container != null) {
            return container;
        } else {
            DocumentCollection collDefinition = new DocumentCollection();
            collDefinition.setId(containerId);
            RequestOptions requestOptions = null;
            ResourceResponse<DocumentCollection> resp = client.createCollection(databaseId, collDefinition, requestOptions);
            return resp.getResource();
        }
    }

    /***
     * Use this to create a container with a parition key. Otherwise you will get the error:
     * Shared throughput collection should have a partition key
     *
     * @param client
     * @param databaseId
     * @param containerId
     * @param paritionKeyPath
     * @return
     * @throws DocumentClientException
     */
    public static Resource createContainerIfNotExists(DocumentClient client, String databaseId, String containerId, String paritionKeyPath) throws DocumentClientException {
        DocumentCollection container = (DocumentCollection) getContainerById(client, databaseId, containerId);
        if (container != null) {
            return container;
        } else {
            DocumentCollection collDefinition = new DocumentCollection();
            collDefinition.setId(containerId);

            List<String> pk = new ArrayList<>();
            pk.add(paritionKeyPath);
            PartitionKeyDefinition partitionKey = new PartitionKeyDefinition();
            partitionKey.setPaths(pk);
            partitionKey.setKind(PartitionKind.Hash);
            collDefinition.setPartitionKey(partitionKey);

            ResourceResponse<DocumentCollection> resp = client.createCollection(databaseId, collDefinition, null);

            return resp.getResource();
        }
    }

    public static void getPartitionKeyRanges(DocumentClient client, String databaseId, String containerId, String paritionKeyPath) {
        log.info("Retrieving partition key ranges of container: {} in database: {}", containerId, databaseId);
        try {
            FeedOptions feedOptions = new FeedOptions();
            //PartitionKey partitionKey = new PartitionKey(paritionKeyPath);

            //feedOptions.setPartitionKey(partitionKey);
            FeedResponse<DocumentCollection> resp = client.queryCollections(databaseId, "SELECT DISTINCT c.partitionKey FROM c", null);
            while (resp.getQueryIterator().hasNext()) {
                log.debug("Key: {}", resp.getQueryIterator().next());
            }
//        if (resp.getQueryIterator().hasNext())
//            return resp.getQueryIterator().next();
        } catch (IllegalStateException ex) {
            //hasNext() does a prefetch to check if there is any data and raises a runtime exception of Container doesnt exist
            if (((DocumentClientException) ex.getCause()).getStatusCode() == 404) {
                log.error("Given Database '{}' doesn't exist in Cosmos account.", databaseId, ex);
                //return null;
            }
        }
        // return null;

    }

    public static void logCosmosDatabaseAccountInfo(DocumentClient client) throws DocumentClientException {
        DatabaseAccount databaseAccount = client.getDatabaseAccount();

        log.info("Address Link: {}", databaseAccount.getAddressesLink());
        log.info("Database Link {}: ", databaseAccount.getDatabasesLink());
        log.info("Media Link {}: ", databaseAccount.getMediaLink());
    }
}
