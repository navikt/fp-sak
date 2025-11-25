package no.nav.foreldrepenger.behandlingslager.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.nøkkeltallbehandling.BehandlingVenteStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.nøkkeltallbehandling.NøkkeltallBehandlingFørsteUttak;
import no.nav.foreldrepenger.behandlingslager.behandling.nøkkeltallbehandling.NøkkeltallBehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.nøkkeltallbehandling.NøkkeltallBehandlingVentefristUtløper;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.JpaExtension;

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
        var nøkkeltall = nøkkeltallBehandlingRepository.hentNøkkeltallSøknadFørsteUttakPrMånedForeldrepenger();
        var resultat = antallTreff(nøkkeltall, forventetNøkkeltallBehandlingVentestatus);
        assertThat(resultat).isPositive();
    }

    private NøkkeltallBehandlingFørsteUttak forventet(ScenarioMorSøkerForeldrepenger søknad) {
        var forventetEnhet = søknad.getBehandling().getBehandlendeEnhet();
        var forventetFørsteUttakMåned = finnFørsteUttakMånedDato(søknad);
        var forventetStatus = BehandlingVenteStatus.PÅ_VENT;
        var forventetBehandlingType = søknad.getBehandling().getType();
        return new NøkkeltallBehandlingFørsteUttak(forventetEnhet, forventetBehandlingType, forventetStatus, forventetFørsteUttakMåned, 1);
    }

    @Test
    void skalRapportereFristUtløper() {
        var søknad = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlendeEnhet("4833")
            .medDefaultFordeling(LocalDate.now().plusDays(20))
            .leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, BehandlingStegType.INNHENT_PERSONOPPLYSNINGER);
        søknad.lagre(repositoryProvider);
        var forventetNøkkeltallBehandlingVentestatus = forventetFrist(søknad);
        var nøkkeltall = nøkkeltallBehandlingRepository.hentNøkkeltallVentefristUtløper();
        var resultat = antallTreff(nøkkeltall, forventetNøkkeltallBehandlingVentestatus);
        assertThat(resultat).isPositive();
    }

    @Test
    void skalRapportereFristUtløperUke() {
        var søknad = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlendeEnhet("4833")
            .medDefaultFordeling(LocalDate.now().plusDays(20))
            .leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, BehandlingStegType.INNHENT_PERSONOPPLYSNINGER);
        søknad.lagre(repositoryProvider);
        var forventetNøkkeltallBehandlingVentestatus = forventetFristUke(søknad);
        var nøkkeltall = nøkkeltallBehandlingRepository.hentNøkkeltallVentefristUtløperUke();
        var resultat = antallTreff(nøkkeltall, forventetNøkkeltallBehandlingVentestatus);
        assertThat(resultat).isPositive();
    }

    private NøkkeltallBehandlingVentefristUtløper forventetFrist(ScenarioMorSøkerForeldrepenger søknad) {
        var forventetEnhet = søknad.getBehandling().getBehandlendeEnhet();
        var forventetFrist = søknad.getBehandling().getAksjonspunkter().stream().findFirst().map(Aksjonspunkt::getFristTid).orElseThrow().toLocalDate();
        var forventetYtelseType = søknad.getFagsak().getYtelseType();
        return new NøkkeltallBehandlingVentefristUtløper(forventetEnhet, forventetYtelseType, forventetFrist, 1L);
    }

    private NøkkeltallBehandlingVentefristUtløper forventetFristUke(ScenarioMorSøkerForeldrepenger søknad) {
        var forventetEnhet = søknad.getBehandling().getBehandlendeEnhet();
        var forventetFrist = søknad.getBehandling().getAksjonspunkter().stream().findFirst()
            .map(Aksjonspunkt::getFristTid).orElseThrow()
            .toLocalDate().with(DayOfWeek.MONDAY);
        var forventetYtelseType = søknad.getFagsak().getYtelseType();
        return new NøkkeltallBehandlingVentefristUtløper(forventetEnhet, forventetYtelseType, forventetFrist, 1L);
    }

    private static int antallTreff(List<NøkkeltallBehandlingFørsteUttak> nøkkeltallInitiell,
                                   NøkkeltallBehandlingFørsteUttak forventet) {
        return nøkkeltallInitiell.stream()
            .filter(i -> Objects.equals(i.behandlendeEnhet(), forventet.behandlendeEnhet()))
            .filter(i -> Objects.equals(i.behandlingType(), forventet.behandlingType()))
            .filter(i -> Objects.equals(i.førsteUttakMåned(), forventet.førsteUttakMåned()))
            .filter(i -> i.behandlingVenteStatus() == forventet.behandlingVenteStatus())
            .mapToInt(NøkkeltallBehandlingFørsteUttak::antall)
            .sum();
    }

    private static Long antallTreff(List<NøkkeltallBehandlingVentefristUtløper> nøkkeltallInitiell,
                                   NøkkeltallBehandlingVentefristUtløper forventet) {
        return nøkkeltallInitiell.stream()
            .filter(i -> Objects.equals(i.behandlendeEnhet(), forventet.behandlendeEnhet()))
            .filter(i -> Objects.equals(i.fagsakYtelseType(), forventet.fagsakYtelseType()))
            .filter(i -> Objects.equals(i.behandlingFrist(), forventet.behandlingFrist()))
            .mapToLong(NøkkeltallBehandlingVentefristUtløper::antall)
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
