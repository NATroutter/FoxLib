package fi.natroutter.foxlib.mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoSecurityException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import fi.natroutter.foxlib.logger.FoxLogger;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.ArrayList;
import java.util.function.Consumer;

public class MongoConnector {

    protected static final FoxLogger logger = new FoxLogger.Builder()
            .setDebug(false)
            .setPruneOlderThanDays(35)
            .setSaveIntervalSeconds(300)
            .setLoggerName("MongoDB")
            .build();

    private final MongoConfig config;

    public MongoConnector(MongoConfig config) {
        this.config = config;
    }

    public void getDatabase(Consumer<MongoDatabase> action) {

        CodecProvider pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
        CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), CodecRegistries.fromProviders(pojoCodecProvider));

        try (MongoClient mongoClient = MongoClients.create(config.getUri())) {
            if (!mongoClient.listDatabaseNames().into(new ArrayList<>()).contains(config.getDatabase())) {
                logger.error("MongoDB Error : database " + config.getDatabase() + " doesn't exists!");
                return;
            }
            MongoDatabase mdb = mongoClient.getDatabase(config.getDatabase()).withCodecRegistry(pojoCodecRegistry);

            action.accept(mdb);
        } catch (MongoSecurityException e) {
            logger.error("MongoDB Error : Failed to authenticate, check your config!");

        } catch (Exception e) {
            logger.error("MongoDB Error : " + e.getMessage());
            e.printStackTrace();
        }
    }

}
