package no.nav.foreldrepenger.domene.iay.modell;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

/**
 * Entitetsklasse for oppgitte arbeidsforhold.
 * <p>
 * Implementert iht. builder pattern (ref. "Effective Java, 2. ed." J.Bloch).
 * Non-public constructors og setters, dvs. immutable.
 * <p>
 * OBS: Legger man til nye felter så skal dette oppdateres mange steder:
 * builder, equals, hashcode etc.
 */
public class OppgittArbeidsforhold implements IndexKey {

    @ChangeTracked
    private DatoIntervallEntitet periode;

    @ChangeTracked
    private boolean erUtenlandskInntekt;

    @ChangeTracked
    private ArbeidType arbeidType;

    private OppgittUtenlandskVirksomhet utenlandskVirksomhet = new OppgittUtenlandskVirksomhet();

    public OppgittArbeidsforhold() {
        // hibernate
    }

    public OppgittArbeidsforhold(OppgittArbeidsforhold oppgittArbeidsforhold) {
        this.periode = oppgittArbeidsforhold.periode;
        this.erUtenlandskInntekt = oppgittArbeidsforhold.erUtenlandskInntekt;
        this.arbeidType = oppgittArbeidsforhold.arbeidType;
        this.utenlandskVirksomhet =
            oppgittArbeidsforhold.utenlandskVirksomhet != null ? new OppgittUtenlandskVirksomhet(oppgittArbeidsforhold.utenlandskVirksomhet) : null;
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

        return Objects.equals(periode, that.periode) && Objects.equals(arbeidType, that.arbeidType) && Objects.equals(utenlandskVirksomhet,
            that.utenlandskVirksomhet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, arbeidType, utenlandskVirksomhet);
    }

    @Override
    public String toString() {
        return "OppgittArbeidsforholdImpl{" + "periode=" + periode + ", erUtenlandskInntekt=" + erUtenlandskInntekt + ", arbeidType=" + arbeidType
            + ", utenlandskVirksomhet=" + utenlandskVirksomhet + '}';
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
