package no.nav.foreldrepenger.domene.arbeidsforhold.svp;

import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;

public class Tilrettleggingsperiode {

    private Arbeidsgiver arbeidsgiver;
    private UttakArbeidType uttakArbeidType;
    private List<PeriodeMedUtbetalingsgrad> periodeMedUtbetalingsgrad;

    Tilrettleggingsperiode(Arbeidsgiver arbeidsgiver, UttakArbeidType uttakArbeidType, List<PeriodeMedUtbetalingsgrad> periodeMedUtbetalingsgrad) {
        this.arbeidsgiver = arbeidsgiver;
        this.uttakArbeidType = uttakArbeidType;
        this.periodeMedUtbetalingsgrad = periodeMedUtbetalingsgrad;
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        return Optional.ofNullable(arbeidsgiver);
    }

    public UttakArbeidType getUttakArbeidType() {
        return uttakArbeidType;
    }

    public List<PeriodeMedUtbetalingsgrad> getPeriodeMedUtbetalingsgrad() {
        return periodeMedUtbetalingsgrad;
    }


}
