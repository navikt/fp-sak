package no.nav.foreldrepenger.domene.iay.modell;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;

public class RefusjonskravDato {

    private Arbeidsgiver arbeidsgiver;

    private LocalDate førsteDagMedRefusjonskrav;

    private LocalDate førsteInnsendingAvRefusjonskrav;

    private boolean harRefusjonFraStart;

    RefusjonskravDato() {
    }

    public RefusjonskravDato(Arbeidsgiver arbeidsgiver, LocalDate førsteDagMedRefusjonskrav, LocalDate førsteInnsendingAvRefusjonskrav,
            boolean harRefusjonFraStart) {
        this.arbeidsgiver = arbeidsgiver;
        this.førsteDagMedRefusjonskrav = førsteDagMedRefusjonskrav;
        this.førsteInnsendingAvRefusjonskrav = førsteInnsendingAvRefusjonskrav;
        this.harRefusjonFraStart = harRefusjonFraStart;
    }

    public RefusjonskravDato(RefusjonskravDato refusjonskravDato) {
        this.arbeidsgiver = refusjonskravDato.getArbeidsgiver();
        this.førsteDagMedRefusjonskrav = refusjonskravDato.førsteDagMedRefusjonskrav;
        this.førsteInnsendingAvRefusjonskrav = refusjonskravDato.førsteInnsendingAvRefusjonskrav;
    }

    /**
     * Virksomheten som har sendt inn inntektsmeldingen
     *
     * @return {@link Virksomhet}
     */
    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public Optional<LocalDate> getFørsteDagMedRefusjonskrav() {
        return Optional.ofNullable(førsteDagMedRefusjonskrav);
    }

    public LocalDate getFørsteInnsendingAvRefusjonskrav() {
        return førsteInnsendingAvRefusjonskrav;
    }

    public boolean harRefusjonFraStart() {
        return harRefusjonFraStart;
    }
}
