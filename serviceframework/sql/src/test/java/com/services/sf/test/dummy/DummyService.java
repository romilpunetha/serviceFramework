package com.services.sf.test.dummy;

import com.services.sf.domain.basesql.BaseOutboxSqlService;

public interface DummyService extends BaseOutboxSqlService<DummyEntity, Dummy, DummyCache> {
}