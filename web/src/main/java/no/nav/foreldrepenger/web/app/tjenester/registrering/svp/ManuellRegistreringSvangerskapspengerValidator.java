package no.nav.foreldrepenger.web.app.tjenester.registrering.svp;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster.FØR_ELLER_LIK_DAGENS_DATO;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster.OPPHOLDSSKJEMA_TOMT;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster.PAAKREVD_FELT;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster.PERIODER_MANGLER;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import no.nav.foreldrepenger.validering.FeltFeilDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorUtil;
import no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorUtil.Periode;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.EgenVirksomhetDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.FrilansDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.UtenlandsoppholdDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.VirksomhetDto;

public class ManuellRegistreringSvangerskapspengerValidator {

    private ManuellRegistreringSvangerskapspengerValidator() {
        // Klassen skal ikke instansieres
    }

    public static List<FeltFeilDto> validerOpplysninger(ManuellRegistreringSvangerskapspengerDto registreringDto) {

        return Stream.concat(
            Stream.of(
                validerEgenVirksomhet(registreringDto.getEgenVirksomhet()))
                .flatMap(Collection::stream),
            Stream.of(
                validerTidligereUtenlandsopphold(registreringDto),
                validerFremtidigUtenlandsopphold(registreringDto),
                validerTermindato(registreringDto),
                validerMottattDato(registreringDto),
                validerFrilans(registreringDto.getFrilans()))
                .filter(Optional::isPresent)
                .map(Optional::get)
        ).toList();
    }


    private static Optional<FeltFeilDto> validerFrilans(FrilansDto frilans) {
        if (Boolean.TRUE.equals(frilans.getHarSøkerPeriodeMedFrilans()) && empty(frilans.getPerioder())) {
            return Optional.of(new FeltFeilDto("frilans", PERIODER_MANGLER));
        }
        return Optional.empty();
    }

    private static boolean empty(Collection<FrilansDto.Frilansperiode> perioder) {
        return perioder == null || perioder.isEmpty();
    }

    private static List<FeltFeilDto> validerEgenVirksomhet(EgenVirksomhetDto egenVirksomhet) {
        var feltFeil = new ArrayList<FeltFeilDto>();
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


    private static Optional<FeltFeilDto> validerTidligereUtenlandsopphold(ManuellRegistreringDto registreringDto) {
        var feltnavn = "tidligereOppholdUtenlands";
        if (registreringDto.getHarTidligereOppholdUtenlands()) {
            if (erTomListe(registreringDto.getTidligereOppholdUtenlands())) {
                return Optional.of(new FeltFeilDto(feltnavn, OPPHOLDSSKJEMA_TOMT));
            }
            return validerTidligereUtenlandsoppholdDatoer(registreringDto.getTidligereOppholdUtenlands(), feltnavn);
        }
        return Optional.empty();
    }

    private static Optional<FeltFeilDto> validerTidligereUtenlandsoppholdDatoer(List<UtenlandsoppholdDto> tidligereOppholdUtenlands, String feltnavn) {
        var feil = new ArrayList<String>();
        var perioder = tidligereOppholdUtenlands.stream().map(tou -> new Periode(tou.getPeriodeFom(), tou.getPeriodeTom())).toList();
        feil.addAll(ManuellRegistreringValidatorUtil.datoIkkeNull(perioder));
        feil.addAll(ManuellRegistreringValidatorUtil.startdatoFørSluttdato(perioder));
        feil.addAll(ManuellRegistreringValidatorUtil.periodeFørDagensDato(perioder));
        feil.addAll(ManuellRegistreringValidatorUtil.overlappendePerioder(perioder));

        if (feil.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new FeltFeilDto(feltnavn, String.join(", ", feil)));
    }

    private static Optional<FeltFeilDto> validerFremtidigUtenlandsopphold(ManuellRegistreringDto registreringDto) {
        var feltnavn = "fremtidigOppholdUtenlands";
        if (registreringDto.getHarFremtidigeOppholdUtenlands()) {
            if (erTomListe(registreringDto.getFremtidigeOppholdUtenlands())) {
                return Optional.of(new FeltFeilDto(feltnavn, OPPHOLDSSKJEMA_TOMT));
            }
            return validerFremtidigOppholdUtenlandsDatoer(registreringDto.getFremtidigeOppholdUtenlands(), registreringDto.getMottattDato(), feltnavn);
        }
        return Optional.empty();
    }

    private static Optional<FeltFeilDto> validerFremtidigOppholdUtenlandsDatoer(List<UtenlandsoppholdDto> fremtidigOppholdUtenlands, LocalDate mottattDato, String feltnavn) {
        var feil = new ArrayList<String>();
        var perioder = fremtidigOppholdUtenlands.stream().map(fou -> new Periode(fou.getPeriodeFom(), fou.getPeriodeTom())).toList();
        feil.addAll(ManuellRegistreringValidatorUtil.datoIkkeNull(perioder));
        feil.addAll(ManuellRegistreringValidatorUtil.startdatoFørSluttdato(perioder));
        feil.addAll(ManuellRegistreringValidatorUtil.startdatoFørMottatDato(perioder, mottattDato));
        feil.addAll(ManuellRegistreringValidatorUtil.overlappendePerioder(perioder));
        if (feil.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new FeltFeilDto(feltnavn, String.join(", ", feil)));
    }

    private static Optional<FeltFeilDto> validerTermindato(ManuellRegistreringDto manuellRegistreringDto) {
        var feltnavn = "terminDato";
        var terminDato = manuellRegistreringDto.getTermindato();
        if (terminDato == null) {
            return Optional.of(new FeltFeilDto(feltnavn, PAAKREVD_FELT));
        }
        return Optional.empty();
    }

    private static Optional<FeltFeilDto> validerMottattDato(ManuellRegistreringDto manuellRegistreringDto) {
        var feltnavn = "mottattDato";
        var mottattDato = manuellRegistreringDto.getMottattDato();
        if (nonNull(mottattDato) && mottattDato.isAfter(LocalDate.now())) {
                return Optional.of(new FeltFeilDto(feltnavn, FØR_ELLER_LIK_DAGENS_DATO));

        }
        if (isNull(mottattDato)) {
            return Optional.of(new FeltFeilDto(feltnavn, PAAKREVD_FELT));
        }
        return Optional.empty();
    }

    private static boolean erTomListe(List<?> list) {
        return isNull(list) || list.isEmpty();
    }


}
