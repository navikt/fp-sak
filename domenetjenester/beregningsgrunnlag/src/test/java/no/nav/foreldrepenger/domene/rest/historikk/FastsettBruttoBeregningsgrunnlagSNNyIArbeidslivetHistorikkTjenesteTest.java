package no.nav.foreldrepenger.domene.rest.historikk;


import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjenesteTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private final HistorikkinnslagRepository historikkRepo = mock(HistorikkinnslagRepository.class);


    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
    }


    @Test
    void skal_lage_historikkinnslag_for_fastsett_bergningsgrunnlag_ved_ny_i_arbeidslivet() {
        //Arrange
        AbstractTestScenario<?> scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandling = scenario.lagre(repositoryProvider);
        var dto = new FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto("en begrunnelse", 20000);

        // Act
        var ref = BehandlingReferanse.fra(behandling);
        new FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste(historikkRepo).lagHistorikk(dto, new AksjonspunktOppdaterParameter(ref, dto));

        // Assert
        var captor = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkRepo).lagre(captor.capture());

        var historikkinnslag = captor.getValue();

        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        assertThat(historikkinnslag.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(historikkinnslag.getFagsakId()).isEqualTo(behandling.getFagsakId());
        assertThat(historikkinnslag.getTittel()).isNull();
        assertThat(historikkinnslag.getSkjermlenke()).isEqualTo(SkjermlenkeType.BEREGNING_FORELDREPENGER);
        assertThat(historikkinnslag.getLinjer()).satisfies(l -> {
            assertThat(l).hasSize(3);
            assertThat(l.get(0).getTekst()).isEqualTo("__Brutto næringsinntekt__ er satt til __20000__.");
            assertThat(l.get(1).getType()).isEqualTo(HistorikkinnslagLinjeType.LINJESKIFT);
            assertThat(l.get(2).getTekst()).isEqualTo("en begrunnelse.");
        });
    }
}
