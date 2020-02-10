package no.nav.foreldrepenger.økonomi.økonomistøtte;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;

public interface AlleMottakereHarPositivKvittering {
    /**
     * Har alle oppdragsmottakere en Oppdrag110 med positiv kvittering?
     * @param oppdragskontroll en {@link Oppdragskontroll}
     * @return {@code true} hvis alle oppdragsmottakere har mottatt minst en positiv kvittering
     */
    boolean vurder(Oppdragskontroll oppdragskontroll);
}
