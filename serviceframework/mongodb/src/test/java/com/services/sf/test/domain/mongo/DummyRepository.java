package com.services.sf.test.domain.mongo;

import com.services.sf.domain.basemongo.BaseMongoRepository;
import io.smallrye.mutiny.Uni;

public interface DummyRepository extends BaseMongoRepository<DummyEntity, Dummy> {

    Uni<Dummy> testSetOnInsertWithSet(Dummy dummy);

    Uni<Dummy> testIncWithSet(Dummy dummy);
}
