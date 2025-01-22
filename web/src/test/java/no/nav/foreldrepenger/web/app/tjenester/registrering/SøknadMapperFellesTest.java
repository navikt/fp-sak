package no.nav.foreldrepenger.web.app.tjenester.registrering;

import static java.util.Collections.singletonList;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.oppdaterDtoForFødsel;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.opprettAdosjonDto;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.opprettAnnenForelderDto;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.opprettBruker;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.opprettNorskVirksomhetMedEndringUtenRegnskapsfører;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.opprettOmsorgDto;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.opprettTestdataForAndreYtelser;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.opprettUtenlandskArbeidsforholdDto;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.opprettUtenlandskVirksomhetMedEndringUtenRegnskapsfører;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.ForeldreType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.ArbeidsforholdDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.RettigheterDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.UtenlandsoppholdDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.es.ManuellRegistreringEngangsstonadDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.fp.ManuellRegistreringForeldrepengerDto;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Adopsjon;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Bruker;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Foedsel;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Omsorgsovertakelse;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Termin;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.UkjentForelder;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.NorskOrganisasjon;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.UtenlandskArbeidsforhold;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.UtenlandskOrganisasjon;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
class SøknadMapperFellesTest {

    @Mock
    private PersoninfoAdapter personinfoAdapter;
    @Mock
    private VirksomhetTjeneste virksomhetTjeneste;

    @BeforeEach
    public void setUp() {
        var virksomhetEntitet = new Virksomhet.Builder().medOrgnr("123").medRegistrert(LocalDate.now()).build();
        lenient().when(virksomhetTjeneste.hentOrganisasjon(anyString())).thenReturn(virksomhetEntitet);
    }

    @Test
    void mapperBrukerBasertPåAktørIdOgBrukerrolle() {
        var navBruker = opprettBruker();

        var søker = ForeldreType.MOR;

        var bruker = SøknadMapperFelles.mapBruker(søker, navBruker);
        assertThat(bruker).isInstanceOf(Bruker.class);
        assertThat(bruker.getSoeknadsrolle().getKode()).isEqualTo(søker.getKode());
        assertThat(bruker.getAktoerId()).isEqualTo(navBruker.getAktørId().getId());
    }

    @Test
    void test_mapRelasjonTilBarnet_adopsjon() {
        var manuellRegistreringEngangsstonadDto = opprettAdosjonDto(FamilieHendelseType.ADOPSJON, LocalDate.now(),
                LocalDate.now().minusMonths(3), 1, LocalDate.now());
        manuellRegistreringEngangsstonadDto.setTema(FamilieHendelseType.ADOPSJON);
        var søkersRelasjonTilBarnet = SøknadMapperFelles.mapRelasjonTilBarnet(manuellRegistreringEngangsstonadDto);
        assertThat(søkersRelasjonTilBarnet).isInstanceOf(Adopsjon.class);
    }

    @Test
    void test_mapRelasjonTilBarnet_fødsel_med_rettighet_knyttet_til_omsorgsovertakelse_satt() {
        var manuellRegistreringEngangsstonadDto = opprettAdosjonDto(FamilieHendelseType.FØDSEL, LocalDate.now(),
                LocalDate.now().minusMonths(3), 1, LocalDate.now());
        manuellRegistreringEngangsstonadDto.setTema(FamilieHendelseType.FØDSEL);
        manuellRegistreringEngangsstonadDto.setSoker(ForeldreType.FAR);
        manuellRegistreringEngangsstonadDto.setRettigheter(RettigheterDto.OVERTA_FORELDREANSVARET_ALENE);
        var søkersRelasjonTilBarnet = SøknadMapperFelles.mapRelasjonTilBarnet(manuellRegistreringEngangsstonadDto);
        assertThat(søkersRelasjonTilBarnet).isInstanceOf(Omsorgsovertakelse.class);
    }

