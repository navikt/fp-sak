package no.nav.foreldrepenger.domene.arbeidsforhold.svp;

import java.util.List;

public class TilretteleggingMedUtbelingsgrad {

    private TilretteleggingArbeidsforhold tilretteleggingArbeidsforhold;
    private List<PeriodeMedUtbetalingsgrad> periodeMedUtbetalingsgrad;

    public TilretteleggingMedUtbelingsgrad(TilretteleggingArbeidsforhold tilretteleggingArbeidsforhold,
                                           List<PeriodeMedUtbetalingsgrad> periodeMedUtbetalingsgrad) {
        this.tilretteleggingArbeidsforhold = tilretteleggingArbeidsforhold;
        this.periodeMedUtbetalingsgrad = periodeMedUtbetalingsgrad;
    }

    public List<PeriodeMedUtbetalingsgrad> getPeriodeMedUtbetalingsgrad() {
        return periodeMedUtbetalingsgrad;
    }

    public TilretteleggingArbeidsforhold getTilretteleggingArbeidsforhold() {
        return tilretteleggingArbeidsforhold;
    }

}
