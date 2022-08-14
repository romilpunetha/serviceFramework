package com.services.sf.test.domain.mongo;

import com.services.sf.domain.basemongo.BaseMongoRepositoryImpl;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import io.smallrye.mutiny.Uni;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.bson.Document;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ApplicationScoped
public class DummyRepositoryImpl extends BaseMongoRepositoryImpl<DummyEntity, Dummy>
        implements DummyRepository {

    @Inject
    public DummyRepositoryImpl(DummyMapper mapper) {
        super(mapper);
    }

    public Uni<Dummy> testSetOnInsertWithSet(Dummy dummy) {
        Instant now = Instant.now();

        FindOneAndUpdateOptions findOneAndUpdateOptions = new FindOneAndUpdateOptions()
                .returnDocument(ReturnDocument.AFTER)
                .upsert(true);

        Document filter = QueryBuilder.builder()
                .eq("hello", dummy.getHello())
                .build();

        Document incDoc = new Document()
                .append("version", 1);

        Document setOnInsertDocument = new Document()
                .append("hello", dummy.getHello())
                .append("a", 2)
                .append("b", 2);

        Document update = QueryBuilder.builder()
                .inc(incDoc)
                .set(toDocument(dummy))
                .set("lastModifiedAt", now)
                .setOnInsert(setOnInsertDocument)
                .build();

        assert (!update.get("$set", Document.class).containsKey("a"));
        assert (!update.get("$set", Document.class).containsKey("b"));

        return super.findOneAndUpdate(filter, update, findOneAndUpdateOptions, null);
    }

    public Uni<Dummy> testIncWithSet(Dummy dummy) {
        Instant now = Instant.now();

        FindOneAndUpdateOptions findOneAndUpdateOptions = new FindOneAndUpdateOptions()
                .returnDocument(ReturnDocument.AFTER)
                .upsert(true);

        Document filter = QueryBuilder.builder()
                .eq("hello", dummy.getHello())
                .build();

        Document incDoc = new Document()
                .append("version", 1)
                .append("a", 2)
                .append("b", 2);

        Document setOnInsertDocument = new Document()
                .append("hello", dummy.getHello());

        Document update = QueryBuilder.builder()
                .inc(incDoc)
                .set(toDocument(dummy))
                .set("lastModifiedAt", now)
                .setOnInsert(setOnInsertDocument)
                .build();

        assert (!update.get("$set", Document.class).containsKey("a"));
        assert (!update.get("$set", Document.class).containsKey("b"));

        return super.findOneAndUpdate(filter, update, findOneAndUpdateOptions, null);
    }
}
