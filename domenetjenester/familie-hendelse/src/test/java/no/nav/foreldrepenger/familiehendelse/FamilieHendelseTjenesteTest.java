package no.nav.foreldrepenger.familiehendelse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.familiehendelse.event.FamiliehendelseEventPubliserer;
import no.nav.fpsak.tidsserie.LocalDateInterval;

class FamilieHendelseTjenesteTest extends EntityManagerAwareTest {

    private static final LocalDate NÅ = LocalDate.now();
    private static final LocalDate FØDSELSDATO_BARN = LocalDate.of(2017, Month.JANUARY, 1);

    private BehandlingRepositoryProvider repositoryProvider;

    private FamilieHendelseTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        tjeneste = new FamilieHendelseTjeneste(mock(FamiliehendelseEventPubliserer.class),
            repositoryProvider.getFamilieHendelseRepository());
    }

    @Test
    void skal_uttrekke_gyldig_fødselsperiode_for_barn_som_fom_en_dag_før_tom_en_dag_etter_dersom_fødselsdato_er_oppgitt() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().leggTilBarn(FØDSELSDATO_BARN);
        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var actually = tjeneste.forventetFødselsIntervaller(lagRef(behandling));
        // Assert
        assertThat(actually).containsExactly(new LocalDateInterval(FØDSELSDATO_BARN.minusWeeks(6), FØDSELSDATO_BARN.plusWeeks(6)));
    }

    @Test
    void skal_uttrekke_gyldig_fødselsperiode_for_barn_som_fom_16_uker_før_tom_4_uker_etter_termindato_dersom_termindato_er_oppgitt() {
        // Arrange
        var termindato = NÅ.plusWeeks(16);
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medNavnPå("LEGENS ISNDASD ASD")
                .medUtstedtDato(termindato)
                .medTermindato(termindato));
        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var actually = tjeneste.forventetFødselsIntervaller(lagRef(behandling));

        // Assert
        var expected = new LocalDateInterval(termindato.minusWeeks(19), termindato.plusWeeks(6));
        assertThat(actually).containsExactly(expected);
    }

    @Test
    void skal_uttrekke_gyldig_fødselsperioder_for_barn_som_eksakt_dag_dersom_fødseldatoer_for_adopsjon_er_oppgitt() {
        // Arrange
        var fødselsdatoBarn = NÅ;
        var fødselsdatoBarn2 = NÅ.minusYears(2);
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
            .medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(NÅ))
            .leggTilBarn(fødselsdatoBarn).leggTilBarn(fødselsdatoBarn2);
        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var actually = tjeneste.forventetFødselsIntervaller(lagRef(behandling));

        // Assert
        var expected = new LocalDateInterval(fødselsdatoBarn.minusWeeks(6), fødselsdatoBarn.plusWeeks(6));
        var expected2 = new LocalDateInterval(fødselsdatoBarn2.minusWeeks(6), fødselsdatoBarn2.plusWeeks(6));
        assertThat(actually).containsExactlyInAnyOrder(expected, expected2);
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }

    @Test
    void skal_uttrekke_tom_liste_for_gyldige_perioder_dersom_fødselsdato_ikke_er_oppgitt() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var actually = tjeneste.forventetFødselsIntervaller(lagRef(behandling));

        // Assert
        assertThat(actually).isEmpty();
    }

    @Test
    void lagre_bekreftet_når_finnes_ovst_fødsel() {
        // Arrange
        var tdato = LocalDate.now().minusDays(2);
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medNavnPå("ASDASD ASD ASD")
                .medUtstedtDato(LocalDate.now())
                .medTermindato(tdato))
            .medAntallBarn(1);
        scenario.medOverstyrtHendelse(scenario.medOverstyrtHendelse()
            .medFødselsDato(tdato)
            .medAntallBarn(1));
        var behandling = scenario.lagre(repositoryProvider);

        // Act
        tjeneste.oppdaterFødselPåGrunnlag(behandling, List.of(new FødtBarnInfo.Builder().medFødselsdato(tdato).medIdent(new PersonIdent("11111111111")).build()));

        // Assert
        var aggregat = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(aggregat.getBekreftetVersjon()).isPresent();
        assertThat(aggregat.getOverstyrtVersjon()).isEmpty();
        // Skal bli kopiert fra søknad
        assertThat(aggregat.getBekreftetVersjon().flatMap(FamilieHendelseEntitet::getTerminbekreftelse)).isPresent();
    }

    @Test
    void sjekk_intervaller_bekreftet_termin() {
        var grunnlagBuilder = FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty());
        var søknadHendelseBuilder = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.SØKNAD);
        var termindato = LocalDate.of(2018, Month.MAY, 6);
        søknadHendelseBuilder.medAntallBarn(1)
            .medTerminbekreftelse(
                søknadHendelseBuilder.getTerminbekreftelseBuilder().medTermindato(termindato).medUtstedtDato(LocalDate.of(2018, Month.FEBRUARY, 6)));
        grunnlagBuilder.medSøknadVersjon(søknadHendelseBuilder);
        var saksbehandlerHendelseBuilder = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.OVERSTYRT);
        saksbehandlerHendelseBuilder.medAntallBarn(1)
            .medTerminbekreftelse(saksbehandlerHendelseBuilder.getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medUtstedtDato(LocalDate.of(2018, Month.FEBRUARY, 6)));
        grunnlagBuilder.medOverstyrtVersjon(saksbehandlerHendelseBuilder);

        var grunnlag = grunnlagBuilder.build();

        var intervaller = FamilieHendelseTjeneste.utledPerioderForRegisterinnhenting(grunnlag);

        assertThat(intervaller).hasSize(1);
        assertThat(intervaller.get(0)).isEqualByComparingTo(new LocalDateInterval(termindato.minusWeeks(19),
            termindato.plusWeeks(6)));
    }


}
