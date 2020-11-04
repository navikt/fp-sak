package no.nav.foreldrepenger.behandling.steg.innsyn;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class VurderInnsynStegTest extends EntityManagerAwareTest {

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
    public void skal_liste_ut_aksjonspunktet_for_vurder_innsyn(){
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBruker(AKTØR_ID_MOR, NavBrukerKjønn.KVINNE);

        scenario.medSøknad();
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(),fagsak.getAktørId(), lås);
        BehandleStegResultat resultat = steg.utførSteg(kontekst);
        assertThat(resultat.getAksjonspunktListe()).containsOnly(AksjonspunktDefinisjon.VURDER_INNSYN);
    }
}
