package com.services.sf.test.mongo;

import com.services.sf.mongodb.BaseOutboxMongoService;

public interface DummyService extends BaseOutboxMongoService<DummyEntity, Dummy, DummyCache> {
}
