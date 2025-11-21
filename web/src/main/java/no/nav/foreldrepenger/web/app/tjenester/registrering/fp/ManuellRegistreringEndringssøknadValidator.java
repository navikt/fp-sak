package no.nav.foreldrepenger.web.app.tjenester.registrering.fp;

import static java.util.Objects.isNull;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster.PAAKREVD_FELT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import no.nav.foreldrepenger.validering.FeltFeilDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorUtil;

public class ManuellRegistreringEndringssøknadValidator {

    private ManuellRegistreringEndringssøknadValidator() {
    }

    public static List<FeltFeilDto> validerOpplysninger(ManuellRegistreringEndringsøknadDto registreringDto) {
        return Stream.of(
            validerTidsromPermisjon(registreringDto))
            .flatMap(Collection::stream)
            .toList();
    }

    private static List<FeltFeilDto> validerTidsromPermisjon(ManuellRegistreringEndringsøknadDto registreringDto) {
        List<FeltFeilDto> result = new ArrayList<>();
        //Valider far(medmor) spesifikke felter
        validerOverføringAvKvoter(registreringDto).ifPresent(result::add);

        //Valider tidspermisjonsfelter som er felles for alle foreldretyper
        var tidsromPermisjon = registreringDto.getTidsromPermisjon();
        if(tidsromPermisjon != null) {
            var feltFeilPermisjonsperiode = validerPermisjonsperiode(tidsromPermisjon);
            feltFeilPermisjonsperiode.ifPresent(result::add);

            if (tidsromPermisjon.getUtsettelsePeriode() != null) {
                result.addAll(ManuellRegistreringSøknadValidator.validerUtsettelse(tidsromPermisjon.getUtsettelsePeriode()));
            }
            if (tidsromPermisjon.getGraderingPeriode() != null) {
                result.addAll(ManuellRegistreringSøknadValidator.validerGradering(tidsromPermisjon.getGraderingPeriode()));
            }
        }

        return result;
    }

    private static Optional<FeltFeilDto> validerPermisjonsperiode(TidsromPermisjonDto tidsromPermisjon) {
        var feltnavn = "permisjonperioder";
        List<String> feil = new ArrayList<>();
        var permisjonperioder = tidsromPermisjon.getPermisjonsPerioder();
        if (!isNull(permisjonperioder)) {
            var perioder = permisjonperioder.stream().map(fkp ->
                new ManuellRegistreringValidatorUtil.Periode(fkp.getPeriodeFom(), fkp.getPeriodeTom())).toList();
            feil.addAll(ManuellRegistreringValidatorUtil.datoIkkeNull(perioder));
            feil.addAll(ManuellRegistreringValidatorUtil.startdatoFørSluttdato(perioder));
            feil.addAll(ManuellRegistreringValidatorUtil.overlappendePerioder(perioder));
        }

        if (feil.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new FeltFeilDto(feltnavn, String.join(", ", feil)));
    }

    private static Optional<FeltFeilDto> validerOverføringAvKvoter(ManuellRegistreringEndringsøknadDto registreringDto) {
        var tidsromPermisjon = registreringDto.getTidsromPermisjon();

        for (var overføringsperiodeDto : tidsromPermisjon.getOverføringsperioder()) {
            if (isNull(overføringsperiodeDto.getOverforingArsak())) {
                return Optional.of(new FeltFeilDto("årsakForOverføring", PAAKREVD_FELT));
            }
        }

        return Optional.empty();
    }

}
