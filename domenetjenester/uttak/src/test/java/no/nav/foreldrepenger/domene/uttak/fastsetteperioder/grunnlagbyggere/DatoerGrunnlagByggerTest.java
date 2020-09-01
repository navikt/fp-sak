package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.input.YtelsespesifiktGrunnlag;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Datoer;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.Repository;

@RunWith(CdiRunner.class)
public class DatoerGrunnlagByggerTest {

    private final LocalDate førsteUttaksdato = LocalDate.now().minusWeeks(12);
    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private UttakRepositoryProvider repositoryProvider = new UttakRepositoryProvider(repositoryRule.getEntityManager());

    @Inject
    private PersonopplysningTjeneste personopplysningTjeneste;

    @Test
    public void skal_ha_familiehendelsedato() {
        LocalDate familiehendelsedato = LocalDate.now().plusWeeks(3);
        Behandling behandling = scenarioMedDatoer(ScenarioMorSøkerForeldrepenger.forFødsel(), null, førsteUttaksdato);

        FamilieHendelse familieHendelse = FamilieHendelse.forFødsel(null, familiehendelsedato, List.of(), 0);
        Datoer grunnlag = byggGrunnlag(lagInput(behandling, familieHendelse));
        assertThat(grunnlag.getFamiliehendelse()).isEqualTo(familiehendelsedato);
    }

    @Test
    public void skal_ha_første_lovlige_uttaksdato() {
        LocalDate familiehendelsedato = LocalDate.now().plusWeeks(3);
        LocalDate førsteLovligeUttaksdag = familiehendelsedato.minusWeeks(12);
        Behandling behandling = scenarioMedDatoer(ScenarioMorSøkerForeldrepenger.forFødsel(), null, førsteLovligeUttaksdag);

        var familieHendelse = FamilieHendelse.forFødsel(familiehendelsedato, null, List.of(), 0);
        Datoer grunnlag = byggGrunnlag(lagInput(behandling, familieHendelse));
        assertThat(grunnlag.getFørsteLovligeUttaksdag()).isEqualTo(førsteLovligeUttaksdag);
    }

    @Test
    public void søker_har_ingen_dødsdato() {
        LocalDate søkersDødsdato = null;
        Behandling behandling = scenarioMedDatoer(ScenarioMorSøkerForeldrepenger.forFødsel(), søkersDødsdato, førsteUttaksdato);

        FamilieHendelse familieHendelse = FamilieHendelse.forFødsel(LocalDate.now(), null, List.of(), 0);
        Datoer grunnlag = byggGrunnlag(lagInput(behandling, familieHendelse));
        assertThat(grunnlag.getDødsdatoer().getSøkersDødsdato()).isEqualTo(søkersDødsdato);
    }

    @Test
    public void søker_har_dødsdato() {
        LocalDate familiehendelsedato = LocalDate.now().minusWeeks(1);
        LocalDate søkersDødsdato = familiehendelsedato.plusDays(2);
        Behandling behandling = scenarioMedDatoer(ScenarioMorSøkerForeldrepenger.forFødsel(), søkersDødsdato, førsteUttaksdato);

        var familieHendelse = FamilieHendelse.forFødsel(familiehendelsedato, null, List.of(), 0);
        Datoer grunnlag = byggGrunnlag(lagInput(behandling, familieHendelse));
        assertThat(grunnlag.getDødsdatoer().getSøkersDødsdato()).isEqualTo(søkersDødsdato);
    }

    @Test
    public void barn_har_dødsdato() {
        LocalDate familiehendelsedato = LocalDate.now().plusWeeks(3);
        LocalDate søkersDødsdato = null;
        LocalDate barnsDødsdato = familiehendelsedato.plusWeeks(1);
        Behandling behandling = scenarioMedDatoer(ScenarioMorSøkerForeldrepenger.forFødsel(), søkersDødsdato, førsteUttaksdato);

        FamilieHendelse familieHendelse = FamilieHendelse.forFødsel(null, familiehendelsedato, List.of(new Barn(barnsDødsdato)), 1);
        Datoer grunnlag = byggGrunnlag(lagInput(behandling, familieHendelse));
        assertThat(grunnlag.getDødsdatoer().getBarnsDødsdato()).isEqualTo(barnsDødsdato);
        assertThat(grunnlag.getDødsdatoer().erAlleBarnDøde()).isEqualTo(true);
    }

