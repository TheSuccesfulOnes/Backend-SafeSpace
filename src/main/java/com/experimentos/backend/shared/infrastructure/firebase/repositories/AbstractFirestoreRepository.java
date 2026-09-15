package com.experimentos.backend.shared.infrastructure.firebase.repositories;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Blocking Firestore adapter for the small aggregate documents used by this application.
 *
 * <p>Documents intentionally contain scalar snapshots of related entities. This keeps reads
 * predictable and avoids lazy-loading or cross-collection joins while preserving the domain API.
 */
public abstract class AbstractFirestoreRepository<T, ID> implements FirestoreRepository<T, ID> {

    private static final Logger log = LoggerFactory.getLogger(AbstractFirestoreRepository.class);
    private static final String COUNTERS_COLLECTION = "_counters";

    protected final Firestore firestore;
    private final Class<T> entityType;
    private final String collectionName;

    protected AbstractFirestoreRepository(
            Firestore firestore, Class<T> entityType, String collectionName) {
        this.firestore = firestore;
        this.entityType = entityType;
        this.collectionName = collectionName;
    }

    @Override
    public <S extends T> S save(S entity) {
        try {
            Object id = readId(entity);
            if (id == null && hasNumericIdField(entity)) {
                id = nextId();
                writeId(entity, id);
            }
            if (id == null && hasUserIdField(entity)) {
                id = readField(entity, "userId");
                if (id == null) {
                    Object user = readField(entity, "user");
                    id = user == null ? null : readField(user, "id");
                    if (id != null) writeField(entity, "userId", id);
                }
            }
            String documentId = documentId(entity, id);
            if (documentId == null || documentId.isBlank()) {
                throw new IllegalArgumentException("A Firestore document id is required");
            }
            assignNestedIds(entity, new IdentityHashMap<>());
            setLifecycleTimestamps(entity);
            firestore.collection(collectionName).document(documentId).set(toDocument(entity)).get();
            return entity;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Firestore write was interrupted", exception);
        } catch (ExecutionException exception) {
            throw firestoreFailure("write", exception);
        }
    }

    @Override
    public Optional<T> findById(ID id) {
        if (id == null) return Optional.empty();
        try {
            DocumentSnapshot document =
                    firestore.collection(collectionName).document(String.valueOf(id)).get().get();
            return document.exists() ? Optional.of(fromDocument(document)) : Optional.empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Firestore read was interrupted", exception);
        } catch (ExecutionException exception) {
            throw firestoreFailure("read", exception);
        }
    }

    @Override
    public List<T> findAll() {
        try {
            QuerySnapshot snapshot = firestore.collection(collectionName).get().get();
            List<T> entities = new ArrayList<>();
            for (QueryDocumentSnapshot document : snapshot.getDocuments()) {
                entities.add(fromDocument(document));
            }
            entities.sort(
                    Comparator.comparing(this::numericId, Comparator.nullsLast(Long::compareTo)));
            return entities;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Firestore read was interrupted", exception);
        } catch (ExecutionException exception) {
            throw firestoreFailure("read", exception);
        }
    }

    @Override
    public boolean existsById(ID id) {
        if (id == null) return false;
        try {
            return firestore
                    .collection(collectionName)
                    .document(String.valueOf(id))
                    .get()
                    .get()
                    .exists();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Firestore read was interrupted", exception);
        } catch (ExecutionException exception) {
            throw firestoreFailure("read", exception);
        }
    }

    @Override
    public void delete(T entity) {
        try {
            Object id = readId(entity);
            firestore.collection(collectionName).document(documentId(entity, id)).delete().get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Firestore delete was interrupted", exception);
        } catch (ExecutionException exception) {
            throw firestoreFailure("delete", exception);
        }
    }

    @Override
    public void deleteById(ID id) {
        if (id == null) return;
        deleteDocumentById(String.valueOf(id));
    }

    /**
     * Deletes a document by its physical Firestore identifier.
     *
     * <p>Some aggregates use a composite identifier at the storage boundary (for example, {@code
     * commentId_userId}). Those repositories must not pass the composite string through the generic
     * {@code ID} API because its {@code toString()} representation is not the Firestore document
     * identifier.
     */
    protected void deleteDocumentById(String documentId) {
        if (documentId == null || documentId.isBlank()) return;
        try {
            firestore.collection(collectionName).document(documentId).delete().get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Firestore delete was interrupted", exception);
        } catch (ExecutionException exception) {
            throw firestoreFailure("delete", exception);
        }
    }

    protected List<T> readAll() {
        return findAll();
    }

    protected Object readField(Object target, String fieldName) {
        Field field = findField(target.getClass(), fieldName);
        if (field == null) return null;
        try {
            field.setAccessible(true);
            return field.get(target);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not read domain field " + fieldName, exception);
        }
    }

