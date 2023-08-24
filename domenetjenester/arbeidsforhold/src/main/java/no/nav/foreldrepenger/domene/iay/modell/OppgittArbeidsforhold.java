package no.nav.foreldrepenger.domene.iay.modell;

import jakarta.persistence.Convert;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Entitetsklasse for oppgitte arbeidsforhold.
 * <p>
 * Implementert iht. builder pattern (ref. "Effective Java, 2. ed." J.Bloch).
 * Non-public constructors og setters, dvs. immutable.
 * <p>
 * OBS: Legger man til nye felter så skal dette oppdateres mange steder:
 * builder, equals, hashcode etc.
 */
public class OppgittArbeidsforhold extends BaseEntitet implements IndexKey {

    @ChangeTracked
    private DatoIntervallEntitet periode;

    @Convert(converter = BooleanToStringConverter.class)
    private boolean erUtenlandskInntekt;

    @Convert(converter = ArbeidType.KodeverdiConverter.class)
    @ChangeTracked
    private ArbeidType arbeidType;

    private OppgittUtenlandskVirksomhet utenlandskVirksomhet = new OppgittUtenlandskVirksomhet();

    public OppgittArbeidsforhold() {
        // hibernate
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(periode, utenlandskVirksomhet, arbeidType);
    }

    public LocalDate getFraOgMed() {
        return periode.getFomDato();
    }

    public LocalDate getTilOgMed() {
        return periode.getTomDato();
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public Boolean erUtenlandskInntekt() {
        return erUtenlandskInntekt;
    }

    public ArbeidType getArbeidType() {
        return arbeidType;
    }

    public OppgittUtenlandskVirksomhet getUtenlandskVirksomhet() {
        return utenlandskVirksomhet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OppgittArbeidsforhold that)) {
            return false;
        }

        return Objects.equals(periode, that.periode) &&
                Objects.equals(arbeidType, that.arbeidType) &&
                Objects.equals(utenlandskVirksomhet, that.utenlandskVirksomhet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, arbeidType, utenlandskVirksomhet);
    }

    @Override
    public String toString() {
        return "OppgittArbeidsforholdImpl{" +
                "periode=" + periode +
                ", erUtenlandskInntekt=" + erUtenlandskInntekt +
                ", arbeidType=" + arbeidType +
                ", utenlandskVirksomhet=" + utenlandskVirksomhet +
                '}';
    }

    void setPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    void setErUtenlandskInntekt(Boolean erUtenlandskInntekt) {
        this.erUtenlandskInntekt = erUtenlandskInntekt;
    }

    void setArbeidType(ArbeidType arbeidType) {
        this.arbeidType = arbeidType;
    }

    void setUtenlandskVirksomhet(OppgittUtenlandskVirksomhet utenlandskVirksomhet) {
        this.utenlandskVirksomhet = utenlandskVirksomhet;
    }
}
