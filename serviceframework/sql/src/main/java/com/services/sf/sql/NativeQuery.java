package com.services.sf.sql;

import io.quarkus.logging.Log;
import org.jooq.Record;
import org.jooq.*;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public record NativeQuery(String sql, List<Object> bindings) {
    public static class Builder {
        private final SelectQuery<Record> selectQuery;
        private final Function<String, Condition> enhancedCondition;
        private String currentTableName;

        public Builder(SelectQuery<Record> selectQuery, Function<String, Condition> enhancedCondition) {
            this.selectQuery = selectQuery;
            this.enhancedCondition = enhancedCondition;
        }

        public Builder fields(SelectFieldOrAsterisk... fields) {
            selectQuery.addSelect(fields);
            return this;
        }

        public Builder from(TableLike<Record> tableLike) {
            return from(Collections.singletonList(tableLike));
        }

        public Builder from(List<TableLike<Record>> tableLikes) {
            this.rawFrom(tableLikes);
            tableLikes.stream()
                    .map(table -> enhancedCondition.apply(table.asTable().getName()))
                    .forEach(selectQuery::addConditions);
            return this;
        }

        public Builder rawFrom(TableLike<Record> tableLike) {
            return rawFrom(Collections.singletonList(tableLike));
        }

        public Builder rawFrom(List<TableLike<Record>> tableLikes) {
            selectQuery.addFrom(tableLikes);
            return this;
        }

        public Builder where(Condition condition) {
            selectQuery.addConditions(condition);
            return this;
        }

        public Builder offset(int offset) {
            selectQuery.addOffset(offset);
            return this;
        }

        public Builder limit(int limit) {
            selectQuery.addLimit(limit);
            return this;
        }

        public Builder orderBy(List<String> sortOrders, String tableName) {
            return orderBy(sortOrders.stream().map(order -> {
                if (order.startsWith("-")) {
                    return DSL.field(DSL.name(tableName, order.substring(1))).sort(SortOrder.DESC);
                } else {
                    return DSL.field(DSL.name(tableName, order)).sort(SortOrder.ASC);
                }
            }).toList());
        }


        public Builder orderBy(SortField<Object> orderFields) {
            return orderBy(Collections.singletonList(orderFields));
        }

        public Builder orderBy(List<SortField<Object>> orderFields) {
            selectQuery.addOrderBy(orderFields);
            return this;
        }

        public Builder tenant(String tenant) {
            selectQuery.addConditions(DSL.field("tenantId").eq(tenant));
            return this;
        }

        public Builder rawJoin(TableLike<Record> table, JoinType joinType, Condition condition) {
            selectQuery.addJoin(table, joinType, condition);
            return this;
        }

        public Builder join(TableLike<Record> table, JoinType joinType, Condition condition) {
            rawJoin(table, joinType, condition);
            selectQuery.addConditions(enhancedCondition.apply(table.asTable().getName()));
            return this;
        }

        public NativeQuery build() {
            var generatedQuery = new NativeQuery(selectQuery.getSQL(), selectQuery.getBindValues());
            Log.debug("GeneratedQuery: %s".formatted(generatedQuery));
            return generatedQuery;
        }

        public String buildInline() {
            var generatedQuery = selectQuery.getSQL(ParamType.INLINED);
            Log.debug("GeneratedQuery: %s".formatted(generatedQuery));
            return generatedQuery;
        }
    }
}
