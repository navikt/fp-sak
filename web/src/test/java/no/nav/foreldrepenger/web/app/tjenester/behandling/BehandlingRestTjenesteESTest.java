package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.HenleggBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
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
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.OpptjeningIUtlandDokStatusTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.RegisterInnhentingIntervall;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsoppretterApplikasjonTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessApplikasjonTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsutredningApplikasjonTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;

public class BehandlingRestTjenesteESTest {

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
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider, new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));
        BehandlingDtoTjeneste behandlingDtoTjeneste = new BehandlingDtoTjeneste(repositoryProvider, beregningsgrunnlagTjeneste,
            tilbakekrevingRepository, skjæringstidspunktTjeneste, opptjeningIUtlandDokStatusTjeneste, behandlingDokumentRepository, relatertBehandlingTjeneste,
            new ForeldrepengerUttakTjeneste(repositoryProvider.getUttakRepository()));

        behandlingRestTjeneste = new BehandlingRestTjeneste(behandlingsutredningApplikasjonTjeneste,
            behandlingsoppretterApplikasjonTjeneste,
            behandlingsprosessTjenste,
            fagsakTjeneste,
            Mockito.mock(HenleggBehandlingTjeneste.class),
            behandlingDtoTjeneste,
            relatertBehandlingTjeneste);
    }

    @Test
    public void skal_hente_behandlinger_for_saksnummer() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();

        Personinfo person = new Personinfo.Builder()
            .medNavn("Helga")
            .medAktørId(AktørId.dummy())
            .medPersonIdent(new PersonIdent("12312411252"))
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
            .medSivilstandType(SivilstandType.SAMBOER)
            .medFødselsdato(LocalDate.now())
            .medRegion(Region.NORDEN)
            .build();

        PersonInformasjon personInformasjon = scenario.opprettBuilderForRegisteropplysninger()
            .leggTilPersonopplysninger(Personopplysning.builder()
                .aktørId(person.getAktørId())
                .navn(person.getNavn())
                .fødselsdato(person.getFødselsdato())
                .sivilstand(SivilstandType.SAMBOER)
                .region(Region.NORDEN)
                .brukerKjønn(person.getKjønn())).build();


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
        repositoryProvider.getYtelsesFordelingRepository().lagre(behandlingId, dekningsgrad);

        OppgittFordelingEntitet fordeling = new OppgittFordelingEntitet(List.of(periode1, periode2), true);
        repositoryProvider.getYtelsesFordelingRepository().lagre(behandlingId, fordeling);

        when(behandlingsutredningApplikasjonTjeneste.hentBehandlingerForSaksnummer(saksnummer)).thenReturn(singletonList(behandling));

        List<BehandlingDto> dto = behandlingRestTjeneste.hentBehandlinger(new SaksnummerDto(saksnummer.getVerdi()));

        assertThat(dto).hasSize(1);
    }

}
