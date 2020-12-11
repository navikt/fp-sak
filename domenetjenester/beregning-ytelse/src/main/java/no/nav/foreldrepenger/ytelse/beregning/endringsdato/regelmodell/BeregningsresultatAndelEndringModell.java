package no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class BeregningsresultatAndelEndringModell {
    private final AktivitetStatus aktivitetStatus;
    private final Inntektskategori inntektskategori;
    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef arbeidsforholdReferanse;
    private final boolean brukerErMottaker;
    private final int dagsats;

    public BeregningsresultatAndelEndringModell(AktivitetStatus aktivitetStatus,
                                                Inntektskategori inntektskategori,
                                                Arbeidsgiver arbeidsgiver,
                                                InternArbeidsforholdRef arbeidsforholdReferanse,
                                                boolean brukerErMottaker,
                                                int dagsats) {
        this.aktivitetStatus = aktivitetStatus;
        this.inntektskategori = inntektskategori;
        this.arbeidsgiver = arbeidsgiver;
        this.brukerErMottaker = brukerErMottaker;
        this.dagsats = dagsats;
        this.arbeidsforholdReferanse = arbeidsforholdReferanse;
    }

    public BeregningsresultatAndelEndringModell(AktivitetStatus aktivitetStatus,
                                                Inntektskategori inntektskategori,
                                                boolean brukerErMottaker,
                                                int dagsats) {
        this.aktivitetStatus = aktivitetStatus;
        this.inntektskategori = inntektskategori;
        this.brukerErMottaker = brukerErMottaker;
        this.dagsats = dagsats;
    }


    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public boolean erBrukerMottaker() {
        return brukerErMottaker;
    }

    public int getDagsats() {
        return dagsats;
    }

    public InternArbeidsforholdRef getArbeidsforholdReferanse() {
        return arbeidsforholdReferanse;
    }

    @Override
    public String toString() {
        return "BeregningsresultatAndelEndringModell{" +
            "aktivitetStatus=" + aktivitetStatus +
            ", inntektskategori=" + inntektskategori +
            ", arbeidsgiver=" + arbeidsgiver +
            ", arbeidsforholdReferanse=" + arbeidsforholdReferanse +
            ", brukerErMottaker=" + brukerErMottaker +
            ", dagsats=" + dagsats +
            '}';
    }
}
