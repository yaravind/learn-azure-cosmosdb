package com.aravind.oss.cosmosdb.documentdb;

import com.aravind.oss.cosmosdb.DocumentClientFactory;
import com.aravind.oss.cosmosdb.Helpers;
import com.aravind.oss.cosmosdb.IdAndLink;
import com.aravind.oss.tvsshow.domain.*;
import com.google.common.base.Stopwatch;
import com.google.gson.Gson;
import com.microsoft.azure.documentdb.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Examples are inspired from https://docs.microsoft.com/en-us/azure/cosmos-db/sql-api-dotnet-samples
 */
public class MainApp {
    private static final Logger log = LoggerFactory.getLogger(MainApp.class);
    public static final Scanner in = new Scanner(System.in);
    public static final String DATABASE_ID = "dbs/cos-bia-tccc-perf-01";

    private static DocumentClient client;

    private static Gson gson = new Gson();

    public static void main(String[] args) {
        log.info("Program arguments: \n\t URI: {} \n\t Primary Key: {}", args[0], args[1]);
        String serviceEndpoint = args[0];
        String masterKey = args[1];

        try {
            client = DocumentClientFactory.getDocumentClient(serviceEndpoint, masterKey);

            String command = "init";
            while (!command.equals("q")) {
                System.out.println("1 - Show CosmosDB account information.");
                System.out.println("2 - Create and Populate TvShows collection.");
                System.out.println("3 - Delete TvShows collection. ***Do this before you quit.***");
                System.out.println("4 - Create JSON using Document API and log it to console.");
                System.out.println("5 - Get partition key ranges for a given container and DB.");
                System.out.println("6 - Read all documents of a given container and DB.");
                System.out.println("q - Quit.");
                System.out.println();
                System.out.printf("Enter your choice and hit enter ...\n");

                command = in.nextLine();

                switch (command) {
                    case "1":
                        Helpers.logCosmosDatabaseAccountInfo(client);
                        for (IdAndLink db : Helpers.getAllDatabases(client)) {
                            log.info("Database id: {}, Database link: {}", db.id, db.link);
                            for (IdAndLink coll : Helpers.getAllContainers(client, db.link)) {
                                log.info("\n\t Collection id: {}, Collection link: {}", coll.id, coll.link);
                            }
                        }
                        break;

                    case "2":
                        log.info("Create and populate 'TvShows' collection");
                        doTvShows(DATABASE_ID);
                        break;

                    case "3":
                        log.info("Delete 'TvShows' collection");
                        deleteTvShows(DATABASE_ID);
                        break;

                    case "4":
                        createJson();
                        break;

                    case "5":
                        Helpers.getPartitionKeyRanges(client, DATABASE_ID, "TvShows", "/name");
                        break;

                    case "6":
                        Helpers.readEntireContainer(client, DATABASE_ID, "dbs/OnUVAA==/colls/OnUVAIMxkao=/", 10);
//                        Helpers.readEntireContainer(client, DATABASE_ID, "dbs/OnUVAA==/colls/OnUVAIMxkao=/", 100);
//                        Helpers.readEntireContainer(client, DATABASE_ID, "dbs/OnUVAA==/colls/OnUVAIMxkao=/", 200);
//                        Helpers.readEntireContainer(client, DATABASE_ID, "dbs/OnUVAA==/colls/OnUVAIMxkao=/", 300);
//                        Helpers.readEntireContainer(client, DATABASE_ID, "dbs/OnUVAA==/colls/OnUVAIMxkao=/", 400);
//                        Helpers.readEntireContainer(client, DATABASE_ID, "dbs/OnUVAA==/colls/OnUVAIMxkao=/", 500);
//                        Helpers.readEntireContainer(client, DATABASE_ID, "dbs/OnUVAA==/colls/OnUVAIMxkao=/", 600);
//                        Helpers.readEntireContainer(client, DATABASE_ID, "dbs/OnUVAA==/colls/OnUVAIMxkao=/", 700);
//                        Helpers.readEntireContainer(client, DATABASE_ID, "dbs/OnUVAA==/colls/OnUVAIMxkao=/", 800);
//                        Helpers.readEntireContainer(client, DATABASE_ID, "dbs/OnUVAA==/colls/OnUVAIMxkao=/", 1000);
                }
            }
        } catch (DocumentClientException e) {
            log.error("Error while opening connection.", e);
        } finally {
            client.close();
        }
    }

