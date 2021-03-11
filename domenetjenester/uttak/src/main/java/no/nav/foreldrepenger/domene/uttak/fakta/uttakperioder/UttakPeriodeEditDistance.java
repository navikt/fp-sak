package no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.domene.uttak.fakta.wagnerfisher.EditDistanceLetter;

public class UttakPeriodeEditDistance implements EditDistanceLetter {
    private final OppgittPeriodeEntitet periode;
    private Boolean periodeErDokumentert;

    public UttakPeriodeEditDistance(OppgittPeriodeEntitet periode) {
        Objects.requireNonNull(periode, "Periode");
        this.periode = periode;
    }

    @Override
    public int kostnadSettInn() {
        return 3;
    }

    @Override
    public int kostnadSlette() {
        return 2;
    }

    @Override
    public int kostnadEndre(EditDistanceLetter annen) {
        var annenUttakPeriode = (UttakPeriodeEditDistance) annen;
        if (!Objects.equals(periode.getÅrsak(), annenUttakPeriode.getPeriode().getÅrsak())) {
            return 6;
        }
        return 1;
    }

    @Override
    public boolean lik(EditDistanceLetter annen) {
        if (!(annen instanceof UttakPeriodeEditDistance)) {
            return false;
        }

        var annenUttakPeriode = (UttakPeriodeEditDistance) annen;
        return annenUttakPeriode.periode.equals(this.periode)
            && Objects.equals(annenUttakPeriode.periodeErDokumentert, this.periodeErDokumentert);
    }

    public static Builder builder(OppgittPeriodeEntitet periode) {
        return new Builder(periode);
    }

    public OppgittPeriodeEntitet getPeriode() {
        return periode;
    }

    public Boolean isPeriodeDokumentert() {
        return periodeErDokumentert;
    }

    public void setPeriodeErDokumentert(Boolean periodeErDokumentert) {
        this.periodeErDokumentert = periodeErDokumentert;
    }


    public static class Builder {
        private final UttakPeriodeEditDistance kladd;

        public Builder(OppgittPeriodeEntitet periode) {
            Objects.requireNonNull(periode, "Periode");
            kladd = new UttakPeriodeEditDistance(periode);
        }

        public Builder medPeriodeErDokumentert(boolean erDokumentert) {
            kladd.periodeErDokumentert = erDokumentert;
            return this;
        }

        public UttakPeriodeEditDistance build() {
            return kladd;
        }
    }
}
