package no.nav.foreldrepenger.web.app.konfig;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.oas.models.media.Schema;

import java.util.Iterator;

public class JsonTypeNameDisctriminatorConverter implements ModelConverter {

    @Override
    public Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        Schema<?> resolved = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;

        if (resolved != null && type.getType() instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type.getType();
            JsonTypeName typeName = clazz.getAnnotation(JsonTypeName.class);
            if (typeName != null && typeName.value() != null && !typeName.value().isBlank()) {
                // Override schema name so swagger-core uses this in discriminator mapping
                resolved.setName(typeName.value());
            }
        }

        return resolved;
    }
}