    protected void writeField(Object target, String fieldName, Object value) {
        Field field = findField(target.getClass(), fieldName);
        if (field == null) return;
        try {
            field.setAccessible(true);
            field.set(target, convertScalar(value, field.getType()));
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not write domain field " + fieldName, exception);
        }
    }

    protected String documentId(T entity, Object id) {
        return id == null ? null : String.valueOf(id);
    }

    protected Map<String, Object> toDocument(T entity) {
        return encodeObject(entity, 2, true);
    }

    protected T fromDocument(DocumentSnapshot document) {
        return decodeObject(document.getData(), entityType);
    }

    private Object readId(Object entity) {
        Field idField = findField(entity.getClass(), "id");
        if (idField != null) return readField(entity, "id");
        Field userIdField = findField(entity.getClass(), "userId");
        return userIdField == null ? null : readField(entity, "userId");
    }

    private boolean hasNumericIdField(Object entity) {
        Field idField = findField(entity.getClass(), "id");
        return idField != null
                && (idField.getType() == Long.class || idField.getType() == long.class);
    }

    private boolean hasUserIdField(Object entity) {
        return findField(entity.getClass(), "userId") != null;
    }

    private void writeId(Object entity, Object id) {
        writeField(entity, "id", id);
    }

    private Long numericId(T entity) {
        Object value = readId(entity);
        if (value instanceof Number number) return number.longValue();
        try {
            return value == null ? null : Long.parseLong(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Long nextId() {
        return nextIdFor(collectionName);
    }

    private Long nextIdFor(String counterName) {
        String counterId = counterName.replace('/', '_');
        DocumentReference counterReference =
                firestore.collection(COUNTERS_COLLECTION).document(counterId);
        try {
            DocumentSnapshot existingCounter = counterReference.get().get();
            long persistedLowerBound =
                    existingCounter.exists() && existingCounter.getLong("value") != null
                            ? existingCounter.getLong("value")
                            : maxPersistedId(counterName);
            return firestore
                    .runTransaction(
                            transaction -> {
                                DocumentSnapshot snapshot = transaction.get(counterReference).get();
                                long current =
                                        snapshot.exists() && snapshot.getLong("value") != null
                                                ? snapshot.getLong("value")
                                                : 0L;
                                long next = Math.max(current, persistedLowerBound) + 1L;
                                Map<String, Object> data = Map.of("value", next);
                                transaction.set(counterReference, data);
                                return next;
                            })
                    .get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Firestore id generation was interrupted", exception);
        } catch (ExecutionException exception) {
            throw firestoreFailure("generate id", exception);
        }
    }

    /**
     * Bootstraps a missing counter from already persisted documents. This matters when the
     * application is migrated to Firestore or when a counter document is restored separately from
     * its collection.
     */
    private long maxPersistedId(String counterName)
            throws InterruptedException, ExecutionException {
        String sourceCollection = counterName.split("__", 2)[0];
        QuerySnapshot snapshot = firestore.collection(sourceCollection).get().get();
        long maximum = 0L;
        for (QueryDocumentSnapshot document : snapshot.getDocuments()) {
            maximum = Math.max(maximum, numericValue(document.getId()));
            maximum = Math.max(maximum, maxNestedId(document.getData()));
        }
        return maximum;
    }

    private long maxNestedId(Object value) {
        if (value instanceof Map<?, ?> map) {
            long maximum = numericValue(map.get("id"));
            for (Object nested : map.values()) maximum = Math.max(maximum, maxNestedId(nested));
            return maximum;
        }
        if (value instanceof Collection<?> collection) {
            long maximum = 0L;
            for (Object nested : collection) maximum = Math.max(maximum, maxNestedId(nested));
            return maximum;
        }
        return 0L;
    }

    private long numericValue(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value == null) return 0L;
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private void setLifecycleTimestamps(Object entity) {
        if (readField(entity, "createdAt") == null) writeField(entity, "createdAt", Instant.now());
        if (findField(entity.getClass(), "updatedAt") != null)
            writeField(entity, "updatedAt", Instant.now());
    }

    private void assignNestedIds(Object value, IdentityHashMap<Object, Boolean> visited) {
        if (value == null || isSimple(value.getClass()) || visited.put(value, Boolean.TRUE) != null)
            return;
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> assignNestedIds(item, visited));
            return;
        }
        Field idField = findField(value.getClass(), "id");
        if (idField != null
                && (idField.getType() == Long.class || idField.getType() == long.class)
                && readField(value, "id") == null) {
            writeField(
                    value,
                    "id",
                    nextIdFor(collectionName + "__" + value.getClass().getSimpleName()));
        }
        for (Field field : allFields(value.getClass())) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
            Object nested = readField(value, field.getName());
            assignNestedIds(nested, visited);
        }
    }

