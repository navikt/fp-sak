package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk;


import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.modell.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.Inntektskategori;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

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

    public void lagHistorikk(HistorikkInnslagTekstBuilder tekstBuilder,
                             List<Lønnsendring> lønnsendringList,
                             InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer = inntektArbeidYtelseGrunnlag.getArbeidsforholdOverstyringer();
        lønnsendringList.forEach(lønnsendring -> {
                lagHistorikkForInntektEndring(tekstBuilder, lønnsendring, arbeidsforholdOverstyringer);
                lagInntektskategoriInnslagHvisEndret(tekstBuilder, lønnsendring);
            }
        );
    }

    private void lagHistorikkForInntektEndring(HistorikkInnslagTekstBuilder tekstBuilder, Lønnsendring lønnsendring, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        Integer nyArbeidsinntekt = lønnsendring.getNyArbeidsinntekt();
        Integer gammelArbeidsinntekt = lønnsendring.getGammelArbeidsinntekt();
        if (nyArbeidsinntekt != null && !nyArbeidsinntekt.equals(gammelArbeidsinntekt)) {
            if (AktivitetStatus.FRILANSER.equals(lønnsendring.getAktivitetStatus())) {
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FRILANS_INNTEKT, gammelArbeidsinntekt, nyArbeidsinntekt);
                settSkjermlenkeOgFerdigstill(tekstBuilder);
            } else {
                String arbeidsforholdInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(lønnsendring.getAktivitetStatus(),
                    lønnsendring.getArbeidsgiver(),
                    lønnsendring.getArbeidsforholdRef(),
                    arbeidsforholdOverstyringer);
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD, arbeidsforholdInfo, gammelArbeidsinntekt, nyArbeidsinntekt);
                settSkjermlenkeOgFerdigstill(tekstBuilder);
            }
        }
    }

    private void lagInntektskategoriInnslagHvisEndret(HistorikkInnslagTekstBuilder historikkBuilder, Lønnsendring endring) {
        Inntektskategori nyInntektskategori = endring.getNyInntektskategori();
        if (nyInntektskategori != null && !nyInntektskategori.equals(endring.getGammelInntektskategori())) {
            historikkBuilder.medEndretFelt(HistorikkEndretFeltType.INNTEKTSKATEGORI, null, nyInntektskategori);
            settSkjermlenkeOgFerdigstill(historikkBuilder);
        }
    }

    private void settSkjermlenkeOgFerdigstill(HistorikkInnslagTekstBuilder tekstBuilder) {
        boolean erSkjermlenkeSatt = tekstBuilder.getHistorikkinnslagDeler().stream().anyMatch(historikkDel -> historikkDel.getSkjermlenke().isPresent());
        if (!erSkjermlenkeSatt) {
            tekstBuilder.medSkjermlenke(SkjermlenkeType.FAKTA_OM_BEREGNING);
        }
        tekstBuilder.ferdigstillHistorikkinnslagDel();
    }


}
