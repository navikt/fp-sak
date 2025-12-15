package no.nav.foreldrepenger.web.app.tjenester.registrering.fp;

import static java.util.Objects.isNull;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster.MANGLER_MORS_AKTIVITET;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster.PAAKREVD_FELT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.validering.FeltFeilDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster;
import no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorUtil;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.DekningsgradDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.EgenVirksomhetDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.FrilansDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.GraderingDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.UtsettelseDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.VirksomhetDto;

public class ManuellRegistreringSøknadValidator {

    private ManuellRegistreringSøknadValidator() {
        // Klassen skal ikke instansieres
    }

    public static List<FeltFeilDto> validerOpplysninger(ManuellRegistreringForeldrepengerDto registreringDto) {
        return Stream.of(
            validerAndreYtelser(),
            validerDekningsgrad(registreringDto.getDekningsgrad()),
            validerEgenVirksomhet(registreringDto.getEgenVirksomhet()),
            validerFrilans(registreringDto.getFrilans()),
            validerTidsromPermisjon(registreringDto))
            .flatMap(Collection::stream)
            .toList();
    }

    public static List<FeltFeilDto> validerFrilans(FrilansDto frilans) {
        if (Boolean.TRUE.equals(frilans.getHarSøkerPeriodeMedFrilans()) && frilans.getPerioder().isEmpty()) {
            return List.of(new FeltFeilDto("", ManuellRegistreringValidatorTekster.MINDRE_ELLER_LIK_LENGDE));
        }
        return List.of();
    }

    public static List<FeltFeilDto> validerEgenVirksomhet(EgenVirksomhetDto egenVirksomhet) {
        List<FeltFeilDto> feltFeil = new ArrayList<>();
        var feltnavn = "harArbeidetIEgenVirksomhet";
        if (egenVirksomhet.getHarArbeidetIEgenVirksomhet() == null) {
            feltFeil.add(new FeltFeilDto(feltnavn, PAAKREVD_FELT));
        }
        if (Boolean.TRUE.equals(egenVirksomhet.getHarArbeidetIEgenVirksomhet())) {
            for (var virksomhet : egenVirksomhet.getVirksomheter()) {
                leggTilFeilForVirksomhet(feltFeil, virksomhet);
            }
        }

        return feltFeil;
    }

    public static List<FeltFeilDto> validerAktivitetskravFarMedmor(TidsromPermisjonDto tidsromPermisjonDto) {
        var manglerMorsAktivitet = Optional.ofNullable(tidsromPermisjonDto)
            .map(TidsromPermisjonDto::getPermisjonsPerioder).orElse(List.of()).stream()
            .filter(p -> Set.of(UttakPeriodeType.FELLESPERIODE, UttakPeriodeType.FORELDREPENGER).contains(p.getPeriodeType()))
            .filter(p -> !p.isFlerbarnsdager())
            .anyMatch(p -> p.getMorsAktivitet() == null || MorsAktivitet.UDEFINERT.equals(p.getMorsAktivitet()));
        return manglerMorsAktivitet ? List.of(new FeltFeilDto("morsAktivitet", MANGLER_MORS_AKTIVITET)) : List.of();
    }

    private static void leggTilFeilForVirksomhet(List<FeltFeilDto> feltFeil, VirksomhetDto virksomhet) {
        if (virksomhet.getNavn() == null) {
            feltFeil.add(new FeltFeilDto("virksomhetNavn", PAAKREVD_FELT));
        }
        if (virksomhet.getVirksomhetRegistrertINorge() == null) {
            feltFeil.add(new FeltFeilDto("virksomhetRegistrertINorge", PAAKREVD_FELT));
        }
        if (virksomhet.getLandJobberFra() == null) {
            feltFeil.add(new FeltFeilDto("landJobberFra", PAAKREVD_FELT));
        }
        if (Boolean.TRUE.equals(virksomhet.getVirksomhetRegistrertINorge())) {
            if (virksomhet.getOrganisasjonsnummer() == null) {
                feltFeil.add(new FeltFeilDto("virksomhetOrganisasjonsnummer", PAAKREVD_FELT));
            }
        } else if (virksomhet.getFom() == null) {
            feltFeil.add(new FeltFeilDto("utenlandskNæringsvirksomhetStartDato", PAAKREVD_FELT));
        }
    }

