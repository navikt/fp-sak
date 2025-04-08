package no.nav.foreldrepenger.web.server.abac;

import no.nav.vedtak.sikkerhet.abac.AbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.StandardAbacAttributtType;

/**
 * AbacAttributtTyper som er i bruk i FPSAK.
 */
public enum AppAbacAttributtType implements AbacAttributtType {

    AKSJONSPUNKT_DEFINISJON(false);

    public static AbacAttributtType AKTØR_ID = StandardAbacAttributtType.AKTØR_ID;

    public static AbacAttributtType BEHANDLING_UUID = StandardAbacAttributtType.BEHANDLING_UUID;

    public static AbacAttributtType FNR = StandardAbacAttributtType.FNR;

    public static AbacAttributtType JOURNALPOST_ID = StandardAbacAttributtType.JOURNALPOST_ID;

    public static AbacAttributtType SAKSNUMMER = StandardAbacAttributtType.SAKSNUMMER;

    private final boolean maskerOutput;

    AppAbacAttributtType() {
        maskerOutput = false;
    }


    AppAbacAttributtType(boolean maskerOutput) {
        this.maskerOutput = maskerOutput;
    }

    @Override
    public boolean getMaskerOutput() {
        return maskerOutput;
    }
}
