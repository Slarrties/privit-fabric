package dev.slarrties.privit.common.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.CommentedConfig;

import dev.slarrties.privit.common.config.annotation.ConfigValue;
import dev.slarrties.privit.common.config.annotation.ConfigIgnore;
import dev.slarrties.privit.common.config.annotation.ConfigSection;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class ConfigSerializer {

    private ConfigSerializer() {}

    public static void readSections(CommentedConfig raw, Iterable<Object> sections) {
        for (Object section : sections) {
            readSection(raw, section);
        }
    }

    public static void writeSections(CommentedConfig raw, Iterable<Object> sections) {
        for (Object section : sections) {
            writeSection(raw, section);
        }
    }

    public static void writeDefaultSections(CommentedConfig raw, Iterable<Object> sections) {
        for (Object section : sections) {
            writeDefaultSection(raw, section);
        }
    }

    // ─────────────────────────────────────────────
    // Section
    // ─────────────────────────────────────────────

    private static void readSection(CommentedConfig raw, Object section) {
        ConfigSection meta = requireSection(section);
        String prefix = meta.path();

        for (Field field : section.getClass().getDeclaredFields()) {
            if (!isConfigField(field)) continue;

            ConfigValue valueMeta = field.getAnnotation(ConfigValue.class);
            String key = resolveKey(field, valueMeta);
            String path = prefix + "." + key;

            try {
                field.setAccessible(true);
                Object current = field.get(section);
                Object parsed = readValue(raw, path, field, current);
                if (parsed != null || !field.getType().isPrimitive()) {
                    field.set(section, parsed);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Failed to read config field: " + path, e);
            }
        }
    }

    private static void writeSection(CommentedConfig raw, Object section) {
        ConfigSection meta = requireSection(section);
        String prefix = meta.path();

        for (Field field : section.getClass().getDeclaredFields()) {
            if (!isConfigField(field)) continue;

            ConfigValue valueMeta = field.getAnnotation(ConfigValue.class);
            String key = resolveKey(field, valueMeta);
            String path = prefix + "." + key;

            try {
                field.setAccessible(true);
                Object value = field.get(section);
                writeValue(raw, path, value);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Failed to write config field: " + path, e);
            }
        }
    }

    private static void writeDefaultSection(CommentedConfig raw, Object section) {
        ConfigSection meta = requireSection(section);
        String prefix = meta.path();

        if (meta.comment().length > 0) {
            raw.setComment(prefix, joinComment(meta.comment()));
        }

        for (Field field : section.getClass().getDeclaredFields()) {
            if (!isConfigField(field)) continue;

            ConfigValue valueMeta = field.getAnnotation(ConfigValue.class);
            String key = resolveKey(field, valueMeta);
            String path = prefix + "." + key;

            try {
                field.setAccessible(true);
                Object value = field.get(section);

                if (valueMeta != null && valueMeta.comment().length > 0) {
                    raw.setComment(path, joinComment(valueMeta.comment()));
                }

                writeValue(raw, path, value);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Failed to write default config field: " + path, e);
            }
        }
    }

    // ─────────────────────────────────────────────
    // Values
    // ─────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object readValue(CommentedConfig raw, String path, Field field, Object fallback) {
        Class<?> type = field.getType();

        if (type.isEnum()) {
            String asString = raw.getOrElse(path, fallback instanceof Enum<?> e ? e.name() : null);
            if (asString == null) return fallback;
            try {
                return Enum.valueOf((Class<? extends Enum>) type, asString.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                return fallback;
            }
        }

        if (List.class.isAssignableFrom(type)) {
            List<String> list = raw.getOrElse(path, fallback instanceof List<?> l
                    ? castStringList(l)
                    : new ArrayList<>());
            return new ArrayList<>(list);
        }

        if (Map.class.isAssignableFrom(type)) {
            Map<String, Boolean> result = new LinkedHashMap<>();
            if (fallback instanceof Map<?, ?> fb) {
                for (Map.Entry<?, ?> e : fb.entrySet()) {
                    if (e.getKey() instanceof String k && e.getValue() instanceof Boolean b) {
                        result.put(k, b);
                    }
                }
            }

            Object tableObj = raw.get(path);
            if (tableObj instanceof Config table) {
                for (Config.Entry entry : table.entrySet()) {
                    Object v = entry.getValue();
                    if (v instanceof Boolean b) {
                        result.put(entry.getKey(), b);
                    }
                }
            }
            return result;
        }

        if (type == boolean.class || type == Boolean.class) {
            return raw.getOrElse(path, fallback instanceof Boolean b ? b : false);
        }
        if (type == int.class || type == Integer.class) {
            return raw.getOrElse(path, fallback instanceof Number n ? n.intValue() : 0);
        }
        if (type == long.class || type == Long.class) {
            return raw.getOrElse(path, fallback instanceof Number n ? n.longValue() : 0L);
        }
        if (type == double.class || type == Double.class) {
            return raw.getOrElse(path, fallback instanceof Number n ? n.doubleValue() : 0.0d);
        }
        if (type == float.class || type == Float.class) {
            return raw.getOrElse(path, fallback instanceof Number n ? n.floatValue() : 0.0f);
        }
        if (type == String.class) {
            return raw.getOrElse(path, fallback instanceof String s ? s : "");
        }

        throw new IllegalStateException("Unsupported config field type: " + type.getName() + " at " + path);
    }

    private static void writeValue(CommentedConfig raw, String path, Object value) {
        if (value == null) {
            raw.set(path, null);
            return;
        }

        if (value instanceof Enum<?> e) {
            raw.set(path, e.name());
            return;
        }

        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    raw.set(path + "." + key, entry.getValue());
                }
            }
            return;
        }

        raw.set(path, value);
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private static ConfigSection requireSection(Object section) {
        ConfigSection meta = section.getClass().getAnnotation(ConfigSection.class);
        if (meta == null) {
            throw new IllegalStateException("Missing @ConfigSection on " + section.getClass().getName());
        }
        return meta;
    }

    private static boolean isConfigField(Field field) {
        int mod = field.getModifiers();
        if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) return false;
        if (field.isAnnotationPresent(ConfigIgnore.class)) return false;

        return field.isAnnotationPresent(ConfigValue.class);
    }

    private static String resolveKey(Field field, ConfigValue meta) {
        if (meta != null && !meta.key().isBlank()) return meta.key();

        return field.getName();
    }

    private static String joinComment(String[] lines) {
        return String.join("\n", lines);
    }

    private static List<String> castStringList(List<?> source) {
        List<String> result = new ArrayList<>();
        for (Object o : source) {
            if (o != null) result.add(String.valueOf(o));
        }
        return result;
    }
}