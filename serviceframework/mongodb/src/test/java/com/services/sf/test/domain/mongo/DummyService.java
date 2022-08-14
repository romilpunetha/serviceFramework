package com.services.sf.test.domain.mongo;

import com.services.sf.domain.basemongo.BaseOutboxMongoService;

public interface DummyService extends BaseOutboxMongoService<DummyEntity, Dummy, DummyCache> {
}
