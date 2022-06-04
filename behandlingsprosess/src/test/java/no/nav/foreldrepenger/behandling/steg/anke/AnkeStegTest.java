package no.nav.foreldrepenger.behandling.steg.anke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioAnkeEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

public class AnkeStegTest {

    @Test
    public void skalOppretteAksjonspunktManuellAvAnkeNårStegKjøres() {
        // Arrange
        var scenario = ScenarioAnkeEngangsstønad.forAvvistAnke(ScenarioMorSøkerEngangsstønad.forAdopsjon());
        var ankeBehandling = scenario.lagMocked();
        var rProvider = scenario.mockBehandlingRepositoryProvider();
        var ankeRepository = Mockito.mock(AnkeRepository.class);
        when(ankeRepository.hentAnkeResultat(any())).thenReturn(Optional.empty());
        var kontekst = new BehandlingskontrollKontekst(ankeBehandling.getFagsakId(),
                ankeBehandling.getAktørId(), new BehandlingLås(ankeBehandling.getId()));

        var steg = new AnkeSteg(ankeRepository, Mockito.mock(KlageRepository.class), Mockito.mock(ProsessTaskTjeneste.class), rProvider);

        // Act
        var behandlingStegResultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(behandlingStegResultat).isNotNull();
        assertThat(behandlingStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(behandlingStegResultat.getAksjonspunktListe()).hasSize(1);

        var aksjonspunktDefinisjon = behandlingStegResultat.getAksjonspunktListe().get(0);
        assertThat(aksjonspunktDefinisjon).isEqualTo(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_ANKE);

    }
}
