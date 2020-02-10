package no.nav.foreldrepenger.domene.typer;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.diff.TraverseValue;

/**
 * Intern arbeidsforhold referanse.
 * <p>
 * Hvis null gjelder det flere arbeidsforhold, ellers for et spesifikt forhold
 */

@Embeddable
public class InternArbeidsforholdRef implements IndexKey, TraverseValue, Serializable {

    /**
     * Instans som representerer alle arbeidsforhold (for en arbeidsgiver).
     */
    private static final InternArbeidsforholdRef NULL_OBJECT = new InternArbeidsforholdRef(null);

    @Column(name = "arbeidsforhold_intern_id")
    private UUID referanse;

    InternArbeidsforholdRef() {
    }

    private InternArbeidsforholdRef(UUID referanse) {
        this.referanse = referanse;
    }

    public static InternArbeidsforholdRef ref(UUID referanse) {
        return referanse == null ? NULL_OBJECT : new InternArbeidsforholdRef(referanse);
    }

    public static InternArbeidsforholdRef ref(String referanse) {
        return referanse == null ? NULL_OBJECT : new InternArbeidsforholdRef(UUID.fromString(referanse));
    }

    public static InternArbeidsforholdRef nullRef() {
        return NULL_OBJECT;
    }

    public static InternArbeidsforholdRef nyRef() {
        return ref(UUID.randomUUID().toString());
    }

    /**
     * Genererer en UUID type 3 basert på angitt seed. Gir konsekvente UUIDer
     */
    public static InternArbeidsforholdRef namedRef(String seed) {
        return ref(UUID.nameUUIDFromBytes(seed.getBytes(Charset.forName("UTF8"))).toString());
    }

    public String getReferanse() {
        return referanse == null ? null : referanse.toString();
    }

    public UUID getUUIDReferanse() {
        return referanse;
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(referanse == null ? null : referanse.toString());
    }

    public boolean gjelderForSpesifiktArbeidsforhold() {
        return referanse != null;
    }

    public boolean gjelderFor(InternArbeidsforholdRef ref) {
        Objects.requireNonNull(ref, "Forventer InternArbeidsforholdRef.nullRef()");
        if (!gjelderForSpesifiktArbeidsforhold() || !ref.gjelderForSpesifiktArbeidsforhold()) {
            return true;
        }
        return Objects.equals(referanse, ref.referanse);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null && this.referanse == null) {
            return true;
        }
        if (o == null || getClass() != o.getClass())
            return false;
        InternArbeidsforholdRef that = (InternArbeidsforholdRef) o;
        return Objects.equals(referanse, that.referanse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referanse);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + (referanse == null ? "" : referanse.toString()) + ">";
    }
}