    @Test
    void test_mapAdopsjon() {
        var omsorgsovertakelsesdato = LocalDate.now();
        var fødselssdato = LocalDate.now().minusMonths(3);
        var ankomstDato = LocalDate.now().minusDays(4);
        LocalDate.now().minusMonths(2);
        var antallBarn = 1;

        var manuellRegistreringEngangsstonadDto = opprettAdosjonDto(FamilieHendelseType.ADOPSJON, omsorgsovertakelsesdato, fødselssdato, antallBarn,
            ankomstDato);
        var adopsjon = SøknadMapperFelles.mapAdopsjon(manuellRegistreringEngangsstonadDto);
        assertThat(adopsjon).isNotNull();
        assertThat(adopsjon.getOmsorgsovertakelsesdato()).isEqualTo(omsorgsovertakelsesdato);
        assertThat(adopsjon.getAntallBarn()).isEqualTo(antallBarn);
        assertThat(adopsjon.getFoedselsdato()).first().isEqualTo(fødselssdato);
        assertThat(adopsjon.getAnkomstdato()).isEqualTo(ankomstDato);
    }

    @Test
    void test_mapRelasjonTilBarnet_omsorg() {
        var manuellRegistreringEngangsstonadDto = opprettOmsorgDto(FamilieHendelseType.OMSORG, LocalDate.now(),
                RettigheterDto.OVERTA_FORELDREANSVARET_ALENE, 1, LocalDate.now());
        var søkersRelasjonTilBarnet = SøknadMapperFelles.mapRelasjonTilBarnet(manuellRegistreringEngangsstonadDto);
        assertThat(søkersRelasjonTilBarnet).isInstanceOf(Omsorgsovertakelse.class);
    }

    @Test
    void test_mapOmsorg() {
        var omsorgsovertakelsesdato = LocalDate.now();
        var fødselsdato = LocalDate.now().minusDays(10);
        var antallBarn = 1;

        var manuellRegistreringEngangsstonadDto = opprettOmsorgDto(FamilieHendelseType.OMSORG, omsorgsovertakelsesdato,
            RettigheterDto.OVERTA_FORELDREANSVARET_ALENE, antallBarn, fødselsdato);
        var omsorgsovertakelse = SøknadMapperFelles.mapOmsorgsovertakelse(manuellRegistreringEngangsstonadDto);
        assertThat(omsorgsovertakelse).isNotNull();
        assertThat(omsorgsovertakelse.getOmsorgsovertakelsesdato()).isEqualTo(omsorgsovertakelsesdato);
        assertThat(omsorgsovertakelse.getAntallBarn()).isEqualTo(antallBarn);
        assertThat(omsorgsovertakelse.getOmsorgsovertakelseaarsak().getKode()).isEqualTo(FarSøkerType.OVERTATT_OMSORG.getKode());
        assertThat(omsorgsovertakelse.getFoedselsdato()).hasSize(1);
        assertThat(omsorgsovertakelse.getFoedselsdato()).first().isEqualTo(fødselsdato);
    }

    @Test
    void test_mapRelasjonTilBarnet_fødsel() {
        var manuellRegistreringEngangsstonadDto = new ManuellRegistreringEngangsstonadDto();
        oppdaterDtoForFødsel(manuellRegistreringEngangsstonadDto, true, LocalDate.now(), 1);
        var søkersRelasjonTilBarnet = SøknadMapperFelles.mapRelasjonTilBarnet(manuellRegistreringEngangsstonadDto);
        assertThat(søkersRelasjonTilBarnet).isInstanceOf(Foedsel.class);
    }

    @Test
    void test_mapFødsel() {
        var fødselssdato = LocalDate.now().minusMonths(3);
        var antallBarn = 1;

        var manuellRegistreringEngangsstonadDto = new ManuellRegistreringEngangsstonadDto();
        oppdaterDtoForFødsel(manuellRegistreringEngangsstonadDto, true, fødselssdato, antallBarn);
        var foedsel = SøknadMapperFelles.mapFødsel(manuellRegistreringEngangsstonadDto);
        assertThat(foedsel).isNotNull();
        assertThat(foedsel.getFoedselsdato()).isEqualTo(fødselssdato);
        assertThat(foedsel.getAntallBarn()).isEqualTo(antallBarn);
    }

