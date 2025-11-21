package no.nav.foreldrepenger.web.app.tjenester.registrering;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster.FØR_ELLER_LIK_DAGENS_DATO;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster.LIKT_ANTALL_BARN_OG_FØDSELSDATOER;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster.MINDRE_ELLER_LIK_LENGDE;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster.OPPHOLDSSKJEMA_TOMT;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster.PAAKREVD_FELT;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster.TERMINBEKREFTELSESDATO_FØR_TERMINDATO;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster.TERMINDATO_ELLER_FØDSELSDATO;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster.TERMINDATO_OG_FØDSELSDATO;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster.UGYLDIG_FØDSELSNUMMER;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.validering.FeltFeilDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorUtil.Periode;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.UtenlandsoppholdDto;

public class ManuellRegistreringFellesValidator {

    private ManuellRegistreringFellesValidator() {
        // Klassen skal ikke instansieres
    }

    public static List<FeltFeilDto> validerOpplysninger(ManuellRegistreringDto registreringDto) {
        return Stream.of(validerTidligereUtenlandsopphold(registreringDto),
                validerFremtidigUtenlandsopphold(registreringDto),
                validerTerminEllerFødselsdato(registreringDto),
                validerTermindato(registreringDto),
                validerTerminBekreftelsesdato(registreringDto),
                validerTerminBekreftelseAntallBarn(registreringDto),
                validerAntallBarn(registreringDto),
                validerFødselsdato(registreringDto),
                validerOmsorgsovertakelsesdato(registreringDto),
                validerKanIkkeOppgiAnnenForelder(registreringDto),
                validerAnnenForelderUtenlandskFoedselsnummer(registreringDto),
                validerAnnenForelderFødselsnummer(registreringDto),
                validerMottattDato(registreringDto))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    static Optional<FeltFeilDto> validerTidligereUtenlandsopphold(ManuellRegistreringDto registreringDto) {
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

    static Optional<FeltFeilDto> validerFremtidigUtenlandsopphold(ManuellRegistreringDto registreringDto) {
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

    static Optional<FeltFeilDto> validerTerminEllerFødselsdato(ManuellRegistreringDto manuellRegistreringDto) {
        var feltnavn = "terminEllerFoedsel";
        if (erFødsel(manuellRegistreringDto)) {
            var harTerminDato = nonNull(manuellRegistreringDto.getTermindato());
            var harFødselsDato = manuellRegistreringDto.getFødselsdato() != null;
            if (!harTerminDato && !harFødselsDato) {
                return Optional.of(new FeltFeilDto(feltnavn, TERMINDATO_ELLER_FØDSELSDATO));
            }
        }
        return Optional.empty();
    }

    static Optional<FeltFeilDto> validerTermindato(ManuellRegistreringDto manuellRegistreringDto) {
        var feltnavn = "terminDato";
        if (erFødsel(manuellRegistreringDto) && !manuellRegistreringDto.getErBarnetFødt()) {
            //Termindato når barnet ikke er født.
            var terminDato = manuellRegistreringDto.getTermindato();
            if (!nonNull(terminDato)) {
                return Optional.of(new FeltFeilDto(feltnavn, PAAKREVD_FELT));
            }
        }
        return Optional.empty();
    }

    static Optional<FeltFeilDto> validerTerminBekreftelsesdato(ManuellRegistreringDto manuellRegistreringDto) {
        var feltnavn = "terminbekreftelseDato";
        if (erFødsel(manuellRegistreringDto) && !manuellRegistreringDto.getErBarnetFødt()) {
            var terminbekreftelseDato = manuellRegistreringDto.getTerminbekreftelseDato();
            var termindato = manuellRegistreringDto.getTermindato();
            var harFødselsdato = manuellRegistreringDto.getFødselsdato() != null;
            var harTermindato = nonNull(termindato);
            if (nonNull(terminbekreftelseDato)) {
                var feltFeilDto = validerTerminBekreftelsesdato(terminbekreftelseDato, termindato, harFødselsdato, harTermindato, feltnavn);
                if (feltFeilDto.isPresent()) {
                    return feltFeilDto;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<FeltFeilDto> validerTerminBekreftelsesdato(LocalDate terminbekreftelseDato, LocalDate termindato, boolean harFødselsdato, boolean harTermindato, String feltnavn) {
        if (harFødselsdato) {
            return Optional.of(new FeltFeilDto(feltnavn, TERMINDATO_OG_FØDSELSDATO));
        }
        if (terminbekreftelseDato.isAfter(LocalDate.now())) {
            return Optional.of(new FeltFeilDto(feltnavn, FØR_ELLER_LIK_DAGENS_DATO));
        }
        if (harTermindato && !terminbekreftelseDato.isBefore(termindato)) {
            return Optional.of(new FeltFeilDto(feltnavn, TERMINBEKREFTELSESDATO_FØR_TERMINDATO));
        }
        return Optional.empty();
    }

    static Optional<FeltFeilDto> validerTerminBekreftelseAntallBarn(ManuellRegistreringDto registreringDto) {
        var feltnavn = "antallBarnFraTerminbekreftelse";
        if (erFødsel(registreringDto) && !registreringDto.getErBarnetFødt()) {
            var harFødselsdato = registreringDto.getFødselsdato() != null;
            if (harFødselsdato && nonNull(registreringDto.getAntallBarnFraTerminbekreftelse())) {
                return Optional.of(new FeltFeilDto(feltnavn, TERMINDATO_OG_FØDSELSDATO));
            }
            if (nonNull(registreringDto.getTermindato()) && isNull(registreringDto.getAntallBarnFraTerminbekreftelse())) {
                return Optional.of(new FeltFeilDto(feltnavn, PAAKREVD_FELT));
            }
        }
        return Optional.empty();
    }

    static Optional<FeltFeilDto> validerAntallBarn(ManuellRegistreringDto registreringDto) {
        var feltnavn = "antallBarn";
        if (erAdopsjonEllerOmsorg(registreringDto) && isNull(registreringDto.getOmsorg().getAntallBarn())) {
            return Optional.of(new FeltFeilDto(feltnavn, PAAKREVD_FELT));
        }
        var harFødselsdato = registreringDto.getFødselsdato() != null;
        if (harFødselsdato && erFødsel(registreringDto) && isNull(registreringDto.getAntallBarn())) {
            return Optional.of(new FeltFeilDto(feltnavn, PAAKREVD_FELT));
        }
        return Optional.empty();
    }


    static Optional<FeltFeilDto> validerOmsorgsovertakelsesdato(ManuellRegistreringDto registreringDto) {
        var feltnavn = "omsorgsovertakelsesdato";
        if (erAdopsjonEllerOmsorg(registreringDto) && isNull(registreringDto.getOmsorg().getOmsorgsovertakelsesdato())) {
            return Optional.of(new FeltFeilDto(feltnavn, PAAKREVD_FELT));
        }
        return Optional.empty();
    }


    static Optional<FeltFeilDto> validerFødselsdato(ManuellRegistreringDto registreringDto) {
        var feltnavn = "foedselsDato";
        Predicate<LocalDate> pred = d -> d.isAfter(LocalDate.now());
        var fødselsdatoer = hentFødselsdatoer(registreringDto);
        if (erAdopsjonEllerOmsorg(registreringDto)) {
            if (fødselsdatoer.isEmpty()) {
                return Optional.of(new FeltFeilDto(feltnavn, LIKT_ANTALL_BARN_OG_FØDSELSDATOER));
            }
            var antallBarn = registreringDto.getOmsorg().getAntallBarn();
            if (nonNull(antallBarn) && antallBarn != fødselsdatoer.size()) {
                return Optional.of(new FeltFeilDto(feltnavn, LIKT_ANTALL_BARN_OG_FØDSELSDATOER));
            }
            if (fødselsdatoer.stream().anyMatch(pred)) {
                return Optional.of(new FeltFeilDto(feltnavn, FØR_ELLER_LIK_DAGENS_DATO));
            }
        } else if (erFødsel(registreringDto) && registreringDto.getErBarnetFødt()) {
            if (fødselsdatoer.isEmpty()) {
                return Optional.of(new FeltFeilDto(feltnavn, PAAKREVD_FELT));
            }
            if (fødselsdatoer.stream().anyMatch(pred)) {
                return Optional.of(new FeltFeilDto(feltnavn, FØR_ELLER_LIK_DAGENS_DATO));
            }
        }
        return Optional.empty();
    }

    private static List<LocalDate> hentFødselsdatoer(ManuellRegistreringDto registreringDto) {
        List<LocalDate> fødselsdatoer;
        if (erAdopsjonEllerOmsorg(registreringDto)) {
            fødselsdatoer = Optional.ofNullable(registreringDto.getOmsorg().getFødselsdato()).orElse(List.of());
        } else {
            fødselsdatoer = Optional.ofNullable(registreringDto.getFødselsdato()).map(List::of).orElse(List.of());
        }
        return fødselsdatoer.stream()
            .filter(Objects::nonNull)
            .toList();
    }

    static Optional<FeltFeilDto> validerKanIkkeOppgiAnnenForelder(ManuellRegistreringDto registreringDto) {
        var feltnavn = "arsak";
        var annenForelder = registreringDto.getAnnenForelder();

        if (!isNull(annenForelder) && TRUE.equals(annenForelder.getKanIkkeOppgiAnnenForelder())) {
            var kanIkkeOppgiBegrunnelse = annenForelder.getKanIkkeOppgiBegrunnelse();
            if (isNull(kanIkkeOppgiBegrunnelse) || kanIkkeOppgiBegrunnelse.getÅrsak().isBlank()) {
                return Optional.of(new FeltFeilDto(feltnavn, PAAKREVD_FELT));
            }
        }
        return Optional.empty();
    }

    static Optional<FeltFeilDto> validerAnnenForelderUtenlandskFoedselsnummer(ManuellRegistreringDto registreringDto) {
        var feltnavn = "utenlandskFoedselsnummer";
        short tillattLengde = 20;
        var annenForelder = registreringDto.getAnnenForelder();
        if (!isNull(annenForelder) && TRUE.equals(annenForelder.getKanIkkeOppgiAnnenForelder())) {
            var kanIkkeOppgiBegrunnelse = annenForelder.getKanIkkeOppgiBegrunnelse();
            if (isNull(kanIkkeOppgiBegrunnelse)) {
                //Har vi ikke valgt årsak kan vi heller ikke validere utenlandsk fødselsnummer.
                return Optional.empty();
            }
            var utenlandskFoedselsnummer = kanIkkeOppgiBegrunnelse.getUtenlandskFødselsnummer();
            if (utenlandskFoedselsnummer != null && !utenlandskFoedselsnummer.isBlank() && erStoerreEnnTillatt(tillattLengde,
                utenlandskFoedselsnummer)) {
                return Optional.of(new FeltFeilDto(feltnavn, MINDRE_ELLER_LIK_LENGDE + tillattLengde));
            }
        }
        return Optional.empty();
    }

    static Optional<FeltFeilDto> validerAnnenForelderFødselsnummer(ManuellRegistreringDto registreringDto) {
        var feltnavn = "foedselsnummer";
        var annenForelder = registreringDto.getAnnenForelder();
        if (!isNull(annenForelder) && !TRUE.equals(annenForelder.getKanIkkeOppgiAnnenForelder())) {
            if (annenForelder.getFødselsnummer() == null || annenForelder.getFødselsnummer().isBlank()) {
                return Optional.of(new FeltFeilDto(feltnavn, PAAKREVD_FELT));
            }
            if (!PersonIdent.erGyldigFnr(annenForelder.getFødselsnummer())) {
                return Optional.of(new FeltFeilDto(feltnavn, UGYLDIG_FØDSELSNUMMER));
            }
        }
        return Optional.empty();
    }

    static Optional<FeltFeilDto> validerMottattDato(ManuellRegistreringDto manuellRegistreringDto) {
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

    private static boolean erFødsel(ManuellRegistreringDto manuellRegistreringDto) {
        return FamilieHendelseType.FØDSEL.equals(manuellRegistreringDto.getTema());
    }

    private static boolean erAdopsjonEllerOmsorg(ManuellRegistreringDto manuellRegistreringDto) {
        return FamilieHendelseType.ADOPSJON.equals(manuellRegistreringDto.getTema()) ||
            FamilieHendelseType.OMSORG.equals(manuellRegistreringDto.getTema());
    }

    private static boolean erTomListe(List<?> list) {
        return isNull(list) || list.isEmpty();
    }

    private static boolean erStoerreEnnTillatt(short lengde, String verdi) {
        return verdi != null && verdi.trim().length() > lengde;
    }

}