    private static void createJson() {
        Document root = new Document();
        root.set("a", 10);
        root.set("b", "11");
        root.set("c", 12.34);
        //root.set("d", Calendar.getInstance().getTime());
        root.set("e", new int[]{10, 11, 12});

        JSONObject nested = new JSONObject();
        nested.put("g", "nested under key: f");
        root.set("f", nested);

        System.out.println(root.toJson());
    }

    private static void deleteTvShows(String dbLink) {
        String collectionId = "TvShows";
        try {
            DocumentCollection collection = (DocumentCollection) Helpers.getContainerById(client, dbLink, collectionId);
            if (collection == null) {
                log.warn("Collection {} doesn't exist.", collectionId);
            } else {
                Database database = Helpers.getDatabaseByLink(client, dbLink);
                client.deleteCollection(collection.getSelfLink(), null);
            }
        } catch (DocumentClientException e) {
            log.warn("Couldn't delete Database with link {}.", dbLink, e);
        }
    }

    private static void doTvShows(String dbLink) {
        String collectionId = "TvShows";

        List<Show> shows = createDomain();
        log.info(gson.toJson(shows.get(0)));

        try {
            DocumentCollection collection = (DocumentCollection) Helpers.createContainerIfNotExists(client, dbLink, collectionId, "/name");
            double totalRU = 0;
            Stopwatch stopwatch = Stopwatch.createStarted();
            for (int i = 0; i < 5000; i++) {
                totalRU = insertShows(collection, shows);
            }
            long elapsed = stopwatch.elapsed(TimeUnit.MINUTES);
            log.info("Took {} minutes and costed {} RU's", elapsed, totalRU);
        } catch (DocumentClientException e) {
            log.error("Couldn't create container: {} in database: {}", collectionId, dbLink, e);
        }
    }

    private static double insertShows(DocumentCollection collection, List<Show> shows) throws DocumentClientException {
        double totalRU = 0.0;
        for (Show show : shows) {
            ResourceResponse<Document> doc = client.createDocument(collection.getSelfLink(), new Document(gson.toJson(show)), null, false);
            totalRU += doc.getRequestCharge();
        }
        return totalRU;
    }

    private static List<Show> createDomain() {
        Show show = new Show();
        show.setGenre(Genre.Comedy);
        show.setName("X Factor");

        Network network = new Network();
        network.setName("NBC");
        show.setTvNetwork(network);

        Calendar premierDate = Calendar.getInstance();
        premierDate.set(1994, 9, 22);
        show.setPremiereDate(premierDate.getTime());

        List<Season> seasons = new ArrayList<>();
        show.setSeasons(seasons);
        String[] airedDatesRaw = {"September 22, 1994", "September 21, 1995", "September 19, 1996", "September 25, 1997", "September 24, 1998", "September 23, 1999", "October 12, 2000", "September 27, 2001", "September 26, 2002", "September 25, 2003"};
        DateFormat df = new SimpleDateFormat("MMMMM dd, yyyy");
        for (int i = 0; i < 10; i++) {
            Season season = new Season();
            season.setName("Season " + (i + 1));
            try {
                season.setPremiereDate(df.parse(airedDatesRaw[i]));
            } catch (ParseException e) {
                log.error("Can't parse airedDateRaw : {}", airedDatesRaw[i], e);
            }
            if (i <= 3) {
                Path path = Paths.get("/Users/o60774/Documents/WS/learn-azure-cosmosdb/document-db-sql-api/src/main/resources/friends/" + "Season" + (i + 1) + ".txt");
                try {
                    List<String> lines = Files.readAllLines(path);
                    List<Episode> episodes = new ArrayList<>(lines.size());
                    for (String line : lines) {
                        Episode episode = new Episode();
                        episode.setName(line);
                        episodes.add(episode);
                    }
                    season.setEpisodes(episodes);
                } catch (IOException e) {
                    log.error("Can't find file: {}", path);
                }
            }
            seasons.add(season);
        }
        List<Show> shows = new ArrayList<>();
        shows.add(show);
        return shows;
    }

}
