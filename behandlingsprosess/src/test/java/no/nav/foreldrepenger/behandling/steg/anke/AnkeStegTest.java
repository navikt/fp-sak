package no.nav.foreldrepenger.behandling.steg.anke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.anke.AnkeVurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioAnkeEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

class AnkeStegTest {

    @Test
    void skalOppretteAksjonspunktManuellAvAnkeNårStegKjøres() {
        // Arrange
        var scenario = ScenarioAnkeEngangsstønad.forAvvistAnke(ScenarioMorSøkerEngangsstønad.forAdopsjon());
        var ankeBehandling = scenario.lagMocked();
        var rProvider = scenario.mockBehandlingRepositoryProvider();
        var ankeRepository = mock(AnkeVurderingTjeneste.class);
        var prosessTaskTjeneste = mock(ProsessTaskTjeneste.class);
        when(ankeRepository.hentAnkeResultatHvisEksisterer(any())).thenReturn(Optional.empty());
        var kontekst = new BehandlingskontrollKontekst(ankeBehandling.getSaksnummer(), ankeBehandling.getFagsakId(),
                new BehandlingLås(ankeBehandling.getId()));

        var steg = new AnkeSteg(ankeRepository, mock(KlageRepository.class), prosessTaskTjeneste, rProvider);

        // Act
        var behandlingStegResultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(behandlingStegResultat).isNotNull();
        assertThat(behandlingStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(behandlingStegResultat.getAksjonspunktListe()).hasSize(1);

        // Sendt til kabl
        verify(prosessTaskTjeneste, times(1)).lagre(any(ProsessTaskData.class));

        var aksjonspunktDefinisjon = behandlingStegResultat.getAksjonspunktListe().get(0);
        assertThat(aksjonspunktDefinisjon).isEqualTo(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_ANKE);

    }
}
