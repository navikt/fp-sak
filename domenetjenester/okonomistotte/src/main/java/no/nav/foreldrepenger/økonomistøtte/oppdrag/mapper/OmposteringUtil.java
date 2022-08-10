package no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper;

import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.MottakerOppdragKjedeOversikt;

class OmposteringUtil {

    /**
     * Sjekker om en mottaker har utbetalinger fra tidligere.
     *
     * @param tidligerOppdragForMottaker det som ble utbetalt til en gitt mottaker tidligere
     * @return true om mottakeren har hatt utbetalinger tidligere, ellers false.
     */
    static boolean harGjeldendeUtbetalingerFraTidligere(MottakerOppdragKjedeOversikt tidligerOppdragForMottaker) {
        return tidligerOppdragForMottaker.getKjeder().values().stream().anyMatch(kjede -> !kjede.tilYtelse().getPerioder().isEmpty());
    }

    static boolean erOpphørForMottaker(MottakerOppdragKjedeOversikt inklNyttOppdragForMottaker) {
        return inklNyttOppdragForMottaker.getKjeder().values().stream().allMatch(kjede -> kjede.tilYtelse().getPerioder().isEmpty());
    }
}
