package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeAleneOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAleneOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.RettOgOmsorg;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class RettOgOmsorgGrunnlagByggerTest {

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private UttakRepositoryProvider repositoryProvider = new UttakRepositoryProvider(repositoryRule.getEntityManager());

    @Test
    public void skalLeggeTilHvemSomHarRett_SøkerMorHarRettAnnenForeldreHarIkkeRett() {
        Behandling behandling = morMedRett(true, false);

        RettOgOmsorg grunnlag = byggGrunnlag(behandling);
        assertThat(grunnlag.getMorHarRett()).isTrue();
        assertThat(grunnlag.getFarHarRett()).isFalse();
    }

    @Test
    public void skalLeggeTilHvemSomHarRett_SøkerMorHarRettAnnenForeldreHarRett() {
        Behandling behandling = morMedRett(true, true);

        RettOgOmsorg grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getMorHarRett()).isTrue();
        assertThat(grunnlag.getFarHarRett()).isTrue();
    }

    @Test
    public void skalLeggeTilHvemSomHarRett_SøkerFarHarRettAnnenForeldreHarRett() {
        Behandling behandling = farMedRett(true, true);

        RettOgOmsorg grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getMorHarRett()).isTrue();
        assertThat(grunnlag.getFarHarRett()).isTrue();
    }

    @Test
    public void skalLeggeTilHvemSomHarRett_SøkerFarHarRettAnnenForeldreHarIkkeRett() {
        Behandling behandling = farMedRett(true, false);

        RettOgOmsorg grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getMorHarRett()).isFalse();
        assertThat(grunnlag.getFarHarRett()).isTrue();
    }

    @Test
    public void skalLeggeTilHvemSomHarRett_SøkerFarHarIkkeRettAnnenForeldreHarIkkeRett() {
        Behandling behandling = farMedRett(false, false);

        RettOgOmsorg grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getMorHarRett()).isFalse();
        assertThat(grunnlag.getFarHarRett()).isFalse();
    }

    @Test
    public void skalLeggeTilHvemSomHarRett_SøkerFarHarIkkeRettAnnenForeldreHarRett() {
        Behandling behandling = farMedRett(false, true);

        RettOgOmsorg grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getMorHarRett()).isTrue();
        assertThat(grunnlag.getFarHarRett()).isFalse();
    }

    @Test
    public void skalLeggeTilHvemSomHarRett_SøkerMorHarIkkeRettAnnenForeldreHarRett() {
        Behandling behandling = morMedRett(false, true);

        RettOgOmsorg grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getMorHarRett()).isFalse();
        assertThat(grunnlag.getFarHarRett()).isTrue();
    }

    @Test
    public void skalLeggeHarAleneomsorgHvisAleneomsorg() {
        Behandling behandling = medAleneomsorg(true);

        RettOgOmsorg grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getAleneomsorg()).isTrue();
    }

    @Test
    public void skalIkkeLeggeTilHarAleneomsorgHvisIkkeAleneomsorg() {
        Behandling behandling = medAleneomsorg(false);

        RettOgOmsorg grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getAleneomsorg()).isFalse();
    }

    private Behandling medAleneomsorg(boolean harAleneomsorg) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medFordeling(new OppgittFordelingEntitet(Collections.emptyList(), true));

        scenario.medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        if (harAleneomsorg) {
            PerioderAleneOmsorgEntitet perioderAleneOmsorg = new PerioderAleneOmsorgEntitet();
            perioderAleneOmsorg.leggTil(new PeriodeAleneOmsorgEntitet(LocalDate.now(), LocalDate.now()));
            scenario.medPeriodeMedAleneomsorg(perioderAleneOmsorg);
        }

        return scenario.lagre(repositoryProvider);
    }

    private RettOgOmsorgGrunnlagBygger grunnlagBygger() {
        return new RettOgOmsorgGrunnlagBygger(repositoryProvider);
    }

    private Behandling morMedRett(boolean søkerHarRett, boolean annenForelderHarRett) {
        return scenarioMedRett(ScenarioMorSøkerForeldrepenger.forFødsel(), søkerHarRett, annenForelderHarRett);
    }

    private Behandling farMedRett(boolean søkerHarRett, boolean annenForelderHarRett) {
        return scenarioMedRett(ScenarioFarSøkerForeldrepenger.forFødsel(), søkerHarRett, annenForelderHarRett);
    }

    private Behandling scenarioMedRett(AbstractTestScenario<?> scenario, boolean søkerRett, boolean annenForelderHarRett) {
        scenario.medFordeling(new OppgittFordelingEntitet(Collections.emptyList(), true));
        scenario.medOppgittRettighet(new OppgittRettighetEntitet(annenForelderHarRett, true, false));

        if (!søkerRett) {
            scenario.medVilkårResultatType(VilkårResultatType.AVSLÅTT);
        }
        return scenario.lagre(repositoryProvider);
    }

    private RettOgOmsorg byggGrunnlag(Behandling behandling) {
        RettOgOmsorgGrunnlagBygger bygger = grunnlagBygger();
        ForeldrepengerGrunnlag fpGrunnlag = new ForeldrepengerGrunnlag();
        return bygger.byggGrunnlag(new UttakInput(BehandlingReferanse.fra(behandling), null, fpGrunnlag)).build();
    }

}
