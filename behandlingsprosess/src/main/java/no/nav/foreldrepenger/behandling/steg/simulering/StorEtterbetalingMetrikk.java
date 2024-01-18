package no.nav.foreldrepenger.behandling.steg.simulering;


import io.prometheus.client.Counter;

public class StorEtterbetalingMetrikk {

    private static final String NAME = "foreldrepenger_etterbetaling_total";
    private static final Counter counter;

    static {
        counter = Counter.build()
            .name(NAME)
            .labelNames("etterbetaling_verdi", "behandlingsaarsaker", "ytelse", "harAndreAksjonspunkt")
            .help("Antall store etterbetalinger")
            .register();
    }

    public static void count(String etterbetaling, String behandlingsaarsaker, String ytelse, boolean harAndreAksjonspunkt) {
        counter
            .labels(
                "etterbetaling_verdi", etterbetaling,
                "behandlingsaarsaker", behandlingsaarsaker,
                "ytelse", ytelse,
                "harAndreAksjonspunkt", String.valueOf(harAndreAksjonspunkt)
                )
            .inc();
    }
}
