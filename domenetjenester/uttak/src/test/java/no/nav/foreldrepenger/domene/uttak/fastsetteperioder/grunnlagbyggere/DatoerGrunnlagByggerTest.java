package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.domene.uttak.PersonopplysningerForUttak;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.input.YtelsespesifiktGrunnlag;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Datoer;

class DatoerGrunnlagByggerTest {

    private final LocalDate førsteUttaksdato = LocalDate.now().minusWeeks(12);
    private UttakRepositoryProvider repositoryProvider;
    private PersonopplysningerForUttak personopplysninger;

    @BeforeEach
    void setUp() {
        repositoryProvider = new UttakRepositoryStubProvider();
        personopplysninger = mock(PersonopplysningerForUttak.class);
    }

    @Test
    void skal_ha_familiehendelsedato() {
        var familiehendelsedato = LocalDate.now().plusWeeks(3);
        var behandling = scenarioMedDatoer(ScenarioMorSøkerForeldrepenger.forFødsel(), null);

        var familieHendelse = FamilieHendelse.forFødsel(null, familiehendelsedato, List.of(), 0);
        var grunnlag = byggGrunnlag(lagInput(behandling, familieHendelse));
        assertThat(grunnlag.getFamiliehendelse()).isEqualTo(familiehendelsedato);
    }

    @Test
    void søker_har_ingen_dødsdato() {
        LocalDate søkersDødsdato = null;
        var behandling = scenarioMedDatoer(ScenarioMorSøkerForeldrepenger.forFødsel(), søkersDødsdato);

        var familieHendelse = FamilieHendelse.forFødsel(LocalDate.now(), null, List.of(), 0);
        var grunnlag = byggGrunnlag(lagInput(behandling, familieHendelse));
        assertThat(grunnlag.getDødsdatoer().getSøkersDødsdato()).isEqualTo(søkersDødsdato);
    }

    @Test
    void søker_har_dødsdato() {
        var familiehendelsedato = LocalDate.now().minusWeeks(1);
        var søkersDødsdato = familiehendelsedato.plusDays(2);
        var behandling = scenarioMedDatoer(ScenarioMorSøkerForeldrepenger.forFødsel(), søkersDødsdato);

        var familieHendelse = FamilieHendelse.forFødsel(familiehendelsedato, null, List.of(), 0);
        var grunnlag = byggGrunnlag(lagInput(behandling, familieHendelse));
        assertThat(grunnlag.getDødsdatoer().getSøkersDødsdato()).isEqualTo(søkersDødsdato);
    }

    @Test
    void barn_har_dødsdato() {
        var familiehendelsedato = LocalDate.now().plusWeeks(3);
        LocalDate søkersDødsdato = null;
        var barnsDødsdato = familiehendelsedato.plusWeeks(1);
        var behandling = scenarioMedDatoer(ScenarioMorSøkerForeldrepenger.forFødsel(), søkersDødsdato);

        var familieHendelse = FamilieHendelse.forFødsel(null, familiehendelsedato,
            List.of(new Barn(barnsDødsdato)), 1);
        var grunnlag = byggGrunnlag(lagInput(behandling, familieHendelse));
        assertThat(grunnlag.getDødsdatoer().getBarnsDødsdato()).isEqualTo(barnsDødsdato);
        assertThat(grunnlag.getDødsdatoer().erAlleBarnDøde()).isTrue();
    }

    @Test
    void barn_har_dødsdato_men_flere_barn() {
        var familiehendelsedato = LocalDate.now().plusWeeks(3);
        LocalDate søkersDødsdato = null;
        var barnsDødsdato = familiehendelsedato.plusWeeks(1);
        var behandling = scenarioMedDatoer(ScenarioMorSøkerForeldrepenger.forFødsel(), søkersDødsdato);
        var familieHendelse = FamilieHendelse.forFødsel(null, familiehendelsedato,
            List.of(new Barn(barnsDødsdato), new Barn()), 1);
        var grunnlag = byggGrunnlag(lagInput(behandling, familieHendelse));
        assertThat(grunnlag.getDødsdatoer().getBarnsDødsdato()).isEqualTo(barnsDødsdato);
        assertThat(grunnlag.getDødsdatoer().erAlleBarnDøde()).isFalse();
    }

    private UttakInput lagInput(Behandling behandling, FamilieHendelse bekreftetFamilieHendelse) {
        var ref = BehandlingReferanse.fra(behandling);
        var familieHendelser = new FamilieHendelser().medBekreftetHendelse(bekreftetFamilieHendelse);
        YtelsespesifiktGrunnlag ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(
            familieHendelser);
        return new UttakInput(ref, Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(førsteUttaksdato).build(), null, ytelsespesifiktGrunnlag);
    }

    private Behandling scenarioMedDatoer(AbstractTestScenario<?> scenario,
                                         LocalDate søkersDødsdato) {
        var behandling = scenario.lagre(repositoryProvider);

        leggTilSøkersDødsdato(behandling, søkersDødsdato);
        lagreUttaksperiodegrense(repositoryProvider.getUttaksperiodegrenseRepository(), behandling.getId());

        return behandling;
    }

    private void leggTilSøkersDødsdato(Behandling behandling, LocalDate søkersDødsdato) {
        when(personopplysninger.søkersDødsdato(BehandlingReferanse.fra(behandling))).thenReturn(
            Optional.ofNullable(søkersDødsdato));
    }

    private void lagreUttaksperiodegrense(UttaksperiodegrenseRepository repository,
                                          Long behandlingId) {
        var grense = new Uttaksperiodegrense(LocalDate.now().minusWeeks(2));
        repository.lagre(behandlingId, grense);
    }

    private DatoerGrunnlagBygger grunnlagBygger() {
        return new DatoerGrunnlagBygger(personopplysninger);
    }

    private Datoer byggGrunnlag(UttakInput input) {
        return grunnlagBygger().byggGrunnlag(input).build();
    }

}