    static List<FeltFeilDto> validerDekningsgrad(DekningsgradDto dekningsgrad) {
        List<FeltFeilDto> feltFeil = new ArrayList<>();
        var feltnavn = "dekningsgrad";
        if (dekningsgrad == null) {
            feltFeil.add(new FeltFeilDto(feltnavn, PAAKREVD_FELT));
        }
        return feltFeil;
    }

    static List<FeltFeilDto> validerAndreYtelser() {
        return new ArrayList<>();
    }

    static List<FeltFeilDto> validerTidsromPermisjon(ManuellRegistreringForeldrepengerDto registreringDto) {

        List<FeltFeilDto> result = new ArrayList<>();
        //Valider far(medmor) spesifikke felter
        validerTidsromPermisjonFarEllerMedmor(registreringDto).ifPresent(result::add);

        //Valider tidspermisjonsfelter som er felles for alle foreldretyper
        var tidsromPermisjon = registreringDto.getTidsromPermisjon();
        var feltFeilPermisjonsperiode = validerPermisjonsperiode(tidsromPermisjon);
        feltFeilPermisjonsperiode.ifPresent(result::add);

        result.addAll(validerUtsettelse(tidsromPermisjon.getUtsettelsePeriode()));
        result.addAll(validerGradering(tidsromPermisjon.getGraderingPeriode()));

        return result;
    }

    private static Optional<FeltFeilDto> validerTidsromPermisjonFarEllerMedmor(ManuellRegistreringForeldrepengerDto registreringDto) {
        var tidsromPermisjon = registreringDto.getTidsromPermisjon();
        var overføringsperioder = tidsromPermisjon.getOverføringsperioder();
        for (var overføringsperiode : overføringsperioder) {
            if (isNull(overføringsperiode.getOverforingArsak())) {
                return Optional.of(new FeltFeilDto("årsakForOverføring", PAAKREVD_FELT));
            }
            //Opprett periode av fra til dato.
            var perioder = List.of(new ManuellRegistreringValidatorUtil.Periode(overføringsperiode.getPeriodeFom(), overføringsperiode.getPeriodeTom()));
            var feil = validerSomIkkePåkrevdePerioder(perioder, "årsakForOverføring");
            if (feil.isPresent()) {
                return feil;
            }

        }
        return Optional.empty();
    }

    static List<FeltFeilDto> validerGradering(List<GraderingDto> graderingPerioder) {
        var feltnavnGradering = "gradering";
        List<FeltFeilDto> feltFeilGradering = new ArrayList<>();
        var perioder = graderingPerioder.stream().map(fkp ->
            new ManuellRegistreringValidatorUtil.Periode(fkp.getPeriodeFom(), fkp.getPeriodeTom())).toList();

        var feilIPerioder = validerSomPåkrevdePerioder(perioder, feltnavnGradering);
        feilIPerioder.ifPresent(feltFeilGradering::add);

        for (var gradering : graderingPerioder) {
            if (gradering.getPeriodeForGradering() == null) {
                feltFeilGradering.add(new FeltFeilDto("periodeForGradering", PAAKREVD_FELT));
            }
            if (gradering.getProsentandelArbeid() == null) {
                feltFeilGradering.add(new FeltFeilDto("prosentandelArbeid", PAAKREVD_FELT));
            }
            if (harSamtidigUttakUtenSamtidigUttaksprosent(gradering)) {
                feltFeilGradering.add(new FeltFeilDto("samtidigUttaksprosent", PAAKREVD_FELT));
            }
            if (gradering.isErArbeidstaker() && manglerArbeidsgiver(gradering)) {
                feltFeilGradering.add(new FeltFeilDto("arbeidsgiver", PAAKREVD_FELT));
            }
        }
        return feltFeilGradering;
    }

