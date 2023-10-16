package no.nav.foreldrepenger.behandlingslager.behandling;

import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.nøkkeltallbehandling.BehandlingVenteStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.nøkkeltallbehandling.NøkkeltallBehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.nøkkeltallbehandling.NøkkeltallBehandlingVentestatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
class NøkkeltallBehandlingRepositoryTest {

    private NøkkeltallBehandlingRepository nøkkeltallBehandlingRepository;
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    void setup(EntityManager entityManager){
        nøkkeltallBehandlingRepository = new NøkkeltallBehandlingRepository(entityManager);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    }

    @Test
    void skalRapportereBehandlingPåVent() {
        var søknad = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlendeEnhet("4833")
            .medDefaultFordeling(LocalDate.now().plusDays(20))
            .leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, BehandlingStegType.INNHENT_PERSONOPPLYSNINGER);
        søknad.lagre(repositoryProvider);
        var forventetNøkkeltallBehandlingVentestatus = forventet(søknad);
        var nøkkeltall = nøkkeltallBehandlingRepository.hentNøkkeltallBehandlingVentestatus();
        var resultat = antallTreff(nøkkeltall, forventetNøkkeltallBehandlingVentestatus);
        assertThat(resultat).isGreaterThanOrEqualTo(1);
    }

    private NøkkeltallBehandlingVentestatus forventet(ScenarioMorSøkerForeldrepenger søknad) {
        var forventetEnhet = søknad.getBehandling().getBehandlendeEnhet();
        var forventetFørsteUttakMåned = finnFørsteUttakMånedDato(søknad);
        var forventetStatus = BehandlingVenteStatus.PÅ_VENT;
        var forventetBehandlingType = søknad.getBehandling().getType();
        return new NøkkeltallBehandlingVentestatus(forventetEnhet, forventetBehandlingType, forventetStatus, forventetFørsteUttakMåned, 1);
    }

    private static int antallTreff(List<NøkkeltallBehandlingVentestatus> nøkkeltallInitiell,
                                   NøkkeltallBehandlingVentestatus forventet) {
        return nøkkeltallInitiell.stream()
            .filter(i -> Objects.equals(i.behandlendeEnhet(), forventet.behandlendeEnhet()))
            .filter(i -> Objects.equals(i.behandlingType(), forventet.behandlingType()))
            .filter(i -> Objects.equals(i.førsteUttakMåned(), forventet.førsteUttakMåned()))
            .filter(i -> i.behandlingVenteStatus() == forventet.behandlingVenteStatus())
            .mapToInt(NøkkeltallBehandlingVentestatus::antall)
            .sum();
    }

    private LocalDate finnFørsteUttakMånedDato(ScenarioMorSøkerForeldrepenger søknad) {
        return repositoryProvider.getYtelsesFordelingRepository()
            .hentAggregat(søknad.getBehandling().getId())
            .getOppgittFordeling().getPerioder().stream()
            .map(OppgittPeriodeEntitet::getFom)
            .min(LocalDate::compareTo)
            .map(d -> d.with(TemporalAdjusters.firstDayOfMonth()))
            .orElseThrow();
    }

}
