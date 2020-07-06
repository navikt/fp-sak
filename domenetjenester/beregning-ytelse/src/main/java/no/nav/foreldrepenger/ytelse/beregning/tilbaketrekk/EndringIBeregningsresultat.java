package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

class EndringIBeregningsresultat {

    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef arbeidsforholdRef;
    private AktivitetStatus aktivitetStatus;
    private int dagsats;
    private int dagsatsFraBg;
    private Boolean brukerErMottaker;
    private Integer originalDagsats;
    private Inntektskategori inntektskategori;

    private EndringIBeregningsresultat(Arbeidsgiver arbeidsgiver,
                                       InternArbeidsforholdRef arbeidsforholdRef,
                                       AktivitetStatus aktivitetStatus,
                                       Inntektskategori inntektskategori,
                                       int dagsats,
                                       int dagsatsFraBg,
                                       Boolean brukerErMottaker,
                                       Integer originalDagsats) {
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdRef = arbeidsforholdRef == null ? InternArbeidsforholdRef.nullRef() : arbeidsforholdRef;
        this.aktivitetStatus = aktivitetStatus;
        this.dagsats = dagsats;
        this.dagsatsFraBg = dagsatsFraBg;
        this.brukerErMottaker = brukerErMottaker;
        this.originalDagsats = originalDagsats;
        this.inntektskategori = inntektskategori;
    }

    EndringIBeregningsresultat(BeregningsresultatAndel beregningsresultatAndel, int nydagsats) {
        this(
            beregningsresultatAndel.getArbeidsgiver().orElse(null),
            beregningsresultatAndel.getArbeidsforholdRef(),
            beregningsresultatAndel.getAktivitetStatus(),
            beregningsresultatAndel.getInntektskategori(),
            nydagsats,
            beregningsresultatAndel.getDagsatsFraBg(),
            beregningsresultatAndel.erBrukerMottaker(),
            null
        );
    }

    EndringIBeregningsresultat(BeregningsresultatAndel beregningsresultatAndel, BeregningsresultatAndel originalResultatAndel) {
        this(
            beregningsresultatAndel.getArbeidsgiver().orElse(null),
            beregningsresultatAndel.getArbeidsforholdRef(),
            beregningsresultatAndel.getAktivitetStatus(),
            beregningsresultatAndel.getInntektskategori(),
            beregningsresultatAndel.getDagsats(),
            beregningsresultatAndel.getDagsatsFraBg(),
            beregningsresultatAndel.erBrukerMottaker(),
            originalResultatAndel.getDagsats()
        );
    }

    EndringIBeregningsresultat(BeregningsresultatAndel beregningsresultatAndel) {
        this(
            beregningsresultatAndel.getArbeidsgiver().orElse(null),
            beregningsresultatAndel.getArbeidsforholdRef(),
            beregningsresultatAndel.getAktivitetStatus(),
            beregningsresultatAndel.getInntektskategori(),
            beregningsresultatAndel.getDagsats(),
            beregningsresultatAndel.getDagsatsFraBg(),
            beregningsresultatAndel.erBrukerMottaker(),
            null
        );
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public int getDagsats() {
        return dagsats;
    }

    public int getDagsatsFraBg() {
        return dagsatsFraBg;
    }

    public Boolean erBrukerMottaker() {
        return brukerErMottaker;
    }

    public void setDagsats(int dagsats) {
        this.dagsats = dagsats;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public boolean gjelderResultatAndel(BeregningsresultatAndel resultatAndel) {
        if (!resultatAndel.getAktivitetStatus().equals(aktivitetStatus) || resultatAndel.erBrukerMottaker() != brukerErMottaker) {
            return false;
        }
        if(resultatAndel.getArbeidsgiver().isPresent()) {
            boolean sammeAG = resultatAndel.getArbeidsgiver().get().equals(arbeidsgiver) && Objects.equals(arbeidsforholdRef.getReferanse(), resultatAndel.getArbeidsforholdRef().getReferanse());
            if (sammeAG) {
                return inntektskategori.equals(resultatAndel.getInntektskategori());
            } else {
                return false;
            }
        }
        return true;
    }

    int finnResterendeTilbaketrekk() {
        return originalDagsats - dagsats;
    }

    static EndringIBeregningsresultat forEndringMedOriginalDagsats(BeregningsresultatAndel beregningsresultatAndel, int originalDagsats) {
        return new EndringIBeregningsresultat(beregningsresultatAndel.getArbeidsgiver().orElse(null),
            beregningsresultatAndel.getArbeidsforholdRef(),
            beregningsresultatAndel.getAktivitetStatus(),
            beregningsresultatAndel.getInntektskategori(),
            beregningsresultatAndel.getDagsats(),
            beregningsresultatAndel.getDagsatsFraBg(),
            beregningsresultatAndel.erBrukerMottaker(),
            originalDagsats);
    }
}
