package no.nav.foreldrepenger.domene.iay.modell;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsmeldingInnsendingsårsak;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

public class RefusjonskravDato {

    private Arbeidsgiver arbeidsgiver;

    private LocalDate førsteDagMedRefusjonskrav;

    private LocalDate førsteInnsendingAvRefusjonskrav;

    RefusjonskravDato() {
    }

    public RefusjonskravDato(Arbeidsgiver arbeidsgiver, LocalDate førsteDagMedRefusjonskrav, LocalDate førsteInnsendingAvRefusjonskrav) {
        this.arbeidsgiver = arbeidsgiver;
        this.førsteDagMedRefusjonskrav = førsteDagMedRefusjonskrav;
        this.førsteInnsendingAvRefusjonskrav = førsteInnsendingAvRefusjonskrav;
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
}
