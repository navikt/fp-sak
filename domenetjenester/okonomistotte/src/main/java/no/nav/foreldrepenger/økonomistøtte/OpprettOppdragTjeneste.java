package no.nav.foreldrepenger.økonomistøtte;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Attestant180;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;

public class OpprettOppdragTjeneste {

    private OpprettOppdragTjeneste() {
    }

    public static void opprettAttestant180(Oppdragslinje150 oppdragslinje150, String ansvarligSaksbehandler) {
        Attestant180.builder()
            .medAttestantId(ansvarligSaksbehandler)
            .medOppdragslinje150(oppdragslinje150)
            .build();
    }

    public static long incrementInitialValue(long initialValue) {
        return ++initialValue;
    }

    public static long genererFagsystemId(long saksnummer, long initialValue) {
        long verdi = incrementInitialValue(initialValue);
        return concatenateValues(saksnummer, verdi);
    }

    public static long concatenateValues(Number... values) {
        String result = Arrays.stream(values).map(Object::toString).collect(Collectors.joining());
        return Long.parseLong(result);
    }
}