    @Test
    void test_mapRelasjonTilBarnet_termin() {
        var manuellRegistreringEngangsstonadDto = new ManuellRegistreringEngangsstonadDto();
        oppdaterDtoForFødsel(manuellRegistreringEngangsstonadDto, false, LocalDate.now(), 1);
        var søkersRelasjonTilBarnet = SøknadMapperFelles.mapRelasjonTilBarnet(manuellRegistreringEngangsstonadDto);
        assertThat(søkersRelasjonTilBarnet).isInstanceOf(Termin.class);
    }

    @Test
    void test_mapTermin() {
        var terminbekreftelseDato = LocalDate.now();
        var termindato = LocalDate.now().plusMonths(3);
        var antallBarn = 1;

        var manuellRegistreringEngangsstonadDto = new ManuellRegistreringEngangsstonadDto();
        manuellRegistreringEngangsstonadDto.setTerminbekreftelseDato(terminbekreftelseDato);
        manuellRegistreringEngangsstonadDto.setTermindato(termindato);
        manuellRegistreringEngangsstonadDto.setAntallBarnFraTerminbekreftelse(antallBarn);
        var termin = SøknadMapperFelles.mapTermin(manuellRegistreringEngangsstonadDto);
        assertThat(termin).isNotNull();
        assertThat(termin.getTermindato()).isEqualTo(termindato);
        assertThat(termin.getUtstedtdato()).isEqualTo(terminbekreftelseDato);
        assertThat(termin.getAntallBarn()).isEqualTo(antallBarn);
    }

    @Test
    void test_mapAnnenForelder() {
        var omsorgsovertakelsesdato = LocalDate.now();
        var antallBarn = 1;

        var manuellRegistreringEngangsstonadDto = opprettOmsorgDto(FamilieHendelseType.OMSORG, omsorgsovertakelsesdato,
            RettigheterDto.OVERTA_FORELDREANSVARET_ALENE, antallBarn, LocalDate.now());

        manuellRegistreringEngangsstonadDto.setAnnenForelder(opprettAnnenForelderDto(true, true, true));
        var annenForelder = SøknadMapperFelles.mapAnnenForelder(manuellRegistreringEngangsstonadDto, personinfoAdapter);
        assertThat(annenForelder).isInstanceOf(UkjentForelder.class);
    }

    @Test
    void testMapperMedlemskapFP_bareOppholdNorge() {

        var registreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        registreringForeldrepengerDto.setMottattDato(LocalDate.now());
        registreringForeldrepengerDto.setHarFremtidigeOppholdUtenlands(false);
        registreringForeldrepengerDto.setHarTidligereOppholdUtenlands(false);
        registreringForeldrepengerDto.setOppholdINorge(true);

        var medlemskap = SøknadMapperFelles.mapMedlemskap(registreringForeldrepengerDto);

        assertThat(medlemskap.isINorgeVedFoedselstidspunkt()).isTrue();
        assertThat(medlemskap.isBoddINorgeSiste12Mnd()).isTrue();
        assertThat(medlemskap.isBorINorgeNeste12Mnd()).isTrue();
        assertThat(medlemskap.getOppholdNorge()).as("Forventer at vi ikke skal ha opphold norge når vi ikke har utenlandsopphold.").isEmpty();
    }

    @Test
    void testMapperMedlemskapFP_utenOppholdNorge() {

        var registreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        registreringForeldrepengerDto.setHarFremtidigeOppholdUtenlands(true);
        registreringForeldrepengerDto.setHarTidligereOppholdUtenlands(true);
        registreringForeldrepengerDto.setOppholdINorge(true);

        var medlemskap = SøknadMapperFelles.mapMedlemskap(registreringForeldrepengerDto);

        assertThat(medlemskap.isINorgeVedFoedselstidspunkt()).isTrue();
        assertThat(medlemskap.isBoddINorgeSiste12Mnd()).isFalse();
        assertThat(medlemskap.isBorINorgeNeste12Mnd()).isFalse();
        assertThat(medlemskap.getOppholdNorge()).as("Forventer at vi ikke har opphold norge når vi har utenlandsopphold.").isEmpty();
    }

