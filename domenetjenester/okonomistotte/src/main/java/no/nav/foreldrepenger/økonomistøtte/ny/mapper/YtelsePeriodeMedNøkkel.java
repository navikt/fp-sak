package no.nav.foreldrepenger.økonomistøtte.ny.mapper;

import no.nav.foreldrepenger.økonomistøtte.ny.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Periode;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.YtelsePeriode;

public class YtelsePeriodeMedNøkkel {
    private KjedeNøkkel nøkkel;
    private YtelsePeriode ytelsePeriode;

    public YtelsePeriodeMedNøkkel(KjedeNøkkel nøkkel, YtelsePeriode ytelsePeriode) {
        this.nøkkel = nøkkel;
        this.ytelsePeriode = ytelsePeriode;
    }

    public KjedeNøkkel getNøkkel() {
        return nøkkel;
    }

    public YtelsePeriode getYtelsePeriode() {
        return ytelsePeriode;
    }

    public Periode getTidsperiode() {
        return ytelsePeriode.getPeriode();
    }
}
