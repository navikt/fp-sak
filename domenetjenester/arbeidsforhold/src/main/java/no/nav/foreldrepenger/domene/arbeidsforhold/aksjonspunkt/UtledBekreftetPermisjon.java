package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.domene.arbeidsforhold.dto.PermisjonDto;
import no.nav.foreldrepenger.domene.iay.modell.BekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;

final class UtledBekreftetPermisjon {

    private UtledBekreftetPermisjon(){
        // Skjul empty constructor
    }

    static BekreftetPermisjon utled(ArbeidsforholdDto arbeidsforholdDto) {

        List<PermisjonDto> permisjoner = arbeidsforholdDto.getPermisjoner();
        BekreftetPermisjonStatus permisjonStatus = UtledBekreftetPermisjonStatus.utled(arbeidsforholdDto);

        if (BekreftetPermisjonStatus.UDEFINERT.equals(permisjonStatus)) {
            throw new IllegalStateException("Utviklerfeil: Forventer en bekreftet permisjonsstatus som ikke er UDEFINERT.");
        }

        if (BekreftetPermisjonStatus.UGYLDIGE_PERIODER.equals(permisjonStatus)) {
            return new BekreftetPermisjon(permisjonStatus);
        }

        // Forventer kun 1 gyldig permisjonsperiode
        LocalDate permisjonFom = permisjoner.get(0).getPermisjonFom();
        LocalDate permisjonTom = permisjoner.get(0).getPermisjonTom() == null ? TIDENES_ENDE : permisjoner.get(0).getPermisjonTom();
        return new BekreftetPermisjon(permisjonFom, permisjonTom, permisjonStatus);

    }


}
