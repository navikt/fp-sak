package no.nav.foreldrepenger.domene.uttak.beregnkontoer;

import java.time.LocalDate;
import java.time.Month;

/*
 * Beholdes for å dokumeentere historikk av lovendringer.
 * Ble ifm håndtering av ved vedtak før ikrafttredselsedato.
 * Etter den tid er logikk basert på regler i fp-stonadskonto
 */
public class UttakCore2024 {

    public static final LocalDate FELLES_80P_IKRAFT_FRA_DATO = LocalDate.of(2024, Month.JULY,1); // LA STÅ
    public static final LocalDate MINSTERETT_10U_IKRAFT_FRA_DATO = LocalDate.of(2024,Month.AUGUST,2); // LA STÅ.

    private UttakCore2024() {
    }
}
