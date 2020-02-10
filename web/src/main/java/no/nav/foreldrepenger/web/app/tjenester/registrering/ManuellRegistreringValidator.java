package no.nav.foreldrepenger.web.app.tjenester.registrering;

import java.util.ArrayList;
import java.util.List;

import no.nav.foreldrepenger.validering.FeltFeilDto;
import no.nav.foreldrepenger.validering.Valideringsfeil;
import no.nav.foreldrepenger.web.app.tjenester.registrering.fp.ManuellRegistreringEndringssøknadValidator;
import no.nav.foreldrepenger.web.app.tjenester.registrering.fp.ManuellRegistreringEndringsøknadDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.fp.ManuellRegistreringForeldrepengerDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.fp.ManuellRegistreringSøknadValidator;
import no.nav.foreldrepenger.web.app.tjenester.registrering.svp.ManuellRegistreringSvangerskapspengerDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.svp.ManuellRegistreringSvangerskapspengerValidator;

public class ManuellRegistreringValidator {

    private ManuellRegistreringValidator() {
        // Klassen skal ikke instansieres
    }

    public static void validerOpplysninger(ManuellRegistreringDto registreringDto) {
        List<FeltFeilDto> feil = new ArrayList<>();
        if (registreringDto instanceof ManuellRegistreringEndringsøknadDto) {
            //Valideringer på endringssøknaden plugges inn her
            feil.addAll(ManuellRegistreringEndringssøknadValidator.validerOpplysninger((ManuellRegistreringEndringsøknadDto) registreringDto));
        } else {
            if (registreringDto instanceof ManuellRegistreringSvangerskapspengerDto) {
                feil.addAll(ManuellRegistreringSvangerskapspengerValidator.validerOpplysninger((ManuellRegistreringSvangerskapspengerDto)registreringDto));
            } else {
                //Valider felles felter mellom engangstønad og foreldrepenger
                feil.addAll(ManuellRegistreringFellesValidator.validerOpplysninger(registreringDto));
                if (registreringDto instanceof ManuellRegistreringForeldrepengerDto) {
                    feil.addAll(ManuellRegistreringSøknadValidator.validerOpplysninger((ManuellRegistreringForeldrepengerDto) registreringDto));
                }
            }
        }

        if (!feil.isEmpty()) {
            throw new Valideringsfeil(feil);
        }
    }
}
