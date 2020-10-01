package no.nav.foreldrepenger.behandling.steg.iverksettevedtak;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerApplikasjonTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class HenleggBehandlingUtenSøknadTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private InternalManipulerBehandling manipulerInternBehandling;

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private BehandlingskontrollServiceProvider serviceProvider = new BehandlingskontrollServiceProvider(repoRule.getEntityManager(),
            new BehandlingModellRepository(), null);

    @Mock
    private ProsessTaskRepository prosessTaskRepositoryMock;

    @Mock
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjenesteMock;

    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;
    private Behandling behandling;

    @Before
    public void setUp() {
        BehandlingskontrollTjenesteImpl behandlingskontrollTjenesteImpl = new BehandlingskontrollTjenesteImpl(serviceProvider);
        henleggBehandlingTjeneste = new HenleggBehandlingTjeneste(repositoryProvider, behandlingskontrollTjenesteImpl,
                dokumentBestillerApplikasjonTjenesteMock, prosessTaskRepositoryMock);
    }

    @Test
    public void kan_henlegge_behandling_uten_søknad_som_er_satt_på_vent() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger // Oppretter scenario uten søknad for å simulere sitausjoner som
                                                                                 // f.eks der inntektsmelding kommer først.
                .forFødselUtenSøknad(AktørId.dummy())
                .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD, BehandlingStegType.REGISTRER_SØKNAD);
        behandling = scenario.lagre(repositoryProvider);
        manipulerInternBehandling.forceOppdaterBehandlingSteg(behandling, BehandlingStegType.REGISTRER_SØKNAD);
        BehandlingResultatType behandlingsresultat = BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET;
        henleggBehandlingTjeneste.henleggBehandling(behandling.getId(), behandlingsresultat, "begrunnelse");
        assertThat(behandling.getAksjonspunkter()).hasSize(1);
        assertThat(behandling.getAksjonspunkter().stream().map(Aksjonspunkt::getStatus).filter(AksjonspunktStatus.AVBRUTT::equals).count())
                .isEqualTo(1);
    }
}
