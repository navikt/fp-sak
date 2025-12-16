package no.nav.foreldrepenger.behandlingslager.kodeverk;

import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;

/** Kodeverk som er portet til java. */
public interface Kodeverdi extends IndexKey {

    // La stå inntil videre! Er ofte lagret i database som "-" i non-null kolonner.
    String STANDARDKODE_UDEFINERT = "-";

    String getKode();


    String getNavn();

    @Override
    default String getIndexKey() {
        return getKode();
    }

}
