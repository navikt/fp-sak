package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import java.util.Set;

public interface MedUttakParametre {

    Set<UttakType> getUttakTyper();

    Set<StønadskontoType> getValgbarForKonto();

    Set<LovEndring> getGyldigForLovendringer();

    enum LovEndring {
        KREVER_SAMMENHENGENDE_UTTAK,
        FRITT_UTTAK
    }

}
