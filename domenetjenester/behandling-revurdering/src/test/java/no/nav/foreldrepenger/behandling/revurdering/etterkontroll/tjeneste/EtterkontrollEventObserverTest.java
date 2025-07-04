package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Period;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingVedtakEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.events.FamiliehendelseEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.etterkontroll.Etterkontroll;
import no.nav.foreldrepenger.behandlingslager.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandlingslager.etterkontroll.KontrollType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
class EtterkontrollEventObserverTest {

    private static AktørId GITT_MOR_AKTØR_ID = AktørId.dummy();
    private static final LocalDate TERMINDATO = LocalDate.now().plusMonths(3);

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, Month.APRIL, 10);

    private BehandlingRepositoryProvider repositoryProvider;

    private EtterkontrollEventObserver etterkontrollEventObserver;
    private EtterkontrollRepository etterkontrollRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        etterkontrollRepository = new EtterkontrollRepository(entityManager);
        familieHendelseRepository = new FamilieHendelseRepository(entityManager);
        etterkontrollEventObserver = new EtterkontrollEventObserver(etterkontrollRepository, familieHendelseRepository,
                Period.parse("P60D"));
    }

    @Test
    void observerFamiliehendelseEvent() {

        var behandling = opprettBehandlingMedOppgittTermin(TERMINDATO);

        var etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId()).medErBehandlet(false)
                .medKontrollTidspunkt(LocalDate.now().atStartOfDay())
                .medKontrollType(KontrollType.MANGLENDE_FØDSEL)
                .build();
        etterkontrollRepository.lagre(etterkontroll);

        var familiehendelseEvent = new FamiliehendelseEvent(
                FamiliehendelseEvent.EventType.TERMIN_TIL_FØDSEL, behandling.getSaksnummer(),
                behandling.getFagsakId(), behandling.getId(), FagsakYtelseType.FORELDREPENGER, null, null);
        etterkontrollEventObserver.observerFamiliehendelseEvent(familiehendelseEvent);

        var ekListe = etterkontrollRepository.finnEtterkontrollForFagsak(behandling.getFagsakId(),
                KontrollType.MANGLENDE_FØDSEL);

        for (var ek : ekListe) {
            ek.setErBehandlet(true);
            assertThat(ek.isBehandlet()).isTrue();
        }

    }

    @Test
    void observerBehandlingVedtakEvent() {

        var behandling = opprettBehandlingMedOppgittTermin(TERMINDATO);

        var vedtak = byggVedtak();
        var event = new BehandlingVedtakEvent(vedtak, behandling);
        etterkontrollEventObserver.observerBehandlingVedtakEvent(event);

        var ekListe = etterkontrollRepository.finnEtterkontrollForFagsak(behandling.getFagsakId(),
                KontrollType.MANGLENDE_FØDSEL);

        // Assert
        for (var ek : ekListe) {
            ek.setErBehandlet(true);
            assertThat(ek.isBehandlet()).isTrue();
        }

    }

    private Behandling opprettBehandlingMedOppgittTermin(LocalDate termindato) {

        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder()
                .medFørsteUttaksdato(SKJÆRINGSTIDSPUNKT_OPPTJENING)
                .medOpprinneligEndringsdato(SKJÆRINGSTIDSPUNKT_OPPTJENING)
                .build();

        var scenario = ScenarioMorSøkerForeldrepenger
                .forFødselMedGittAktørId(GITT_MOR_AKTØR_ID);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medUtstedtDato(LocalDate.now())
                .medTermindato(termindato)
                .medNavnPå("LEGEN MIN"));
        scenario.medAvklarteUttakDatoer(avklarteUttakDatoer);

        var søker = scenario.opprettBuilderForRegisteropplysninger()
                .medPersonas()
                .kvinne(GITT_MOR_AKTØR_ID, SivilstandType.GIFT)
                .statsborgerskap(Landkoder.NOR)
                .build();
        scenario.medRegisterOpplysninger(søker);

        var rettighet = OppgittRettighetEntitet.beggeRett();
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
