package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Period;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.HenleggBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personopplysning;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingOpprettingTjeneste;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.OpptjeningIUtlandDokStatusTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.RegisterInnhentingIntervall;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsoppretterTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsutredningTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
public class BehandlingRestTjenesteESTest {

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
    @Mock
    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;
    @Mock
    private BeregningTjeneste beregningTjeneste;

    private BehandlingRepositoryProvider repositoryProvider;

    private BehandlingRestTjeneste behandlingRestTjeneste;

    @BeforeEach
    public void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var fagsakTjeneste = new FagsakTjeneste(repositoryProvider.getFagsakRepository(),
            repositoryProvider.getSøknadRepository(), null);
        var relatertBehandlingTjeneste = new RelatertBehandlingTjeneste(repositoryProvider);
        var skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
            new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));
        var behandlingDtoTjeneste = new BehandlingDtoTjeneste(repositoryProvider, beregningTjeneste,
            tilbakekrevingRepository, skjæringstidspunktTjeneste, opptjeningIUtlandDokStatusTjeneste,
            behandlingDokumentRepository, relatertBehandlingTjeneste,
            new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository()), null, null, null, mock(TotrinnTjeneste.class));

        henleggBehandlingTjeneste = mock(HenleggBehandlingTjeneste.class);
        behandlingRestTjeneste = new BehandlingRestTjeneste(behandlingsutredningTjeneste, behandlingsoppretterTjeneste,
            behandlingOpprettingTjeneste, behandlingsprosessTjenste, fagsakTjeneste, henleggBehandlingTjeneste, behandlingDtoTjeneste);
    }

    @Test
    public void skal_hente_behandlinger_for_saksnummer() {
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
        var saksnummer = behandling.getFagsak().getSaksnummer();

        when(behandlingsutredningTjeneste.hentBehandlingerForSaksnummer(saksnummer)).thenReturn(
            singletonList(behandling));

        var dto = behandlingRestTjeneste.hentBehandlinger(new SaksnummerDto(saksnummer.getVerdi()));

        assertThat(dto).hasSize(1);
    }

}
