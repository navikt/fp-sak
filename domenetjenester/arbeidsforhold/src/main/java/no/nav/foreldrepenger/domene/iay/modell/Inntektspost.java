package no.nav.foreldrepenger.domene.iay.modell;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.*;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class Inntektspost extends BaseEntitet implements IndexKey {

    private static final Map<String, Map<String, ? extends YtelseType>> YTELSE_TYPER = new LinkedHashMap<>();

    static {
        YTELSE_TYPER.put(OffentligYtelseType.KODEVERK, OffentligYtelseType.kodeMap());
        YTELSE_TYPER.put(NæringsinntektType.KODEVERK, NæringsinntektType.kodeMap());
        YTELSE_TYPER.put(PensjonTrygdType.KODEVERK, PensjonTrygdType.kodeMap());

    }
    private InntektspostType inntektspostType;

    private SkatteOgAvgiftsregelType skatteOgAvgiftsregelType = SkatteOgAvgiftsregelType.UDEFINERT;

    private Inntekt inntekt;

    /**
     * Brukes kun til FK validering. Default OffentligYtelseType. Må settes sammen
     * med {@link #ytelse}
     */
    private String ytelseType = OffentligYtelseType.KODEVERK;

    private String ytelse = OffentligYtelseType.UDEFINERT.getKode();

    private DatoIntervallEntitet periode;

    @ChangeTracked
    private Beløp beløp;

    public Inntektspost() {
        // hibernate
    }

    /**
     * Deep copy.
     */
    Inntektspost(Inntektspost inntektspost) {
        this.inntektspostType = inntektspost.getInntektspostType();
        this.skatteOgAvgiftsregelType = inntektspost.getSkatteOgAvgiftsregelType();
        this.ytelse = inntektspost.getYtelseType().getKode();
        this.periode = inntektspost.getPeriode();
        this.beløp = inntektspost.getBeløp();
        this.ytelseType = inntektspost.getYtelseType().getKodeverk();
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(getInntektspostType(), getYtelseType().getKodeverk(), getYtelseType().getKode(), getSkatteOgAvgiftsregelType(),
                periode);
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

    public YtelseType getYtelseType() {
        var yt = YTELSE_TYPER.getOrDefault(ytelseType, Collections.emptyMap()).get(ytelse);
        return yt != null ? yt : OffentligYtelseType.UDEFINERT;
    }

    public Inntekt getInntekt() {
        return inntekt;
    }

    void setInntekt(Inntekt inntekt) {
        this.inntekt = inntekt;
    }

    void setYtelse(YtelseType ytelse) {
        this.ytelseType = ytelse.getKodeverk();
        this.ytelse = ytelse.getKode();
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
                && Objects.equals(this.getYtelseType(), other.getYtelseType())
                && Objects.equals(this.getSkatteOgAvgiftsregelType(), other.getSkatteOgAvgiftsregelType())
                && Objects.equals(this.getPeriode().getFomDato(), other.getPeriode().getFomDato())
                && Objects.equals(this.getPeriode().getTomDato(), other.getPeriode().getTomDato());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInntektspostType(), getYtelseType(), getSkatteOgAvgiftsregelType(), getPeriode().getFomDato(),
                getPeriode().getTomDato());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
                "ytelseType=" + ytelseType +
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
