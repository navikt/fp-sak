package no.nav.foreldrepenger.web.app.konfig;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;

import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.*;

public class NotNullAwareModelConverter implements ModelConverter {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        Schema<?> schema = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
        if (schema == null || schema.getProperties() == null) {
            return schema;
        }

        JavaType javaType = TypeFactory.defaultInstance().constructType(type.getType());
        Class<?> rawClass = javaType.getRawClass();

        Map properties = schema.getProperties();
        Map newProps = new LinkedHashMap();
        List<String> required = new ArrayList<>();

        for (Object entryObj : properties.entrySet()) {
            Map.Entry entry = (Map.Entry) entryObj;
            String name = (String) entry.getKey();
            if (name == null) {
                continue;
            }
            Schema propertySchema = (Schema) entry.getValue();

            boolean notNull = false;
            boolean isDeprecated = false;

            if (rawClass.isRecord()) {
                RecordComponent rc = Arrays.stream(rawClass.getRecordComponents())
                    .filter(c -> c.getName().equals(name))
                    .findFirst().orElse(null);
                if (rc != null) {
                    notNull = rc.isAnnotationPresent(NotNull.class);
                    isDeprecated = rc.isAnnotationPresent(Deprecated.class);
                }
            } else {
                try {
                    Field field = rawClass.getDeclaredField(name);
                    notNull = field.isAnnotationPresent(NotNull.class);
                    isDeprecated = field.isAnnotationPresent(Deprecated.class);
                } catch (NoSuchFieldException ignored) {
                    // Field not found, treat as nullable
                }
            }

            if (isDeprecated) {
                // Skip deprecated fields
                newProps.put(name, propertySchema);
                continue;
            }

            // If field has @NotNull: required = true, nullable = false
            // If field does not have @NotNull: required = true, nullable = true
            required.add(name);
            propertySchema.setNullable(!notNull);
            newProps.put(name, propertySchema);
        }

        schema.setProperties(newProps);
        schema.setRequired(required);
        return schema;
    }
}
