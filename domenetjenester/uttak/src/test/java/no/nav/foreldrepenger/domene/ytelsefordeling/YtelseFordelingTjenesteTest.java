package no.nav.foreldrepenger.domene.ytelsefordeling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.Rettighetstype;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

class YtelseFordelingTjenesteTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();
    private final YtelseFordelingTjeneste tjeneste = new YtelseFordelingTjeneste(repositoryProvider.getYtelsesFordelingRepository());

    @Test
    void test_bekreft_aksjonspunkt_annenforelder_har_ikke_rett() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);

        tjeneste.avklarRettighet(behandling.getId(), Rettighetstype.BARE_MOR_RETT);

        var perioderAnnenforelderHarRett = tjeneste.hentAggregat(behandling.getId()).getAnnenForelderRettAvklaring();

        assertThat(perioderAnnenforelderHarRett).isNotNull().isFalse();
    }

    @Test
    void test_bekreft_aksjonspunkt_annenforelder_har_rett() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);

        tjeneste.avklarRettighet(behandling.getId(), Rettighetstype.BEGGE_RETT);

        var perioderAnnenforelderHarRett = tjeneste.hentAggregat(behandling.getId()).getAnnenForelderRettAvklaring();

        assertThat(perioderAnnenforelderHarRett).isNotNull().isTrue();
    }

    @Test
    void skalKasteExceptionHvisOverlappIPerioder() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        var periode1 = mødrekvote(LocalDate.now(), LocalDate.now().plusWeeks(1));
        var periode2 = mødrekvote(LocalDate.now().plusWeeks(1), LocalDate.now().plusWeeks(2));
        var perioder = List.of(periode1, periode2);
        var behandlingId = behandling.getId();
        assertThrows(IllegalArgumentException.class, () -> tjeneste.overstyrSøknadsperioder(behandlingId, perioder));
    }

    private OppgittPeriodeEntitet mødrekvote(LocalDate fom, LocalDate tom) {
        return OppgittPeriodeBuilder.ny().medPeriode(fom, tom).medPeriodeType(UttakPeriodeType.MØDREKVOTE).build();
    }
}
