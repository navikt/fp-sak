package no.nav.foreldrepenger.web.app.tjenester.registrering;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.ForeldreType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.web.app.tjenester.kodeverk.dto.AndreYtelserDto;
import no.nav.foreldrepenger.web.app.tjenester.kodeverk.dto.NaringsvirksomhetTypeDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.AnnenForelderDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.ArbeidsforholdDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.EgenVirksomhetDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.GraderingDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.OmsorgDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.RettigheterDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.UtsettelseDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.VirksomhetDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.es.ManuellRegistreringEngangsstonadDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.fp.PermisjonPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.fp.TidsromPermisjonDto;

public class SøknadMapperUtil {

    public static EgenVirksomhetDto opprettEgenVirksomhetDto() {
        var egenVirksomhetDto = new EgenVirksomhetDto();
        egenVirksomhetDto.setVirksomheter(List.of(opprettNorskVirksomhetMedEndringUtenRegnskapsfører()));
        return egenVirksomhetDto;
    }

    public static void oppdaterDtoForFødsel(ManuellRegistreringDto dto,
                                            boolean erBarnetFødt,
                                            LocalDate fødselssdato,
                                            int antallBarn) {
        dto.setTema(FamilieHendelseType.FØDSEL);
        dto.setSøker(ForeldreType.MOR);
        dto.setMottattDato(LocalDate.now());
        dto.setFødselsdato(fødselssdato);
        dto.setAntallBarn(antallBarn);
        dto.setErBarnetFødt(erBarnetFødt);
    }

    public static UtsettelseDto opprettUtsettelseDto(LocalDate fraDato, LocalDate tilDato, UttakPeriodeType gradering) {
        var dto = new UtsettelseDto();
        dto.setArsakForUtsettelse(UtsettelseÅrsak.FERIE);
        dto.setPeriodeFom(fraDato);
        dto.setPeriodeTom(tilDato);
        dto.setPeriodeForUtsettelse(gradering);
        return dto;
    }

    public static PermisjonPeriodeDto opprettPermisjonPeriodeDto(LocalDate fraDato,
                                                                 LocalDate tilDato,
                                                                 UttakPeriodeType uttakPeriodeType,
                                                                 MorsAktivitet morsAktivitet) {
        var dto = new PermisjonPeriodeDto();
        dto.setPeriodeFom(fraDato);
        dto.setPeriodeTom(tilDato);
        dto.setPeriodeType(uttakPeriodeType);
        dto.setMorsAktivitet(morsAktivitet);
        return dto;
    }

    public static NavBruker opprettBruker() {
        return NavBruker.opprettNyNB(AktørId.dummy());

    }

    static ManuellRegistreringEngangsstonadDto opprettAdosjonDto(FamilieHendelseType tema,
                                                                 LocalDate omsorgsovertakelsesdato,
                                                                 LocalDate fødselsdato,
                                                                 int antallBarn,
                                                                 LocalDate ankomstdato) {
        var manuellRegistreringEngangsstonadDto = new ManuellRegistreringEngangsstonadDto();
        manuellRegistreringEngangsstonadDto.setTema(tema);
        manuellRegistreringEngangsstonadDto.setSøker(ForeldreType.MOR);

        var omsorgDto = new OmsorgDto();
        omsorgDto.setOmsorgsovertakelsesdato(omsorgsovertakelsesdato);
        omsorgDto.setFødselsdato(List.of(fødselsdato));
        omsorgDto.setAntallBarn(antallBarn);
        omsorgDto.setAnkomstdato(ankomstdato);
        manuellRegistreringEngangsstonadDto.setOmsorg(omsorgDto);

        return manuellRegistreringEngangsstonadDto;
    }

