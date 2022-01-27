package no.nav.foreldrepenger.behandlingslager.kodeverk;

import com.fasterxml.jackson.annotation.JsonValue;

/** Kodeverk som er portet til java. */
public interface Kodeverdi extends BasisKodeverdi {

    @JsonValue
    @Override
    String getKode();

    @Override
    String getKodeverk();

}
