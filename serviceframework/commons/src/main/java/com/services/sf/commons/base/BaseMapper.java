package com.services.sf.commons.base;

import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Set;

public interface BaseMapper<T, E> {

    T toFirst(E second);

    E toSecond(T first);

    void toFirst(@MappingTarget T first, E second);

    void toSecond(T t, @MappingTarget E second);

    List<T> toFirst(List<E> second);

    List<E> toSecond(List<T> first);

    void toFirst(@MappingTarget List<T> first, List<E> second);

    void toSecond(List<T> first, @MappingTarget List<E> second);

    Set<T> toFirst(Set<E> second);

    Set<E> toSecond(Set<T> first);

    void toFirst(@MappingTarget Set<T> first, Set<E> second);

    void toSecond(Set<T> first, @MappingTarget Set<E> second);
}