    static ManuellRegistreringEngangsstonadDto opprettOmsorgDto(FamilieHendelseType tema,
                                                                LocalDate omsorgsovertakelsesdato,
                                                                RettigheterDto rettighet,
                                                                int antallBarn,
                                                                LocalDate fødselsdato) {
        var manuellRegistreringEngangsstonadDto = new ManuellRegistreringEngangsstonadDto();
        manuellRegistreringEngangsstonadDto.setSøker(ForeldreType.MOR);
        manuellRegistreringEngangsstonadDto.setRettigheter(rettighet);
        manuellRegistreringEngangsstonadDto.setTema(tema);

        var omsorgDto = new OmsorgDto();
        omsorgDto.setOmsorgsovertakelsesdato(omsorgsovertakelsesdato);
        omsorgDto.setAntallBarn(antallBarn);
        omsorgDto.setFødselsdato(List.of(fødselsdato));
        manuellRegistreringEngangsstonadDto.setOmsorg(omsorgDto);

        return manuellRegistreringEngangsstonadDto;
    }

    public static AnnenForelderDto opprettAnnenForelderDto(Boolean kanIkkeOppgiAnnenForelder,
                                                           Boolean harAleneomsorg,
                                                           Boolean harRettPåForeldrepenger) {
        var annenForelderDto = new AnnenForelderDto();
        if (kanIkkeOppgiAnnenForelder) {
            annenForelderDto.setKanIkkeOppgiAnnenForelder(kanIkkeOppgiAnnenForelder);
            var kanIkkeOppgiBegrunnelse = new AnnenForelderDto.KanIkkeOppgiBegrunnelse();
            annenForelderDto.setKanIkkeOppgiBegrunnelse(kanIkkeOppgiBegrunnelse);
            annenForelderDto.setDenAndreForelderenHarRettPåForeldrepenger(harRettPåForeldrepenger);
            annenForelderDto.setSøkerHarAleneomssorg(harAleneomsorg);
        }
        return annenForelderDto;
    }

    public static GraderingDto opprettGraderingDto(LocalDate fraDato,
                                                   LocalDate tilDato,
                                                   BigDecimal prosentandel,
                                                   UttakPeriodeType uttakPeriodeType,
                                                   boolean erArbeidstaker,
                                                   boolean erFrilanser,
                                                   boolean erSelvstNæringsdrivende,
                                                   String orgNr) {
        var dto = new GraderingDto();
        dto.setPeriodeFom(fraDato);
        dto.setPeriodeTom(tilDato);
        dto.setPeriodeForGradering(uttakPeriodeType);
        dto.setProsentandelArbeid(prosentandel);
        dto.setArbeidsgiverIdentifikator(orgNr);
        dto.setErArbeidstaker(erArbeidstaker);
        dto.setErFrilanser(erFrilanser);
        dto.setErSelvstNæringsdrivende(erSelvstNæringsdrivende);
        dto.setSkalGraderes(true);
        return dto;
    }

    static ArbeidsforholdDto opprettUtenlandskArbeidsforholdDto(String navn,
                                                                String landKode,
                                                                LocalDate periodeFom,
                                                                LocalDate periodeTom) {
        var dto = new ArbeidsforholdDto();
        dto.setArbeidsgiver(navn);
        dto.setLand(landKode);
        dto.setPeriodeFom(periodeFom);
        dto.setPeriodeTom(periodeTom);
        return dto;
    }

    static VirksomhetDto opprettNorskVirksomhetMedEndringUtenRegnskapsfører() {
        var naringsvirksomhetTypeDto = opprettNaringsvirksomhetTypeAnnenNæringsvirksomhet();
        return opprettVirksomhetDto("minarbeidsplass as", true, KUNSTIG_ORG, null, naringsvirksomhetTypeDto, true, true,
            LocalDate.now().minusMonths(3), "Ny Lavvo", false, null, null, false, null);
    }

