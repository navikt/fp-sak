package no.nav.foreldrepenger.domene.person.verge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.FiktiveFnr;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.person.tps.TpsAdapter;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.person.verge.dto.AvklarVergeDto;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

public class VergeOppdatererTest {

    private static final AksjonspunktDefinisjon AKSJONSPUNKT_DEF = AksjonspunktDefinisjon.AVKLAR_VERGE;

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private PersonopplysningTjeneste personTjeneste = Mockito.mock(PersonopplysningTjeneste.class);
    private HistorikkTjenesteAdapter historikkTjeneste = Mockito.mock(HistorikkTjenesteAdapter.class);
    private TpsTjeneste tpsTjeneste = Mockito.mock(TpsTjeneste.class);

    private TpsAdapter tpsAdapter;
    private NavBrukerRepository navBrukerRepository;

    private NavBruker vergeBruker;
    private Personinfo pInfo;

    @Before
    public void oppsett() {
        tpsAdapter = mock(TpsAdapter.class);
        navBrukerRepository = mock(NavBrukerRepository.class);

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();

        @SuppressWarnings("unused")
        Behandling behandling = scenario.lagre(repositoryProvider);

        pInfo = new Personinfo.Builder()
            .medNavn("Verger Vergusen")
            .medAktørId(AktørId.dummy())
            .medPersonIdent(new PersonIdent(new FiktiveFnr().nesteKvinneFnr()))
            .medFødselsdato(LocalDate.now().minusYears(33))
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
            .medForetrukketSpråk(Språkkode.NB)
            .build();

        vergeBruker = NavBruker.opprettNy(pInfo);

        when(tpsAdapter.hentAktørIdForPersonIdent(Mockito.any())).thenReturn(Optional.of(AktørId.dummy()));
        when(navBrukerRepository.hent(Mockito.any())).thenReturn(Optional.of(vergeBruker));
    }

    @Test
    public void lagre_verge() {
        new VergeBuilder()
            .medVergeType(VergeType.BARN)
            .medBruker(vergeBruker)
            .build();
    }

    @Test
    public void skal_generere_historikkinnslag_ved_bekreftet() {
        // Behandling
        var behandling = opprettBehandling();
        AvklarVergeDto dto = opprettDtoVerge();
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        new VergeOppdaterer(personTjeneste, historikkTjeneste,
            tpsTjeneste, mock(VergeRepository.class)).oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt.orElse(null), dto));

        // Verifiserer HistorikkinnslagDto
        ArgumentCaptor<Historikkinnslag> historikkCapture = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkTjeneste).lagInnslag(historikkCapture.capture());
        Historikkinnslag historikkinnslag = historikkCapture.getValue();
        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.REGISTRER_OM_VERGE);
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        HistorikkinnslagDel del = historikkinnslag.getHistorikkinnslagDeler().get(0);
        assertThat(del.getSkjermlenke()).as("skjermlenke").hasValueSatisfying(skjermlenke -> assertThat(skjermlenke).isEqualTo(SkjermlenkeType.FAKTA_OM_VERGE.getKode()));
        assertThat(del.getHendelse()).as("hendelse").hasValueSatisfying(hendelse -> assertThat(hendelse.getNavn()).as("navn").isEqualTo(HistorikkinnslagType.REGISTRER_OM_VERGE.getKode()));
    }

    private AvklarVergeDto opprettDtoVerge() {
        AvklarVergeDto dto = new AvklarVergeDto();
        dto.setNavn("Navn");
        dto.setFnr("12345678901");
        dto.setGyldigFom(LocalDate.now().minusDays(10));
        dto.setGyldigTom(LocalDate.now().plusDays(10));
        dto.setVergeType(VergeType.BARN);
        return dto;
    }

    private Behandling opprettBehandling() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknad();
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.KONTROLLER_FAKTA);
        scenario.lagre(repositoryProvider);

        return scenario.getBehandling();
    }

}

