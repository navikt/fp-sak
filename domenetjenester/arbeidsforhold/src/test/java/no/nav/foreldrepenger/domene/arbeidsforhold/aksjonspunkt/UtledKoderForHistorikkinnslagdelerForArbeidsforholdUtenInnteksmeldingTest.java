package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Test;

import no.nav.foreldrepenger.historikk.VurderArbeidsforholdHistorikkinnslag;

public class UtledKoderForHistorikkinnslagdelerForArbeidsforholdUtenInnteksmeldingTest {

    @Test
    public void skal_returne_BRUK_MED_OVERSTYRTE_PERIODER_når_arbeidsforholdet_har_en_overstyrt_tom() {
        // Arrange
        ArbeidsforholdDto arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setOverstyrtTom(LocalDate.now());
        arbeidsforholdDto.setFortsettBehandlingUtenInntektsmelding(true);
        // Act
        Optional<VurderArbeidsforholdHistorikkinnslag> kodeOpt = UtledKoderForHistorikkinnslagdelerForArbeidsforholdUtenInnteksmelding.utled(arbeidsforholdDto);
        // Assert
        assertThat(kodeOpt).hasValueSatisfying(kode ->
            assertThat(kode).isEqualTo(VurderArbeidsforholdHistorikkinnslag.BRUK_MED_OVERSTYRTE_PERIODER)
        );
    }

    @Test
    public void skal_returne_LAGT_TIL_AV_SAKSBEHANDLER_når_arbeidsforholdet_er_lagt_til_av_saksbehandler() {
        // Arrange
        ArbeidsforholdDto arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setLagtTilAvSaksbehandler(true);
        arbeidsforholdDto.setFortsettBehandlingUtenInntektsmelding(true);
        // Act
        Optional<VurderArbeidsforholdHistorikkinnslag> kodeOpt = UtledKoderForHistorikkinnslagdelerForArbeidsforholdUtenInnteksmelding.utled(arbeidsforholdDto);
        // Assert
        assertThat(kodeOpt).hasValueSatisfying(kode ->
            assertThat(kode).isEqualTo(VurderArbeidsforholdHistorikkinnslag.LAGT_TIL_AV_SAKSBEHANDLER)
        );
    }

    @Test
    public void skal_returne_INNTEKT_IKKE_MED_I_BG_når_arbeidsforholdet_har_inntekt_med_til_bg_satt_til_false() {
        // Arrange
        ArbeidsforholdDto arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setInntektMedTilBeregningsgrunnlag(false);
        arbeidsforholdDto.setFortsettBehandlingUtenInntektsmelding(true);
        // Act
        Optional<VurderArbeidsforholdHistorikkinnslag> kodeOpt = UtledKoderForHistorikkinnslagdelerForArbeidsforholdUtenInnteksmelding.utled(arbeidsforholdDto);
        // Assert
        assertThat(kodeOpt).hasValueSatisfying(kode ->
            assertThat(kode).isEqualTo(VurderArbeidsforholdHistorikkinnslag.INNTEKT_IKKE_MED_I_BG)
        );
    }

    @Test
    public void skal_returne_MANGLENDE_OPPLYSNINGER_når_arbeidsforholdet_ikke_skal_forsette_uten_innteksmelding() {
        // Arrange
        ArbeidsforholdDto arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setFortsettBehandlingUtenInntektsmelding(false);
        // Act
        Optional<VurderArbeidsforholdHistorikkinnslag> kodeOpt = UtledKoderForHistorikkinnslagdelerForArbeidsforholdUtenInnteksmelding.utled(arbeidsforholdDto);
        // Assert
        assertThat(kodeOpt).hasValueSatisfying(kode ->
            assertThat(kode).isEqualTo(VurderArbeidsforholdHistorikkinnslag.MANGLENDE_OPPLYSNINGER)
        );
    }

    @Test
    public void skal_returne_BENYTT_A_INNTEKT_I_BG_når_arbeidsforholdet_skal_forsette_uten_innteksmelding() {
        // Arrange
        ArbeidsforholdDto arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setFortsettBehandlingUtenInntektsmelding(true);
        // Act
        Optional<VurderArbeidsforholdHistorikkinnslag> kodeOpt = UtledKoderForHistorikkinnslagdelerForArbeidsforholdUtenInnteksmelding.utled(arbeidsforholdDto);
        // Assert
        assertThat(kodeOpt).hasValueSatisfying(kode ->
            assertThat(kode).isEqualTo(VurderArbeidsforholdHistorikkinnslag.BENYTT_A_INNTEKT_I_BG)
        );
    }

    @Test
    public void skal_returne_empty_når_arbeidsforholdet_har_null_på_alle_verdiene() {
        // Arrange
        ArbeidsforholdDto arbeidsforholdDto = new ArbeidsforholdDto();
        // Act
        Optional<VurderArbeidsforholdHistorikkinnslag> kodeOpt = UtledKoderForHistorikkinnslagdelerForArbeidsforholdUtenInnteksmelding.utled(arbeidsforholdDto);
        // Assert
        assertThat(kodeOpt).isEmpty();
    }

}
