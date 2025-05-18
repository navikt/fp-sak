package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import static no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType.BEREGNING;
import static no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT;
import static no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType.UTTAKSVILKÅR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ExtendWith(MockitoExtension.class)
class StartpunktUtlederYtelseFordelingTest extends EntityManagerAwareTest {

    private static final BigDecimal ARBEIDSPROSENT_30 = new BigDecimal(30);
    private static final String AG1 = "123";
    private static final String AG2 = "456";

    private BehandlingRepositoryProvider repositoryProvider;
    private StartpunktUtlederYtelseFordeling utleder;
    private SøknadRepository søknadRepository;
    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @BeforeEach
    public void oppsett() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        søknadRepository = new SøknadRepository(entityManager, new BehandlingRepository(entityManager));
        utleder = new StartpunktUtlederYtelseFordeling(repositoryProvider, skjæringstidspunktTjeneste);
    }

    @Test
    void skal_returnere_inngangsvilkår_dersom_skjæringstidspunkt_er_endret() {
        // Arrange
        var originalBehandling = lagFørstegangsBehandling();

        var revurdering = lagRevurdering(originalBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);

        opprettYtelsesFordeling(revurdering, AG1);

        var førsteuttaksdato = LocalDate.now();
        var endretUttaksdato = førsteuttaksdato.plusDays(1);

        var skjæringstidspunkt = Skjæringstidspunkt.builder().medFørsteUttaksdato(førsteuttaksdato).medUtledetSkjæringstidspunkt(førsteuttaksdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandling.getId())).thenReturn(skjæringstidspunkt);

        var revSkjæringstidspunkt = Skjæringstidspunkt.builder().medFørsteUttaksdato(endretUttaksdato).medUtledetSkjæringstidspunkt(endretUttaksdato).build();

        // Act/Assert
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering), revSkjæringstidspunkt, 1L, 2L)).isEqualTo(INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
    }

    @Test
    void skal_returnere_opplysningplikt_ved_endret_stp_tross_gradering() {
        // Arrange
        var originalBehandling = lagFørstegangsBehandling();

        var revurdering = lagRevurdering(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);

        lagreEndringssøknad(revurdering);
        opprettYtelsesFordelingMedGradering(revurdering, AG2, ARBEIDSPROSENT_30);

        var førsteuttaksdato = LocalDate.now();
        var endretUttaksdato = førsteuttaksdato.plusDays(1);
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medFørsteUttaksdato(førsteuttaksdato).medUtledetSkjæringstidspunkt(førsteuttaksdato).build();
        var revSkjæringstidspunkt = Skjæringstidspunkt.builder().medFørsteUttaksdato(endretUttaksdato).medUtledetSkjæringstidspunkt(endretUttaksdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandling.getId())).thenReturn(skjæringstidspunkt);

        // Act/Assert
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering), revSkjæringstidspunkt, 1L, 2L)).isEqualTo(INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
    }

    @Test
    void skal_returnere_beregning_dersom_søker_gradering_på_andel_uten_dagsats() {
        // Arrange
        var originalBehandling = lagFørstegangsBehandling();

        var revurdering = lagRevurdering(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);

        lagreEndringssøknad(revurdering);
        opprettYtelsesFordelingMedGradering(revurdering, AG2, ARBEIDSPROSENT_30);

        var førsteuttaksdato = LocalDate.now();
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medFørsteUttaksdato(førsteuttaksdato).medUtledetSkjæringstidspunkt(førsteuttaksdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandling.getId())).thenReturn(skjæringstidspunkt);

        // Act/Assert
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering), skjæringstidspunkt, 1L, 2L)).isEqualTo(BEREGNING);
    }

    @Test
    void startpunkt_uttak_dersom_søknad_gradering_og_orig_behandling_har_ingen_aktiviter_lik_null_dagsats() {
        // Arrange
        var originalBehandling = lagFørstegangsBehandling();

        var revurdering = lagRevurdering(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);

        opprettYtelsesFordelingMedGradering(revurdering, AG1, ARBEIDSPROSENT_30);

        var førsteuttaksdato = LocalDate.now();
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medFørsteUttaksdato(førsteuttaksdato).medUtledetSkjæringstidspunkt(førsteuttaksdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandling.getId())).thenReturn(skjæringstidspunkt);

        // Act/Assert
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering), skjæringstidspunkt, 1L, 2L)).isEqualTo(UTTAKSVILKÅR);
    }

    @Test
    void startpunkt_beregning_dersom_søknad_gradering_og_orig_behandling_har_en_aktivitet_lik_null_dagsats() {
        // Arrange
        var originalBehandling = lagFørstegangsBehandling();

        var revurdering = lagRevurdering(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);

        lagreEndringssøknad(revurdering);
        opprettYtelsesFordelingMedGradering(revurdering, AG2, ARBEIDSPROSENT_30);

        var førsteuttaksdato = LocalDate.now();
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medFørsteUttaksdato(førsteuttaksdato).medUtledetSkjæringstidspunkt(førsteuttaksdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandling.getId())).thenReturn(skjæringstidspunkt);

        // Act/Assert
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering), skjæringstidspunkt, 1L, 2L)).isEqualTo(BEREGNING);
    }

    @Test
    void skal_returnere_beregning_dersom_søker_gradering_og_kunn_ett_arbeidsforhold_i_orginalbehandling() {
        // Arrange
        var originalBehandling = lagFørstegangsBehandling();

        var revurdering = lagRevurdering(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);

        var førsteuttaksdato = LocalDate.now();
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(førsteuttaksdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandling.getId())).thenReturn(skjæringstidspunkt);

        lagreEndringssøknad(revurdering);
        opprettYtelsesFordelingMedGradering(revurdering, AG1, ARBEIDSPROSENT_30);

        // Act/Assert
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering), skjæringstidspunkt, 1L, 2L)).isEqualTo(BEREGNING);
    }

    @Test
    void startpunkt_beregning_dersom_søknad_er_endringssøknad_med_gradering() {
        // Arrange
        var originalBehandling = lagFørstegangsBehandling();

        var revurdering = lagRevurdering(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);

        lagreEndringssøknad(revurdering);
        opprettYtelsesFordelingMedGradering(revurdering, AG1, BigDecimal.valueOf(50));

        var førsteuttaksdato = LocalDate.now();
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medFørsteUttaksdato(førsteuttaksdato).medUtledetSkjæringstidspunkt(førsteuttaksdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandling.getId())).thenReturn(skjæringstidspunkt);

        // Act
        var startpunkt = utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering), skjæringstidspunkt, 1L, 2L);

        // Assert
        assertThat(startpunkt).isEqualTo(BEREGNING);
    }

    @Test
    void startpunkt_uttak_dersom_søknad_er_endringssøknad_uten_gradering() {
        // Arrange
        var originalBehandling = lagFørstegangsBehandling();

        var revurdering = lagRevurdering(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);

        var førsteuttaksdato = LocalDate.now();
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medFørsteUttaksdato(førsteuttaksdato).medUtledetSkjæringstidspunkt(førsteuttaksdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandling.getId())).thenReturn(skjæringstidspunkt);

        opprettYtelsesFordelingMedGradering(revurdering, AG1, BigDecimal.ZERO);
        lagreEndringssøknad(revurdering);

        // Act
        var startpunkt = utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering), skjæringstidspunkt, 1L, 2L);

        // Assert
        assertThat(startpunkt).isEqualTo(UTTAKSVILKÅR);
    }


    private Behandling lagFørstegangsBehandling() {
        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        return førstegangScenario.lagre(repositoryProvider);
    }

    private Behandling lagRevurdering(Behandling originalBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING);
        revurderingScenario.medOriginalBehandling(originalBehandling, behandlingÅrsakType);
        return revurderingScenario.lagre(repositoryProvider);
    }

    private void opprettYtelsesFordeling(Behandling revurdering, String arbeidsgiver) {
        opprettYtelsesFordelingMedGradering(revurdering, arbeidsgiver, BigDecimal.ZERO);
    }

    private void opprettYtelsesFordelingMedGradering(Behandling behandling, String arbeidsgiver, BigDecimal arbeidsProsent) {
        var periode = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now(), LocalDate.now().plusDays(7))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(arbeidsgiver))
            .medArbeidsprosent(arbeidsProsent)
            .build();
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var oppgittFordeling = new OppgittFordelingEntitet(List.of(periode), true);
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOppgittFordeling(oppgittFordeling);

        ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());
    }

    private void lagreEndringssøknad(Behandling behandling) {
        byggFamilieHendelse(behandling.getId());
        var søknad = new SøknadEntitet.Builder()
            .medElektroniskRegistrert(true)
            .medSøknadsdato(LocalDate.now())
            .medMottattDato(LocalDate.now())
            .medErEndringssøknad(true)
            .build();
        søknadRepository.lagreOgFlush(behandling, søknad);
    }

    private FamilieHendelseEntitet byggFamilieHendelse(Long behandlingId) {
        var søknadHendelse = repositoryProvider.getFamilieHendelseRepository()
            .opprettBuilderForSøknad(behandlingId)
            .medAntallBarn(1);
        søknadHendelse.medTerminbekreftelse(søknadHendelse.getTerminbekreftelseBuilder()
            .medTermindato(LocalDate.now())
            .medUtstedtDato(LocalDate.now()));
        repositoryProvider.getFamilieHendelseRepository().lagreSøknadHendelse(behandlingId, søknadHendelse);
        return repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandlingId).getSøknadVersjon();
    }


}
