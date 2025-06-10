package no.nav.foreldrepenger.behandling.steg.iverksettevedtak;

import static no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling.forceOppdaterBehandlingSteg;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;

class HenleggBehandlingUtenSøknadTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;

    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        var serviceProvider = new BehandlingskontrollServiceProvider(getEntityManager(), null);
        var behandlingskontrollTjenesteImpl = new BehandlingskontrollTjenesteImpl(serviceProvider);
        henleggBehandlingTjeneste = new HenleggBehandlingTjeneste(repositoryProvider, behandlingskontrollTjenesteImpl,
                mock(BehandlingEventPubliserer.class));
    }

    @Test
    void kan_henlegge_behandling_uten_søknad_som_er_satt_på_vent() {
        var scenario = ScenarioMorSøkerForeldrepenger // Oppretter scenario uten søknad for å simulere sitausjoner som
                                                                                 // f.eks der inntektsmelding kommer først.
                .forFødselUtenSøknad(AktørId.dummy())
                .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD, BehandlingStegType.REGISTRER_SØKNAD);
        var behandling = scenario.lagre(repositoryProvider);
        forceOppdaterBehandlingSteg(behandling, BehandlingStegType.REGISTRER_SØKNAD);
        var behandlingsresultat = BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET;
        henleggBehandlingTjeneste.henleggBehandlingManuell(behandling.getId(), behandlingsresultat, "begrunnelse");
        assertThat(behandling.getAksjonspunkter()).hasSize(1);
        assertThat(behandling.getAksjonspunkter().stream().map(Aksjonspunkt::getStatus).filter(AksjonspunktStatus.AVBRUTT::equals).count())
                .isEqualTo(1);
    }
}
