package no.nav.foreldrepenger.domene.registerinnhenting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningInnhenter;
import no.nav.foreldrepenger.domene.registerinnhenting.impl.Endringskontroller;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktRegisterinnhentingTjeneste;

public class RegisterdataInnhenterTest {

    private final String durationInstance = "PT10H";

    @Test
    public void skal_innhente_registeropplysninger_på_nytt_når_det_ble_hentet_inn_i_går() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel()
            .medOpplysningerOppdatertTidspunkt(LocalDateTime.now().minusDays(1));
        var behandling = scenario.lagMocked();

        // Act
        var registerdataEndringshåndterer = lagRegisterdataInnhenter(scenario, durationInstance);
        Boolean harHentetInn = registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling);

        // Assert
        assertThat(harHentetInn).isTrue();
    }

    @Test
    public void skal_ikke_innhente_registeropplysninger_på_nytt_når_det_nettopp_har_blitt_hentet_inn() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel()
            .medOpplysningerOppdatertTidspunkt(LocalDateTime.now());
        var behandling = scenario.lagMocked();

        // Act
        var registerdataEndringshåndterer = lagRegisterdataInnhenter(scenario, durationInstance);
        Boolean harHentetInn = registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling);

        // Assert
        assertThat(harHentetInn).isFalse();
    }

    @Test
    public void skal_ikke_innhente_registeropplysninger_på_nytt_når_det_ikke_har_blitt_hentet_inn_tidligere() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel()
            .medOpplysningerOppdatertTidspunkt(null);
        var behandling = scenario.lagMocked();

        // Act
        var registerdataEndringshåndterer = lagRegisterdataInnhenter(scenario, durationInstance);
        Boolean harHentetInn = registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling);

        // Assert
        assertThat(harHentetInn).isFalse();
    }

    @Test
    public void skal_innhente_registeropplysninger_ut_ifra_midnatt_når_konfigurasjonsverdien_mangler() {
        // Arrange
        var midnatt = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

        var scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel()
            .medOpplysningerOppdatertTidspunkt(midnatt.minusMinutes(1));
        var behandling = scenario.lagMocked();
        var registerdataEndringshåndterer = lagRegisterdataInnhenter(scenario, null);

        // Act
        Boolean harHentetInn = registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling);

        // Assert
        assertThat(harHentetInn).isTrue();
    }

    @Test
    public void skal_innhente_registeropplysninger_mellom_midnatt_og_klokken_3_men_ikke_ellers_grunnet_konfigverdien() {
        // Arrange
        var midnatt = LocalDate.now().atStartOfDay();
        var opplysningerOppdatertTidspunkt = midnatt.minusHours(1); // en time før midnatt

        var scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel()
            .medOpplysningerOppdatertTidspunkt(opplysningerOppdatertTidspunkt);

        var registerdataOppdatererEngangsstønad = lagRegisterdataInnhenter(scenario, "PT3H");

        // Act
        Boolean skalInnhente = registerdataOppdatererEngangsstønad.erOpplysningerOppdatertTidspunktFør(midnatt,
            Optional.of(opplysningerOppdatertTidspunkt));

        // Assert
        assertThat(skalInnhente).isTrue();
    }

    @Test
    public void skal_ikke_innhente_opplysninger_på_nytt_selvom_det_ble_hentet_inn_i_går_fordi_konfigverdien_er_mer_enn_midnatt() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel()
            .medOpplysningerOppdatertTidspunkt(LocalDateTime.now().minusHours(20));
        var behandling = scenario.lagMocked();

        var registerdataEndringshåndterer = lagRegisterdataInnhenter(scenario, "PT30H");

        // Act
        Boolean harHentetInn = registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling);

        // Assert
        assertThat(harHentetInn).isFalse();
    }

    @Test
    public void kan_mappe_tilleggsopplysninger() {
        // Arrange
        var json = """
            {
              &#34;pleietrengende&#34; : "9999999999999",
              "innleggelsesPerioder" : [ {
                "fom" : "2021-05-03",
                "tom" : "2021-05-09"
              } ]
            }
            """;
        var fikset = json.replace("&#34;", "\"");
        var deser = StandardJsonConfig.fromJson(fikset, RegisterdataInnhenter.PleiepengerOpplysninger.class);

        assertThat(deser.pleietrengende()).isEqualTo(new AktørId("9999999999999"));
        assertThat(deser.innleggelsesPerioder()).hasSize(1);
        assertThat(deser.innleggelsesPerioder().get(0).fom()).isEqualTo(LocalDate.of(2021,5,3));
    }

    private RegisterdataEndringshåndterer lagRegisterdataInnhenter(AbstractTestScenario<?> scenario, String durationInstance) {
        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        return lagRegisterdataOppdaterer(repositoryProvider, durationInstance);
    }

    private RegisterdataEndringshåndterer lagRegisterdataOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                                                    String durationInstance) {

        var innhenter = lagRegisterdataInnhenter(repositoryProvider);

        var oppdaterer = lagRegisterdataOppdaterer(repositoryProvider, durationInstance, innhenter);
        return oppdaterer;
    }

    private RegisterdataEndringshåndterer lagRegisterdataOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                                                    String durationInstance, RegisterdataInnhenter innhenter) {
        var endringskontroller = mock(Endringskontroller.class);
        when(endringskontroller.erRegisterinnhentingPassert(any())).thenReturn(Boolean.TRUE);
        var oppdaterer = new RegisterdataEndringshåndterer(repositoryProvider, innhenter, durationInstance, endringskontroller, null, null, null);
        return oppdaterer;
    }

    private RegisterdataInnhenter lagRegisterdataInnhenter(BehandlingRepositoryProvider repositoryProvider) {
        var skjæringstidspunktTjeneste = mock(SkjæringstidspunktRegisterinnhentingTjeneste.class);
        var opplysningsPeriodeTjeneste = new OpplysningsPeriodeTjeneste(skjæringstidspunktTjeneste,
            Period.of(1, 0, 0), Period.of(0, 6, 0), Period.of(0, 4, 0), Period.of(1, 0, 0), Period.of(1, 0, 0), Period.of(0, 6, 0));

        var personinfoAdapter = mock(PersoninfoAdapter.class);
        var medlemTjeneste = mock(MedlemTjeneste.class);

        return new RegisterdataInnhenter(new PersonopplysningInnhenter(personinfoAdapter), medlemTjeneste, repositoryProvider,
                null, null, opplysningsPeriodeTjeneste, null);
    }
}
