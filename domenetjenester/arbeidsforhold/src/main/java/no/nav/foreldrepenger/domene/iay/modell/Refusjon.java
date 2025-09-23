package no.nav.foreldrepenger.domene.iay.modell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.domene.typer.Beløp;

public class Refusjon implements IndexKey {

    @ChangeTracked
    private Beløp refusjonsbeløpMnd;

    @ChangeTracked @NotNull
    private LocalDate fom;

    public Refusjon() {
    }

    public Refusjon(BigDecimal refusjonsbeløpMnd, LocalDate fom) {
        this.refusjonsbeløpMnd = refusjonsbeløpMnd == null ? null : new Beløp(refusjonsbeløpMnd);
        this.fom = fom;
    }

    Refusjon(Refusjon refusjon) {
        this.refusjonsbeløpMnd = refusjon.getRefusjonsbeløp();
        this.fom = refusjon.getFom();
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(fom, refusjonsbeløpMnd);
    }

    public Beløp getRefusjonsbeløp() {
        return refusjonsbeløpMnd;
    }

    public LocalDate getFom() {
        return fom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Refusjon that)) {
            return false;
        }
        return Objects.equals(refusjonsbeløpMnd, that.refusjonsbeløpMnd) &&
                Objects.equals(fom, that.fom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(refusjonsbeløpMnd, fom);
    }
}