    @Test
    public void barn_har_dødsdato_men_flere_barn() {
        LocalDate familiehendelsedato = LocalDate.now().plusWeeks(3);
        LocalDate søkersDødsdato = null;
        LocalDate barnsDødsdato = familiehendelsedato.plusWeeks(1);
        Behandling behandling = scenarioMedDatoer(ScenarioMorSøkerForeldrepenger.forFødsel(), søkersDødsdato, førsteUttaksdato);
        FamilieHendelse familieHendelse = FamilieHendelse.forFødsel(null, familiehendelsedato, List.of(new Barn(barnsDødsdato), new Barn()), 1);
        Datoer grunnlag = byggGrunnlag(lagInput(behandling, familieHendelse));
        assertThat(grunnlag.getDødsdatoer().getBarnsDødsdato()).isEqualTo(barnsDødsdato);
        assertThat(grunnlag.getDødsdatoer().erAlleBarnDøde()).isEqualTo(false);
    }

    private UttakInput lagInput(Behandling behandling, FamilieHendelse bekreftetFamilieHendelse) {
        var ref = BehandlingReferanse.fra(behandling, førsteUttaksdato);
        FamilieHendelser familieHendelser = new FamilieHendelser().medBekreftetHendelse(bekreftetFamilieHendelse);
        YtelsespesifiktGrunnlag ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser);
        return new UttakInput(ref, null, ytelsespesifiktGrunnlag);
    }

    private Behandling scenarioMedDatoer(AbstractTestScenario<?> scenario, LocalDate søkersDødsdato, LocalDate førsteLovligeUttaksdag) {
        Behandling behandling = scenario.lagre(repositoryProvider);

        leggTilSøkersDødsdato(behandling, søkersDødsdato);
        Repository repository = repositoryRule.getRepository();
        var br = repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId());
        repository.lagre(br);
        lagreUttaksperiodegrense(repositoryProvider.getUttaksperiodegrenseRepository(), førsteLovligeUttaksdag, behandling.getId());

        return behandling;
    }

    private void leggTilSøkersDødsdato(Behandling behandling, LocalDate søkersDødsdato) {
        PersonopplysningRepository personopplysningRepository = new PersonopplysningRepository(repositoryProvider.getEntityManager());
        final PersonInformasjonBuilder builder = personopplysningRepository.opprettBuilderForRegisterdata(behandling.getId());
        final PersonInformasjonBuilder.PersonopplysningBuilder personopplysningBuilder = builder.getPersonopplysningBuilder(behandling.getAktørId());
        personopplysningBuilder.medDødsdato(søkersDødsdato);
        builder.leggTil(personopplysningBuilder);
        personopplysningRepository.lagre(behandling.getId(), builder);
    }

    private void lagreUttaksperiodegrense(UttaksperiodegrenseRepository repository, LocalDate førsteLovligeUttaksdag, Long behandlingId) {
        var br = repositoryProvider.getBehandlingsresultatRepository().hent(behandlingId);
        var grense = new Uttaksperiodegrense.Builder(br)
            .medFørsteLovligeUttaksdag(førsteLovligeUttaksdag)
            .medMottattDato(LocalDate.now().minusWeeks(2)).build();
        repository.lagre(behandlingId, grense);
    }

    private DatoerGrunnlagBygger grunnlagBygger() {
        return new DatoerGrunnlagBygger(repositoryProvider.getUttaksperiodegrenseRepository(), personopplysningTjeneste);
    }

    private Datoer byggGrunnlag(UttakInput input) {
        return grunnlagBygger().byggGrunnlag(input).build();
    }

}
