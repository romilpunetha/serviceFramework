package com.services.sf.test.sql.dummy;

import com.services.sf.sql.BaseOutboxSqlService;

public interface DummyService extends BaseOutboxSqlService<DummyEntity, Dummy, DummyCache> {
}