    static VirksomhetDto opprettUtenlandskVirksomhetMedEndringUtenRegnskapsfører() {
        var naringsvirksomhetTypeDto = opprettNaringsvirksomhetTypeAnnenNæringsvirksomhet();
        return opprettVirksomhetDto("utenlandsk org as", false, KUNSTIG_ORG + "123", "ENG", naringsvirksomhetTypeDto,
            true, true, LocalDate.now().minusMonths(3), "Ny Lavvo", false, null, null, false, LocalDate.now());
    }

    static VirksomhetDto opprettVirksomhetDto(String virksomhetsNavn,
                                              boolean virksomhetRegistrertINorge,
                                              String orgNr,
                                              String landJobberFra,
                                              NaringsvirksomhetTypeDto virksomhetstype,
                                              boolean nyoppstartet,
                                              boolean varigEndring,
                                              LocalDate endretDato,
                                              String beskrivelseAvEndring,
                                              boolean harRegnskapsfører,
                                              String navnRegnskapsfører,
                                              String tlfRegnskapsfører,
                                              boolean tilknyttetNaringen,
                                              LocalDate utenlandskVirksomhetStartDato) {
        var virksomhetDto = new VirksomhetDto();
        virksomhetDto.setBeskrivelseAvEndring(beskrivelseAvEndring);
        virksomhetDto.setVarigEndringGjeldendeFom(endretDato);
        virksomhetDto.setFamilieEllerVennerTilknyttetNaringen(tilknyttetNaringen);
        virksomhetDto.setHarRegnskapsforer(harRegnskapsfører);
        virksomhetDto.setNavnRegnskapsforer(navnRegnskapsfører);
        virksomhetDto.setTlfRegnskapsforer(tlfRegnskapsfører);
        virksomhetDto.setLandJobberFra(landJobberFra);
        virksomhetDto.setNavn(virksomhetsNavn);
        virksomhetDto.setOrganisasjonsnummer(orgNr);
        virksomhetDto.setErNyoppstartet(nyoppstartet);
        virksomhetDto.setTypeVirksomhet(virksomhetstype);
        virksomhetDto.setHarVarigEndring(varigEndring);
        virksomhetDto.setVirksomhetRegistrertINorge(virksomhetRegistrertINorge);
        virksomhetDto.setFom(utenlandskVirksomhetStartDato);

        return virksomhetDto;
    }

    static NaringsvirksomhetTypeDto opprettNaringsvirksomhetTypeAnnenNæringsvirksomhet() {
        var naringsvirksomhetTypeDto = new NaringsvirksomhetTypeDto();
        naringsvirksomhetTypeDto.setAnnen(true);
        return naringsvirksomhetTypeDto;

    }

    static List<AndreYtelserDto> opprettTestdataForAndreYtelser() {
        List<AndreYtelserDto> result = new ArrayList<>();
        result.add(
            opprettAndreYtelserDto(ArbeidType.ETTERLØNN_SLUTTPAKKE, LocalDate.now().minusWeeks(2), LocalDate.now()));
        result.add(opprettAndreYtelserDto(ArbeidType.LØNN_UNDER_UTDANNING, LocalDate.now().minusWeeks(4),
            LocalDate.now().minusWeeks(2)));
        result.add(opprettAndreYtelserDto(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, LocalDate.now().minusWeeks(6),
            LocalDate.now().minusWeeks(4)));
        return result;

    }

    static AndreYtelserDto opprettAndreYtelserDto(ArbeidType ytelseType, LocalDate periodeFom, LocalDate periodeTom) {
        var result = new AndreYtelserDto();
        result.setYtelseType(ytelseType);
        result.setPeriodeFom(periodeFom);
        result.setPeriodeTom(periodeTom);

        return result;
    }

    public static TidsromPermisjonDto opprettTidsromPermisjonDto(List<PermisjonPeriodeDto> permisjonsPerioder) {
        var dto = new TidsromPermisjonDto();
        dto.setPermisjonsPerioder(permisjonsPerioder);
        return dto;
    }
}
