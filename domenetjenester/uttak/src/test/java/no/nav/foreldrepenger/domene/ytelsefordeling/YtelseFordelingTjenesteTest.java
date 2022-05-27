package no.nav.foreldrepenger.domene.ytelsefordeling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.UttakDokumentasjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

public class YtelseFordelingTjenesteTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();
    private final YtelseFordelingTjeneste tjeneste = new YtelseFordelingTjeneste(
        repositoryProvider.getYtelsesFordelingRepository());

    @Test
    public void test_lagring_perioderuttakdokumentasjon() {
        var enDag = LocalDate.of(2018, 3, 15);
        var dokumentasjonPerioder = List.of(
            new PeriodeUttakDokumentasjonEntitet(enDag, enDag.plusDays(1), UttakDokumentasjonType.SYK_SØKER),
            new PeriodeUttakDokumentasjonEntitet(enDag.plusDays(4), enDag.plusDays(7),
                UttakDokumentasjonType.SYK_SØKER));

        var opprinnelig = mødrekvote(enDag, enDag.plusDays(7));
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(opprinnelig), true));
        var behandling = scenario.lagre(repositoryProvider);

        var ny = mødrekvote(enDag, enDag.plusDays(7));

        tjeneste.overstyrSøknadsperioder(behandling.getId(), Collections.singletonList(ny), dokumentasjonPerioder);

        var perioderUttak = tjeneste.hentAggregat(behandling.getId()).getPerioderUttakDokumentasjon();

        assertThat(perioderUttak).isNotNull();
        var perioder = perioderUttak.orElseThrow().getPerioder();
        assertThat(perioder).isNotEmpty();
        assertThat(perioder).hasSize(2);
    }

    @Test
    public void test_bekreft_aksjonspunkt_annenforelder_har_ikke_rett() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);

        tjeneste.bekreftAnnenforelderHarRett(behandling.getId(), false, null);

        var perioderAnnenforelderHarRett = tjeneste.hentAggregat(
            behandling.getId()).getAnnenForelderRettAvklaring();

        assertThat(perioderAnnenforelderHarRett).isNotNull();
        assertThat(perioderAnnenforelderHarRett).isFalse();
    }

    @Test
    public void test_bekreft_aksjonspunkt_annenforelder_har_rett() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);

        tjeneste.bekreftAnnenforelderHarRett(behandling.getId(), true, null);

        var perioderAnnenforelderHarRett = tjeneste.hentAggregat(
            behandling.getId()).getAnnenForelderRettAvklaring();

        assertThat(perioderAnnenforelderHarRett).isNotNull();
        assertThat(perioderAnnenforelderHarRett).isTrue();
    }

    @Test
    public void skalKasteExceptionHvisOverlappIPerioder() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        var periode1 = mødrekvote(LocalDate.now(), LocalDate.now().plusWeeks(1));
        var periode2 = mødrekvote(LocalDate.now().plusWeeks(1), LocalDate.now().plusWeeks(2));
        var perioder = List.of(periode1, periode2);
        assertThrows(IllegalArgumentException.class,
            () -> tjeneste.overstyrSøknadsperioder(behandling.getId(), perioder, List.of()));
    }

    private OppgittPeriodeEntitet mødrekvote(LocalDate fom, LocalDate tom) {
        return OppgittPeriodeBuilder.ny().medPeriode(fom, tom).medPeriodeType(UttakPeriodeType.MØDREKVOTE).build();
    }
}