    private Map<String, Object> encodeObject(Object source, int depth, boolean rootEntity) {
        Map<String, Object> data = new HashMap<>();
        for (Field field : allFields(source.getClass())) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
            // Password hashes belong only to the users collection. Related snapshots must never
            // duplicate credentials in surveys, reports, comments, or other documents.
            if (!rootEntity && field.getName().equals("passwordHash")) continue;
            try {
                field.setAccessible(true);
                Object value = field.get(source);
                if (value == null) continue;
                if (depth < 0 && !isSimple(value.getClass())) {
                    if (field.getName().equals("id")) data.put(field.getName(), value);
                    continue;
                }
                data.put(field.getName(), encodeValue(value, depth - 1));
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException(
                        "Could not serialize domain field " + field.getName(), exception);
            }
        }
        return data;
    }

    private Object encodeValue(Object value, int depth) {
        if (value == null || isSimple(value.getClass())) {
            if (value instanceof Instant || value instanceof LocalDate) return value.toString();
            if (value instanceof Enum<?> enumeration) return enumeration.name();
            if (value instanceof byte[] bytes) return Base64.getEncoder().encodeToString(bytes);
            return value;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(item -> encodeValue(item, depth)).toList();
        }
        return encodeObject(value, depth, false);
    }

    private <R> R decodeObject(Map<String, Object> data, Class<R> type) {
        if (data == null) return null;
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            R target = constructor.newInstance();
            for (Field field : allFields(type)) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
                if (!data.containsKey(field.getName())) continue;
                Object converted =
                        decodeValue(
                                data.get(field.getName()), field.getType(), field.getGenericType());
                if (converted == null && field.getType().isPrimitive()) continue;
                field.setAccessible(true);
                field.set(target, converted);
            }
            return target;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Could not deserialize " + type.getSimpleName(), exception);
        }
    }

    private Object decodeValue(Object value, Class<?> type, Type genericType) {
        if (value == null) return null;
        if (type == String.class || type == Object.class) return value.toString();
        if (type == Long.class || type == long.class) return ((Number) value).longValue();
        if (type == Integer.class || type == int.class) return ((Number) value).intValue();
        if (type == Boolean.class || type == boolean.class)
            return value instanceof Boolean b ? b : Boolean.parseBoolean(value.toString());
        if (type == Double.class || type == double.class) return ((Number) value).doubleValue();
        if (type == byte[].class) return Base64.getDecoder().decode(value.toString());
        if (type == Instant.class) return Instant.parse(value.toString());
        if (type == LocalDate.class) return LocalDate.parse(value.toString());
        if (type.isEnum()) return decodeEnum(value, type);
        if (Collection.class.isAssignableFrom(type) && value instanceof List<?> values) {
            Class<?> elementType = Object.class;
            if (genericType instanceof ParameterizedType parameterizedType
                    && parameterizedType.getActualTypeArguments()[0] instanceof Class<?> clazz) {
                elementType = clazz;
            }
            List<Object> decoded = new ArrayList<>();
            for (Object item : values) decoded.add(decodeValue(item, elementType, elementType));
            return decoded;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new HashMap<>();
            map.forEach((key, item) -> normalized.put(String.valueOf(key), item));
            return decodeObject(normalized, type);
        }
        return convertScalar(value, type);
    }

    private Object decodeEnum(Object value, Class<?> type) {
        for (Object constant : type.getEnumConstants()) {
            if (((Enum<?>) constant).name().equals(value.toString())) return constant;
        }
        throw new IllegalArgumentException("Unknown " + type.getSimpleName() + " value: " + value);
    }

    private Object convertScalar(Object value, Class<?> type) {
        if (value == null) return null;
        if (type.isInstance(value)) return value;
        if (type == Long.class || type == long.class) return Long.valueOf(value.toString());
        if (type == Integer.class || type == int.class) return Integer.valueOf(value.toString());
        if (type == Boolean.class || type == boolean.class)
            return Boolean.valueOf(value.toString());
        return value;
    }

    private boolean isSimple(Class<?> type) {
        return type.isPrimitive()
                || Number.class.isAssignableFrom(type)
                || type == String.class
                || type == Boolean.class
                || type == Double.class
                || type == Float.class
                || type == Instant.class
                || type == LocalDate.class
                || type.isEnum()
                || type == byte[].class;
    }

    private List<Field> allFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type;
                current != null && current != Object.class;
                current = current.getSuperclass()) {
            fields.addAll(List.of(current.getDeclaredFields()));
        }
        return fields;
    }

    private Field findField(Class<?> type, String name) {
        for (Class<?> current = type;
                current != null && current != Object.class;
                current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // Continue through the class hierarchy.
            }
        }
        return null;
    }

    private IllegalStateException firestoreFailure(String operation, Exception exception) {
        log.error("Firestore {} failed", operation, exception);
        return new IllegalStateException("Firestore " + operation + " failed", exception);
    }
}
