package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import java.util.Optional;

import no.nav.foreldrepenger.historikk.VurderArbeidsforholdHistorikkinnslag;

final class UtledKoderForHistorikkinnslagdelerForArbeidsforholdUtenInnteksmelding {

    private UtledKoderForHistorikkinnslagdelerForArbeidsforholdUtenInnteksmelding() {
        // Skjul default constructor
    }

    static Optional<VurderArbeidsforholdHistorikkinnslag> utled(ArbeidsforholdDto arbeidsforholdDto) {
        if (arbeidsforholdDto.getOverstyrtTom() != null) {
            return Optional.of(VurderArbeidsforholdHistorikkinnslag.BRUK_MED_OVERSTYRTE_PERIODER);
        }
        if (Boolean.TRUE.equals(arbeidsforholdDto.getLagtTilAvSaksbehandler())) {
            return Optional.of(VurderArbeidsforholdHistorikkinnslag.LAGT_TIL_AV_SAKSBEHANDLER);
        }
        if (Boolean.FALSE.equals(arbeidsforholdDto.getInntektMedTilBeregningsgrunnlag())){
            return Optional.of(VurderArbeidsforholdHistorikkinnslag.INNTEKT_IKKE_MED_I_BG);
        }
        if (Boolean.FALSE.equals(arbeidsforholdDto.getFortsettBehandlingUtenInntektsmelding())) {
            return Optional.of(VurderArbeidsforholdHistorikkinnslag.MANGLENDE_OPPLYSNINGER);
        }
        if (Boolean.TRUE.equals(arbeidsforholdDto.getFortsettBehandlingUtenInntektsmelding())) {
            return Optional.of(VurderArbeidsforholdHistorikkinnslag.BENYTT_A_INNTEKT_I_BG);
        }
        return Optional.empty();
    }

}
