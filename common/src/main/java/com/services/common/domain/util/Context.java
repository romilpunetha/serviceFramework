package com.services.common.domain.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@SuperBuilder(toBuilder = true)
@Getter
@Setter
@AllArgsConstructor
@ToString(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public final class Context {

    @JsonProperty
    @Builder.Default
    volatile ConcurrentHashMap<String, Object> entries = new ConcurrentHashMap<>(8);

    private Context() {
        this.entries = new ConcurrentHashMap<>(8);
    }

    private Context(Map<String, ?> initialEntries) {
        this.entries = new ConcurrentHashMap<>(initialEntries);
    }

    public static Context empty() {
        return new Context();
    }

    public static Context of(Object... entries) {
        Objects.requireNonNull(entries, "The entries array cannot be null");
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("Arguments must be balanced to form (key, value) pairs");
        } else {
            HashMap<String, Object> map = new HashMap<>();

            for (int i = 0; i < entries.length; i += 2) {
                Objects.requireNonNull(entries[i], "key cannot be null");
                Objects.requireNonNull(entries[i + 1], "value cannot be null");
                String key = entries[i].toString();
                Object value = entries[i + 1];
                map.put(key, value);
            }

            return new Context(map);
        }
    }

    public static Context from(Map<String, ?> entries) {
        return new Context(Objects.requireNonNull(entries, "The entries map cannot be null"));
    }

    public boolean contains(String key) {
        return this.entries != null && this.entries.containsKey(key);
    }

    public <T> T get(String key) throws NoSuchElementException {
        if (this.entries == null) {
            throw new NoSuchElementException("The context is empty");
        } else {
            T value = (T) this.entries.get(key);
            if (value == null) {
                throw new NoSuchElementException("The context does not have a value for key " + key);
            } else {
                return value;
            }
        }
    }

    public <T> T getOrElse(String key, Supplier<? extends T> alternativeSupplier) {
        if (this.entries != null) {
            T value = (T) this.entries.get(key);
            if (value != null) {
                return value;
            }
        }

        return alternativeSupplier.get();
    }

    public Context put(String key, Object value) {
        if (this.entries == null) {
            synchronized (this) {
                if (this.entries == null) {
                    this.entries = new ConcurrentHashMap<>(8);
                }
            }
        }

        this.entries.put(key, value);
        return this;
    }

    public Context delete(String key) {
        if (this.entries != null) {
            this.entries.remove(key);
        }

        return this;
    }

    public boolean isEmpty() {
        return this.entries == null || this.entries.isEmpty();
    }

    public Set<String> keys() {
        if (this.entries == null) {
            return Collections.emptySet();
        } else {
            HashSet<String> set = new HashSet<>();
            Enumeration<String> enumeration = this.entries.keys();

            while (enumeration.hasMoreElements()) {
                set.add(enumeration.nextElement());
            }

            return set;
        }
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other != null && this.getClass() == other.getClass()) {
            Context context = (Context) other;
            return Objects.equals(this.entries, context.entries);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(this.entries);
    }

}
