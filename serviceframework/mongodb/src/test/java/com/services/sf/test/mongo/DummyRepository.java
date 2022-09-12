package com.services.sf.test.mongo;

import com.services.sf.mongodb.BaseMongoRepository;
import io.smallrye.mutiny.Uni;

public interface DummyRepository extends BaseMongoRepository<DummyEntity, Dummy> {

    Uni<Dummy> testSetOnInsertWithSet(Dummy dummy);

    Uni<Dummy> testIncWithSet(Dummy dummy);
}
