package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.Test;

import no.nav.foreldrepenger.historikk.VurderArbeidsforholdHistorikkinnslag;

public class UtledKoderForHistorikkinnslagdelerForNyttEllerErstattetArbeidsforholdTest {

    @Test
    public void skal_returne_NYTT_ARBEIDSFORHOLD_når_arbeidsforholdet_har_nytt_arbeidsforhold_satt_til_true() {
        // Arrange
        ArbeidsforholdDto arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setErNyttArbeidsforhold(true);
        // Act
        Optional<VurderArbeidsforholdHistorikkinnslag> kodeOpt = UtledKoderForHistorikkinnslagdelerForNyttEllerErstattetArbeidsforhold.utled(arbeidsforholdDto);
        // Assert
        assertThat(kodeOpt).hasValueSatisfying(kode ->
            assertThat(kode).isEqualTo(VurderArbeidsforholdHistorikkinnslag.NYTT_ARBEIDSFORHOLD)
        );
    }

    @Test
    public void skal_returne_SLÅTT_SAMMEN_MED_ANNET_når_arbeidsforholdet_har_en_arbeidsforhold_id_som_skal_erstattes() {
        // Arrange
        ArbeidsforholdDto arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setErstatterArbeidsforholdId("123");
        // Act
        Optional<VurderArbeidsforholdHistorikkinnslag> kodeOpt = UtledKoderForHistorikkinnslagdelerForNyttEllerErstattetArbeidsforhold.utled(arbeidsforholdDto);
        // Assert
        assertThat(kodeOpt).hasValueSatisfying(kode ->
            assertThat(kode).isEqualTo(VurderArbeidsforholdHistorikkinnslag.SLÅTT_SAMMEN_MED_ANNET)
        );
    }

    @Test
    public void skal_returne_empty_når_arbeidsforholdet_ikke_er_nytt_eller_erstattes() {
        // Arrange
        ArbeidsforholdDto arbeidsforholdDto = new ArbeidsforholdDto();
        // Act
        Optional<VurderArbeidsforholdHistorikkinnslag> kodeOpt = UtledKoderForHistorikkinnslagdelerForNyttEllerErstattetArbeidsforhold.utled(arbeidsforholdDto);
        // Assert
        assertThat(kodeOpt).isEmpty();
    }

}
