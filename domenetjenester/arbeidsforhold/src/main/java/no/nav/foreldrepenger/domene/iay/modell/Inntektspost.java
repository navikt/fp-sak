package no.nav.foreldrepenger.domene.iay.modell;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.SkatteOgAvgiftsregelType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;

public class Inntektspost implements IndexKey {

    @ChangeTracked
    private InntektspostType inntektspostType;

    @ChangeTracked
    private SkatteOgAvgiftsregelType skatteOgAvgiftsregelType = SkatteOgAvgiftsregelType.UDEFINERT;

    private Inntekt inntekt;

    @ChangeTracked
    private DatoIntervallEntitet periode;

    @ChangeTracked
    private Beløp beløp;

    @ChangeTracked
    private InntektYtelseType inntektYtelseType;

    public Inntektspost() {
        // hibernate
    }

    /**
     * Deep copy.
     */
    Inntektspost(Inntektspost inntektspost) {
        this.inntektspostType = inntektspost.getInntektspostType();
        this.skatteOgAvgiftsregelType = inntektspost.getSkatteOgAvgiftsregelType();
        this.periode = inntektspost.getPeriode();
        this.beløp = inntektspost.getBeløp();
        this.inntektYtelseType = inntektspost.getInntektYtelseType();
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(getInntektspostType(), getInntektYtelseType(), getSkatteOgAvgiftsregelType(), periode);
    }

    /**
     * Underkategori av utbetaling
     * <p>
     * F.eks
     * <ul>
     * <li>Lønn</li>
     * <li>Ytelse</li>
     * <li>Næringsinntekt</li>
     * </ul>
     *
     * @return {@link InntektspostType}
     */
    public InntektspostType getInntektspostType() {
        return inntektspostType;
    }

    void setInntektspostType(InntektspostType inntektspostType) {
        this.inntektspostType = inntektspostType;
    }

    /**
     * En kodeverksverdi som angir særskilt beskatningsregel. Den er ikke alltid
     * satt, og kommer fra inntektskomponenten
     *
     * @return {@link SkatteOgAvgiftsregelType}
     */
    public SkatteOgAvgiftsregelType getSkatteOgAvgiftsregelType() {
        return skatteOgAvgiftsregelType;
    }

    void setSkatteOgAvgiftsregelType(SkatteOgAvgiftsregelType skatteOgAvgiftsregelType) {
        this.skatteOgAvgiftsregelType = skatteOgAvgiftsregelType;
    }

    void setPeriode(LocalDate fom, LocalDate tom) {
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    /**
     * Beløpet som har blitt utbetalt i perioden
     *
     * @return Beløpet
     */
    public Beløp getBeløp() {
        return beløp;
    }

    void setBeløp(Beløp beløp) {
        this.beløp = beløp;
    }

    public Inntekt getInntekt() {
        return inntekt;
    }

    void setInntekt(Inntekt inntekt) {
        this.inntekt = inntekt;
    }

    public InntektYtelseType getInntektYtelseType() {
        return inntektYtelseType;
    }

    public void setInntektYtelseType(InntektYtelseType inntektYtelseType) {
        this.inntektYtelseType = inntektYtelseType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Inntektspost other)) {
            return false;
        }
        return Objects.equals(this.getInntektspostType(), other.getInntektspostType())
                && Objects.equals(this.getInntektYtelseType(), other.getInntektYtelseType())
                && Objects.equals(this.getSkatteOgAvgiftsregelType(), other.getSkatteOgAvgiftsregelType())
                && Objects.equals(this.getPeriode().getFomDato(), other.getPeriode().getFomDato())
                && Objects.equals(this.getPeriode().getTomDato(), other.getPeriode().getTomDato());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInntektspostType(), getInntektYtelseType(), getSkatteOgAvgiftsregelType(), getPeriode().getFomDato(),
                getPeriode().getTomDato());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
                "inntektYtelseType=" + inntektYtelseType +
                "inntektspostType=" + inntektspostType +
                "skatteOgAvgiftsregelType=" + skatteOgAvgiftsregelType +
                ", fraOgMed=" + periode.getFomDato() +
                ", tilOgMed=" + periode.getTomDato() +
                ", beløp=" + beløp +
                '>';
    }

    public boolean hasValues() {
        return inntektspostType != null || periode.getFomDato() != null || periode.getTomDato() != null || beløp != null;
    }

}
