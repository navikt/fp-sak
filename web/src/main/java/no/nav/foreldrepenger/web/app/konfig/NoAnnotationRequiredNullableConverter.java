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

public class NoAnnotationRequiredNullableConverter implements ModelConverter {

    @Override
    public Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        Schema<?> schema = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
        if (schema == null || schema.getProperties() == null) {
            return schema;
        }

        JavaType javaType = TypeFactory.defaultInstance().constructType(type.getType());
        Class<?> rawClass = javaType.getRawClass();

        Map<String, Schema> props = schema.getProperties();
        Map<String, Schema> newProps = new LinkedHashMap<>();

        for (Map.Entry<String, Schema> entry : props.entrySet()) {
            String name = entry.getKey();
            if (name == null) {
                continue;
            }
            Schema<?> propertySchema = entry.getValue();

            boolean notNull = false;
            boolean deprecated = false;

            if (rawClass.isRecord()) {
                RecordComponent rc = Arrays.stream(rawClass.getRecordComponents())
                    .filter(c -> c.getName().equals(name))
                    .findFirst().orElse(null);
                if (rc != null) {
                    notNull = rc.isAnnotationPresent(NotNull.class);
                    deprecated = rc.isAnnotationPresent(Deprecated.class);
                }
            } else {
                try {
                    Field field = rawClass.getDeclaredField(name);
                    notNull = field.isAnnotationPresent(NotNull.class);
                    deprecated = field.isAnnotationPresent(Deprecated.class);
                } catch (NoSuchFieldException ignored) {
                }
            }

            if (deprecated) {
                continue; // skip deprecated fields entirely
            }

            schema.addRequiredItem(name);

            if (!notNull) {
                // replace with oneOf: [ original, null ]
                Schema<Object> nullSchema = new Schema<>().type("null");

                Schema<Object> copy = new Schema<>();
                copy.setType(propertySchema.getType());
                copy.setFormat(propertySchema.getFormat());
                copy.set$ref(propertySchema.get$ref());

                if (propertySchema.getEnum() != null) {
                    copy.setEnum(new ArrayList<>(propertySchema.getEnum()));
                }

                Schema<Object> wrapper = new Schema<>();
                wrapper.setOneOf(List.of(copy, nullSchema));
                propertySchema = wrapper;
            } else {
                propertySchema.setNullable(false);
            }

            newProps.put(name, propertySchema);
        }

        schema.setProperties(newProps);
        return schema;
    }
}


