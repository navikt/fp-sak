package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.util.Optional;

import no.nav.foreldrepenger.domene.iay.modell.BekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;

final class UtledBrukAvPermisjonForWrapper {

    private UtledBrukAvPermisjonForWrapper() {
        // Skjuler default public constructor
    }

    static Boolean utled(Optional<BekreftetPermisjon> bekreftetPermisjonOpt){
        if (bekreftetPermisjonOpt.isPresent() && !BekreftetPermisjonStatus.UDEFINERT.equals(bekreftetPermisjonOpt.get().getStatus())) {
            return BekreftetPermisjonStatus.BRUK_PERMISJON.equals(bekreftetPermisjonOpt.get().getStatus());
        }
        return null;
    }

}
