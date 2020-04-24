package no.nav.foreldrepenger.web.app.tjenester.behandling;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.YtelseMaksdatoTjeneste;
import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.HenleggBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.OpptjeningIUtlandDokStatusTjeneste;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktUtils;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsoppretterApplikasjonTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessApplikasjonTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsutredningApplikasjonTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import java.time.Period;

import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThat;

public class BehandlingRestTjenesteTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRestTjeneste behandlingRestTjeneste;
    private BehandlingRepositoryProvider repositoryProvider;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    private BehandlingsutredningApplikasjonTjeneste behandlingsutredningApplikasjonTjeneste = mock(BehandlingsutredningApplikasjonTjeneste.class);
    private BehandlingsoppretterApplikasjonTjeneste behandlingsoppretterApplikasjonTjeneste = mock(BehandlingsoppretterApplikasjonTjeneste.class);
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjenste = mock(BehandlingsprosessApplikasjonTjeneste.class);
    private OpptjeningIUtlandDokStatusTjeneste opptjeningIUtlandDokStatusTjeneste = mock(OpptjeningIUtlandDokStatusTjeneste.class);
    private TilbakekrevingRepository tilbakekrevingRepository = mock(TilbakekrevingRepository.class);
    private BehandlingDokumentRepository behandlingDokumentRepository = mock(BehandlingDokumentRepository.class);

    @Before
    public void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
        var beregningsgrunnlagTjeneste = new HentOgLagreBeregningsgrunnlagTjeneste(repoRule.getEntityManager());
        var fagsakTjeneste = new FagsakTjeneste(repositoryProvider, null);
        var relatertBehandlingTjeneste = new RelatertBehandlingTjeneste(repositoryProvider);
        var stputil = new SkjæringstidspunktUtils(Period.parse("P10M"),
            Period.parse("P6M"), Period.parse("P1Y"), Period.parse("P6M"));
        var ytelseMaksdatoTjeneste = new YtelseMaksdatoTjeneste(repositoryProvider, new RelatertBehandlingTjeneste(repositoryProvider));
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider, ytelseMaksdatoTjeneste, stputil);
        var behandlingDtoTjeneste = new BehandlingDtoTjeneste(repositoryProvider, beregningsgrunnlagTjeneste, tilbakekrevingRepository,
            skjæringstidspunktTjeneste, opptjeningIUtlandDokStatusTjeneste, behandlingDokumentRepository, relatertBehandlingTjeneste,
            new ForeldrepengerUttakTjeneste(repositoryProvider.getUttakRepository()), null);

        behandlingRestTjeneste = new BehandlingRestTjeneste(behandlingsutredningApplikasjonTjeneste,
            behandlingsoppretterApplikasjonTjeneste,
            behandlingsprosessTjenste,
            fagsakTjeneste,
            Mockito.mock(HenleggBehandlingTjeneste.class),
            behandlingDtoTjeneste,
            relatertBehandlingTjeneste);
    }

    @Test
    public void henteAnnenPartsGjeldeneBehandling() {
        // Arrange
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling morsBehandling = scenario.lagre(repositoryProvider);

        Behandlingsresultat behandlingsresultat = morsBehandling.getBehandlingsresultat();
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        repoRule.getRepository().lagre(behandlingsresultat);
        morsBehandling.avsluttBehandling();
        repoRule.getRepository().lagre(morsBehandling);

        Behandling farsBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(morsBehandling.getFagsak(), Dekningsgrad._100);
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morsBehandling.getFagsak(), farsBehandling.getFagsak(), morsBehandling);

        repoRule.getRepository().flushAndClear();

        // Act
        @SuppressWarnings("resource")
        Response response = behandlingRestTjeneste.hentAnnenPartsGjeldendeBehandling(new SaksnummerDto(farsBehandling.getFagsak().getSaksnummer()));

        // Assert
        assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
    }
}
