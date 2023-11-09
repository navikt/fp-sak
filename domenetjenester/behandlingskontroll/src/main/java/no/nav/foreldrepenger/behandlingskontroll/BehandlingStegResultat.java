package no.nav.foreldrepenger.behandlingskontroll;

import java.util.Map;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;

/**
 * Signaliserer status på resultat av å kjøre et {@link BehandlingSteg}.
 */
public enum BehandlingStegResultat {

    /**
     * Signaliser at steget er startet, men ikke utført (pågår)
     */
    STARTET,

    /**
     * Signaliser at steget settes på vent. Ingenting pågår mens det står på vent,
     * og det må 'vekkes' opp igjen ved en handling (Saksbehandler), en melding
     * mottas, elleren prosesstask.
     */
    SETT_PÅ_VENT,

    /**
     * Signaliser at steget er ferdig kjørt og del-resultat generert foreligger.
     */
    UTFØRT,

    /**
     * Signaliser at steget er avbrutt og tidligere behandlingssteg skal kjøres på
     * nytt
     */
    TILBAKEFØRT,

    /**
     * Signaliser at steget er ført fremover gjennom overhopp
     */
    FREMOVERFØRT;

    private static final Map<BehandlingStegResultat, BehandlingStegStatus> MAP_HANDLING_STATUS = Map.ofEntries(
            Map.entry(BehandlingStegResultat.STARTET, BehandlingStegStatus.STARTET),
            Map.entry(BehandlingStegResultat.SETT_PÅ_VENT, BehandlingStegStatus.VENTER),
            Map.entry(BehandlingStegResultat.UTFØRT, BehandlingStegStatus.UTFØRT),
            Map.entry(BehandlingStegResultat.FREMOVERFØRT, BehandlingStegStatus.FREMOVERFØRT),
            Map.entry(BehandlingStegResultat.TILBAKEFØRT, BehandlingStegStatus.TILBAKEFØRT));

    static BehandlingStegStatus mapTilStatus(BehandlingStegResultat behandleStegHandling) {
        return Optional.ofNullable(MAP_HANDLING_STATUS.get(behandleStegHandling)).orElseThrow(() -> new IllegalArgumentException(
                "Utvikler-feil: ukjent mapping fra " + BehandlingStegResultat.class.getSimpleName() + "." + behandleStegHandling));

    }
}
