package no.nav.foreldrepenger.behandlingslager.kodeverk;

import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;

public interface BasisKodeverdi extends IndexKey {
    String getKode();
    
    String getOffisiellKode();
    
    String getKodeverk();

    String getNavn();
    
    @Override
    default String getIndexKey() {
        return getKode();
    }
}
