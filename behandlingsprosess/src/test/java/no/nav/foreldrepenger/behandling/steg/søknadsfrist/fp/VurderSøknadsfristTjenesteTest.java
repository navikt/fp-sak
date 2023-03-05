package no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.JpaExtension;

@ExtendWith(JpaExtension.class)
class VurderSøknadsfristTjenesteTest {

    @Test
    void skal_lagre_uttaksperiodegrense_og_ikke_få_aksjonspunkt(EntityManager entityManager) {
        var fødselsdato = LocalDate.of(2021, 2, 4);
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFødselAdopsjonsdato(List.of(fødselsdato))
            .medDefaultFordeling(fødselsdato);
        scenario.medSøknad().medMottattDato(fødselsdato);
        var behandling = scenario.lagre(repositoryProvider);

        var tjeneste = new VurderSøknadsfristTjeneste(repositoryProvider);

        var resultat = tjeneste.vurder(behandling.getId());
        assertThat(resultat).isEmpty();
        var uttaksperiodegrense = repositoryProvider.getUttaksperiodegrenseRepository()
            .hentHvisEksisterer(behandling.getId());
        assertThat(uttaksperiodegrense).isPresent();
    }

    @Test
    void skal_lagre_uttaksperiodegrense_og_få_aksjonspunkt(EntityManager entityManager) {
        var fødselsdato = LocalDate.of(2021, 2, 4);
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFødselAdopsjonsdato(List.of(fødselsdato))
            .medDefaultFordeling(fødselsdato);
        scenario.medSøknad().medMottattDato(fødselsdato.plusWeeks(100));
        var behandling = scenario.lagre(repositoryProvider);

        var tjeneste = new VurderSøknadsfristTjeneste(repositoryProvider);

        var resultat = tjeneste.vurder(behandling.getId());
        assertThat(resultat).isPresent();
        var uttaksperiodegrense = repositoryProvider.getUttaksperiodegrenseRepository()
            .hentHvisEksisterer(behandling.getId());
        assertThat(uttaksperiodegrense).isPresent();
    }

    @Test
    void skal_lagre_uttaksperiodegrense_og_ikke_få_aksjonspunkt_hvis_bare_søkt_utsettelse(EntityManager entityManager) {
        var fødselsdato = LocalDate.of(2021, 2, 4);
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var utsettelse = utsettelse(fødselsdato);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFødselAdopsjonsdato(List.of(fødselsdato))
            .medFordeling(new OppgittFordelingEntitet(List.of(utsettelse), true));
        scenario.medSøknad().medMottattDato(fødselsdato.plusWeeks(100));
        var behandling = scenario.lagre(repositoryProvider);

        var tjeneste = new VurderSøknadsfristTjeneste(repositoryProvider);

        var resultat = tjeneste.vurder(behandling.getId());
        assertThat(resultat).isEmpty();
        var uttaksperiodegrense = repositoryProvider.getUttaksperiodegrenseRepository()
            .hentHvisEksisterer(behandling.getId());
        assertThat(uttaksperiodegrense).isPresent();
    }

    private OppgittPeriodeEntitet utsettelse(LocalDate fom) {
        return OppgittPeriodeBuilder.ny()
            .medPeriode(fom, fom.plusWeeks(10))
            .medÅrsak(UtsettelseÅrsak.ARBEID)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .build();
    }
}
