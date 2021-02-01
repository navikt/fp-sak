package no.nav.foreldrepenger.økonomi.økonomistøtte;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Attestant180;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming115;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragsenhet120;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodekomponent;

public class OpprettOppdragTjeneste {

    private static final String TYPE_ENHET = "BOS";
    private static final String ENHET = "8020";
    private static final LocalDate DATO_ENHET_FOM = LocalDate.of(1900, 1, 1);

    private OpprettOppdragTjeneste() {
    }

    public static void opprettOppdragsenhet120(Optional<Oppdrag110> oppdrag110Opt) {
        if (!oppdrag110Opt.isPresent()) {
            return;
        }
        Oppdrag110 oppdrag110 = oppdrag110Opt.get();
        opprettOppdragsenhet120(oppdrag110);
    }

    public static void opprettOppdragsenhet120(Oppdrag110 oppdrag110) {
        Oppdragsenhet120.builder()
            .medTypeEnhet(TYPE_ENHET)
            .medEnhet(ENHET)
            .medDatoEnhetFom(DATO_ENHET_FOM)
            .medOppdrag110(oppdrag110)
            .build();
    }

    public static void opprettAttestant180(Oppdragslinje150 oppdragslinje150, String ansvarligSaksbehandler) {
        Attestant180.builder()
            .medAttestantId(ansvarligSaksbehandler)
            .medOppdragslinje150(oppdragslinje150)
            .build();
    }

    public static Avstemming115 opprettAvstemming115(String localDateTimeStr) {
        return Avstemming115.builder()
            .medKodekomponent(ØkonomiKodekomponent.VLFP.getKodekomponent())
            .medNokkelAvstemming(localDateTimeStr)
            .medTidspnktMelding(localDateTimeStr)
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