    @Test
    void testMapperMedlemskapFP_med_FremtidigUtenlandsopphold() {

        var land = "FRA";
        LocalDate periodeFom = LocalDate.now().plusMonths(2), periodeTom = LocalDate.now().plusMonths(5);

        var registreringEngangsstonadDto = new ManuellRegistreringEngangsstonadDto();
        registreringEngangsstonadDto.setMottattDato(LocalDate.now());
        registreringEngangsstonadDto.setHarFremtidigeOppholdUtenlands(true);
        registreringEngangsstonadDto.setHarTidligereOppholdUtenlands(false);
        registreringEngangsstonadDto.setOppholdINorge(true);
        var utenlandsoppholdDto = new UtenlandsoppholdDto();
        utenlandsoppholdDto.setPeriodeFom(periodeFom);
        utenlandsoppholdDto.setPeriodeTom(periodeTom);
        utenlandsoppholdDto.setLand(land);
        registreringEngangsstonadDto.setFremtidigeOppholdUtenlands(singletonList(utenlandsoppholdDto));

        var medlemskap = SøknadMapperFelles.mapMedlemskap(registreringEngangsstonadDto);

        // Assert tidligere opphold i norge(siden vi ikke har tidligere
        // utenlandsopphold.)
        assertThat(medlemskap.isINorgeVedFoedselstidspunkt()).isTrue();
        assertThat(medlemskap.isBoddINorgeSiste12Mnd()).isTrue();
        assertThat(medlemskap.isBorINorgeNeste12Mnd()).isFalse();

        var oppholdNorgeListe = medlemskap.getOppholdNorge();
        assertThat(oppholdNorgeListe)
            .isNotNull()
            .isEmpty();

        var alleOppholdUtlandet = medlemskap.getOppholdUtlandet();
        assertThat(alleOppholdUtlandet)
            .isNotNull()
            .hasSize(1);

        var oppholdUtlandet = alleOppholdUtlandet.get(0);
        assertThat(oppholdUtlandet.getLand()).isNotNull();
        assertThat(oppholdUtlandet.getLand().getKode()).isEqualTo(land);
        assertThat(oppholdUtlandet.getPeriode()).isNotNull();
        assertThat(oppholdUtlandet.getPeriode().getFom()).isEqualTo(periodeFom);
        assertThat(oppholdUtlandet.getPeriode().getTom()).isEqualTo(periodeTom);
    }

    @Test
    void testMapperMedlemskapFP_med_TidligereUtenlandsopphold() {

        var land = "FRA";
        LocalDate periodeFom = LocalDate.now().minusMonths(6), periodeTom = LocalDate.now().minusMonths(3);

        var registreringEngangsstonadDto = new ManuellRegistreringEngangsstonadDto();
        registreringEngangsstonadDto.setMottattDato(LocalDate.now());
        registreringEngangsstonadDto.setHarFremtidigeOppholdUtenlands(false); // Ikke fremtidige utenlandsopphold, så da får vi fremtidg opphold i
                                                                              // norge
        registreringEngangsstonadDto.setHarTidligereOppholdUtenlands(true);
        registreringEngangsstonadDto.setOppholdINorge(true);
        var utenlandsoppholdDto = new UtenlandsoppholdDto();
        utenlandsoppholdDto.setPeriodeFom(periodeFom);
        utenlandsoppholdDto.setPeriodeTom(periodeTom);
        utenlandsoppholdDto.setLand(land);
        registreringEngangsstonadDto.setTidligereOppholdUtenlands(singletonList(utenlandsoppholdDto));

        var medlemskap = SøknadMapperFelles.mapMedlemskap(registreringEngangsstonadDto);

        // Assert fremtidg opphold i norge(siden vi ikke har fremtidig utenlandsopphold.
        assertThat(medlemskap.isINorgeVedFoedselstidspunkt()).isTrue();
        assertThat(medlemskap.isBoddINorgeSiste12Mnd()).isFalse();
        assertThat(medlemskap.isBorINorgeNeste12Mnd()).isTrue();

        var oppholdNorgeListe = medlemskap.getOppholdNorge();
        assertThat(oppholdNorgeListe)
            .isNotNull()
            .isEmpty();

        var oppholdUtenlandsListe = medlemskap.getOppholdUtlandet();
        assertThat(oppholdUtenlandsListe)
            .isNotNull()
            .hasSize(1);
        var utenlandsopphold = oppholdUtenlandsListe.get(0);
        assertThat(utenlandsopphold.getLand()).isNotNull();
        assertThat(utenlandsopphold.getLand().getKode()).isEqualTo(land);
        assertThat(utenlandsopphold.getPeriode()).isNotNull();
        assertThat(utenlandsopphold.getPeriode().getFom()).isEqualTo(periodeFom);
        assertThat(utenlandsopphold.getPeriode().getTom()).isEqualTo(periodeTom);
    }

