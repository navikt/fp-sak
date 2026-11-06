package no.nav.foreldrepenger.domene.typer;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;

/**
 * Saksnummer refererer til saksnummer genrert av FP-sak
 */
public record Saksnummer(@JsonValue String verdi) implements Serializable, IndexKey {
    private static final String CHARS = "a-z0-9_:-";

    private static final Pattern VALID = Pattern.compile("^(-?[1-9]|[a-z0])[" + CHARS + "]*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern INVALID = Pattern.compile("[^" + CHARS + "]+", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    public Saksnummer {
        Objects.requireNonNull(verdi, "saksnummer");
        if (!VALID.matcher(verdi).matches()) {
            // skal ikke skje, funksjonelle feilmeldinger håndteres ikke her.
            throw new IllegalArgumentException(
                "Ugyldig saksnummer, støtter kun A-Z/0-9/:/-/_ tegn. Var: " + verdi.replaceAll(INVALID.pattern(), "?") + " (vasket)");
        }
    }

    @Override
    public String getIndexKey() {
        return verdi;
    }

    public String getVerdi() {
        return verdi;
    }

    public static Saksnummer fra(String saksnummer) {
        return saksnummer == null ? null : new Saksnummer(saksnummer);
    }

    @Converter
    public static class SaksnummerConverter implements AttributeConverter<Saksnummer, String> {

        @Override
        public String convertToDatabaseColumn(Saksnummer attribute) {
            return Optional.ofNullable(attribute).map(Saksnummer::verdi).orElse(null);
        }

        @Override
        public Saksnummer convertToEntityAttribute(String dbData) {
            return Saksnummer.fra(dbData);
        }
    }
}
