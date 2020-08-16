package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Period;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.Etterkontroll;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamiliehendelseEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class EtterkontrollEventObserverTest {

    private static AktørId GITT_MOR_AKTØR_ID = AktørId.dummy();
    private static final LocalDate TERMINDATO = LocalDate.now().plusMonths(3);

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, Month.APRIL, 10);

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());

    private EtterkontrollEventObserver etterkontrollEventObserver;
    private final EtterkontrollRepository etterkontrollRepository = new EtterkontrollRepository( repositoryRule.getEntityManager());
    private FamilieHendelseRepository familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();

    @Before
    public void setUp() throws Exception {
        etterkontrollEventObserver = new EtterkontrollEventObserver(etterkontrollRepository, familieHendelseRepository, behandlingRepository, Period.parse("P60D"));
    }

    @Test
    public void observerFamiliehendelseEvent() throws Exception {

        Behandling behandling = opprettBehandlingMedOppgittTermin(TERMINDATO);

        Etterkontroll etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId()).medErBehandlet(false).medKontrollTidspunkt(LocalDate.now().atStartOfDay()).medKontrollType(KontrollType.MANGLENDE_FØDSEL).build();
        etterkontrollRepository.lagre(etterkontroll);

        FamiliehendelseEvent familiehendelseEvent = new FamiliehendelseEvent(FamiliehendelseEvent.EventType.TERMIN_TIL_FØDSEL, behandling.getAktørId(),
            behandling.getFagsakId(), behandling.getId(), FagsakYtelseType.FORELDREPENGER, null, null);
        etterkontrollEventObserver.observerFamiliehendelseEvent(familiehendelseEvent);

        List<Etterkontroll> ekListe  = etterkontrollRepository.finnEtterkontrollForFagsak(behandling.getFagsakId(),KontrollType.MANGLENDE_FØDSEL);

        for(Etterkontroll ek : ekListe){
            ek.setErBehandlet(true);
            assertThat(ek.isBehandlet()).isTrue();
        }

    }

    @Test
    public void observerBehandlingVedtakEvent() throws Exception {

        Behandling behandling = opprettBehandlingMedOppgittTermin(TERMINDATO);

        BehandlingVedtak vedtak = byggVedtak();
        BehandlingVedtakEvent event = new BehandlingVedtakEvent(vedtak,behandling);
        etterkontrollEventObserver.observerBehandlingVedtakEvent(event);

        List<Etterkontroll> ekListe  = etterkontrollRepository.finnEtterkontrollForFagsak(behandling.getFagsakId(),KontrollType.MANGLENDE_FØDSEL);

        // Assert
        for(Etterkontroll ek : ekListe){
            ek.setErBehandlet(true);
            assertThat(ek.isBehandlet()).isTrue();
        }

    }

    private Behandling opprettBehandlingMedOppgittTermin(LocalDate termindato) {

        AvklarteUttakDatoerEntitet avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder()
            .medFørsteUttaksdato(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medOpprinneligEndringsdato(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .build();

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger
            .forFødselMedGittAktørId(GITT_MOR_AKTØR_ID);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medUtstedtDato(LocalDate.now())
            .medTermindato(termindato)
            .medNavnPå("LEGEN MIN"));
        scenario.medAvklarteUttakDatoer(avklarteUttakDatoer);

        PersonInformasjon søker = scenario.opprettBuilderForRegisteropplysninger()
            .medPersonas()
            .kvinne(GITT_MOR_AKTØR_ID, SivilstandType.GIFT, Region.NORDEN)
            .statsborgerskap(Landkoder.NOR)
            .build();
        scenario.medRegisterOpplysninger(søker);

        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(true, true, false);
        scenario.medOppgittRettighet(rettighet);
        return scenario.lagre(repositoryProvider);
    }

    private BehandlingVedtak byggVedtak() {
        return BehandlingVedtak.builder()
            .medAnsvarligSaksbehandler("s142443")
            .medIverksettingStatus(IverksettingStatus.IVERKSATT)
            .medVedtakstidspunkt(LocalDateTime.now())
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .build();
    }

}
