package no.nav.foreldrepenger.behandlingslager.kodeverk;

import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;

/**
 * Kodeverk som er portet til java.
 */
public interface Kodeverdi extends IndexKey {

    String getKode();

    String getKodeverk();

    String getNavn();

    @Override
    default String getIndexKey() {
        return getKode();
    }

}
