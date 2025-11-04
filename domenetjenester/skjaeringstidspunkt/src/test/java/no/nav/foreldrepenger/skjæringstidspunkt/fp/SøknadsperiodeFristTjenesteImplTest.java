package no.nav.foreldrepenger.skjæringstidspunkt.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.skjæringstidspunkt.SøknadsperiodeFristTjeneste;

class SøknadsperiodeFristTjenesteImplTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
    }

    @Test
    void skal_ignorere_utsettelser() {
        var fedrekvoteFom = LocalDate.of(2021, 2, 4);
        var utsettelse1 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medÅrsak(UtsettelseÅrsak.ARBEID)
            .medPeriode(fedrekvoteFom.minusWeeks(1), fedrekvoteFom.minusDays(1))
            .build();
        var fedrekvoteTom = fedrekvoteFom.plusWeeks(2);
        var fedrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(fedrekvoteFom, fedrekvoteTom)
            .build();
        var utsettelse2 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medÅrsak(UtsettelseÅrsak.ARBEID)
            .medPeriode(fedrekvote.getTom().plusDays(1), fedrekvote.getTom().plusDays(10))
            .build();
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(utsettelse1, fedrekvote, utsettelse2), true));
        scenario.medSøknad().medMottattDato(fedrekvoteFom);
        var behandling = scenario.lagre(repositoryProvider);

        var tjeneste = tjeneste(repositoryProvider);

        assertThat(tjeneste.finnSøknadsfrist(behandling.getId()).getSøknadGjelderPeriode().getFomDato()).isEqualTo(fedrekvoteFom);
        assertThat(tjeneste.finnSøknadsfrist(behandling.getId()).getSøknadGjelderPeriode().getTomDato()).isEqualTo(fedrekvoteTom);
    }

    @Test
    void skal_returnere_empty_hvis_bare_utsettelser() {
        var fedrekvoteFom = LocalDate.of(2021, 2, 4);
        var utsettelse = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medÅrsak(UtsettelseÅrsak.ARBEID)
            .medPeriode(fedrekvoteFom.minusWeeks(1), fedrekvoteFom.minusDays(1))
            .build();
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(utsettelse), true));
        var behandling = scenario.lagre(repositoryProvider);

        var tjeneste = tjeneste(repositoryProvider);
        var resultat = tjeneste.finnSøknadsfrist(behandling.getId());
        assertThat(resultat.getSøknadGjelderPeriode()).isNull();
    }

    private SøknadsperiodeFristTjeneste tjeneste(BehandlingRepositoryProvider repositoryProvider) {
        return new SøknadsperiodeFristTjenesteImpl(repositoryProvider.getYtelsesFordelingRepository(), repositoryProvider.getSøknadRepository());
    }



}
