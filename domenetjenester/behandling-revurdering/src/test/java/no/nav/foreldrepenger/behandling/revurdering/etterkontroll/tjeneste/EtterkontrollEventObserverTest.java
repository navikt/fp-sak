package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Period;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

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
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ExtendWith(MockitoExtension.class)
public class EtterkontrollEventObserverTest extends EntityManagerAwareTest {

    private static AktørId GITT_MOR_AKTØR_ID = AktørId.dummy();
    private static final LocalDate TERMINDATO = LocalDate.now().plusMonths(3);

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, Month.APRIL, 10);

    private BehandlingRepositoryProvider repositoryProvider;

    private EtterkontrollEventObserver etterkontrollEventObserver;
    private EtterkontrollRepository etterkontrollRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    public void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        etterkontrollRepository = new EtterkontrollRepository(getEntityManager());
        familieHendelseRepository = new FamilieHendelseRepository(getEntityManager());
        behandlingRepository = new BehandlingRepository(getEntityManager());
        etterkontrollEventObserver = new EtterkontrollEventObserver(etterkontrollRepository, familieHendelseRepository,
            behandlingRepository, Period.parse("P60D"));
    }

    @Test
    public void observerFamiliehendelseEvent() throws Exception {

        Behandling behandling = opprettBehandlingMedOppgittTermin(TERMINDATO);

        Etterkontroll etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId()).medErBehandlet(false)
            .medKontrollTidspunkt(LocalDate.now().atStartOfDay())
            .medKontrollType(KontrollType.MANGLENDE_FØDSEL)
            .build();
        etterkontrollRepository.lagre(etterkontroll);

        FamiliehendelseEvent familiehendelseEvent = new FamiliehendelseEvent(
            FamiliehendelseEvent.EventType.TERMIN_TIL_FØDSEL, behandling.getAktørId(),
            behandling.getFagsakId(), behandling.getId(), FagsakYtelseType.FORELDREPENGER, null, null);
        etterkontrollEventObserver.observerFamiliehendelseEvent(familiehendelseEvent);

        List<Etterkontroll> ekListe = etterkontrollRepository.finnEtterkontrollForFagsak(behandling.getFagsakId(),
            KontrollType.MANGLENDE_FØDSEL);

        for (Etterkontroll ek : ekListe) {
            ek.setErBehandlet(true);
            assertThat(ek.isBehandlet()).isTrue();
        }

    }

    @Test
    public void observerBehandlingVedtakEvent() throws Exception {

        Behandling behandling = opprettBehandlingMedOppgittTermin(TERMINDATO);

        BehandlingVedtak vedtak = byggVedtak();
        BehandlingVedtakEvent event = new BehandlingVedtakEvent(vedtak, behandling);
        etterkontrollEventObserver.observerBehandlingVedtakEvent(event);

        List<Etterkontroll> ekListe = etterkontrollRepository.finnEtterkontrollForFagsak(behandling.getFagsakId(),
            KontrollType.MANGLENDE_FØDSEL);

        // Assert
        for (Etterkontroll ek : ekListe) {
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
