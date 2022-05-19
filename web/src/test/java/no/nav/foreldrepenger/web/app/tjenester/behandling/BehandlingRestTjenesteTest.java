package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import javax.persistence.EntityManager;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.YtelseMaksdatoTjeneste;
import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.HenleggBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingOpprettingTjeneste;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.OpptjeningIUtlandDokStatusTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktUtils;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.MinsterettBehandling2022;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseBehandling2021;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsoppretterTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsutredningTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.verge.VergeTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
public class BehandlingRestTjenesteTest {

    private BehandlingRestTjeneste behandlingRestTjeneste;

    @Mock
    private BehandlingsutredningTjeneste behandlingsutredningTjeneste;
    @Mock
    private BehandlingsoppretterTjeneste behandlingsoppretterTjeneste;
    @Mock
    private BehandlingOpprettingTjeneste behandlingOpprettingTjeneste;
    @Mock
    private BehandlingsprosessTjeneste behandlingsprosessTjenste;
    @Mock
    private OpptjeningIUtlandDokStatusTjeneste opptjeningIUtlandDokStatusTjeneste;
    @Mock
    private TilbakekrevingRepository tilbakekrevingRepository;
    @Mock
    private BehandlingDokumentRepository behandlingDokumentRepository;
    private BehandlingRepositoryProvider repositoryProvider;
    private EntityManager entityManager;

    @BeforeEach
    public void setUp(EntityManager entityManager) {
        var beregningsgrunnlagTjeneste = new HentOgLagreBeregningsgrunnlagTjeneste(entityManager);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        this.entityManager = entityManager;
        var søknadRepository = repositoryProvider.getSøknadRepository();
        var fagsakRepository = repositoryProvider.getFagsakRepository();
        var fagsakTjeneste = new FagsakTjeneste(fagsakRepository, søknadRepository, null);
        var relatertBehandlingTjeneste = new RelatertBehandlingTjeneste(repositoryProvider);
        var stputil = new SkjæringstidspunktUtils();
        var ytelseMaksdatoTjeneste = new YtelseMaksdatoTjeneste(repositoryProvider,
            new RelatertBehandlingTjeneste(repositoryProvider));
        var skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider, ytelseMaksdatoTjeneste,
            stputil, mock(UtsettelseBehandling2021.class), mock(MinsterettBehandling2022.class));
        var behandlingDtoTjeneste = new BehandlingDtoTjeneste(repositoryProvider, beregningsgrunnlagTjeneste,
            tilbakekrevingRepository, skjæringstidspunktTjeneste, opptjeningIUtlandDokStatusTjeneste,
            behandlingDokumentRepository, relatertBehandlingTjeneste,
            new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository()), null, null);

        behandlingRestTjeneste = new BehandlingRestTjeneste(behandlingsutredningTjeneste, behandlingsoppretterTjeneste,
            behandlingOpprettingTjeneste, behandlingsprosessTjenste, fagsakTjeneste, mock(VergeTjeneste.class),
            Mockito.mock(HenleggBehandlingTjeneste.class), behandlingDtoTjeneste, relatertBehandlingTjeneste,
            mock(TotrinnTjeneste.class));
    }

    @Test
    public void henteAnnenPartsGjeldeneBehandling() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var morsBehandling = scenario.lagre(repositoryProvider);

        var behandlingsresultat = morsBehandling.getBehandlingsresultat();
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultat)
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        entityManager.persist(behandlingsresultat);
        morsBehandling.avsluttBehandling();
        entityManager.persist(morsBehandling);

        var farsBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(morsBehandling.getFagsak(), Dekningsgrad._100);
        repositoryProvider.getFagsakRelasjonRepository()
            .kobleFagsaker(morsBehandling.getFagsak(), farsBehandling.getFagsak(), morsBehandling);

        // Act
        var response = behandlingRestTjeneste.hentAnnenPartsGjeldendeBehandling(
            new SaksnummerDto(farsBehandling.getFagsak().getSaksnummer()));

        // Assert
        assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
    }
}
