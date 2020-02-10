package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.Test;

import no.nav.foreldrepenger.historikk.VurderArbeidsforholdHistorikkinnslag;

public class UtledKoderForHistorikkinnslagdelerForPermisjonTest {

    @Test
    public void skal_utlede_at_søker_er_i_permisjon_når_bruk_permisjon_er_true() {
        // Arrange
        ArbeidsforholdDto arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukPermisjon(true);
        // Act
        Optional<VurderArbeidsforholdHistorikkinnslag> kodeOpt = UtledKoderForHistorikkinnslagdelerForArbeidsforholdMedPermisjon.utled(arbeidsforholdDto);
        // Assert
        assertThat(kodeOpt).hasValueSatisfying(kode ->
            assertThat(kode).isEqualTo(VurderArbeidsforholdHistorikkinnslag.SØKER_ER_I_PERMISJON)
        );

    }

    @Test
    public void skal_utlede_at_søker_ikke_er_i_permisjon_når_bruk_permisjon_er_false() {
        // Arrange
        ArbeidsforholdDto arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukPermisjon(false);
        // Act
        Optional<VurderArbeidsforholdHistorikkinnslag> kodeOpt = UtledKoderForHistorikkinnslagdelerForArbeidsforholdMedPermisjon.utled(arbeidsforholdDto);
        // Assert
        assertThat(kodeOpt).hasValueSatisfying(kode ->
            assertThat(kode).isEqualTo(VurderArbeidsforholdHistorikkinnslag.SØKER_ER_IKKE_I_PERMISJON)
        );
    }

    @Test
    public void skal_utlede_optional_empty_når_bruk_permisjon_er_null() {
        // Arrange
        ArbeidsforholdDto arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukPermisjon(null);
        // Act
        Optional<VurderArbeidsforholdHistorikkinnslag> kodeOpt = UtledKoderForHistorikkinnslagdelerForArbeidsforholdMedPermisjon.utled(arbeidsforholdDto);
        // Assert
        assertThat(kodeOpt).isEmpty();
    }

}
