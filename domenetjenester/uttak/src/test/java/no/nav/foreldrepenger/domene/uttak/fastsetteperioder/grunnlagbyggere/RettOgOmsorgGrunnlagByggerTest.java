package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAleneOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.RettOgOmsorg;

public class RettOgOmsorgGrunnlagByggerTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();

    @Test
    public void skalLeggeTilHvemSomHarRett_SøkerMorHarRettAnnenForeldreHarIkkeRett() {
        var behandling = morMedRett(true, false);

        var grunnlag = byggGrunnlag(behandling);
        assertThat(grunnlag.getMorHarRett()).isTrue();
        assertThat(grunnlag.getFarHarRett()).isFalse();
    }

    @Test
    public void skalLeggeTilHvemSomHarRett_SøkerMorHarRettAnnenForeldreHarRett() {
        var behandling = morMedRett(true, true);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getMorHarRett()).isTrue();
        assertThat(grunnlag.getFarHarRett()).isTrue();
    }

    @Test
    public void skalLeggeTilHvemSomHarRett_SøkerFarHarRettAnnenForeldreHarRett() {
        var behandling = farMedRett(true, true);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getMorHarRett()).isTrue();
        assertThat(grunnlag.getFarHarRett()).isTrue();
    }

    @Test
    public void skalLeggeTilHvemSomHarRett_SøkerFarHarRettAnnenForeldreHarIkkeRett() {
        var behandling = farMedRett(true, false);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getMorHarRett()).isFalse();
        assertThat(grunnlag.getFarHarRett()).isTrue();
    }

    @Test
    public void skalLeggeTilHvemSomHarRett_SøkerFarHarIkkeRettAnnenForeldreHarIkkeRett() {
        var behandling = farMedRett(false, false);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getMorHarRett()).isFalse();
        assertThat(grunnlag.getFarHarRett()).isFalse();
    }

    @Test
    public void skalLeggeTilHvemSomHarRett_SøkerFarHarIkkeRettAnnenForeldreHarRett() {
        var behandling = farMedRett(false, true);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getMorHarRett()).isTrue();
        assertThat(grunnlag.getFarHarRett()).isFalse();
    }

    @Test
    public void skalLeggeTilHvemSomHarRett_SøkerMorHarIkkeRettAnnenForeldreHarRett() {
        var behandling = morMedRett(false, true);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getMorHarRett()).isFalse();
        assertThat(grunnlag.getFarHarRett()).isTrue();
    }

    @Test
    public void skalLeggeHarAleneomsorgHvisAleneomsorg() {
        var behandling = medAleneomsorg(true);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getAleneomsorg()).isTrue();
    }

    @Test
    public void skalIkkeLeggeTilHarAleneomsorgHvisIkkeAleneomsorg() {
        var behandling = medAleneomsorg(false);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getAleneomsorg()).isFalse();
    }

    private Behandling medAleneomsorg(boolean harAleneomsorg) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medFordeling(new OppgittFordelingEntitet(Collections.emptyList(), true));

        scenario.medOppgittRettighet(new OppgittRettighetEntitet(true, false, false, false));
        if (harAleneomsorg) {
            var perioderAleneOmsorg = new PerioderAleneOmsorgEntitet(true);
            scenario.medPeriodeMedAleneomsorg(perioderAleneOmsorg);
        }

        return scenario.lagre(repositoryProvider);
    }

    private RettOgOmsorgGrunnlagBygger grunnlagBygger() {
        var repositoryProvider = this.repositoryProvider;
        return new RettOgOmsorgGrunnlagBygger(repositoryProvider,
            new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository()));
    }

    private Behandling morMedRett(boolean søkerHarRett, boolean annenForelderHarRett) {
        return scenarioMedRett(ScenarioMorSøkerForeldrepenger.forFødsel(), søkerHarRett, annenForelderHarRett);
    }

    private Behandling farMedRett(boolean søkerHarRett, boolean annenForelderHarRett) {
        return scenarioMedRett(ScenarioFarSøkerForeldrepenger.forFødsel(), søkerHarRett, annenForelderHarRett);
    }

    private Behandling scenarioMedRett(AbstractTestScenario<?> scenario,
                                       boolean søkerRett,
                                       boolean annenForelderHarRett) {
        scenario.medFordeling(new OppgittFordelingEntitet(Collections.emptyList(), true));
        scenario.medOppgittRettighet(new OppgittRettighetEntitet(annenForelderHarRett, false, false, false));

        if (!søkerRett) {
            var behandlingsresultat = behandlingsresultatMedAvslåttVilkår();
            scenario.medBehandlingsresultat(behandlingsresultat);
        } return scenario.lagre(repositoryProvider);
    }

    private Behandlingsresultat behandlingsresultatMedAvslåttVilkår() {
        var vilkårBuilder = VilkårResultat.builder().medVilkårResultatType(VilkårResultatType.AVSLÅTT);
        vilkårBuilder.leggTilVilkårAvslått(VilkårType.ADOPSJONSVILKARET_FORELDREPENGER, VilkårUtfallMerknad.VM_1004);
        var behandlingsresultat = Behandlingsresultat.builderForInngangsvilkår().build();
        behandlingsresultat.medOppdatertVilkårResultat(vilkårBuilder.build());
        return behandlingsresultat;
    }

    private RettOgOmsorg byggGrunnlag(Behandling behandling) {
        var bygger = grunnlagBygger();
        var fpGrunnlag = new ForeldrepengerGrunnlag();
        return bygger.byggGrunnlag(new UttakInput(BehandlingReferanse.fra(behandling), null, fpGrunnlag)).build();
    }

}
