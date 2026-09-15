package com.experimentos.backend.shared.infrastructure.firebase.repositories;

import java.util.Optional;

/** Minimal repository contract used by the application services. */
public interface FirestoreRepository<T, ID> {
    <S extends T> S save(S entity);

    Optional<T> findById(ID id);

    java.util.List<T> findAll();

    boolean existsById(ID id);

    void delete(T entity);

    void deleteById(ID id);

    default void flush() {}
}
