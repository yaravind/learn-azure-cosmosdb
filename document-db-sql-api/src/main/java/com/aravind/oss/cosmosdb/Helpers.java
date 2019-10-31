package com.aravind.oss.cosmosdb;

import com.aravind.oss.cosmosdb.documentdb.MainApp;
import com.microsoft.azure.documentdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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

    public static Database getDatabaseByLink(DocumentClient client, String dbLink) throws DocumentClientException {
        ResourceResponse<Database> response = client.readDatabase(dbLink, null);
        return response.getResource();
    }

    public static List<IdAndLink> getAllContainers(DocumentClient client, String dbSelfLink) {
        FeedResponse<DocumentCollection> resp = client.queryCollections(dbSelfLink, "SELECT * FROM root r", null);
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
     * @param dbSelfLink
     * @param containerId
     * @return
     */
    public static Resource getContainerById(DocumentClient client, String dbSelfLink, String containerId) {
        log.info("Retrieving container: {} in database: {}", containerId, dbSelfLink);
        FeedResponse<DocumentCollection> resp = client.queryCollections(dbSelfLink, "SELECT * FROM root r WHERE r.id = '" + containerId + "'", null);
        if (resp.getQueryIterator().hasNext())
            return resp.getQueryIterator().next();
        else
            return null;
    }

    public static Resource createContainerIfNotExists(DocumentClient client, String dbSelfLink, String containerId) throws DocumentClientException {
        DocumentCollection container = (DocumentCollection) getContainerById(client, dbSelfLink, containerId);
        if (container != null) {
            return container;
        } else {
            DocumentCollection collDefinition = new DocumentCollection();
            collDefinition.setId(containerId);
            RequestOptions requestOptions = null;
            ResourceResponse<DocumentCollection> resp = client.createCollection(dbSelfLink, collDefinition, requestOptions);
            return resp.getResource();
        }
    }

    public static void logCosmosDatabaseAccountInfo(DocumentClient client) throws DocumentClientException {
        DatabaseAccount databaseAccount = client.getDatabaseAccount();

        log.info("Address Link: {}", databaseAccount.getAddressesLink());
        log.info("Database Link {}: ", databaseAccount.getDatabasesLink());
        log.info("Media Link {}: ", databaseAccount.getMediaLink());
    }
}
