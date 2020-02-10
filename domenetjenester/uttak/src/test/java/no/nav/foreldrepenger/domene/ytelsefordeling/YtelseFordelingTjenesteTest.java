package no.nav.foreldrepenger.domene.ytelsefordeling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeAnnenforelderHarRettEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAnnenforelderHarRettEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.UttakDokumentasjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;

public class YtelseFordelingTjenesteTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryProvider(repoRule.getEntityManager());
    private YtelseFordelingTjeneste tjeneste = new YtelseFordelingTjeneste(new YtelsesFordelingRepository(repoRule.getEntityManager()));


    @Test
    public void test_lagring_perioderuttakdokumentasjon() {
        final LocalDate enDag = LocalDate.of(2018, 3, 15);
        List<PeriodeUttakDokumentasjonEntitet> dokumentasjonPerioder = List.of(
            new PeriodeUttakDokumentasjonEntitet(enDag, enDag.plusDays(1), UttakDokumentasjonType.SYK_SØKER),
            new PeriodeUttakDokumentasjonEntitet(enDag.plusDays(4), enDag.plusDays(7), UttakDokumentasjonType.SYK_SØKER)
        );

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandling = scenario.lagre(repositoryProvider);

        OppgittPeriodeEntitet opprinnelig = mødrekvote(enDag, enDag.plusDays(7));

        OppgittFordelingEntitet oppgittPerioder = new OppgittFordelingEntitet(Collections.singletonList(opprinnelig), true);

        repositoryProvider.getYtelsesFordelingRepository().lagre(behandling.getId(), oppgittPerioder);


        OppgittPeriodeEntitet ny = mødrekvote(enDag, enDag.plusDays(7));

        tjeneste.overstyrSøknadsperioder(behandling.getId(), Collections.singletonList(ny), dokumentasjonPerioder);

        Optional<PerioderUttakDokumentasjonEntitet> perioderUttak = tjeneste.hentAggregat(behandling.getId()).getPerioderUttakDokumentasjon();

        assertThat(perioderUttak).isNotNull();
        List<PeriodeUttakDokumentasjonEntitet> perioder = perioderUttak.get().getPerioder();
        assertThat(perioder).isNotEmpty();
        assertThat(perioder).hasSize(2);
    }

    @Test
    public void test_bekreft_aksjonspunkt_annenforelder_har_ikke_rett() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandling = scenario.lagre(repositoryProvider);

        tjeneste.bekreftAnnenforelderHarRett(behandling.getId(), false);

        Optional<PerioderAnnenforelderHarRettEntitet> perioderAnnenforelderHarRett = tjeneste.hentAggregat(behandling.getId()).getPerioderAnnenforelderHarRett();

        assertThat(perioderAnnenforelderHarRett).isPresent();
        List<PeriodeAnnenforelderHarRettEntitet> perioder = perioderAnnenforelderHarRett.get().getPerioder();
        assertThat(perioder).isEmpty();
    }

    @Test
    public void test_bekreft_aksjonspunkt_annenforelder_har_rett() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandling = scenario.lagre(repositoryProvider);

        tjeneste.bekreftAnnenforelderHarRett(behandling.getId(), true);

        Optional<PerioderAnnenforelderHarRettEntitet> perioderAnnenforelderHarRett = tjeneste.hentAggregat(behandling.getId()).getPerioderAnnenforelderHarRett();

        assertThat(perioderAnnenforelderHarRett).isPresent();
        List<PeriodeAnnenforelderHarRettEntitet> perioder = perioderAnnenforelderHarRett.get().getPerioder();
        assertThat(perioder).isNotEmpty();
        assertThat(perioder).hasSize(1);
    }

    @Test
    public void skalKasteExceptionHvisOverlappIPerioder() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        var periode1 = mødrekvote(LocalDate.now(), LocalDate.now().plusWeeks(1));
        var periode2 = mødrekvote(LocalDate.now().plusWeeks(1), LocalDate.now().plusWeeks(2));
        var perioder = List.of(periode1, periode2);
        assertThatThrownBy(() -> tjeneste.overstyrSøknadsperioder(behandling.getId(), perioder, List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    private OppgittPeriodeEntitet mødrekvote(LocalDate fom, LocalDate tom) {
        return OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();
    }
}
