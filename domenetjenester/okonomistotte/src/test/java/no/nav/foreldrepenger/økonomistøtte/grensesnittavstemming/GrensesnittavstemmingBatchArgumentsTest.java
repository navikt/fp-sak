package no.nav.foreldrepenger.økonomistøtte.grensesnittavstemming;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomioppdragRepository;
import no.nav.foreldrepenger.økonomistøtte.grensesnittavstemming.queue.producer.GrensesnittavstemmingJmsProducer;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class GrensesnittavstemmingBatchArgumentsTest {

    public static final String ANTALL_DAGER = "antallDager";
    public static final String FAGOMRÅDE = "fagomrade";
    public static final String TOM = "tom";
    public static final String FOM = "fom";

    private final GrensesnittavstemmingBatchTjeneste mock =
        new GrensesnittavstemmingBatchTjeneste(mock(ØkonomioppdragRepository.class), mock(GrensesnittavstemmingJmsProducer.class));

    @Test
    void skal_parse_antall_dager() {
        var argMap = new Properties();
        argMap.setProperty(ANTALL_DAGER, "5");
        argMap.setProperty(FAGOMRÅDE, "FP");
        assertDoesNotThrow(() -> mock.launch(argMap));
    }

    @Test
    void skal_parse_antall_8_dager() {
        var argMap = new Properties();
        argMap.setProperty(ANTALL_DAGER, "8");
        argMap.setProperty(FAGOMRÅDE, "FP");
        assertDoesNotThrow(() -> mock.launch(argMap));
    }

    @Test
    void skal_parse_antall_9_dager_utover_max() {
        var argMap = new Properties();
        argMap.setProperty(FAGOMRÅDE, "FP");
        argMap.setProperty(ANTALL_DAGER, "9");
        assertThrows(IllegalArgumentException.class, () -> mock.launch(argMap));
    }

    @Test
    void skal_parse_dato() {
        var argMap = new Properties();
        argMap.setProperty(FOM, "2014-11-01");
        argMap.setProperty(TOM, "2014-11-07");
        argMap.setProperty(FAGOMRÅDE, "FP");
        assertDoesNotThrow(() -> mock.launch(argMap));
    }

    @Test
    void skal_parse_dato_periode_utover_max() {
        var argMap = new Properties();
        argMap.setProperty(FOM, "2014-11-01");
        argMap.setProperty(TOM, "2014-11-09");
        argMap.setProperty(FAGOMRÅDE, "FP");
        assertThrows(IllegalArgumentException.class, () -> mock.launch(argMap));
    }

    @Test
    void skal_parse_dato_periode_7_dager() {
        var argMap = new Properties();
        argMap.setProperty(FOM, "2014-11-01");
        argMap.setProperty(TOM, "2014-11-07");
        argMap.setProperty(FAGOMRÅDE, "FP");
        assertDoesNotThrow(() -> mock.launch(argMap));
    }

    @Test
    void skal_feile_ved_satt_for_mange_properties() {
        var argMap = new Properties();
        argMap.setProperty(FOM, "2014-11-01");
        argMap.setProperty(TOM, "2014-11-20");
        argMap.setProperty(ANTALL_DAGER, "5");
        argMap.setProperty(FAGOMRÅDE, "FP");
        assertThrows(IllegalArgumentException.class, () -> mock.launch(argMap));
    }

    @Test
    void skal_feile_fordi_satt_parametre_er_ikke_entydig() {
        var argMap = new Properties();
        argMap.setProperty(FOM, "2014-11-01");
        argMap.setProperty(ANTALL_DAGER, "5");
        argMap.setProperty(FAGOMRÅDE, "FP");
        assertDoesNotThrow(() -> mock.launch(argMap));
    }

    @Test
    void skal_feile_fordi_fagområde_er_ikke_satt() {
        var argMap = new Properties();
        argMap.setProperty(FOM, "2014-11-01");
        argMap.setProperty(ANTALL_DAGER, "5");
        assertThrows(NullPointerException.class, () -> mock.launch(argMap));
    }

    @Test
    void skal_feile_fordi_fagområde_er_feil() {
        var argMap = new Properties();
        argMap.setProperty(FOM, "2014-11-01");
        argMap.setProperty(ANTALL_DAGER, "5");
        argMap.setProperty(FAGOMRÅDE, "blabla");
        assertThrows(IllegalArgumentException.class, () -> mock.launch(argMap));
    }

    @Test
    void skal_feile_fordi_fagområde_er_null() {
        var argMap = new Properties();
        argMap.setProperty(ANTALL_DAGER, "8");
        assertThrows(NullPointerException.class, () -> mock.launch(argMap));
    }

}
