package no.nav.foreldrepenger.behandlingslager.kodeverk;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;

/** Kodeverk som er portet til java. */
public interface Kodeverdi extends IndexKey {

    @JsonValue
    String getKode();

    String getKodeverk();

    String getNavn();

    @Override
    default String getIndexKey() {
        return getKode();
    }

}
