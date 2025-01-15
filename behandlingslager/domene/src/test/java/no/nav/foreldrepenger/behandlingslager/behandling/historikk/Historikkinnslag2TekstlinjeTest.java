package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HistorikkinnslagLinjeTest {

    @ParameterizedTest
    @ValueSource(strings = {"Oppgave til NAV (123456789) om å sende inntektsmelding.", "__Svangerskapsvilkåret__ er satt til __Vilkåret er oppfylt__"})
    void sjekk_at_tekst_har_gyldig_bold_syntax(String tekst) {
        assertThatNoException().isThrownBy(() -> {
            var linje = HistorikkinnslagLinje.tekst(tekst, 1);
            assertThat(linje.getTekst()).isEqualTo(tekst);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"en_tekst__", "Overstyrt vurdering fra __0,5__ til __1,7_.", "Endret adresse type til __Bostedsadresse"})
    void ikke_kast_feil_for_ugyldig_bold_syntax(String tekst) {
        assertThatNoException().isThrownBy(() -> HistorikkinnslagLinje.tekst(tekst, 1));
    }
}
