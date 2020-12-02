package no.nav.foreldrepenger.domene.arbeidsforhold.svp;

import java.math.BigDecimal;

import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class PeriodeMedUtbetalingsgrad {
    private DatoIntervallEntitet periode;
    private BigDecimal utbetalingsgrad;

    public PeriodeMedUtbetalingsgrad(DatoIntervallEntitet periode, BigDecimal utbetalingsgrad) {
        this.periode = periode;
        this.utbetalingsgrad = utbetalingsgrad;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }
}
