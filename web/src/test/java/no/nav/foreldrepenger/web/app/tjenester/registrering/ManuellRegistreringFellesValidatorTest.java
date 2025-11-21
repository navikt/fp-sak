package no.nav.foreldrepenger.web.app.tjenester.registrering;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster.PAAKREVD_FELT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.FiktiveFnr;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.AnnenForelderDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.OmsorgDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.UtenlandsoppholdDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.es.ManuellRegistreringEngangsstonadDto;

class ManuellRegistreringFellesValidatorTest {

    @Test
    void validererTidligereUtenlandsopphold() {
        var forventetFeltnavn = "tidligereOppholdUtenlands";
        ManuellRegistreringDto registreringDto = new ManuellRegistreringEngangsstonadDto();
        registreringDto.setHarTidligereOppholdUtenlands(true);
        var feltFeil = ManuellRegistreringFellesValidator.validerTidligereUtenlandsopphold(registreringDto);
        assertThat(feltFeil).as("Detaljer om tidligere utenlandsopphold er påkrevd hvis man har oppholdt seg i utlandet").isPresent();
        assertThat(feltFeil.get().getMelding()).as("Detaljer om tidligere utenlandsopphold er påkrevd hvis man har oppholdt seg i utlandet")
                .isEqualTo(ManuellRegistreringValidatorTekster.OPPHOLDSSKJEMA_TOMT);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);

