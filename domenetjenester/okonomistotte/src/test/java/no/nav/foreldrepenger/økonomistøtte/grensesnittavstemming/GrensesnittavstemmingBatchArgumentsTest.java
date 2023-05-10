package no.nav.foreldrepenger.økonomistøtte.grensesnittavstemming;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomioppdragRepository;
import no.nav.foreldrepenger.økonomistøtte.grensesnittavstemming.queue.producer.GrensesnittavstemmingJmsProducer;

class GrensesnittavstemmingBatchArgumentsTest {

    public static final String ANTALL_DAGER = "antallDager";
    public static final String FAGOMRÅDE = "fagomrade";
    public static final String TOM = "tom";
    public static final String FOM = "fom";

    private GrensesnittavstemmingBatchTjeneste mock =
        new GrensesnittavstemmingBatchTjeneste(mock(ØkonomioppdragRepository.class), mock(GrensesnittavstemmingJmsProducer.class));

    @Test
    void skal_parse_antall_dager() {
        final var argMap = new Properties();
        argMap.setProperty(ANTALL_DAGER, "5");
        argMap.setProperty(FAGOMRÅDE, "FP");
        mock.launch(argMap);
    }

    @Test
    void skal_parse_antall_8_dager() {
        final var argMap = new Properties();
        argMap.setProperty(ANTALL_DAGER, "8");
        argMap.setProperty(FAGOMRÅDE, "FP");
        mock.launch(argMap);
    }

    @Test
    void skal_parse_antall_9_dager_utover_max() {
        final var argMap = new Properties();
        argMap.setProperty(FAGOMRÅDE, "FP");
        argMap.setProperty(ANTALL_DAGER, "9");
        assertThrows(IllegalArgumentException.class, () -> mock.launch(argMap));
    }

    @Test
    void skal_parse_dato() {
        final var argMap = new Properties();
        argMap.setProperty(FOM, "2014-11-01");
        argMap.setProperty(TOM, "2014-11-07");
        argMap.setProperty(FAGOMRÅDE, "FP");
        mock.launch(argMap);
    }

    @Test
    void skal_parse_dato_periode_utover_max() {
        final var argMap = new Properties();
        argMap.setProperty(FOM, "2014-11-01");
        argMap.setProperty(TOM, "2014-11-09");
        argMap.setProperty(FAGOMRÅDE, "FP");
        assertThrows(IllegalArgumentException.class, () -> mock.launch(argMap));
    }

    @Test
    void skal_parse_dato_periode_7_dager() {
        final var argMap = new Properties();
        argMap.setProperty(FOM, "2014-11-01");
        argMap.setProperty(TOM, "2014-11-07");
        argMap.setProperty(FAGOMRÅDE, "FP");
        mock.launch(argMap);
    }

    @Test
    void skal_feile_ved_satt_for_mange_properties() {
        final var argMap = new Properties();
        argMap.setProperty(FOM, "2014-11-01");
        argMap.setProperty(TOM, "2014-11-20");
        argMap.setProperty(ANTALL_DAGER, "5");
        argMap.setProperty(FAGOMRÅDE, "FP");
        assertThrows(IllegalArgumentException.class, () -> mock.launch(argMap));
    }

    @Test
    void skal_feile_fordi_satt_parametre_er_ikke_entydig() {
        final var argMap = new Properties();
        argMap.setProperty(FOM, "2014-11-01");
        argMap.setProperty(ANTALL_DAGER, "5");
        argMap.setProperty(FAGOMRÅDE, "FP");
        mock.launch(argMap);
    }

    @Test
    void skal_feile_fordi_fagområde_er_ikke_satt() {
        final var argMap = new Properties();
        argMap.setProperty(FOM, "2014-11-01");
        argMap.setProperty(ANTALL_DAGER, "5");
        assertThrows(NullPointerException.class, () -> mock.launch(argMap));
    }

    @Test
    void skal_feile_fordi_fagområde_er_feil() {
        final var argMap = new Properties();
        argMap.setProperty(FOM, "2014-11-01");
        argMap.setProperty(ANTALL_DAGER, "5");
        argMap.setProperty(FAGOMRÅDE, "blabla");
        assertThrows(IllegalArgumentException.class, () -> mock.launch(argMap));
    }

    @Test
    void skal_feile_fordi_fagområde_er_null() {
        final var argMap = new Properties();
        argMap.setProperty(ANTALL_DAGER, "8");
        assertThrows(NullPointerException.class, () -> mock.launch(argMap));
    }

}
