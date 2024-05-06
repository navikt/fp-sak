package no.nav.foreldrepenger.behandling.steg.dekningsgrad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarnEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;

class VurderDekningsgradVedDødsfallTest {

    @Test
    void skal_ikke_få_aksjonspunkt5087_når_dødsdato_er_seks_uker_pluss_en_dag() {
        var fødselsdato = LocalDate.now();
        var dødsdato = fødselsdato.plusWeeks(6).plusDays(1);
        var barnList = new UidentifisertBarnEntitet(0, fødselsdato, dødsdato);

        var skalHaAksjonspunkt = VurderDekningsgradVedDødsfall.skalEndreDekningsgrad(Dekningsgrad._80, List.of(barnList));

        assertThat(skalHaAksjonspunkt).isFalse();
    }

    @Test
    void skal_få_aksjonspunkt5087_når_dødsdato_er_seks_uker_minus_en_dag() {
        var fødselsdato = LocalDate.now();
        var dødsdato = fødselsdato.plusWeeks(6).minusDays(1);
        var barnList = new UidentifisertBarnEntitet(0, fødselsdato, dødsdato);

        var skalHaAksjonspunkt = VurderDekningsgradVedDødsfall.skalEndreDekningsgrad(Dekningsgrad._80, List.of(barnList));

        assertThat(skalHaAksjonspunkt).isTrue();
    }

    @Test
    void skal_få_aksjonspunkt5087_når_dødsdato_er_seks_uker() {
        var fødselsdato = LocalDate.now();
        var dødsdato = fødselsdato.plusWeeks(6);
        var barnList = new UidentifisertBarnEntitet(0, fødselsdato, dødsdato);

        var resultat = VurderDekningsgradVedDødsfall.skalEndreDekningsgrad(Dekningsgrad._80, List.of(barnList));
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_få_aksjonspunkt5087_når_dekningsgrad_er_annerledes_en_80() {
        var fødselsdato = LocalDate.now();
        var dødsdato = fødselsdato.plusWeeks(6);
        var barn = new UidentifisertBarnEntitet(0, fødselsdato, dødsdato);

        var resultat = VurderDekningsgradVedDødsfall.skalEndreDekningsgrad(Dekningsgrad._100, List.of(barn));

        assertThat(resultat).isFalse();
    }

    @Test
    void skal_få_aksjonspunkt5087_når_barn_ikke_finnes() {
        var resultat = VurderDekningsgradVedDødsfall.skalEndreDekningsgrad(Dekningsgrad._80, List.of());

        assertThat(resultat).isFalse();
    }

}
