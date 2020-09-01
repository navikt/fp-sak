package no.nav.foreldrepenger.behandlingslager.behandling.repository;

import java.time.LocalDateTime;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class BehandlingKandidaterRepositoryTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private BehandlingKandidaterRepository sutRepo;

    @Inject
    private BehandlingRepository behandlingRepository;

    private BasicBehandlingBuilder behandlingBuilder = new BasicBehandlingBuilder(repoRule.getEntityManager());

    @Test
    public void skal_finne_en_kandidat_for_automatisk_gjenopptagelse() throws Exception {
        // Arrange
        var behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        var aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT);
        AksjonspunktTestSupport.setFrist(aksjonspunkt, LocalDateTime.now().minusMinutes(1), Venteårsak.FOR_TIDLIG_SOKNAD);
        behandlingRepository.lagre(behandling);

        // Act
        var behandlinger = sutRepo.finnBehandlingerForAutomatiskGjenopptagelse();

        // Assert
        Assertions.assertThat(behandlinger).containsOnly(behandling);

    }
}
