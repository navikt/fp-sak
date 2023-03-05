package no.nav.foreldrepenger.økonomistøtte.grensesnittavstemming;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

class GrensesnittavstemmingBatchArgumentsTest {

    public static final String ANTALL_DAGER = "antallDager";
    public static final String FAGOMRÅDE = "fagomrade";
    public static final String TOM = "tom";
    public static final String FOM = "fom";

    @Test
    void skal_parse_antall_dager() {
        final var argMap = new HashMap<String, String>();
        argMap.put(ANTALL_DAGER, "5");
        argMap.put(FAGOMRÅDE, "FP");
        var args = new GrensesnittavstemmingBatchArguments(argMap);
        assertThat(args.isValid()).isTrue();
        assertThat(args.getFom()).isNotNull();
        assertThat(args.getTom()).isNotNull();
    }

    @Test
    void skal_parse_antall_8_dager() {
        final var argMap = new HashMap<String, String>();
        argMap.put(ANTALL_DAGER, "8");
        argMap.put(FAGOMRÅDE, "FP");
        var args = new GrensesnittavstemmingBatchArguments(argMap);
        assertThat(args.isValid()).isTrue();
        assertThat(args.getFom()).isNotNull();
        assertThat(args.getTom()).isNotNull();
    }

    @Test
    void skal_parse_antall_9_dager_utover_max() {
        final var argMap = new HashMap<String, String>();
        argMap.put(FAGOMRÅDE, "FP");
        argMap.put(ANTALL_DAGER, "9");
        var args = new GrensesnittavstemmingBatchArguments(argMap);
        assertThat(args.isValid()).isFalse();
        assertThat(args.getFom()).isNotNull();
        assertThat(args.getTom()).isNotNull();
    }

    @Test
    void skal_parse_dato() {
        final var argMap = new HashMap<String, String>();
        argMap.put(FOM, "01-11-2014");
        argMap.put(TOM, "07-11-2014");
        argMap.put(FAGOMRÅDE, "FP");
        var args = new GrensesnittavstemmingBatchArguments(argMap);
        assertThat(args.isValid()).isTrue();
        assertThat(args.getFom()).isNotNull();
        assertThat(args.getTom()).isNotNull();
    }

    @Test
    void skal_parse_dato_periode_utover_max() {
        final var argMap = new HashMap<String, String>();
        argMap.put(FOM, "01-11-2014");
        argMap.put(TOM, "09-11-2014");
        argMap.put(FAGOMRÅDE, "FP");
        var args = new GrensesnittavstemmingBatchArguments(argMap);
        assertThat(args.isValid()).isFalse();
        assertThat(args.getFom()).isNotNull();
        assertThat(args.getTom()).isNotNull();
    }

    @Test
    void skal_parse_dato_periode_7_dager() {
        final var argMap = new HashMap<String, String>();
        argMap.put(FOM, "01-11-2014");
        argMap.put(TOM, "08-11-2014");
        argMap.put(FAGOMRÅDE, "FP");
        var args = new GrensesnittavstemmingBatchArguments(argMap);
        assertThat(args.isValid()).isTrue();
        assertThat(args.getFom()).isNotNull();
        assertThat(args.getTom()).isNotNull();
    }

    @Test
    void skal_ikke_feile_ved_satt_for_mange_properties() {
        final var argMap = new HashMap<String, String>();
        argMap.put(FOM, "01-11-2014");
        argMap.put(TOM, "20-11-2014");
        argMap.put(ANTALL_DAGER, "5");
        argMap.put(FAGOMRÅDE, "FP");
        var args = new GrensesnittavstemmingBatchArguments(argMap);
        assertThat(args.isValid()).isFalse();
    }

    @Test
    void skal_feile_fordi_satt_parametre_er_ikke_entydig() {
        final var argMap = new HashMap<String, String>();
        argMap.put(FOM, "01-11-2014");
        argMap.put(ANTALL_DAGER, "5");
        argMap.put(FAGOMRÅDE, "FP");
        var args = new GrensesnittavstemmingBatchArguments(argMap);
        assertThat(args.isValid()).isFalse();
    }

    @Test
    void skal_feile_fordi_fagområde_er_ikke_satt() {
        final var argMap = new HashMap<String, String>();
        argMap.put(FOM, "01-11-2014");
        argMap.put(ANTALL_DAGER, "5");
        var args = new GrensesnittavstemmingBatchArguments(argMap);
        assertThat(args.isValid()).isFalse();
    }

    @Test
    void skal_feile_fordi_fagområde_er_feil() {
        final var argMap = new HashMap<String, String>();
        argMap.put(FOM, "01-11-2014");
        argMap.put(ANTALL_DAGER, "5");
        argMap.put(FAGOMRÅDE, "blabla");
        var args = new GrensesnittavstemmingBatchArguments(argMap);
        assertThat(args.isValid()).isFalse();
    }

    @Test
    void skal_feile_fordi_fagområde_er_null() {
        final var argMap = new HashMap<String, String>();
        argMap.put(ANTALL_DAGER, "8");
        var args = new GrensesnittavstemmingBatchArguments(argMap);
        assertThat(args.isValid()).isFalse();
        assertThat(args.getFom()).isNotNull();
        assertThat(args.getTom()).isNotNull();
    }

}
