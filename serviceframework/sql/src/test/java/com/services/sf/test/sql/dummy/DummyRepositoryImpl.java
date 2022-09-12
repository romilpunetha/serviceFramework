package com.services.sf.test.sql.dummy;

import com.google.common.collect.Sets;
import com.querydsl.core.types.Path;
import com.services.sf.sql.BaseSqlRepositoryImpl;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ApplicationScoped
public class DummyRepositoryImpl extends BaseSqlRepositoryImpl<DummyEntity, Dummy, QDummyEntity>
        implements DummyRepository {

    private final Set<String> extraSortFields = Sets.union(Stream.<Path<?>>of(
            QDummyEntity.dummyEntity.hello,
            QDummyEntity.dummyEntity.someOtherStuff
    ).map(field -> field.getMetadata().getName()).collect(Collectors.toUnmodifiableSet()), super.getAllowedSortFields());

    @Inject
    public DummyRepositoryImpl(DummyMapper mapper) {
        super(mapper, DummyEntity.class, QDummyEntity.class);
    }

    @Override
    public Set<String> getAllowedSortFields() {
        return extraSortFields;
    }

}