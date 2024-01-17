package no.nav.foreldrepenger.domene.iay.modell;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class BekreftetPermisjon {

    private BekreftetPermisjonStatus status = BekreftetPermisjonStatus.UDEFINERT;

    @ChangeTracked
    private DatoIntervallEntitet periode;

    BekreftetPermisjon() {
    }

    public BekreftetPermisjon(BekreftetPermisjonStatus status) {
        this.status = status;
    }

    public BekreftetPermisjon(LocalDate permisjonFom, LocalDate permisjonTom, BekreftetPermisjonStatus status) {
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(permisjonFom, permisjonTom);
        this.status = status;
    }

    public BekreftetPermisjon(BekreftetPermisjon bekreftetPermisjon) {
        this.periode = bekreftetPermisjon.getPeriode();
        this.status = bekreftetPermisjon.getStatus();
    }

    public BekreftetPermisjonStatus getStatus() {
        return status;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BekreftetPermisjon that)) {
            return false;
        }
        return Objects.equals(periode, that.periode)
                && Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, status);
    }

    @Override
    public String toString() {
        return "BekreftetPermisjon<" +
                "periode=" + periode +
                ", status=" + status +
                '>';
    }

}
