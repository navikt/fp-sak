package no.nav.foreldrepenger.web.app.tjenester.registrering;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.ForeldreType;
import no.nav.foreldrepenger.validering.FeltFeilDto;
import no.nav.foreldrepenger.validering.Valideringsfeil;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.AnnenForelderDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.fp.ManuellRegistreringEndringssøknadValidator;
import no.nav.foreldrepenger.web.app.tjenester.registrering.fp.ManuellRegistreringEndringsøknadDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.fp.ManuellRegistreringForeldrepengerDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.fp.ManuellRegistreringSøknadValidator;
import no.nav.foreldrepenger.web.app.tjenester.registrering.fp.TidsromPermisjonDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.svp.ManuellRegistreringSvangerskapspengerDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.svp.ManuellRegistreringSvangerskapspengerValidator;

class ManuellRegistreringValidator {

    private ManuellRegistreringValidator() {
        // Klassen skal ikke instansieres
    }

    static void validerOpplysninger(ManuellRegistreringDto registreringDto) {
        List<FeltFeilDto> feil = new ArrayList<>();
        if (registreringDto instanceof ManuellRegistreringEndringsøknadDto endringsøknadDto) {
            //Valideringer på endringssøknaden plugges inn her
            feil.addAll(ManuellRegistreringEndringssøknadValidator.validerOpplysninger(endringsøknadDto));
        } else {
            if (registreringDto instanceof ManuellRegistreringSvangerskapspengerDto svangerskapspengerDto) {
                feil.addAll(ManuellRegistreringSvangerskapspengerValidator.validerOpplysninger(svangerskapspengerDto));
            } else {
                //Valider felles felter mellom engangstønad og foreldrepenger
                feil.addAll(ManuellRegistreringFellesValidator.validerOpplysninger(registreringDto));
                if (registreringDto instanceof ManuellRegistreringForeldrepengerDto foreldrepengerDto) {
                    feil.addAll(ManuellRegistreringSøknadValidator.validerOpplysninger(foreldrepengerDto));
                }
            }
        }

        if (!feil.isEmpty()) {
            throw new Valideringsfeil(feil);
        }
    }

    static void validerAktivitetskrav(ManuellRegistreringDto registreringDto, RelasjonsRolleType relasjonsRolleType) {
        TidsromPermisjonDto uttaksperioder = null;
        if (registreringDto instanceof ManuellRegistreringEndringsøknadDto endringsøknadDto && !RelasjonsRolleType.MORA.equals(relasjonsRolleType)) {
            uttaksperioder = endringsøknadDto.getTidsromPermisjon();
        } else if (registreringDto instanceof ManuellRegistreringForeldrepengerDto foreldrepengerDto && !ForeldreType.MOR.equals(registreringDto.getSøker())
            && Optional.ofNullable(registreringDto.getAnnenForelder()).filter(AnnenForelderDto::getSøkerHarAleneomsorg).isEmpty()) {
            uttaksperioder = foreldrepengerDto.getTidsromPermisjon();
        }

        if (!ManuellRegistreringSøknadValidator.validerAktivitetskravFarMedmor(uttaksperioder).isEmpty()) {
            throw new Valideringsfeil(List.of());
        }
    }
}
