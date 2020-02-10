package no.nav.foreldrepenger.domene.iay.modell;

import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektPeriodeType;
import no.nav.foreldrepenger.domene.typer.Beløp;

public class YtelseStørrelse extends BaseEntitet implements IndexKey {

    @ChangeTracked
    private InntektPeriodeType hyppighet = InntektPeriodeType.UDEFINERT;

    @ChangeTracked
    private OrgNummer virksomhetOrgnr;

    @ChangeTracked
    private Beløp beløp;

    public YtelseStørrelse() {
        // hibernate
    }

    public YtelseStørrelse(YtelseStørrelse ytelseStørrelse) {
        ytelseStørrelse.getVirksomhet().ifPresent(tidligereVirksomhet -> this.virksomhetOrgnr = tidligereVirksomhet);
        this.beløp = ytelseStørrelse.getBeløp();
        this.hyppighet = ytelseStørrelse.getHyppighet();
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(virksomhetOrgnr);
    }

    public Optional<String> getOrgnr() {
        return Optional.ofNullable(virksomhetOrgnr == null ? null : virksomhetOrgnr.getId());
    }

    /**
     * Returner orgnr dersom virksomhet. Null ellers.
     *
     * @see #getVirksomhet()
     */
    public OrgNummer getVirksomhetOrgnr() {
        return virksomhetOrgnr;
    }

    public Optional<OrgNummer> getVirksomhet() {
        return Optional.ofNullable(virksomhetOrgnr);
    }

    public Beløp getBeløp() {
        return beløp;
    }

    public InntektPeriodeType getHyppighet() {
        return hyppighet;
    }

    void setVirksomhet(OrgNummer virksomhetOrgnr) {
        this.virksomhetOrgnr = virksomhetOrgnr;
    }

    void setBeløp(Beløp beløp) {
        this.beløp = beløp;
    }

    void setHyppighet(InntektPeriodeType hyppighet) {
        this.hyppighet = hyppighet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof YtelseStørrelse))
            return false;
        YtelseStørrelse that = (YtelseStørrelse) o;
        return Objects.equals(virksomhetOrgnr, that.virksomhetOrgnr) &&
            Objects.equals(beløp, that.beløp) &&
            Objects.equals(hyppighet, that.hyppighet);
    }

    @Override
    public int hashCode() {

        return Objects.hash(virksomhetOrgnr, beløp, hyppighet);
    }

    @Override
    public String toString() {
        return "YtelseStørrelseEntitet{" +
            "virksomhet=" + virksomhetOrgnr +
            ", beløp=" + beløp +
            ", hyppighet=" + hyppighet +
            '}';
    }

    boolean hasValues() {
        return beløp != null || hyppighet != null || virksomhetOrgnr != null;
    }
}
