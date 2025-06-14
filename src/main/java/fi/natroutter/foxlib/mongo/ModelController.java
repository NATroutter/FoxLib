package fi.natroutter.foxlib.mongo;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Getter
public abstract class ModelController<T extends MongoData> {

    private final MongoConnector connector;
    private final String collectionName;
    private final String idField;
    private final Class<T> clazz;

    public ModelController(MongoConnector connector, String collectionName,String idField, Class<T> clazz) {
        this.connector = connector;
        this.collectionName = collectionName;
        this.idField = idField;
        this.clazz = clazz;

        //Check if collection exists, if not create it!
        connector.getDatabase(db ->{
            List<String> databaseNames = db.listCollectionNames().into(new ArrayList<>());
            if (!databaseNames.contains(collectionName)) {
                db.createCollection(collectionName);
            }
        });
    }

    public void getCollection(Consumer<MongoCollection<T>> data) {
        getConnector().getDatabase(db-> {
            data.accept(db.getCollection(collectionName, clazz));
        });
    }

    public void getAll(Consumer<FindIterable<T>> data) {
        getCollection(entries->{
            data.accept(entries.find());
        });
    }

    public void getFirst(Consumer<T> entry) {
        getCollection(col->{
            entry.accept(col.find().first());
        });
    }

    public void findBy(String fieldName, Object fieldValue, Consumer<T> data) {
        getCollection(entries->{
            T entry = entries.find(Filters.eq(fieldName, fieldValue)).first();
            data.accept(entry);
        });
    }

    public void findByID(String id, Consumer<T> entry) {
        findBy(idField, id, entry);
    }

    public void replaceBy(String fieldName, Object fieldValue, T newValue) {
        getCollection(col -> {
            col.findOneAndReplace(Filters.eq(fieldName, fieldValue), newValue);
        });
    }

    public void save(T data) {
        getCollection(col-> {
            T entry = col.find(Filters.eq(idField, data.id())).first();
            if (entry == null) {
                try {
                    T newEntry = clazz.getDeclaredConstructor(String.class).newInstance(data.id());
                    col.insertOne(newEntry);
                } catch (Exception e) {
                    MongoConnector.logger.error("MongoDB Error while saving data: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                replaceBy(idField, data.id(), data);
            }
        });
    }

}
