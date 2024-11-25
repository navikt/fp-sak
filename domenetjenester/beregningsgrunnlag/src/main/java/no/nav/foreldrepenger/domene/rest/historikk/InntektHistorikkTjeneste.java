package no.nav.foreldrepenger.domene.rest.historikk;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;

@ApplicationScoped
public class InntektHistorikkTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;

    public InntektHistorikkTjeneste() {
        // For CDI
    }

    @Inject
    public InntektHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste) {
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
    }

    public List<HistorikkinnslagTekstlinjeBuilder> lagHistorikk(List<Lønnsendring> lønnsendringList,
                                                                InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        var arbeidsforholdOverstyringer = inntektArbeidYtelseGrunnlag.getArbeidsforholdOverstyringer();
        List<HistorikkinnslagTekstlinjeBuilder> tekstlinjerBuilder = new ArrayList<>();
        lønnsendringList.forEach(lønnsendring -> {
            tekstlinjerBuilder.add(lagHistorikkForInntektEndring(lønnsendring, arbeidsforholdOverstyringer));
            tekstlinjerBuilder.add(lagInntektskategoriInnslagHvisEndret(lønnsendring));
            tekstlinjerBuilder.add(new HistorikkinnslagTekstlinjeBuilder().linjeskift());
        });
        return tekstlinjerBuilder;
    }

    private HistorikkinnslagTekstlinjeBuilder lagHistorikkForInntektEndring(Lønnsendring lønnsendring,
                                                                            List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        var nyArbeidsinntekt = lønnsendring.getNyArbeidsinntekt();
        var gammelArbeidsinntekt = lønnsendring.getGammelArbeidsinntekt();
        HistorikkinnslagTekstlinjeBuilder tekstlinjeBuilder = new HistorikkinnslagTekstlinjeBuilder();
        if (nyArbeidsinntekt != null && !nyArbeidsinntekt.equals(gammelArbeidsinntekt)) {
            if (AktivitetStatus.FRILANSER.equals(lønnsendring.getAktivitetStatus())) {
                tekstlinjeBuilder.fraTil("Frilansinntekt", gammelArbeidsinntekt, nyArbeidsinntekt);
            } else {
                var arbeidsforholdInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(
                    lønnsendring.getAktivitetStatus(), lønnsendring.getArbeidsgiver(), lønnsendring.getArbeidsforholdRef(),
                    arbeidsforholdOverstyringer);
                tekstlinjeBuilder.fraTil("Inntekt fra " + arbeidsforholdInfo, gammelArbeidsinntekt, nyArbeidsinntekt);
            }
        }
        return tekstlinjeBuilder;
    }

    private HistorikkinnslagTekstlinjeBuilder lagInntektskategoriInnslagHvisEndret(Lønnsendring endring) {
        var nyInntektskategori = endring.getNyInntektskategori();
        if (nyInntektskategori != null && !nyInntektskategori.equals(endring.getGammelInntektskategori())) {
            return new HistorikkinnslagTekstlinjeBuilder().fraTil("Inntektskategori", null, nyInntektskategori);
        }
        return null;
    }


}