    private static boolean manglerArbeidsgiver(GraderingDto gradering) {
        return gradering.getArbeidsgiverIdentifikator() == null || gradering.getArbeidsgiverIdentifikator().isEmpty();
    }

    private static boolean harSamtidigUttakUtenSamtidigUttaksprosent(GraderingDto gradering) {
        return gradering.getHarSamtidigUttak() && gradering.getSamtidigUttaksprosent() == null;
    }

    private static Optional<FeltFeilDto> validerSomPåkrevdePerioder(List<ManuellRegistreringValidatorUtil.Periode> perioder, String feltnavn) {
        List<String> feilPerioder = new ArrayList<>(ManuellRegistreringValidatorUtil.datoIkkeNull(perioder));
        if (!feilPerioder.isEmpty()) {
            return Optional.of(new FeltFeilDto(feltnavn, String.join(", ", feilPerioder)));
        }
        return validerSomIkkePåkrevdePerioder(perioder, feltnavn);
    }

    private static Optional<FeltFeilDto> validerSomIkkePåkrevdePerioder(List<ManuellRegistreringValidatorUtil.Periode> perioder, String feltnavn) {
        List<String> feilPerioder = new ArrayList<>();

        feilPerioder.addAll(ManuellRegistreringValidatorUtil.startdatoFørSluttdato(perioder));
        feilPerioder.addAll(ManuellRegistreringValidatorUtil.overlappendePerioder(perioder));

        if (!feilPerioder.isEmpty()) {
            return Optional.of(new FeltFeilDto(feltnavn, String.join(", ", feilPerioder)));
        }
        return Optional.empty();
    }

    static List<FeltFeilDto> validerUtsettelse(List<UtsettelseDto> utsettelsePerioder) {
        var feltnavnTidsromForPermisjon = "utsettelsePerioder";
        List<String> feilUtsettelsePerioder = new ArrayList<>();
        List<FeltFeilDto> feltFeilUtsettelse = new ArrayList<>();

        var perioder = utsettelsePerioder.stream().map(fkp ->
            new ManuellRegistreringValidatorUtil.Periode(fkp.getPeriodeFom(), fkp.getPeriodeTom())).toList();

        feilUtsettelsePerioder.addAll(ManuellRegistreringValidatorUtil.datoIkkeNull(perioder));
        feilUtsettelsePerioder.addAll(ManuellRegistreringValidatorUtil.startdatoFørSluttdato(perioder));
        feilUtsettelsePerioder.addAll(ManuellRegistreringValidatorUtil.overlappendePerioder(perioder));

        if (!feilUtsettelsePerioder.isEmpty()) {
            var feltFeilUtsettelsePerioder = new FeltFeilDto(feltnavnTidsromForPermisjon, String.join(", ", feilUtsettelsePerioder));
            feltFeilUtsettelse.add(feltFeilUtsettelsePerioder);
        }

        for (var utsettelse : utsettelsePerioder) {
            if (utsettelse.getArsakForUtsettelse() == null) {
                feltFeilUtsettelse.add(new FeltFeilDto("arsakForUtsettelse", PAAKREVD_FELT));
            }
        }
        return feltFeilUtsettelse;
    }

    static Optional<FeltFeilDto> validerPermisjonsperiode(TidsromPermisjonDto tidsromPermisjon) {
        var feltnavn = "permisjonperioder";
        List<String> feil = new ArrayList<>();
        var permisjonperioder = tidsromPermisjon.getPermisjonsPerioder();
        var perioder = permisjonperioder.stream().map(fkp ->
            new ManuellRegistreringValidatorUtil.Periode(fkp.getPeriodeFom(), fkp.getPeriodeTom())).toList();
        feil.addAll(ManuellRegistreringValidatorUtil.datoIkkeNull(perioder));
        feil.addAll(ManuellRegistreringValidatorUtil.startdatoFørSluttdato(perioder));
        feil.addAll(ManuellRegistreringValidatorUtil.overlappendePerioder(perioder));

        if (feil.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new FeltFeilDto(feltnavn, String.join(", ", feil)));
    }
}
