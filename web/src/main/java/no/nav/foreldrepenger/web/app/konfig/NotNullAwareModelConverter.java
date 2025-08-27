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

/**
 * Denne er midlertidig for å gjøre om undefined typer til null. På sikt ønsker vi at selve objectmapperen skal outputte undefined. Men først må vi sync'e FE+BE
 */
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

        // Disse har en annen objectmapper som outputter undefined.
        if (rawClass.getPackageName().contains("no.nav.folketrygdloven.kalkulus")) {
            return schema;
        }
        // Debug logging to help identify which schemas are being processed
        System.out.println("Processing schema for class: " + rawClass.getSimpleName());

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
                    // Try multiple approaches to get annotations for records

                    // Approach 1: Direct record component annotations
                    notNull = rc.isAnnotationPresent(NotNull.class);
                    isDeprecated = rc.isAnnotationPresent(Deprecated.class);

                    // Approach 2: Try accessor method annotations
                    if (!notNull) {
                        try {
                            var accessorMethod = rawClass.getMethod(name);
                            notNull = accessorMethod.isAnnotationPresent(NotNull.class);
                            isDeprecated = accessorMethod.isAnnotationPresent(Deprecated.class);
                            System.out.println("    Accessor method annotations: " + Arrays.toString(accessorMethod.getAnnotations()));
                        } catch (NoSuchMethodException ignored) {
                            // Accessor method not found
                        }
                    }

                    // Approach 3: Try constructor parameter annotations
                    if (!notNull) {
                        try {
                            var constructors = rawClass.getConstructors();
                            if (constructors.length > 0) {
                                var constructor = constructors[0]; // Records have only one constructor
                                var params = constructor.getParameters();
                                var components = rawClass.getRecordComponents();

                                for (int i = 0; i < params.length && i < components.length; i++) {
                                    if (components[i].getName().equals(name)) {
                                        var param = params[i];
                                        notNull = param.isAnnotationPresent(NotNull.class);
                                        isDeprecated = param.isAnnotationPresent(Deprecated.class);
                                        System.out.println("    Constructor param annotations: " + Arrays.toString(param.getAnnotations()));
                                        break;
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                            // Constructor approach failed
                        }
                    }

                    System.out.println("  Record field '" + name + "': @NotNull=" + notNull + ", @Deprecated=" + isDeprecated);
                } else {
                    System.out.println("  Record component '" + name + "' not found!");
                }
            } else {
                try {
                    Field field = rawClass.getDeclaredField(name);
                    notNull = field.isAnnotationPresent(NotNull.class);
                    isDeprecated = field.isAnnotationPresent(Deprecated.class);
                    System.out.println("  Class field '" + name + "': @NotNull=" + notNull + ", @Deprecated=" + isDeprecated);
                } catch (NoSuchFieldException ignored) {
                    System.out.println("  Field '" + name + "' not found in class, treating as nullable");
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

            // Handle nullable property - special handling for $ref schemas
            if (propertySchema.get$ref() != null && notNull == false) {
                // For nullable $ref schemas, we need to wrap them in allOf to ensure nullable is serialized
                Schema wrapperSchema = new Schema();
                wrapperSchema.setNullable(true);
                List<Schema> allOfList = new ArrayList<>();
                allOfList.add(propertySchema);
                wrapperSchema.setAllOf(allOfList);
                propertySchema = wrapperSchema;
                System.out.println("  Wrapped $ref field '" + name + "' in allOf with nullable=true");
            } else {
                // For simple types or non-nullable $ref, set nullable directly
                propertySchema.setNullable(!notNull);
            }

            // Debug: Check what type of schema this is
            System.out.println("  Field '" + name + "': type=" + propertySchema.getType() +
                              ", $ref=" + propertySchema.get$ref() +
                              ", nullable=" + propertySchema.getNullable());

            newProps.put(name, propertySchema);
        }

        schema.setProperties(newProps);
        schema.setRequired(required);

        // Debug: Final check of properties after setting them
        System.out.println("Final schema properties for " + rawClass.getSimpleName() + ":");
        for (Object entryObj : newProps.entrySet()) {
            Map.Entry entry = (Map.Entry) entryObj;
            String name = (String) entry.getKey();
            Schema propSchema = (Schema) entry.getValue();
            System.out.println("  " + name + ": nullable=" + propSchema.getNullable() +
                              ", $ref=" + propSchema.get$ref() + ", type=" + propSchema.getType());
        }

        return schema;
    }
}
