package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import no.nav.vedtak.exception.TekniskException;

class UttakEntitetFeil {

    private UttakEntitetFeil() {

    }

    static TekniskException manueltFastettePerioderManglerEksisterendeResultat(Long behandlingId) {
        return new TekniskException("FP-661902", String
                .format("Behandling må ha eksisterende uttaksresultat ved lagring av manuelt fastsatte perioder. Behandling id %s", behandlingId));
    }

}
