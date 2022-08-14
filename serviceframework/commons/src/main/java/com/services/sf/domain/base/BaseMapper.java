package com.services.sf.domain.base;

import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Set;

public interface BaseMapper<T, E> {

    T toFirst(E e);

    E toSecond(T t);

    void toFirst(@MappingTarget T t, E e);

    void toSecond(T t, @MappingTarget E e);

    List<T> toFirst(List<E> e);

    List<E> toSecond(List<T> t);

    void toFirst(@MappingTarget List<T> t, List<E> e);

    void toSecond(List<T> t, @MappingTarget List<E> e);

    Set<T> toFirst(Set<E> e);

    Set<E> toSecond(Set<T> t);

    void toFirst(@MappingTarget Set<T> t, Set<E> e);

    void toSecond(Set<T> t, @MappingTarget Set<E> e);
}
