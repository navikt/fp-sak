package no.nav.foreldrepenger.domene.iay.modell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.NaturalYtelseType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;

public class NaturalYtelse implements IndexKey {

    @ChangeTracked @NotNull
    private DatoIntervallEntitet periode;

    @ChangeTracked @NotNull
    private Beløp beloepPerMnd;

    @NotNull private NaturalYtelseType type = NaturalYtelseType.UDEFINERT;

    NaturalYtelse() {
    }

    public NaturalYtelse(LocalDate fom, LocalDate tom, BigDecimal beloepPerMnd, NaturalYtelseType type) {
        this.beloepPerMnd = new Beløp(beloepPerMnd);
        this.type = type;
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    public NaturalYtelse(DatoIntervallEntitet datoIntervall, BigDecimal beloepPerMnd, NaturalYtelseType type) {
        this.beloepPerMnd = new Beløp(beloepPerMnd);
        this.type = type;
        this.periode = datoIntervall;
    }

    NaturalYtelse(NaturalYtelse naturalYtelse) {
        this.periode = naturalYtelse.getPeriode();
        this.beloepPerMnd = naturalYtelse.getBeloepPerMnd();
        this.type = naturalYtelse.getType();
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(type, periode);
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public Beløp getBeloepPerMnd() {
        return beloepPerMnd;
    }

    public NaturalYtelseType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NaturalYtelse that)) {
            return false;
        }
        return Objects.equals(periode, that.periode) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, type);
    }

    @Override
    public String toString() {
        return "NaturalYtelseEntitet{" +
                "periode=" + periode +
                ", beloepPerMnd=" + beloepPerMnd +
                ", type=" + type +
                '}';
    }
}
