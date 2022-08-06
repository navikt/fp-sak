package no.nav.foreldrepenger.domene.uttak.søknadsfrist.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.søknadsfrist.SøktPeriodeTjeneste;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;


class SøktPeriodeTjenesteImplTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();

    @Test
    public void skal_ignorere_utsettelser() {
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
        var behandling = scenario.lagre(repositoryProvider);

        var tjeneste = tjeneste(repositoryProvider.getYtelsesFordelingRepository());

        assertThat(tjeneste.finnSøktPeriode(input(behandling, fedrekvoteFom))).isPresent();
        assertThat(tjeneste.finnSøktPeriode(input(behandling, fedrekvoteFom)).orElseThrow().getFomDato()).isEqualTo(fedrekvoteFom);
        assertThat(tjeneste.finnSøktPeriode(input(behandling, fedrekvoteFom)).orElseThrow().getTomDato()).isEqualTo(fedrekvoteTom);
    }

    @Test
    public void skal_returnere_empty_hvis_bare_utsettelser() {
        var fedrekvoteFom = LocalDate.of(2021, 2, 4);
        var utsettelse = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medÅrsak(UtsettelseÅrsak.ARBEID)
            .medPeriode(fedrekvoteFom.minusWeeks(1), fedrekvoteFom.minusDays(1))
            .build();
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(utsettelse), true));
        var behandling = scenario.lagre(repositoryProvider);

        var tjeneste = tjeneste(repositoryProvider.getYtelsesFordelingRepository());
        var resultat = tjeneste.finnSøktPeriode(input(behandling, fedrekvoteFom));
        System.out.println(resultat);
        assertThat(resultat).isEmpty();
    }

    private UttakInput input(Behandling behandling, LocalDate mottattDato) {
        return new UttakInput(BehandlingReferanse.fra(behandling), null, new ForeldrepengerGrunnlag())
            .medSøknadMottattDato(mottattDato);
    }

    private SøktPeriodeTjeneste tjeneste(YtelsesFordelingRepository ytelsesFordelingRepository) {
        return new SøktPeriodeTjenesteImpl(new YtelseFordelingTjeneste(ytelsesFordelingRepository));
    }

}

