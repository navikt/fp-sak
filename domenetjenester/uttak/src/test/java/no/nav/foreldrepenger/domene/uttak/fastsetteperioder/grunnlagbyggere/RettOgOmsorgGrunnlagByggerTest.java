package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.RettOgOmsorg;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Rettighetstype;

class RettOgOmsorgGrunnlagByggerTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();

    @Test
    void skalLeggeTilHvemSomHarRett_SøkerMorAnnenForeldreHarIkkeRett() {
        var behandling = morMedRett(false).lagre(repositoryProvider);

        var grunnlag = byggGrunnlag(behandling);
        assertThat(grunnlag.rettighetsType()).isEqualTo(Rettighetstype.BARE_MOR_RETT);
        assertThat(grunnlag.getMorOppgittUføretrygd()).isFalse();
    }

    @Test
    void skalLeggeTilHvemSomHarRett_SøkerMorAnnenForeldreHarRett() {
        var behandling = morMedRett(true).lagre(repositoryProvider);

        var grunnlag = byggGrunnlag(behandling);
        assertThat(grunnlag.rettighetsType()).isEqualTo(Rettighetstype.BEGGE_RETT);
    }

    @Test
    void skalLeggeTilHvemSomHarRett_SøkerFarAnnenForeldreHarRett() {
        var behandling = farMedRett(true).lagre(repositoryProvider);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.rettighetsType()).isEqualTo(Rettighetstype.BEGGE_RETT);
        assertThat(grunnlag.getMorOppgittUføretrygd()).isFalse();
    }

    @Test
    void skalLeggeTilHvemSomHarRett_SøkerFarAnnenForeldreHarIkkeRett() {
        var behandling = farMedRett(false).lagre(repositoryProvider);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.rettighetsType()).isEqualTo(Rettighetstype.BARE_FAR_RETT);
        assertThat(grunnlag.getMorOppgittUføretrygd()).isFalse();
    }

    @Test
    void skalLeggeTilHvemSomHarRett_SøkerFarHarAnnenForeldreHarRett() {
        var behandling = farMedRett(true).lagre(repositoryProvider);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.rettighetsType()).isEqualTo(Rettighetstype.BEGGE_RETT);
        assertThat(grunnlag.getMorOppgittUføretrygd()).isFalse();
    }

    @Test
    void skalLeggeHarAleneomsorgHvisAleneomsorg() {
        var scenario = medAleneomsorg();
        scenario.medOverstyrtRettighet(new OppgittRettighetEntitet(false, true, null, null, null));
        var behandling = scenario.lagre(repositoryProvider);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.rettighetsType()).isEqualTo(Rettighetstype.ALENEOMSORG);
        assertThat(grunnlag.getMorOppgittUføretrygd()).isFalse();
    }

    @Test
    void skalIkkeLeggeTilHarAleneomsorgHvisIkkeAleneomsorg() {
        var scenario = medAleneomsorg();
        scenario.medOverstyrtRettighet(new OppgittRettighetEntitet(true, false, null, null, null));
        var behandling = scenario.lagre(repositoryProvider);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.rettighetsType()).isEqualTo(Rettighetstype.BEGGE_RETT);
        assertThat(grunnlag.getMorOppgittUføretrygd()).isFalse();
    }

    @Test
    void skalLeggeTilOppgittOgRegisterUføre() {
        var behandling = bareFarMedRett(true, false).medOverstyrtRettighet(new OppgittRettighetEntitet(false, false, null, null, null))
            .lagre(repositoryProvider);

        var grunnlag = byggGrunnlagMedRegisterUføre(behandling, true);

        assertThat(grunnlag.rettighetsType()).isEqualTo(Rettighetstype.BARE_FAR_RETT_MOR_UFØR);
        assertThat(grunnlag.getMorOppgittUføretrygd()).isTrue();
    }

    @Test
    void skalLeggeTilOppgittOgIkkeRegisterUføre() {
        var behandling = bareFarMedRett(true, false).medOverstyrtRettighet(new OppgittRettighetEntitet(false, false, null, null, null))
            .lagre(repositoryProvider);

        var grunnlag = byggGrunnlagMedRegisterUføre(behandling, false);

        assertThat(grunnlag.rettighetsType()).isEqualTo(Rettighetstype.BARE_FAR_RETT);
        assertThat(grunnlag.getMorOppgittUføretrygd()).isTrue();
    }

    @Test
    void skalLeggeTilOppgittIkkeRegisterMenOverstyrtUføre() {
        var behandling = bareFarMedRett(true, false).medOverstyrtRettighet(new OppgittRettighetEntitet(false, false, true, null, null))
            .lagre(repositoryProvider);

        var grunnlag = byggGrunnlagMedRegisterUføre(behandling, false);

        assertThat(grunnlag.rettighetsType()).isEqualTo(Rettighetstype.BARE_FAR_RETT_MOR_UFØR);
        assertThat(grunnlag.getMorOppgittUføretrygd()).isTrue();
    }

    @Test
    void skalLeggeTilOppgittIkkeRegisterMenOverstyrtIkkeUføre() {
        var behandling = bareFarMedRett(true, false).medOverstyrtRettighet(new OppgittRettighetEntitet(false, false, false, null, null))
            .lagre(repositoryProvider);

        var grunnlag = byggGrunnlagMedRegisterUføre(behandling, false);

        assertThat(grunnlag.rettighetsType()).isEqualTo(Rettighetstype.BARE_FAR_RETT);
        assertThat(grunnlag.getMorOppgittUføretrygd()).isTrue();
    }


    @Test
    void skalLeggeTilOppgittOgBekreftetEØS() {
        var scenario = bareFarMedRett(false, true);
        scenario.medOverstyrtRettighet(new OppgittRettighetEntitet(null, null, null, true, true));
        var behandling = scenario.lagre(repositoryProvider);
        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.rettighetsType()).isEqualTo(Rettighetstype.BEGGE_RETT);
    }

    @Test
    void skalLeggeTilOppgittOgAvkreftetEØS() {
        var scenario = bareFarMedRett(false, true);
        scenario.medOverstyrtRettighet(new OppgittRettighetEntitet(null, null, null, false, false));
        var behandling = scenario.lagre(repositoryProvider);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.rettighetsType()).isEqualTo(Rettighetstype.BARE_FAR_RETT);
    }

    @Test
    void skal_bruke_overstyrt_rettighetstype() {
        var scenario = bareFarMedRett(false, true);
        var behandling = scenario.lagre(repositoryProvider);
        var yfa = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());
        var overstyrtYfa = YtelseFordelingAggregat.oppdatere(yfa)
            .medOverstyrtRettighetstype(no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.Rettighetstype.ALENEOMSORG)
            .build();
        repositoryProvider.getYtelsesFordelingRepository().lagre(behandling.getId(), overstyrtYfa);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.rettighetsType()).isEqualTo(Rettighetstype.ALENEOMSORG);
    }

    private AbstractTestScenario<?> medAleneomsorg() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medFordeling(new OppgittFordelingEntitet(Collections.emptyList(), true));

        scenario.medOppgittRettighet(OppgittRettighetEntitet.aleneomsorg());
        return scenario;
    }

    private RettOgOmsorgGrunnlagBygger grunnlagBygger() {
        var repositoryProvider = this.repositoryProvider;
        return new RettOgOmsorgGrunnlagBygger(repositoryProvider, new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository()));
    }

    private AbstractTestScenario<?> morMedRett(boolean annenForelderHarRett) {
        return scenarioMedRett(ScenarioMorSøkerForeldrepenger.forFødsel(), annenForelderHarRett);
    }

    private AbstractTestScenario<?> farMedRett(boolean annenForelderHarRett) {
        return scenarioMedRett(ScenarioFarSøkerForeldrepenger.forFødsel(), annenForelderHarRett);
    }

    private AbstractTestScenario<?> bareFarMedRett(boolean morOppgittUføre, boolean morOppgittEØS) {
        return scenarioMedRett(ScenarioFarSøkerForeldrepenger.forFødsel(), false, morOppgittUføre, morOppgittEØS);
    }

    private AbstractTestScenario<?> scenarioMedRett(AbstractTestScenario<?> scenario, boolean annenForelderHarRett) {
        return scenarioMedRett(scenario, annenForelderHarRett, false, false);
    }

    private AbstractTestScenario<?> scenarioMedRett(AbstractTestScenario<?> scenario,
                                                    boolean annenForelderHarRett,
                                                    boolean morOppgittUføre,
                                                    boolean morOppgittEØS) {
        scenario.medFordeling(new OppgittFordelingEntitet(Collections.emptyList(), true));
        scenario.medOppgittRettighet(new OppgittRettighetEntitet(annenForelderHarRett, false, morOppgittUføre, morOppgittEØS, morOppgittEØS));
        return scenario;
    }

    private RettOgOmsorg byggGrunnlag(Behandling behandling) {
        var bygger = grunnlagBygger();
        var fpGrunnlag = new ForeldrepengerGrunnlag();
        return bygger.byggGrunnlag(new UttakInput(BehandlingReferanse.fra(behandling), null, null, fpGrunnlag)).build();
    }

    private RettOgOmsorg byggGrunnlagMedRegisterUføre(Behandling behandling, boolean uføreVerdi) {
        var bygger = grunnlagBygger();
        var fpGrunnlag = new ForeldrepengerGrunnlag().medUføretrygdGrunnlag(UføretrygdGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medRegisterUføretrygd(uføreVerdi, LocalDate.now(), LocalDate.now())
            .medBehandlingId(behandling.getId())
            .medAktørIdUføretrygdet(uføreVerdi ? AktørId.dummy() : null)
            .build());
        return bygger.byggGrunnlag(new UttakInput(BehandlingReferanse.fra(behandling), null, null, fpGrunnlag)).build();
    }

}
