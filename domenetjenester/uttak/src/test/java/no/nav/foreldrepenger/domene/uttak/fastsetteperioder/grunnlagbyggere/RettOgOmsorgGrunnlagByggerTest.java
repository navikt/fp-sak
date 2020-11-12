package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
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
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryProviderForTest;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.RettOgOmsorg;

public class RettOgOmsorgGrunnlagByggerTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryProviderForTest();

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
            PerioderAleneOmsorgEntitet perioderAleneOmsorg = new PerioderAleneOmsorgEntitet(true);
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
        scenario.medOppgittRettighet(new OppgittRettighetEntitet(annenForelderHarRett, true, false));

        if (!søkerRett) {
            var behandlingsresultat = behandlingsresultatMedAvslåttVilkår();
            scenario.medBehandlingsresultat(behandlingsresultat);
        } return scenario.lagre(repositoryProvider);
    }

    private Behandlingsresultat behandlingsresultatMedAvslåttVilkår() {
        var vilkårBuilder = VilkårResultat.builder().medVilkårResultatType(VilkårResultatType.AVSLÅTT);
        vilkårBuilder.leggTilVilkårResultat(VilkårType.ADOPSJONSVILKARET_FORELDREPENGER,
            VilkårUtfallType.IKKE_OPPFYLT, VilkårUtfallMerknad.UDEFINERT, null, Avslagsårsak.UDEFINERT, false,
            false, null, null);
        var behandlingsresultat = Behandlingsresultat.builderForInngangsvilkår().build();
        behandlingsresultat.medOppdatertVilkårResultat(vilkårBuilder.build());
        return behandlingsresultat;
    }

    private RettOgOmsorg byggGrunnlag(Behandling behandling) {
        RettOgOmsorgGrunnlagBygger bygger = grunnlagBygger();
        ForeldrepengerGrunnlag fpGrunnlag = new ForeldrepengerGrunnlag();
        return bygger.byggGrunnlag(new UttakInput(BehandlingReferanse.fra(behandling), null, fpGrunnlag)).build();
    }

}
