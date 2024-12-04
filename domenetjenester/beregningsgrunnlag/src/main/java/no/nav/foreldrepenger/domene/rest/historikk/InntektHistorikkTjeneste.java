package no.nav.foreldrepenger.domene.rest.historikk;

import static no.nav.foreldrepenger.domene.rest.historikk.FordelBeregningsgrunnlagHistorikkUtil.fraInntektskategori;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
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

    public List<HistorikkinnslagLinjeBuilder> lagHistorikk(List<Lønnsendring> lønnsendringList,
                                                           InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        var arbeidsforholdOverstyringer = inntektArbeidYtelseGrunnlag.getArbeidsforholdOverstyringer();
        List<HistorikkinnslagLinjeBuilder> linjeBuilder = new ArrayList<>();
        lønnsendringList.forEach(lønnsendring -> {
            linjeBuilder.add(lagHistorikkForInntektEndring(lønnsendring, arbeidsforholdOverstyringer));
            linjeBuilder.add(lagInntektskategoriInnslagHvisEndret(lønnsendring));
            linjeBuilder.add(HistorikkinnslagLinjeBuilder.LINJESKIFT);
        });
        return linjeBuilder;
    }

    private HistorikkinnslagLinjeBuilder lagHistorikkForInntektEndring(Lønnsendring lønnsendring,
                                                                       List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        var nyArbeidsinntekt = lønnsendring.getNyArbeidsinntekt();
        var gammelArbeidsinntekt = lønnsendring.getGammelArbeidsinntekt();
        HistorikkinnslagLinjeBuilder linjeBuilder = new HistorikkinnslagLinjeBuilder();
        if (nyArbeidsinntekt != null && !nyArbeidsinntekt.equals(gammelArbeidsinntekt)) {
            if (AktivitetStatus.FRILANSER.equals(lønnsendring.getAktivitetStatus())) {
                linjeBuilder.fraTil("Frilansinntekt", gammelArbeidsinntekt, nyArbeidsinntekt);
            } else {
                var arbeidsforholdInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(
                    lønnsendring.getAktivitetStatus(), lønnsendring.getArbeidsgiver(), lønnsendring.getArbeidsforholdRef(),
                    arbeidsforholdOverstyringer);
                linjeBuilder.fraTil("Inntekt fra " + arbeidsforholdInfo, gammelArbeidsinntekt, nyArbeidsinntekt);
            }
        }
        return linjeBuilder;
    }

    private HistorikkinnslagLinjeBuilder lagInntektskategoriInnslagHvisEndret(Lønnsendring endring) {
        var nyInntektskategori = endring.getNyInntektskategori();
        if (nyInntektskategori != null && !nyInntektskategori.equals(endring.getGammelInntektskategori())) {
            return new HistorikkinnslagLinjeBuilder().fraTil("Inntektskategori", null, fraInntektskategori(nyInntektskategori));
        }
        return null;
    }


}
