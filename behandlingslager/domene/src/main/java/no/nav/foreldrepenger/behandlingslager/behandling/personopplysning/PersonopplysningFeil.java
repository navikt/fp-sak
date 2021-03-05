package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import no.nav.vedtak.exception.TekniskException;


final class PersonopplysningFeil {

    private PersonopplysningFeil() {
    }

    static TekniskException måBasereSegPåEksisterendeVersjon() {
        return new TekniskException("FP-124903", "Må basere seg på eksisterende versjon av personopplysning");
    }
}

