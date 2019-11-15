package com.aravind.oss.cosmosdb;

import com.microsoft.azure.cosmosdb.rx.AsyncDocumentClient;
import com.microsoft.azure.documentdb.ConnectionPolicy;
import com.microsoft.azure.documentdb.ConsistencyLevel;
import com.microsoft.azure.documentdb.DocumentClient;

public class DocumentClientFactory {
  private static DocumentClient docClient;
    private static AsyncDocumentClient asyncDocClient;

    public static DocumentClient getDocumentClient(String serviceEndpointUri, String masterKey) {
        if (docClient == null) {
            docClient = new DocumentClient(serviceEndpointUri, masterKey, ConnectionPolicy.GetDefault(), ConsistencyLevel.Session);
        }

        return docClient;
    }

    /**
     * Sync and Async API's use different copies of <code>ConnectionPolicy</code>
     * and <code>ConsistencyLevel</code>
     */
    public static AsyncDocumentClient getAsyncDocumentClient(String serviceEndpointUri, String masterKey) {
        if (asyncDocClient == null) {
            asyncDocClient = new AsyncDocumentClient.Builder().withServiceEndpoint(serviceEndpointUri)
                    .withMasterKeyOrResourceToken(masterKey)
                    .withConnectionPolicy(com.microsoft.azure.cosmosdb.ConnectionPolicy.GetDefault())
                    .withConsistencyLevel(com.microsoft.azure.cosmosdb.ConsistencyLevel.Session).build();
        }

        return asyncDocClient;
    }
}