package no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@ExtendWith(JpaExtension.class)
class SøktPeriodeTjenesteImplTest {

    @Test
    public void skal_ignorere_utsettelser(EntityManager entityManager) {
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
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var behandling = scenario.lagre(repositoryProvider);

        var tjeneste = tjeneste(repositoryProvider.getYtelsesFordelingRepository());

        assertThat(tjeneste.finnSøktPeriode(input(behandling))).isPresent();
        assertThat(tjeneste.finnSøktPeriode(input(behandling)).orElseThrow().getFomDato()).isEqualTo(fedrekvoteFom);
        assertThat(tjeneste.finnSøktPeriode(input(behandling)).orElseThrow().getTomDato()).isEqualTo(fedrekvoteTom);
    }

    @Test
    public void skal_returnere_empty_hvis_bare_utsettelser(EntityManager entityManager) {
        var fedrekvoteFom = LocalDate.of(2021, 2, 4);
        var utsettelse = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medÅrsak(UtsettelseÅrsak.ARBEID)
            .medPeriode(fedrekvoteFom.minusWeeks(1), fedrekvoteFom.minusDays(1))
            .build();
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(utsettelse), true));
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var behandling = scenario.lagre(repositoryProvider);

        var tjeneste = tjeneste(repositoryProvider.getYtelsesFordelingRepository());

        assertThat(tjeneste.finnSøktPeriode(input(behandling))).isEmpty();
    }

    private UttakInput input(Behandling behandling) {
        return new UttakInput(BehandlingReferanse.fra(behandling), null, new ForeldrepengerGrunnlag());
    }

    private SøktPeriodeTjenesteImpl tjeneste(YtelsesFordelingRepository ytelsesFordelingRepository) {
        return new SøktPeriodeTjenesteImpl(
            new YtelseFordelingTjeneste(ytelsesFordelingRepository));
    }

}
