package no.nav.foreldrepenger.behandling.steg.anke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.anke.AnkeVurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioAnkeEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;

class AnkeStegTest {

    @Test
    void skalOppretteAksjonspunktVentKabalNårStegKjøres() {
        // Arrange
        var scenario = ScenarioAnkeEngangsstønad.forAvvistAnke(ScenarioMorSøkerEngangsstønad.forAdopsjon());
        var ankeBehandling = scenario.lagMocked();
        var rProvider = scenario.mockBehandlingRepositoryProvider();
        var ankeRepository = mock(AnkeVurderingTjeneste.class);
        var mockAnkeresultat = mock(AnkeResultatEntitet.class);
        when(mockAnkeresultat.erBehandletAvKabal()).thenReturn(Boolean.TRUE);
        when(ankeRepository.hentAnkeResultatHvisEksisterer(any())).thenReturn(Optional.of(mockAnkeresultat));
        var kontekst = new BehandlingskontrollKontekst(ankeBehandling,
                new BehandlingLås(ankeBehandling.getId()));

        var steg = new AnkeSteg(ankeRepository, rProvider);

        // Act
        var behandlingStegResultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(behandlingStegResultat).isNotNull();
        assertThat(behandlingStegResultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);
        assertThat(behandlingStegResultat.getAksjonspunktListe()).hasSize(1);

        var aksjonspunktDefinisjon = behandlingStegResultat.getAksjonspunktListe().get(0);
        assertThat(aksjonspunktDefinisjon).isEqualTo(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_ANKE);
    }
}
