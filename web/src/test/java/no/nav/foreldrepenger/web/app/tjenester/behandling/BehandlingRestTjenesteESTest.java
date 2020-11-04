package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.HenleggBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personopplysning;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingOpprettingTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.OpptjeningIUtlandDokStatusTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.RegisterInnhentingIntervall;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.web.RepositoryAwareTest;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsoppretterTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsutredningTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.verge.VergeTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;

@ExtendWith(MockitoExtension.class)
public class BehandlingRestTjenesteESTest extends RepositoryAwareTest {

    private BehandlingRestTjeneste behandlingRestTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

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

    @BeforeEach
    public void setUp() {
        var beregningsgrunnlagTjeneste = new HentOgLagreBeregningsgrunnlagTjeneste(getEntityManager());
        var fagsakTjeneste = new FagsakTjeneste(fagsakRepository, søknadRepository, null);
        var relatertBehandlingTjeneste = new RelatertBehandlingTjeneste(repositoryProvider);
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
                new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));
        var behandlingDtoTjeneste = new BehandlingDtoTjeneste(repositoryProvider, beregningsgrunnlagTjeneste,
                tilbakekrevingRepository, skjæringstidspunktTjeneste, opptjeningIUtlandDokStatusTjeneste, behandlingDokumentRepository,
                relatertBehandlingTjeneste,
                new ForeldrepengerUttakTjeneste(fpUttakRepository), null);

        behandlingRestTjeneste = new BehandlingRestTjeneste(behandlingsutredningTjeneste,
                behandlingsoppretterTjeneste,
                behandlingOpprettingTjeneste,
                behandlingsprosessTjenste,
                fagsakTjeneste,
                mock(VergeTjeneste.class),
                mock(HenleggBehandlingTjeneste.class),
                behandlingDtoTjeneste,
                relatertBehandlingTjeneste,
                mock(TotrinnTjeneste.class));
    }

    @Test
    public void skal_hente_behandlinger_for_saksnummer() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();

        PersonInformasjon personInformasjon = scenario.opprettBuilderForRegisteropplysninger()
                .leggTilPersonopplysninger(Personopplysning.builder()
                        .aktørId(AktørId.dummy())
                        .navn("Helga")
                        .fødselsdato(LocalDate.now())
                        .sivilstand(SivilstandType.SAMBOER)
                        .region(Region.NORDEN)
                        .brukerKjønn(NavBrukerKjønn.KVINNE))
                .build();

        scenario.medRegisterOpplysninger(personInformasjon);
        scenario.medDefaultBekreftetTerminbekreftelse();
        Behandling behandling = scenario.lagre(repositoryProvider);
        Saksnummer saksnummer = behandling.getFagsak().getSaksnummer();

        OppgittPeriodeEntitet periode1 = OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
                .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(6))
                .build();

        OppgittPeriodeEntitet periode2 = OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
                .medPeriode(LocalDate.now().plusWeeks(6).plusDays(1), LocalDate.now().plusWeeks(10))
                .build();

        OppgittDekningsgradEntitet dekningsgrad = OppgittDekningsgradEntitet.bruk100();
        Long behandlingId = behandling.getId();
        ytelsesfordelingRepository.lagre(behandlingId, dekningsgrad);

        OppgittFordelingEntitet fordeling = new OppgittFordelingEntitet(List.of(periode1, periode2), true);
        ytelsesfordelingRepository.lagre(behandlingId, fordeling);

        when(behandlingsutredningTjeneste.hentBehandlingerForSaksnummer(saksnummer)).thenReturn(singletonList(behandling));

        List<BehandlingDto> dto = behandlingRestTjeneste.hentBehandlinger(new SaksnummerDto(saksnummer.getVerdi()));

        assertThat(dto).hasSize(1);
    }

}
