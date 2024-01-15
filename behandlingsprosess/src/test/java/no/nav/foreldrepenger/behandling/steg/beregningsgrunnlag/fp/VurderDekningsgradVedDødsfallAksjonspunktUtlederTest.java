package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.VurderDekningsgradVedDødsfallAksjonspunktUtleder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarnEntitet;

class VurderDekningsgradVedDødsfallAksjonspunktUtlederTest {

    private LocalDate fødselsdato;

    @BeforeEach
    public void setup() {
        fødselsdato = LocalDate.now();
    }

    @Test
    void skal_ikke_få_aksjonspunkt5087_når_dødsdato_er_seks_uker_pluss_en_dag() {
        // Arrange
        var dødsdato = fødselsdato.plusWeeks(6).plusDays(1);
        var barnList = new UidentifisertBarnEntitet(0, fødselsdato, dødsdato);

        // Act
        var skalHaAksjonspunkt = VurderDekningsgradVedDødsfallAksjonspunktUtleder.utled(Dekningsgrad._80, List.of(barnList));

        // Assert
        assertThat(skalHaAksjonspunkt).isFalse();
    }

    @Test
    void skal_få_aksjonspunkt5087_når_dødsdato_er_seks_uker_minus_en_dag() {
        // Arrange
        var dødsdato = fødselsdato.plusWeeks(6).minusDays(1);
        var barnList = new UidentifisertBarnEntitet(0, fødselsdato, dødsdato);
        var dekningsgrad = 80;

        // Act
        var skalHaAksjonspunkt = VurderDekningsgradVedDødsfallAksjonspunktUtleder.utled(Dekningsgrad._80, List.of(barnList));

        // Assert
        assertThat(skalHaAksjonspunkt).isTrue();
    }

    @Test
    void skal_få_aksjonspunkt5087_når_dødsdato_er_seks_uker() {
        // Arrange
        var dødsdato = fødselsdato.plusWeeks(6);
        var barnList = new UidentifisertBarnEntitet(0, fødselsdato, dødsdato);
        var dekningsgrad = 80;

        // Act
        var resultat = VurderDekningsgradVedDødsfallAksjonspunktUtleder.utled(Dekningsgrad._80, List.of(barnList));
        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_få_aksjonspunkt5087_når_dekningsgrad_er_annerledes_en_80() {
        // Arrange
        var dødsdato = fødselsdato.plusWeeks(6);
        var barn = new UidentifisertBarnEntitet(0, fødselsdato, dødsdato);
        var dekningsgrad = 100;

        // Act
        var resultat = VurderDekningsgradVedDødsfallAksjonspunktUtleder.utled(Dekningsgrad._100, List.of(barn));

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_få_aksjonspunkt5087_når_barn_ikke_finnes() {
        // Arrange
        var dekningsgrad = 80;

        // Act
        var resultat = VurderDekningsgradVedDødsfallAksjonspunktUtleder.utled(Dekningsgrad._80, List.of());

        // Assert
        assertThat(resultat).isFalse();
    }
}
