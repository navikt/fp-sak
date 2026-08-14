package no.nav.foreldrepenger.behandling.kabal;

/*
 * Kilde repo kabal-api og type ExternalUtfall
 *
 * Disse sendes ikke til oss og er ikke tatt med (uviss håndtering)
 * - NNSTILLING_GJENOPPTAS_KAS_VEDTAK_STADFESTES, INNSTILLING_GJENOPPTAS_IKKE
 * - GJENOPPTATT_STADFESTET, IKKE_GJENOPPTATT
 * - HENVIST
 */
public enum KabalUtfall {
    TRUKKET, // Bruker trekker klage/anke
    HENLAGT, // Klage/anke henlagt av høyere instans
    HEVET, // Anke i TrR der bruker trekker anken
    RETUR, // Retur fra høyere instans
    AVVIST,
    OPPHEVET,
    MEDHOLD,
    DELVIS_MEDHOLD,
    UGUNST,
    STADFESTELSE,
    INNSTILLING_STADFESTELSE,
    INNSTILLING_AVVIST,
    MEDHOLD_ETTER_FVL_35,
    GJENOPPTATT_DELVIS_ELLER_FULLT_MEDHOLD,
    GJENOPPTATT_OPPHEVET
}