        registreringDto.setHarTidligereOppholdUtenlands(false);
        feltFeil = ManuellRegistreringFellesValidator.validerTidligereUtenlandsopphold(registreringDto);
        assertThat(feltFeil).as("Detaljer om tidligere utenlandsopphold er ikke påkrevd hvis man ikke har oppholdt seg i utlandet").isNotPresent();
    }

    @Test
    void validererTidligereUtenlandsoppholdDatoer() {
        var forventetFeltnavn = "tidligereOppholdUtenlands";
        ManuellRegistreringDto registreringDto = new ManuellRegistreringEngangsstonadDto();
        registreringDto.setHarTidligereOppholdUtenlands(true);

        var idag = LocalDate.now();
        var overlappDagensDato = opprettUtenlandsOpphold(idag.minusDays(1), idag.plusDays(2), "FRA");
        registreringDto.setTidligereOppholdUtenlands(singletonList(overlappDagensDato));
        var feltFeil = ManuellRegistreringFellesValidator.validerTidligereUtenlandsopphold(registreringDto);
        assertThat(feltFeil).as("Periode for utenlandsopphold kan ikke overlappe dagens dato").isPresent();
        assertThat(feltFeil.get().getMelding()).as(" Periode for utenlandsopphold kan ikke overlappe dagensdato")
                .isEqualTo(ManuellRegistreringValidatorTekster.TIDLIGERE_DATO);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);

        var etterDagensDato = opprettUtenlandsOpphold(idag, idag.plusDays(3), "FRA");
        registreringDto.setTidligereOppholdUtenlands(singletonList(etterDagensDato));
        feltFeil = ManuellRegistreringFellesValidator.validerTidligereUtenlandsopphold(registreringDto);
        assertThat(feltFeil).as("Periode for utenlandsopphold kan ikke være etter dagens dato").isPresent();
        assertThat(feltFeil.get().getMelding()).as("Periode for utenlandsopphold kan ikke være etter dagens dato")
                .isEqualTo(ManuellRegistreringValidatorTekster.TIDLIGERE_DATO);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);

        var fireMånederSidenTilTreMånederSiden = opprettUtenlandsOpphold(idag.minusMonths(4), idag.minusMonths(3), "FRA");
        registreringDto.setTidligereOppholdUtenlands(asList(fireMånederSidenTilTreMånederSiden, fireMånederSidenTilTreMånederSiden));
        feltFeil = ManuellRegistreringFellesValidator.validerTidligereUtenlandsopphold(registreringDto);
        assertThat(feltFeil).as("Perioder for utenlandsopphold kan ikke fullstendig overlappe").isPresent();
        assertThat(feltFeil.get().getMelding()).as("Perioder for utenlandsopphold kan ikke fullstendig overlappe")
                .isEqualTo(ManuellRegistreringValidatorTekster.OVERLAPPENDE_PERIODER);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);

        var femMånederSidenTilToMånederSiden = opprettUtenlandsOpphold(idag.minusMonths(5), idag.minusMonths(2), "FRA");
        registreringDto.setTidligereOppholdUtenlands(asList(fireMånederSidenTilTreMånederSiden, femMånederSidenTilToMånederSiden));
        feltFeil = ManuellRegistreringFellesValidator.validerTidligereUtenlandsopphold(registreringDto);
        assertThat(feltFeil).as("Perioder for utenlandsopphold kan ikke delvis overlappe").isPresent();
        assertThat(feltFeil.get().getMelding()).as("Perioder for utenlandsopphold kan ikke delvis overlappe")
                .isEqualTo(ManuellRegistreringValidatorTekster.OVERLAPPENDE_PERIODER);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);

        var treMånederSidenTilToMånederSiden = opprettUtenlandsOpphold(idag.minusMonths(3), idag.minusMonths(2), "FRA");
        registreringDto.setTidligereOppholdUtenlands(asList(femMånederSidenTilToMånederSiden, treMånederSidenTilToMånederSiden));
        feltFeil = ManuellRegistreringFellesValidator.validerTidligereUtenlandsopphold(registreringDto);
        assertThat(feltFeil).as("Perioder for utenlandsopphold kan ikke delvis overlappe").isPresent();
        assertThat(feltFeil.get().getMelding()).as("Perioder for utenlandsopphold kan ikke delvis overlappe")
                .isEqualTo(ManuellRegistreringValidatorTekster.OVERLAPPENDE_PERIODER);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);

        registreringDto.setTidligereOppholdUtenlands(asList(fireMånederSidenTilTreMånederSiden, treMånederSidenTilToMånederSiden));
        feltFeil = ManuellRegistreringFellesValidator.validerTidligereUtenlandsopphold(registreringDto);
        assertThat(feltFeil).as("To perioder kan slutte/starte på samme dag uten å overlappe").isNotPresent();

        var fomEtterTom = opprettUtenlandsOpphold(idag.minusMonths(4), idag.minusMonths(5), "SUI");
        registreringDto.setTidligereOppholdUtenlands(singletonList(fomEtterTom));
        feltFeil = ManuellRegistreringFellesValidator.validerTidligereUtenlandsopphold(registreringDto);
        assertThat(feltFeil).as("Dato for start av periode må være før dato for slutt av periode.").isPresent();
        assertThat(feltFeil.get().getMelding()).as("Dato for start av periode må være før dato for slutt av periode.")
                .isEqualTo(ManuellRegistreringValidatorTekster.STARTDATO_FØR_SLUTTDATO);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);
    }

    @Test
    void validererFremtidigUtenlandsoppholdDatoer() {
        var forventetFeltnavn = "fremtidigOppholdUtenlands";
        ManuellRegistreringDto registreringDto = new ManuellRegistreringEngangsstonadDto();
        registreringDto.setHarFremtidigeOppholdUtenlands(true);
        var idag = LocalDate.now();
        registreringDto.setMottattDato(idag);

        var sammeFomOgTom = opprettUtenlandsOpphold(idag.plusMonths(1), idag.plusMonths(1), "SWE");
        registreringDto.setFremtidigeOppholdUtenlands(singletonList(sammeFomOgTom));
        var feltFeil = ManuellRegistreringFellesValidator.validerFremtidigUtenlandsopphold(registreringDto);
        assertThat(feltFeil).as("Kan ha samme start- og sluttdato").isEmpty();

        var fomFørMottattDato = opprettUtenlandsOpphold(registreringDto.getMottattDato().minusDays(1), idag.plusMonths(1), "SWE");
        registreringDto.setFremtidigeOppholdUtenlands(singletonList(fomFørMottattDato));
        feltFeil = ManuellRegistreringFellesValidator.validerFremtidigUtenlandsopphold(registreringDto);
        assertThat(feltFeil).as("Startdato kan ikke være før mottatt dato").isPresent();
        assertThat(feltFeil.get().getMelding()).as("Startdato kan ikke være før mottatt dato")
                .isEqualTo(ManuellRegistreringValidatorTekster.LIK_ELLER_ETTER_MOTTATT_DATO);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);

        var tomFørFom = opprettUtenlandsOpphold(idag.plusDays(10), idag.plusDays(2), "SWE");
        registreringDto.setFremtidigeOppholdUtenlands(singletonList(tomFørFom));
        feltFeil = ManuellRegistreringFellesValidator.validerFremtidigUtenlandsopphold(registreringDto);
        assertThat(feltFeil).as("Startdato må være før sluttdato").isPresent();
        assertThat(feltFeil.get().getMelding()).as("Startdato må være før sluttdato")
                .isEqualTo(ManuellRegistreringValidatorTekster.STARTDATO_FØR_SLUTTDATO);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);

        var periode1 = opprettUtenlandsOpphold(idag.plusDays(10), idag.plusDays(100), "SWE");
        var tomLikPeriode1Fom = opprettUtenlandsOpphold(idag.plusDays(5), idag.plusDays(10), "SWE");
        registreringDto.setFremtidigeOppholdUtenlands(asList(periode1, tomLikPeriode1Fom));
        feltFeil = ManuellRegistreringFellesValidator.validerFremtidigUtenlandsopphold(registreringDto);
        assertThat(feltFeil).as("Startdato kan være lik sluttdato til en annen periode").isNotPresent();

        var annenPeriode = opprettUtenlandsOpphold(idag.plusDays(150), idag.plusDays(200), "SWE");
        registreringDto.setFremtidigeOppholdUtenlands(asList(periode1, annenPeriode));
        feltFeil = ManuellRegistreringFellesValidator.validerFremtidigUtenlandsopphold(registreringDto);
        assertThat(feltFeil).as("Gyldige perioder skal ikke gi valideringsfeil").isNotPresent();
    }

    @Test
    void validererFremtidigUtenlandsopphold() {
        var forventetFeltnavn = "fremtidigOppholdUtenlands";
        ManuellRegistreringDto registreringDto = new ManuellRegistreringEngangsstonadDto();
        registreringDto.setHarFremtidigeOppholdUtenlands(true);

        var feltFeil = ManuellRegistreringFellesValidator.validerFremtidigUtenlandsopphold(registreringDto);
        assertThat(feltFeil).as("Detaljer om fremtidige utenlandsopphold er påkrevd hvis man skal oppholde seg i utlandet").isPresent();
        assertThat(feltFeil.get().getMelding()).as("Detaljer om fremtidige utenlandsopphold er påkrevd hvis man skal oppholde seg i utlandet")
                .isEqualTo(ManuellRegistreringValidatorTekster.OPPHOLDSSKJEMA_TOMT);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);

        registreringDto.setHarFremtidigeOppholdUtenlands(false);
        feltFeil = ManuellRegistreringFellesValidator.validerFremtidigUtenlandsopphold(registreringDto);
        assertThat(feltFeil).as("Detaljer om fremtidige utenlandsopphold er ikke påkrevd hvis man ikke skal oppholde seg i utlandet").isNotPresent();
    }

    @Test
    void validerTerminEllerFødsel() {
        var forventetFeltnavn = "terminEllerFoedsel";
        ManuellRegistreringDto registreringDto = new ManuellRegistreringEngangsstonadDto();
        registreringDto.setTema(FamilieHendelseType.FØDSEL);
        registreringDto.setErBarnetFødt(Boolean.TRUE);

        var feltFeil = ManuellRegistreringFellesValidator.validerTerminEllerFødselsdato(registreringDto);

        assertThat(feltFeil).as("Enten termin- eller fødselsdato må være fylt ut").isPresent();
        assertThat(feltFeil.get().getMelding()).isEqualTo(ManuellRegistreringValidatorTekster.TERMINDATO_ELLER_FØDSELSDATO);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);

        registreringDto.setFødselsdato(LocalDate.now());
        registreringDto.setTermindato(LocalDate.now());

        feltFeil = ManuellRegistreringFellesValidator.validerTerminEllerFødselsdato(registreringDto);

        assertThat(feltFeil).isNotPresent(); // Ikke feil at termin- og fødselsdato begge er satt. Er barnet født styrer
                                             // hvilken vi bruker.

        registreringDto.setTermindato(null);

        feltFeil = ManuellRegistreringFellesValidator.validerTerminEllerFødselsdato(registreringDto);

        assertThat(feltFeil).isNotPresent();

        registreringDto.setTermindato(LocalDate.now());
        registreringDto.setFødselsdato(null);

        feltFeil = ManuellRegistreringFellesValidator.validerTerminEllerFødselsdato(registreringDto);

        assertThat(feltFeil).isNotPresent();
    }

    @Test
    void validererTermindato() {
        ManuellRegistreringDto registreringDto = new ManuellRegistreringEngangsstonadDto();
        registreringDto.setTema(FamilieHendelseType.FØDSEL);
        registreringDto.setErBarnetFødt(Boolean.FALSE);

        var feltFeil = ManuellRegistreringFellesValidator.validerTermindato(registreringDto);
        assertThat(feltFeil).isPresent();
    }

    @Test
    void validererTerminBekreftelsesdato() {
        var forventetFeltnavn = "terminbekreftelseDato";
        ManuellRegistreringDto registreringDto = new ManuellRegistreringEngangsstonadDto();
        registreringDto.setTema(FamilieHendelseType.FØDSEL);
        registreringDto.setErBarnetFødt(Boolean.FALSE);
        registreringDto.setTerminbekreftelseDato(now());

        var feltFeil = ManuellRegistreringFellesValidator.validerTerminBekreftelsesdato(registreringDto);
        assertThat(feltFeil).as("Terminbekreftelsesdato kan være lik dagens dato").isNotPresent();

        registreringDto.setTerminbekreftelseDato(now().minusDays(1));
        feltFeil = ManuellRegistreringFellesValidator.validerTerminBekreftelsesdato(registreringDto);
        assertThat(feltFeil).as("Terminbekreftelsesdato kan være før dagens dato").isNotPresent();

        registreringDto.setTerminbekreftelseDato(now().plusDays(1));
        feltFeil = ManuellRegistreringFellesValidator.validerTerminBekreftelsesdato(registreringDto);
        assertThat(feltFeil).as("Terminbekreftelsesdato kan ikke være etter dagens dato").isPresent();
        assertThat(feltFeil.get().getMelding()).as("Terminbekreftelsesdato kan ikke være etter dagens dato")
                .isEqualTo(ManuellRegistreringValidatorTekster.FØR_ELLER_LIK_DAGENS_DATO);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);

        registreringDto.setTermindato(now().minusWeeks(1));
        registreringDto.setTerminbekreftelseDato(now().minusWeeks(1).plusDays(1));
        feltFeil = ManuellRegistreringFellesValidator.validerTerminBekreftelsesdato(registreringDto);
        assertThat(feltFeil).as("Terminbekreftelsesdato kan ikke være etter termindato").isPresent();
        assertThat(feltFeil.get().getMelding()).as("Terminbekreftelsesdato kan ikke være etter termindato")
                .isEqualTo(ManuellRegistreringValidatorTekster.TERMINBEKREFTELSESDATO_FØR_TERMINDATO);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);

        registreringDto.setTermindato(now().minusWeeks(1));
        registreringDto.setTerminbekreftelseDato(now().minusWeeks(1));
        feltFeil = ManuellRegistreringFellesValidator.validerTerminBekreftelsesdato(registreringDto);
        assertThat(feltFeil).as("Terminbekreftelsesdato kan ikke være lik termindato").isPresent();
        assertThat(feltFeil.get().getMelding()).as("Terminbekreftelsesdato kan ikke være lik termindato")
                .isEqualTo(ManuellRegistreringValidatorTekster.TERMINBEKREFTELSESDATO_FØR_TERMINDATO);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);

        registreringDto.setFødselsdato(LocalDate.now());
        registreringDto.setTerminbekreftelseDato(LocalDate.now().minusWeeks(1));
        feltFeil = ManuellRegistreringFellesValidator.validerTerminBekreftelsesdato(registreringDto);
        assertThat(feltFeil).as("Skal ikke fylle inn terminbekreftelsesdato når fødselsdato er fylt ut").isPresent();
        assertThat(feltFeil.get().getMelding()).as("Skal ikke fylle inn terminbekreftelsesdato når fødselsdato er fylt ut")
                .isEqualTo(ManuellRegistreringValidatorTekster.TERMINDATO_OG_FØDSELSDATO);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);

    }

    @Test
    void validererTerminbekreftelseAntallBarn() {
        var forventetFeltnavn = "antallBarnFraTerminbekreftelse";
        ManuellRegistreringDto registreringDto = new ManuellRegistreringEngangsstonadDto();
        registreringDto.setTema(FamilieHendelseType.FØDSEL);
        registreringDto.setErBarnetFødt(Boolean.FALSE);

        var feltFeil = ManuellRegistreringFellesValidator.validerTerminBekreftelseAntallBarn(registreringDto);
        assertThat(feltFeil).as("Antall barn fra terminbekreftelse er ikke påkrevd dersom termindato ikke er satt").isNotPresent();

        registreringDto.setTermindato(LocalDate.now().plusWeeks(14));
        feltFeil = ManuellRegistreringFellesValidator.validerTerminBekreftelseAntallBarn(registreringDto);
        assertThat(feltFeil).as("Antall barn må være fylt ut").isPresent();
        assertThat(feltFeil.get().getMelding()).as("Antall barn må være fylt ut").isEqualTo(PAAKREVD_FELT);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);

        registreringDto.setFødselsdato(LocalDate.now());
        registreringDto.setAntallBarnFraTerminbekreftelse(1);
        feltFeil = ManuellRegistreringFellesValidator.validerTerminBekreftelseAntallBarn(registreringDto);
        assertThat(feltFeil).as("Skal ikke fylle inn terminbekreftelseAntallBarn når fødselsdato er fylt ut").isPresent();
        assertThat(feltFeil.get().getMelding()).as("Skal ikke fylle inn terminbekreftelseAntallBarn når fødselsdato er fylt ut")
                .isEqualTo(ManuellRegistreringValidatorTekster.TERMINDATO_OG_FØDSELSDATO);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);
    }

    @Test
    void validerAntallBarnFoedsel() {
        var forventetFeltnavn = "antallBarn";
        ManuellRegistreringDto registreringDto = new ManuellRegistreringEngangsstonadDto();
        registreringDto.setTema(FamilieHendelseType.FØDSEL);
        registreringDto.setFødselsdato(LocalDate.now());

        var feltFeil = ManuellRegistreringFellesValidator.validerAntallBarn(registreringDto);

        assertThat(feltFeil).as("Antall barn er påkrevd når fødselsdato er fylt ut").isPresent();
        assertThat(feltFeil.get().getMelding()).as("Antall barn er påkrevd når fødselsdato er fylt ut").isEqualTo(PAAKREVD_FELT);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);

        registreringDto.setAntallBarn(1);

        feltFeil = ManuellRegistreringFellesValidator.validerAntallBarn(registreringDto);

        assertThat(feltFeil).isNotPresent();
    }

    @Test
    void validerAntallBarnAdopsjon() {
        var forventetFeltnavn = "antallBarn";
        ManuellRegistreringDto registreringDto = new ManuellRegistreringEngangsstonadDto();
        registreringDto.setTema(FamilieHendelseType.ADOPSJON);
        registreringDto.setOmsorg(new OmsorgDto());

        var feltFeil = ManuellRegistreringFellesValidator.validerAntallBarn(registreringDto);

        assertThat(feltFeil).as("Antall barn er påkrevd ved adopsjon").isPresent();
        assertThat(feltFeil.get().getMelding()).as("Antall barn er påkrevd ved adopsjon").isEqualTo(PAAKREVD_FELT);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);
    }

    @Test
    void validerFødselsdatoFødsel() {
        ManuellRegistreringDto registreringDto = new ManuellRegistreringEngangsstonadDto();
        registreringDto.setTema(FamilieHendelseType.FØDSEL);
        registreringDto.setErBarnetFødt(Boolean.TRUE);
        registreringDto.setAntallBarn(1);
        registreringDto.setFødselsdato(LocalDate.now().minusMonths(1));

        var feltFeil = ManuellRegistreringFellesValidator.validerFødselsdato(registreringDto);

        assertThat(feltFeil).isNotPresent();

        registreringDto.setFødselsdato(null);
        registreringDto.setAntallBarn(null);

        feltFeil = ManuellRegistreringFellesValidator.validerFødselsdato(registreringDto);

        assertThat(feltFeil).as("Fødselsdato er påkrevd når barnet er født er satt til JA").isPresent();
    }

    @Test
    void validererFødselsdatoAdopsjon() {
        var forventetFeltnavn = "foedselsDato";
        ManuellRegistreringDto registreringDto = new ManuellRegistreringEngangsstonadDto();
        registreringDto.setTema(FamilieHendelseType.ADOPSJON);
        registreringDto.setErBarnetFødt(Boolean.TRUE);

        var omsorgDto = new OmsorgDto();
        omsorgDto.setAntallBarn(2);
        omsorgDto.setOmsorgsovertakelsesdato(LocalDate.now().minusWeeks(2));
        List<LocalDate> fødselsdatoer = new ArrayList<>();
        omsorgDto.setFødselsdato(fødselsdatoer);
        registreringDto.setOmsorg(omsorgDto);

        var feltFeil = ManuellRegistreringFellesValidator.validerFødselsdato(registreringDto);
        assertThat(feltFeil).as("En fødselsdato pr barn er påkrevd (ingen er fylt ut)").isPresent();
        assertThat(feltFeil.get().getMelding()).as("En fødselsdato pr barn er påkrevd (ingen er fylt ut)")
                .isEqualTo(ManuellRegistreringValidatorTekster.LIKT_ANTALL_BARN_OG_FØDSELSDATOER);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);

        fødselsdatoer.add(LocalDate.now().minusYears(2));
        feltFeil = ManuellRegistreringFellesValidator.validerFødselsdato(registreringDto);
        assertThat(feltFeil).as("En fødselsdato pr barn er påkrevd (kun en er fylt ut)").isPresent();
        assertThat(feltFeil.get().getMelding()).as("En fødselsdato pr barn er påkrevd (kun en er fylt ut)")
                .isEqualTo(ManuellRegistreringValidatorTekster.LIKT_ANTALL_BARN_OG_FØDSELSDATOER);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);

        fødselsdatoer.add(LocalDate.now().minusYears(1));
        feltFeil = ManuellRegistreringFellesValidator.validerFødselsdato(registreringDto);
        assertThat(feltFeil).as("En fødselsdato pr barn er påkrevd (alle er fylt ut)").isNotPresent();
    }

    @Test
    void validerOmsorgsoveFrtakelsesdato() {
        var forventetFeltnavn = "omsorgsovertakelsesdato";
        ManuellRegistreringDto registreringDto = new ManuellRegistreringEngangsstonadDto();
        registreringDto.setTema(FamilieHendelseType.ADOPSJON);
        registreringDto.setOmsorg(new OmsorgDto());

        var feltFeil = ManuellRegistreringFellesValidator.validerOmsorgsovertakelsesdato(registreringDto);

        assertThat(feltFeil).as("Dato for omsorgsovertakelse er påkrevd").isPresent();
        assertThat(feltFeil.get().getMelding()).as("Dato for omsorgsovertakelse er påkrevd").isEqualTo(PAAKREVD_FELT);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);

        registreringDto.getOmsorg().setOmsorgsovertakelsesdato(LocalDate.now());

        feltFeil = ManuellRegistreringFellesValidator.validerOmsorgsovertakelsesdato(registreringDto);

        assertThat(feltFeil).isNotPresent();
    }

    @Test
    void validerAnnenForelderUtenlandskFoedselsnummer() {
        var forventetFeltnavn = "utenlandskFoedselsnummer";
        ManuellRegistreringDto registreringDto = new ManuellRegistreringEngangsstonadDto();
        var annenForelderDto = new AnnenForelderDto();
        registreringDto.setAnnenForelder(annenForelderDto);
        annenForelderDto.setKanIkkeOppgiAnnenForelder(true);
        annenForelderDto.setKanIkkeOppgiBegrunnelse(new AnnenForelderDto.KanIkkeOppgiBegrunnelse());
        var kanIkkeOppgiBegrunnelse = annenForelderDto.getKanIkkeOppgiBegrunnelse();

        kanIkkeOppgiBegrunnelse.setUtenlandskFødselsnummer("123456789012345678801");
        var feltFeil = ManuellRegistreringFellesValidator.validerAnnenForelderUtenlandskFoedselsnummer(registreringDto);
        assertThat(feltFeil.get().getMelding()).as("Fornavn til annen forelder er stoerre enn 20")
                .isEqualTo(ManuellRegistreringValidatorTekster.MINDRE_ELLER_LIK_LENGDE + "20");
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);
    }

    @Test
    void validerAnnenForelderFødselsnummer() {
        var forventetFeltnavn = "foedselsnummer";
        ManuellRegistreringDto registreringDto = new ManuellRegistreringEngangsstonadDto();
        var annenForelderDto = new AnnenForelderDto();
        registreringDto.setAnnenForelder(annenForelderDto);

        var feltFeil = ManuellRegistreringFellesValidator.validerAnnenForelderFødselsnummer(registreringDto);
        assertThat(feltFeil).as("Fødselsnummer til annen forelder er påkrevd").isPresent();
        assertThat(feltFeil.get().getMelding()).as("Fødselsnummer til annen forelder er påkrevd").isEqualTo(PAAKREVD_FELT);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);

        annenForelderDto.setFødselsnummer(new FiktiveFnr().nesteMannFnr());
        feltFeil = ManuellRegistreringFellesValidator.validerAnnenForelderFødselsnummer(registreringDto);
        assertThat(feltFeil).as("Fødselsnummer til annen forelder er påkrevd og fylt ut").isNotPresent();

        annenForelderDto.setKanIkkeOppgiAnnenForelder(true);
        annenForelderDto.setFødselsnummer(null);
        feltFeil = ManuellRegistreringFellesValidator.validerAnnenForelderFødselsnummer(registreringDto);
        assertThat(feltFeil).as("Fødselsnummer er ikke påkrevd hvis man ikke kan oppgi annen forelder").isNotPresent();
    }

    @Test
    void validerMottattdato() {
        var forventetFeltnavn = "mottattDato";
        ManuellRegistreringDto registreringDto = new ManuellRegistreringEngangsstonadDto();

        var feltFeil = ManuellRegistreringFellesValidator.validerMottattDato(registreringDto);

        assertThat(feltFeil).as("Dato for mottatt er påkrevd").isPresent();
        assertThat(feltFeil.get().getMelding()).as("Dato for mottatt er påkrevd").isEqualTo(PAAKREVD_FELT);
        assertThat(feltFeil.get().getNavn()).isEqualTo(forventetFeltnavn);

        registreringDto.setMottattDato(LocalDate.now());

        feltFeil = ManuellRegistreringFellesValidator.validerMottattDato(registreringDto);

        assertThat(feltFeil).isNotPresent();
    }

    private UtenlandsoppholdDto opprettUtenlandsOpphold(LocalDate fom, LocalDate tom, String land) {
        var utenlandsopphold = new UtenlandsoppholdDto();
        utenlandsopphold.setPeriodeFom(fom);
        utenlandsopphold.setPeriodeTom(tom);
        utenlandsopphold.setLand(land);
        return utenlandsopphold;
    }
}
