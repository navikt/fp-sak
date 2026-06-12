package no.nav.foreldrepenger.ytelse.beregning;

import java.util.Map;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

public class Feriepengedager {

    // La stå.
    private static final Map<FagsakYtelseType, Integer> YTELSE_DAGER = Map.of(
        FagsakYtelseType.FORELDREPENGER, 60,
        FagsakYtelseType.SVANGERSKAPSPENGER, 64
    );

    private Feriepengedager() {
    }

    public static int forYtelse(FagsakYtelseType ytelseType) {
        return Optional.ofNullable(YTELSE_DAGER.get(ytelseType)).orElseThrow();
    }
}
