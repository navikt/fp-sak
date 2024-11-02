package no.nav.foreldrepenger.behandling.kabal;

public enum KabalUtfall {
    TRUKKET, // Bruker trekker klage/anke
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
    MEDHOLD_ETTER_FVL_35
}
