package no.nav.foreldrepenger.behandling.steg.innsyn;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;

class VurderInnsynStegTest extends EntityManagerAwareTest {

    private static final AktørId AKTØR_ID_MOR = AktørId.dummy();

    private BehandlingRepositoryProvider repositoryProvider;

    private BehandlingRepository behandlingRepository;

    private final VurderInnsynSteg steg = new VurderInnsynSteg();

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Test
    void skal_liste_ut_aksjonspunktet_for_vurder_innsyn() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
                .medBruker(AKTØR_ID_MOR, NavBrukerKjønn.KVINNE);

        scenario.medSøknad();
        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var lås = behandlingRepository.taSkriveLås(behandling);
        var fagsak = behandling.getFagsak();
        var kontekst = new BehandlingskontrollKontekst(fagsak.getSaksnummer(), fagsak.getId(), lås);
        var resultat = steg.utførSteg(kontekst);
        assertThat(resultat.getAksjonspunktListe()).containsOnly(AksjonspunktDefinisjon.VURDER_INNSYN);
    }
}
