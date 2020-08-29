package no.nav.foreldrepenger.domene.registerinnhenting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.person.tps.PersoninfoAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktRegisterinnhentingTjeneste;

public class RegisterdataInnhenterTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    private String durationInstance = "PT10H";

    @Test
    public void skal_innhente_registeropplysninger_på_nytt_når_det_ble_hentet_inn_i_går() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel()
            .medOpplysningerOppdatertTidspunkt(LocalDateTime.now().minusDays(1));
        Behandling behandling = scenario.lagMocked();

        // Act
        RegisterdataEndringshåndterer registerdataEndringshåndterer = lagRegisterdataInnhenter(scenario, durationInstance);
        Boolean harHentetInn = registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling);

        // Assert
        assertThat(harHentetInn).isTrue();
    }

    @Test
    public void skal_ikke_innhente_registeropplysninger_på_nytt_når_det_nettopp_har_blitt_hentet_inn() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel()
            .medOpplysningerOppdatertTidspunkt(LocalDateTime.now());
        Behandling behandling = scenario.lagMocked();

        // Act
        RegisterdataEndringshåndterer registerdataEndringshåndterer = lagRegisterdataInnhenter(scenario, durationInstance);
        Boolean harHentetInn = registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling);

        // Assert
        assertThat(harHentetInn).isFalse();
    }

    @Test
    public void skal_ikke_innhente_registeropplysninger_på_nytt_når_det_ikke_har_blitt_hentet_inn_tidligere() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel()
            .medOpplysningerOppdatertTidspunkt(null);
        Behandling behandling = scenario.lagMocked();

        // Act
        RegisterdataEndringshåndterer registerdataEndringshåndterer = lagRegisterdataInnhenter(scenario, durationInstance);
        Boolean harHentetInn = registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling);

        // Assert
        assertThat(harHentetInn).isFalse();
    }

    @Test
    public void skal_innhente_registeropplysninger_ut_ifra_midnatt_når_konfigurasjonsverdien_mangler() {
        // Arrange
        LocalDateTime midnatt = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel()
            .medOpplysningerOppdatertTidspunkt(midnatt.minusMinutes(1));
        Behandling behandling = scenario.lagMocked();
        RegisterdataEndringshåndterer registerdataEndringshåndterer = lagRegisterdataInnhenter(scenario, null);

        // Act
        Boolean harHentetInn = registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling);

        // Assert
        assertThat(harHentetInn).isTrue();
    }

    @Test
    public void skal_innhente_registeropplysninger_mellom_midnatt_og_klokken_3_men_ikke_ellers_grunnet_konfigverdien() {
        // Arrange
        LocalDateTime midnatt = LocalDate.now().atStartOfDay();
        LocalDateTime opplysningerOppdatertTidspunkt = midnatt.minusHours(1); // en time før midnatt

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel()
            .medOpplysningerOppdatertTidspunkt(opplysningerOppdatertTidspunkt);

        RegisterdataEndringshåndterer registerdataOppdatererEngangsstønad = lagRegisterdataInnhenter(scenario, "PT3H");

        // Act
        Boolean skalInnhente = registerdataOppdatererEngangsstønad.erOpplysningerOppdatertTidspunktFør(midnatt,
            Optional.of(opplysningerOppdatertTidspunkt));

        // Assert
        assertThat(skalInnhente).isTrue();
    }

    @Test
    public void skal_ikke_innhente_opplysninger_på_nytt_selvom_det_ble_hentet_inn_i_går_fordi_konfigverdien_er_mer_enn_midnatt() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel()
            .medOpplysningerOppdatertTidspunkt(LocalDateTime.now().minusHours(20));
        Behandling behandling = scenario.lagMocked();

        RegisterdataEndringshåndterer registerdataEndringshåndterer = lagRegisterdataInnhenter(scenario, "PT30H");

        // Act
        Boolean harHentetInn = registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling);

        // Assert
        assertThat(harHentetInn).isFalse();
    }

    private RegisterdataEndringshåndterer lagRegisterdataInnhenter(AbstractTestScenario<?> scenario, String durationInstance) {
        BehandlingRepositoryProvider repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        return lagRegisterdataOppdaterer(repositoryProvider, durationInstance);
    }

    private RegisterdataEndringshåndterer lagRegisterdataOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                                                    String durationInstance) {

        RegisterdataInnhenter innhenter = lagRegisterdataInnhenter(repositoryProvider);

        RegisterdataEndringshåndterer oppdaterer = lagRegisterdataOppdaterer(repositoryProvider, durationInstance, innhenter);
        return oppdaterer;
    }

    private RegisterdataEndringshåndterer lagRegisterdataOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                                                    String durationInstance, RegisterdataInnhenter innhenter) {

        RegisterdataEndringshåndterer oppdaterer = new RegisterdataEndringshåndterer(repositoryProvider, innhenter, durationInstance, null, null, null, null);
        return oppdaterer;
    }

    private RegisterdataInnhenter lagRegisterdataInnhenter(BehandlingRepositoryProvider repositoryProvider) {
        SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste = mock(SkjæringstidspunktRegisterinnhentingTjeneste.class);
        OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste = new OpplysningsPeriodeTjeneste(skjæringstidspunktTjeneste,
            Period.of(1, 0, 0), Period.of(0, 6, 0), Period.of(0, 4, 0), Period.of(1, 0, 0), Period.of(1, 0, 0), Period.of(0, 6, 0));

        PersoninfoAdapter personinfoAdapter = mock(PersoninfoAdapter.class);
        MedlemTjeneste medlemTjeneste = mock(MedlemTjeneste.class);

        return new RegisterdataInnhenter(personinfoAdapter,
            medlemTjeneste,
            repositoryProvider,
                null, null, opplysningsPeriodeTjeneste,
            null,
            null, Period.parse("P1W"),
            Period.parse("P4W"));
    }
}
