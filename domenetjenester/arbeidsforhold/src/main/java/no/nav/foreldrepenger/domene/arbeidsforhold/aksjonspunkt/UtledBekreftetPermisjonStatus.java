package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;

final class UtledBekreftetPermisjonStatus {

    private UtledBekreftetPermisjonStatus(){
        // Skjul empty constructor
    }

    static BekreftetPermisjonStatus utled(ArbeidsforholdDto arbeidsforholdDto) {
        BekreftetPermisjonStatus status = BekreftetPermisjonStatus.UDEFINERT;
        if (arbeidsforholdDto.getPermisjoner().size() > 1){
            return BekreftetPermisjonStatus.UGYLDIGE_PERIODER;
        }
        if (Boolean.TRUE.equals(arbeidsforholdDto.getBrukPermisjon())) {
            return BekreftetPermisjonStatus.BRUK_PERMISJON;
        }
        if (Boolean.FALSE.equals(arbeidsforholdDto.getBrukPermisjon())) {
            return BekreftetPermisjonStatus.IKKE_BRUK_PERMISJON;
        }
        return status;
    }

}
