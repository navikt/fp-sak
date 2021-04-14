package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;

import no.nav.foreldrepenger.domene.iay.modell.BekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;

final class UtledBekreftetPermisjon {

    private UtledBekreftetPermisjon() {
        // Skjul empty constructor
    }

    static BekreftetPermisjon utled(ArbeidsforholdDto arbeidsforholdDto) {

        var permisjoner = arbeidsforholdDto.getPermisjoner();
        var permisjonStatus = UtledBekreftetPermisjonStatus.utled(arbeidsforholdDto);

        if (BekreftetPermisjonStatus.UDEFINERT.equals(permisjonStatus)) {
            throw new IllegalStateException("Utviklerfeil: Forventer en bekreftet permisjonsstatus som ikke er UDEFINERT.");
        }

        if (BekreftetPermisjonStatus.UGYLDIGE_PERIODER.equals(permisjonStatus)) {
            return new BekreftetPermisjon(permisjonStatus);
        }

        // Forventer kun 1 gyldig permisjonsperiode
        var permisjonFom = permisjoner.get(0).getPermisjonFom();
        var permisjonTom = permisjoner.get(0).getPermisjonTom() == null ? TIDENES_ENDE : permisjoner.get(0).getPermisjonTom();
        return new BekreftetPermisjon(permisjonFom, permisjonTom, permisjonStatus);

    }

}
