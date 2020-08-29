package no.nav.foreldrepenger.familiehendelse.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.threeten.extra.Interval;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.IntervallUtil;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.event.FamiliehendelseEventPubliserer;

public class FamilieHendelseTjenesteImplTest {

    private static final LocalDate NÅ = LocalDate.now();
    private static final LocalDate FØDSELSDATO_BARN = LocalDate.of(2017, Month.JANUARY, 1);

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private final BasisPersonopplysningTjeneste personopplysningTjeneste = new BasisPersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository());
    private final FamilieHendelseTjeneste tjeneste = new FamilieHendelseTjeneste(personopplysningTjeneste, mock(FamiliehendelseEventPubliserer.class), repositoryProvider);

    @Test
    public void skal_uttrekke_gyldig_fødselsperiode_for_barn_som_fom_en_dag_før_tom_en_dag_etter_dersom_fødselsdato_er_oppgitt() {
        final ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().leggTilBarn(FØDSELSDATO_BARN);
        final Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        List<Interval> actually = tjeneste.beregnGyldigeFødselsperioder(lagRef(behandling));
        // Assert
        assertThat(actually).containsExactly(IntervallUtil.byggIntervall(FØDSELSDATO_BARN.minusDays(1), FØDSELSDATO_BARN.plusDays(1)));
    }

    @Test
    public void skal_uttrekke_gyldig_fødselsperiode_for_barn_som_fom_16_uker_før_tom_4_uker_etter_termindato_dersom_termindato_er_oppgitt() {
        // Arrange
        LocalDate termindato = NÅ.plusWeeks(16);
        final ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medNavnPå("LEGENS ISNDASD ASD")
                .medUtstedtDato(termindato)
                .medTermindato(termindato));
        final Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        List<Interval> actually = tjeneste.beregnGyldigeFødselsperioder(lagRef(behandling));

        // Assert
        final Interval expected = IntervallUtil.byggIntervall(termindato.minusWeeks(16), termindato.plusWeeks(4));
        assertThat(actually).containsExactly(expected);
    }

    @Test
    public void skal_uttrekke_gyldig_fødselsperioder_for_barn_som_eksakt_dag_dersom_fødseldatoer_for_adopsjon_er_oppgitt() {
        // Arrange
        LocalDate fødselsdatoBarn = NÅ;
        final ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
            .medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(NÅ))
            .leggTilBarn(fødselsdatoBarn);
        final Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        List<Interval> actually = tjeneste.beregnGyldigeFødselsperioder(lagRef(behandling));

        // Assert
        final Interval expected = IntervallUtil.byggIntervall(fødselsdatoBarn, fødselsdatoBarn);
        assertThat(actually).containsExactly(expected);
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }

    @Test
    public void skal_uttrekke_tom_liste_for_gyldige_perioder_dersom_fødselsdato_ikke_er_oppgitt() {
        // Arrange
        final ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        final Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        List<Interval> actually = tjeneste.beregnGyldigeFødselsperioder(lagRef(behandling));

        // Assert
        assertThat(actually).isEmpty();
    }

    @Test
    public void lagre_bekreftet_når_finnes_ovst_fødsel() {
        // Arrange
        LocalDate tdato = LocalDate.now().minusDays(2);
        final ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medNavnPå("ASDASD ASD ASD")
                .medUtstedtDato(LocalDate.now())
                .medTermindato(tdato))
            .medAntallBarn(1);
        scenario.medOverstyrtHendelse(scenario.medOverstyrtHendelse()
            .medFødselsDato(tdato)
            .medAntallBarn(1));
        final Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        tjeneste.oppdaterFødselPåGrunnlag(behandling, List.of(new FødtBarnInfo.Builder().medFødselsdato(tdato).medIdent(new PersonIdent("11111111111")).build()));

        // Assert
        var aggregat = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(aggregat.getBekreftetVersjon()).isPresent();
        assertThat(aggregat.getOverstyrtVersjon()).isEmpty();
        // Skal bli kopiert fra søknad
        assertThat(aggregat.getBekreftetVersjon().flatMap(FamilieHendelseEntitet::getTerminbekreftelse)).isPresent();
    }
}
