package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.HenleggBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personopplysning;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingOpprettingTjeneste;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.UtregnetStønadskontoTjeneste;
import no.nav.foreldrepenger.domene.vedtak.TotrinnTjeneste;
import no.nav.foreldrepenger.domene.vedtak.intern.VedtaksbrevStatusUtleder;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsoppretterTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsutredningTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
class BehandlingRestTjenesteESTest {

    @Mock
    private BehandlingsutredningTjeneste behandlingsutredningTjeneste;
    @Mock
    private BehandlingsoppretterTjeneste behandlingsoppretterTjeneste;
    @Mock
    private BehandlingOpprettingTjeneste behandlingOpprettingTjeneste;
    @Mock
    private BehandlingsprosessTjeneste behandlingsprosessTjenste;
    @Mock
    private TilbakekrevingRepository tilbakekrevingRepository;
    @Mock
    private BehandlingDokumentRepository behandlingDokumentRepository;
    @Mock
    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;
    @Mock
    private BeregningTjeneste beregningTjeneste;
    @Mock
    private VergeRepository vergeRepository;
    @Mock
    private VedtaksbrevStatusUtleder vedtaksbrevStatusUtleder;

    private BehandlingRepositoryProvider repositoryProvider;

    private BehandlingRestTjeneste behandlingRestTjeneste;

    @BeforeEach
    public void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var fagsakTjeneste = new FagsakTjeneste(repositoryProvider.getFagsakRepository(),
            repositoryProvider.getSøknadRepository());
        var skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider);
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider);
        var uttakTjeneste = new UttakTjeneste(repositoryProvider.getBehandlingRepository(), null, null);
        var behandlingDtoTjeneste = new BehandlingDtoTjeneste(repositoryProvider, beregningTjeneste, uttakTjeneste, tilbakekrevingRepository,
            skjæringstidspunktTjeneste, behandlingDokumentRepository, mock(TotrinnTjeneste.class), null, null, fagsakRelasjonTjeneste,
            new UtregnetStønadskontoTjeneste(fagsakRelasjonTjeneste, mock(ForeldrepengerUttakTjeneste.class)), mock(DekningsgradTjeneste.class), vergeRepository,
            vedtaksbrevStatusUtleder);

        henleggBehandlingTjeneste = mock(HenleggBehandlingTjeneste.class);
        behandlingRestTjeneste = new BehandlingRestTjeneste(behandlingsutredningTjeneste, behandlingsoppretterTjeneste,
            behandlingOpprettingTjeneste, behandlingsprosessTjenste, fagsakTjeneste, henleggBehandlingTjeneste, behandlingDtoTjeneste);
    }

    @Test
    void skal_hente_behandlinger_for_saksnummer() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();

        var personInformasjon = scenario.opprettBuilderForRegisteropplysninger()
            .leggTilPersonopplysninger(Personopplysning.builder()
                .aktørId(AktørId.dummy())
                .navn("Helga")
                .fødselsdato(LocalDate.now())
                .sivilstand(SivilstandType.SAMBOER)
                .brukerKjønn(NavBrukerKjønn.KVINNE))
            .build();

        scenario.medRegisterOpplysninger(personInformasjon);
        scenario.medDefaultBekreftetTerminbekreftelse();
        var behandling = scenario.lagre(repositoryProvider);
        var saksnummer = behandling.getSaksnummer();

        when(behandlingsutredningTjeneste.hentBehandlingerForSaksnummer(saksnummer)).thenReturn(
            singletonList(behandling));

        var dto = behandlingRestTjeneste.hentBehandlinger(new SaksnummerDto(saksnummer.getVerdi()));

        assertThat(dto).hasSize(1);
    }

}
