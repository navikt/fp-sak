package no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding.ForespørselStatusResponse.Vurdering;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding.ForespørselStatusResponse.Årsak;

class ForespørselStatusResponseTest {

    private static final String SAKSNUMMER = "1234567890";
    private static final String ORGNR = "999999999";

    @ParameterizedTest
    @EnumSource(Årsak.class)
    void hver_aarsak_har_en_vurdering(Årsak årsak) {
        assertThat(årsak.vurdering()).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(Årsak.class)
    void av_setter_vurderingen_som_hoerer_til_aarsaken(Årsak årsak) {
        var response = ForespørselStatusResponse.av(SAKSNUMMER, ORGNR, årsak);

        assertThat(response.vurdering()).isEqualTo(årsak.vurdering());
    }

    @Test
    void kun_manglende_inntektsmelding_gir_trengs() {
        assertThat(Årsak.values()).filteredOn(å -> å.vurdering() == Vurdering.TRENGS).containsExactly(Årsak.MANGLER_INNTEKTSMELDING);
    }

    @Test
    void usikre_aarsaker_gir_ukjent_slik_at_foresporselen_ikke_lukkes() {
        assertThat(Årsak.values()).filteredOn(å -> å.vurdering() == Vurdering.UKJENT).containsExactly(Årsak.SAK_IKKE_FUNNET);
    }

    @Test
    void ingen_behandling_gir_trengs_ikke() {
        assertThat(Årsak.INGEN_BEHANDLING.vurdering()).isEqualTo(Vurdering.TRENGS_IKKE);
    }

    @Test
    void ugyldig_kombinasjon_av_vurdering_og_aarsak_avvises() {
        assertThatThrownBy(() -> new ForespørselStatusResponse(SAKSNUMMER, ORGNR, Vurdering.TRENGS, Årsak.SAK_AVSLUTTET)).isInstanceOf(
            IllegalArgumentException.class).hasMessageContaining("SAK_AVSLUTTET");
    }

    @Test
    void toString_maskerer_orgnummer() {
        var response = ForespørselStatusResponse.av(SAKSNUMMER, ORGNR, Årsak.MANGLER_INNTEKTSMELDING);

        assertThat(response.toString()).doesNotContain(ORGNR);
    }
}