    @Test
    void test_mapOpptjening_andreYtelser() {
        var annenOpptjenings = SøknadMapperFelles.mapAndreYtelser(opprettTestdataForAndreYtelser());
        assertThat(annenOpptjenings).as("Forventer en opptjening pr angitt ytelse.").hasSize(3);
    }

    @Test
    void test_mapOpptjening_andreYtelser_null() {
        var annenOpptjenings = SøknadMapperFelles.mapAndreYtelser(null);
        assertThat(annenOpptjenings).as("Forventer en opptjening pr angitt ytelse.").isEmpty();
    }

    @Test
    void test_mapEgenNæring() {
        var virksomhetDto = opprettNorskVirksomhetMedEndringUtenRegnskapsfører();
        var egenNaering = SøknadMapperFelles.mapEgenNæring(virksomhetDto, virksomhetTjeneste);
        assertThat(egenNaering)
            .isNotNull()
            .isInstanceOf(NorskOrganisasjon.class);
        assertThat(egenNaering.getVirksomhetstype().get(0).getKode()).isEqualTo(VirksomhetType.ANNEN.getKode());
    }

    @Test
    void test_mapUtenlandskOrgEgenNæring() {
        var virksomhetDto = opprettUtenlandskVirksomhetMedEndringUtenRegnskapsfører();
        var egenNaering = SøknadMapperFelles.mapEgenNæring(virksomhetDto, virksomhetTjeneste);
        assertThat(egenNaering)
            .isNotNull()
            .isInstanceOf(UtenlandskOrganisasjon.class);
        assertThat(egenNaering.getVirksomhetstype().get(0).getKode()).isEqualTo(VirksomhetType.ANNEN.getKode());
        assertThat(((UtenlandskOrganisasjon) egenNaering).getRegistrertILand()).isNotNull();
        assertThat(egenNaering.getPeriode().getFom())
            .isNotNull()
            .isEqualTo(LocalDate.now());
        assertThat(egenNaering.getPeriode().getTom())
            .isNotNull()
            .isEqualTo(Tid.TIDENES_ENDE);
    }

    @Test
    void test_mapUtenlandskArbeidsforhold() {

        var periodeFom = LocalDate.now();
        var periodeTom = periodeFom.plusWeeks(10);
        var arbeidsforholdDto = opprettUtenlandskArbeidsforholdDto("arbg. navn", "FIN", periodeFom, periodeTom);
        var arbeidsforhold = SøknadMapperFelles
                .mapAlleUtenlandskeArbeidsforhold(Collections.singletonList(arbeidsforholdDto));
        assertThat(arbeidsforhold)
            .isNotNull()
            .anySatisfy(arbForhold -> assertThat(arbForhold).isInstanceOf(UtenlandskArbeidsforhold.class))
            .anySatisfy(arbForhold -> assertThat(arbForhold.getArbeidsgiversnavn()).isEqualTo("arbg. navn"))
            .anySatisfy(arbForhold -> assertThat(arbForhold.getArbeidsland().getKode()).isEqualTo("FIN"));
    }

    @Test
    void test_mapUtenlandskArbeidsforhold_null_liste() {
        var arbeidsforhold = SøknadMapperFelles.mapAlleUtenlandskeArbeidsforhold(null);
        assertThat(arbeidsforhold).isEmpty();
    }

    @Test
    void test_mapUtenlandskArbeidsforhold_null_element_i_liste() {
        var arbeidsforholdDto = new ArbeidsforholdDto();
        var arbeidsforhold = SøknadMapperFelles
                .mapAlleUtenlandskeArbeidsforhold(Collections.singletonList(arbeidsforholdDto));
        assertThat(arbeidsforhold).isEmpty();
    }
}
