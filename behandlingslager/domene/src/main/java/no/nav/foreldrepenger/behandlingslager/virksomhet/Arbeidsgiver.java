package no.nav.foreldrepenger.behandlingslager.virksomhet;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.tilMaskertNummer;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.diff.TraverseValue;
import no.nav.foreldrepenger.domene.typer.AktørId;

/** En arbeidsgiver (enten virksomhet eller personlig arbeidsgiver). */
@Embeddable
public class Arbeidsgiver implements Serializable, TraverseValue, IndexKey {

    /**
     * Kun en av denne og {@link #arbeidsgiverAktørId} kan være satt. Sett denne hvis Arbeidsgiver er en Organisasjon.
     */
    @ChangeTracked
    @Column(name = "arbeidsgiver_orgnr", updatable = false)
    private String arbeidsgiverOrgnr;

    /**
     * Kun en av denne og {@link #virksomhet} kan være satt. Sett denne hvis Arbeidsgiver er en Enkelt person.
     */
    @ChangeTracked
    @Embedded
    @AttributeOverride(name = "aktørId", column = @Column(name = "arbeidsgiver_aktor_id", updatable = false))
    private AktørId arbeidsgiverAktørId;

    @SuppressWarnings("unused")
    Arbeidsgiver() {
        // for JPA
    }

    protected Arbeidsgiver(String arbeidsgiverOrgnr, AktørId arbeidsgiverAktørId) {
        if(arbeidsgiverAktørId==null && arbeidsgiverOrgnr==null) {
            throw new IllegalArgumentException("Utvikler-feil: arbeidsgiver uten hverken orgnr eller aktørId");
        }
        if (arbeidsgiverAktørId!=null && arbeidsgiverOrgnr!=null) {
            throw new IllegalArgumentException("Utvikler-feil: arbeidsgiver med både orgnr og aktørId");
        }
        this.arbeidsgiverOrgnr = arbeidsgiverOrgnr;
        this.arbeidsgiverAktørId = arbeidsgiverAktørId;
    }

    @Override
    public String getIndexKey() {
        return getAktørId() != null
            ? IndexKey.createKey("arbeidsgiverAktørId", getAktørId())
            : IndexKey.createKey("virksomhet", getOrgnr());
    }

    public static Arbeidsgiver virksomhet(String arbeidsgiverOrgnr) {
        return new Arbeidsgiver(arbeidsgiverOrgnr, null);
    }

    public static Arbeidsgiver virksomhet(OrgNummer arbeidsgiverOrgnr) {
        return new Arbeidsgiver(arbeidsgiverOrgnr.getId(), null);
    }

    public static Arbeidsgiver person(AktørId arbeidsgiverAktørId) {
        return new Arbeidsgiver(null, arbeidsgiverAktørId);
    }

    /** Virksomhets orgnr. Leser bør ta høyde for at dette kan være juridisk orgnr (istdf. virksomhets orgnr). */
    public String getOrgnr() {
        return arbeidsgiverOrgnr;
    }

    /** Hvis arbeidsgiver er en privatperson, returner aktørId for person. */
    public AktørId getAktørId() {
        return arbeidsgiverAktørId;
    }

    /**
     * Returneer ident for arbeidsgiver. Kan være Org nummer eller Aktør id (dersom arbeidsgiver er en enkelt person -
     * f.eks. for Frilans el.)
     */
    public String getIdentifikator() {
        if (arbeidsgiverAktørId != null) {
            return getAktørId().getId();
        }
        return getOrgnr();
    }

    /**
     * Return true hvis arbeidsgiver er en {@link Virksomhet}, false hvis en Person.
     */
    public boolean getErVirksomhet() {
        return getOrgnr() != null;
    }

    /**
     * Return true hvis arbeidsgiver er en {@link AktørId}, ellers false.
     */
    public boolean erAktørId() {
        return getAktørId() != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Arbeidsgiver that)) {
            return false;
        }
        return Objects.equals(getOrgnr(), that.getOrgnr()) &&
            Objects.equals(getAktørId(), that.getAktørId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOrgnr(), getAktørId());
    }

    @Override
    public String toString() {
        return "Arbeidsgiver{" +
            "virksomhet=" + tilMaskertNummer(getOrgnr()) +
            ", arbeidsgiverAktørId='" + getAktørId() + '\'' +
            '}';
    }

    public static Arbeidsgiver fra(Arbeidsgiver arbeidsgiver) {
        if(arbeidsgiver==null) return null;
        return new Arbeidsgiver(arbeidsgiver.getOrgnr(), arbeidsgiver.getAktørId());
    }

    public static Arbeidsgiver fra(Virksomhet virksomhet) {
        return fra(virksomhet(virksomhet.getOrgnr()));
    }

    public static Arbeidsgiver fra(AktørId aktørId) {
        return fra(person(aktørId));
    }
}
