package no.nav.foreldrepenger.domene.typer;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;

/**
 * Id som genereres fra Nav aktørregister. Denne iden benyttes til interne forhold i Nav og vil ikke endres f.eks. dersom bruker går fra
 * DNR til FNR i Folkeregisteret. Tilsvarende vil den kunne referere personer som har ident fra et utenlandsk system.
 */
@Embeddable
public class AktørId implements Serializable, Comparable<AktørId>, IndexKey {
    private static final String VALID_REGEXP = "^\\d{13}$";

    private static final Pattern VALID = Pattern.compile(VALID_REGEXP, Pattern.CASE_INSENSITIVE);

    @JsonValue
    @NotNull
    @jakarta.validation.constraints.Pattern(regexp = VALID_REGEXP, message = "aktørId ${validatedValue} har ikke gyldig verdi (pattern '{regexp}')")
    @Column(name = "aktoer_id", updatable = false, length = 50)
    private String aktørId;

    protected AktørId() {
        // for hibernate
    }

    public AktørId(Long aktørId) {
        Objects.requireNonNull(aktørId, "aktørId");
        this.aktørId = validateAktørId(aktørId.toString());
    }

    public AktørId(String aktørId) {
        this.aktørId = validateAktørId(aktørId);
    }

    private String validateAktørId(String aktørId) {
        Objects.requireNonNull(aktørId, "aktørId");
        if (!VALID.matcher(aktørId).matches()) {
            // skal ikke skje, funksjonelle feilmeldinger håndteres ikke her.
            throw new IllegalArgumentException("Ugyldig aktørId '" + aktørId +"', tillatt pattern: "+ VALID_REGEXP);
        }
        return aktørId;
    }

    @Override
    public String getIndexKey() {
        return aktørId;
    }

    public String getId() {
        return aktørId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        var other = (AktørId) obj;
        return Objects.equals(aktørId, other.aktørId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + maskerAktørId() + ">";
    }

    @Override
    public int compareTo(AktørId o) {
        // TODO: Burde ikke finnes
        return aktørId.compareTo(o.aktørId);
    }

    public static boolean erGyldigAktørId(String aktørId) {
        return aktørId != null && VALID.matcher(aktørId).matches();
    }

    private String maskerAktørId() {
        if (aktørId == null) {
            return "";
        }
        var length = aktørId.length();
        if (length <= 4) {
            return "*".repeat(length);
        }
        return "*".repeat(length - 4) + aktørId.substring(length - 4);
    }

    private static AtomicLong DUMMY_AKTØRID = new AtomicLong(1000000000000L);

    /** Genererer dummy aktørid unikt for test. */
    public static AktørId dummy( ) {
        return new AktørId(DUMMY_AKTØRID.getAndIncrement());
    }
}
