package service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import model.annotation.Id;

public class Repository<T> {
    private final HashMap<String, T> entities = new HashMap<>();

    public T save(T entity) {
        Objects.requireNonNull(entity, "entity");
        String id = findId(entity);
        entities.put(id, entity);
        return entity;
    }

    public T findById(String id) {
        return entities.get(Objects.requireNonNull(id, "id"));
    }

    public ArrayList<T> findAll() {
        return new ArrayList<>(entities.values());
    }

    public boolean delete(String id) {
        return entities.remove(Objects.requireNonNull(id, "id")) != null;
    }

    @SafeVarargs
    public final void saveAll(T... items) {
        Objects.requireNonNull(items, "items");
        for (T item : items) {
            save(item);
        }
    }

    public static <N extends Number> double average(N[] values) {
        Objects.requireNonNull(values, "values");
        if (values.length == 0) {
            throw new IllegalArgumentException("Cannot average an empty array.");
        }

        double sum = 0;
        for (N value : values) {
            sum += Objects.requireNonNull(value, "value").doubleValue();
        }
        return sum / values.length;
    }

    private String findId(T entity) {
        for (Class<?> type = entity.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Id.class)) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    if (!(value instanceof String id) || id.isBlank()) {
                        throw new IllegalStateException(
                                "@Id field must contain a non-blank String: " + field.getName());
                    }
                    return id;
                } catch (IllegalAccessException | SecurityException exception) {
                    throw new IllegalStateException(
                            "Could not read @Id field: " + field.getName(), exception);
                }
            }
        }
        throw new IllegalArgumentException(
                "Entity type has no @Id field: " + entity.getClass().getName());
    }
}
