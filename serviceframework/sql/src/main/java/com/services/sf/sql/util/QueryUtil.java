package com.services.sf.sql.util;


import com.querydsl.core.types.ParamNotSetException;
import com.querydsl.core.types.dsl.Param;
import com.querydsl.jpa.HQLTemplates;
import com.querydsl.jpa.JPQLSerializer;
import com.querydsl.jpa.impl.JPAQuery;
import io.quarkus.arc.properties.IfBuildProperty;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;

import javax.enterprise.context.ApplicationScoped;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


@FieldDefaults(level = AccessLevel.PRIVATE)
@ApplicationScoped
@IfBuildProperty(name = "framework.sql.enabled", stringValue = "true")
public class QueryUtil {
    /**
     * Project the given results to the requested class
     *
     * @param queryResultUni
     * @param clz            Class to project the result to. The first constructor of class should have all the necessary args.
     * @param <X>
     * @return
     */
    public <X> Uni<X> projectResultsToClass(Uni<?> queryResultUni, Class<X> clz) {
        var constructor = getFirstConstructor(clz);
        return queryResultUni.onItem().ifNotNull().transform(resultRow -> getNewInstance(constructor, (Object[]) resultRow));
    }

    /**
     * Project the given results to the requested class
     *
     * @param queryResultMulti
     * @param clz              Class to project the result to. The first constructor of class should have all the necessary args.
     * @param <X>
     * @return
     */
    public <X> Multi<X> projectResultsToClass(Multi<?> queryResultMulti, Class<X> clz) {
        var constructor = getFirstConstructor(clz);
        return queryResultMulti.map(result -> (X) getNewInstance(constructor, (Object[]) result));
    }

    private <X> Constructor<X> getFirstConstructor(Class<X> clz) {
        return (Constructor<X>) Arrays.stream(clz.getConstructors()).findFirst().get();
    }

    @SneakyThrows
    private <X> X getNewInstance(Constructor<X> ctor, Object[] initArgs) {
        return ctor.newInstance(initArgs);
    }

    private <X> Constructor<X> getConstructorForTuple(Class<X> clz, Object[] result) {
        var requiredCount = result.length;
        var optionalConstructor = (Optional<Constructor<?>>) Arrays.stream(clz.getConstructors()).filter(a -> (a.getParameterCount() == requiredCount)).findAny();
        if (optionalConstructor.isEmpty()) {
            throw new UnsupportedOperationException("Required constructor not present");
        }
        return (Constructor<X>) optionalConstructor.get();
    }

    public <T> List<Object> getBindings(JPAQuery<T> query) {
        var querySerializer = new JPQLSerializer(HQLTemplates.DEFAULT);
        querySerializer.serialize(query.getMetadata(), false, null);
        var constants = querySerializer.getConstants();
        var params = query.getMetadata().getParams();

        return constants.stream().map(constant -> {
            if (constant instanceof Param<?> param) {
                constant = params.get(constant);
                if (constant == null) {
                    throw new ParamNotSetException(param);
                }
            }
            return constant;
        }).toList();
    }
}
