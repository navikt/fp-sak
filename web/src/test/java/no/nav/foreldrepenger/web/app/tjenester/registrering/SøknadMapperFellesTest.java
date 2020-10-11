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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
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
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.VirksomhetDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.es.ManuellRegistreringEngangsstonadDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.fp.ManuellRegistreringForeldrepengerDto;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Adopsjon;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.AnnenForelder;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Bruker;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Foedsel;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Omsorgsovertakelse;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.OppholdNorge;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.OppholdUtlandet;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.SoekersRelasjonTilBarnet;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Termin;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.UkjentForelder;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.AnnenOpptjening;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.EgenNaering;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.NorskOrganisasjon;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.UtenlandskArbeidsforhold;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.UtenlandskOrganisasjon;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
public class SøknadMapperFellesTest {

    @Mock
    private PersoninfoAdapter personinfoAdapter;
    @Mock
    private VirksomhetTjeneste virksomhetTjeneste;

    @BeforeEach
    public void setUp() {
        Virksomhet virksomhetEntitet = new Virksomhet.Builder().medOrgnr("123").medRegistrert(LocalDate.now()).build();
        lenient().when(virksomhetTjeneste.hentOrganisasjon(anyString())).thenReturn(virksomhetEntitet);
    }

    @Test
    public void mapperBrukerBasertPåAktørIdOgBrukerrolle() {
        NavBruker navBruker = opprettBruker();

        ForeldreType søker = ForeldreType.MOR;

        Bruker bruker = SøknadMapperFelles.mapBruker(søker, navBruker);
        assertThat(bruker).isInstanceOf(Bruker.class);
        assertThat(bruker.getSoeknadsrolle().getKode()).isEqualTo(søker.getKode());
        assertThat(bruker.getAktoerId()).isEqualTo(navBruker.getAktørId().getId());
    }

    @Test
    public void test_mapRelasjonTilBarnet_adopsjon() {
        ManuellRegistreringEngangsstonadDto manuellRegistreringEngangsstonadDto = opprettAdosjonDto(FamilieHendelseType.ADOPSJON, LocalDate.now(),
                LocalDate.now().minusMonths(3), 1, LocalDate.now());
        manuellRegistreringEngangsstonadDto.setTema(FamilieHendelseType.ADOPSJON);
        SoekersRelasjonTilBarnet søkersRelasjonTilBarnet = SøknadMapperFelles.mapRelasjonTilBarnet(manuellRegistreringEngangsstonadDto);
        assertThat(søkersRelasjonTilBarnet).isInstanceOf(Adopsjon.class);
    }

    @Test
    public void test_mapRelasjonTilBarnet_fødsel_med_rettighet_knyttet_til_omsorgsovertakelse_satt() {
        ManuellRegistreringEngangsstonadDto manuellRegistreringEngangsstonadDto = opprettAdosjonDto(FamilieHendelseType.FØDSEL, LocalDate.now(),
                LocalDate.now().minusMonths(3), 1, LocalDate.now());
        manuellRegistreringEngangsstonadDto.setTema(FamilieHendelseType.FØDSEL);
        manuellRegistreringEngangsstonadDto.setSoker(ForeldreType.FAR);
        manuellRegistreringEngangsstonadDto.setRettigheter(RettigheterDto.OVERTA_FORELDREANSVARET_ALENE);
        SoekersRelasjonTilBarnet søkersRelasjonTilBarnet = SøknadMapperFelles.mapRelasjonTilBarnet(manuellRegistreringEngangsstonadDto);
        assertThat(søkersRelasjonTilBarnet).isInstanceOf(Omsorgsovertakelse.class);
    }

    @Test
    public void test_mapAdopsjon() throws Exception {
        final LocalDate omsorgsovertakelsesdato = LocalDate.now();
        final LocalDate fødselssdato = LocalDate.now().minusMonths(3);
        final LocalDate ankomstDato = LocalDate.now().minusDays(4);
        LocalDate.now().minusMonths(2);
        final int antallBarn = 1;

        ManuellRegistreringEngangsstonadDto manuellRegistreringEngangsstonadDto = opprettAdosjonDto(FamilieHendelseType.ADOPSJON,
                omsorgsovertakelsesdato, fødselssdato, antallBarn, ankomstDato);
        Adopsjon adopsjon = SøknadMapperFelles.mapAdopsjon(manuellRegistreringEngangsstonadDto);
        assertThat(adopsjon).isNotNull();
        assertThat(adopsjon.getOmsorgsovertakelsesdato()).isEqualTo(omsorgsovertakelsesdato);
        assertThat(adopsjon.getAntallBarn()).isEqualTo(antallBarn);
        assertThat(adopsjon.getFoedselsdato()).first().isEqualTo(fødselssdato);
        assertThat(adopsjon.getAnkomstdato()).isEqualTo(ankomstDato);
    }

    @Test
    public void test_mapRelasjonTilBarnet_omsorg() {
        ManuellRegistreringEngangsstonadDto manuellRegistreringEngangsstonadDto = opprettOmsorgDto(FamilieHendelseType.OMSORG, LocalDate.now(),
                RettigheterDto.OVERTA_FORELDREANSVARET_ALENE, 1, LocalDate.now());
        SoekersRelasjonTilBarnet søkersRelasjonTilBarnet = SøknadMapperFelles.mapRelasjonTilBarnet(manuellRegistreringEngangsstonadDto);
        assertThat(søkersRelasjonTilBarnet).isInstanceOf(Omsorgsovertakelse.class);
    }

    @Test
    public void test_mapOmsorg() throws Exception {
        final LocalDate omsorgsovertakelsesdato = LocalDate.now();
        final LocalDate fødselsdato = LocalDate.now().minusDays(10);
        final int antallBarn = 1;

        ManuellRegistreringEngangsstonadDto manuellRegistreringEngangsstonadDto = opprettOmsorgDto(FamilieHendelseType.OMSORG,
                omsorgsovertakelsesdato, RettigheterDto.OVERTA_FORELDREANSVARET_ALENE, antallBarn, fødselsdato);
        Omsorgsovertakelse omsorgsovertakelse = SøknadMapperFelles.mapOmsorgsovertakelse(manuellRegistreringEngangsstonadDto);
        assertThat(omsorgsovertakelse).isNotNull();
        assertThat(omsorgsovertakelse.getOmsorgsovertakelsesdato()).isEqualTo(omsorgsovertakelsesdato);
        assertThat(omsorgsovertakelse.getAntallBarn()).isEqualTo(antallBarn);
        assertThat(omsorgsovertakelse.getOmsorgsovertakelseaarsak().getKode()).isEqualTo(FarSøkerType.OVERTATT_OMSORG.getKode());
        assertThat(omsorgsovertakelse.getFoedselsdato()).hasSize(1);
        assertThat(omsorgsovertakelse.getFoedselsdato()).first().isEqualTo(fødselsdato);
    }

    @Test
    public void test_mapRelasjonTilBarnet_fødsel() {
        ManuellRegistreringEngangsstonadDto manuellRegistreringEngangsstonadDto = new ManuellRegistreringEngangsstonadDto();
        oppdaterDtoForFødsel(manuellRegistreringEngangsstonadDto, true, LocalDate.now(), 1);
        SoekersRelasjonTilBarnet søkersRelasjonTilBarnet = SøknadMapperFelles.mapRelasjonTilBarnet(manuellRegistreringEngangsstonadDto);
        assertThat(søkersRelasjonTilBarnet).isInstanceOf(Foedsel.class);
    }

    @Test
    public void test_mapFødsel() throws Exception {
        final LocalDate fødselssdato = LocalDate.now().minusMonths(3);
        final int antallBarn = 1;

        ManuellRegistreringEngangsstonadDto manuellRegistreringEngangsstonadDto = new ManuellRegistreringEngangsstonadDto();
        oppdaterDtoForFødsel(manuellRegistreringEngangsstonadDto, true, fødselssdato, antallBarn);
        Foedsel foedsel = SøknadMapperFelles.mapFødsel(manuellRegistreringEngangsstonadDto);
        assertThat(foedsel).isNotNull();
        assertThat(foedsel.getFoedselsdato()).isEqualTo(fødselssdato);
        assertThat(foedsel.getAntallBarn()).isEqualTo(antallBarn);
    }

    @Test
    public void test_mapRelasjonTilBarnet_termin() {
        ManuellRegistreringEngangsstonadDto manuellRegistreringEngangsstonadDto = new ManuellRegistreringEngangsstonadDto();
        oppdaterDtoForFødsel(manuellRegistreringEngangsstonadDto, false, LocalDate.now(), 1);
        SoekersRelasjonTilBarnet søkersRelasjonTilBarnet = SøknadMapperFelles.mapRelasjonTilBarnet(manuellRegistreringEngangsstonadDto);
        assertThat(søkersRelasjonTilBarnet).isInstanceOf(Termin.class);
    }

    @Test
    public void test_mapTermin() throws Exception {
        final LocalDate terminbekreftelseDato = LocalDate.now();
        final LocalDate termindato = LocalDate.now().plusMonths(3);
        final int antallBarn = 1;

        ManuellRegistreringEngangsstonadDto manuellRegistreringEngangsstonadDto = new ManuellRegistreringEngangsstonadDto();
        manuellRegistreringEngangsstonadDto.setTerminbekreftelseDato(terminbekreftelseDato);
        manuellRegistreringEngangsstonadDto.setTermindato(termindato);
        manuellRegistreringEngangsstonadDto.setAntallBarnFraTerminbekreftelse(antallBarn);
        Termin termin = SøknadMapperFelles.mapTermin(manuellRegistreringEngangsstonadDto);
        assertThat(termin).isNotNull();
        assertThat(termin.getTermindato()).isEqualTo(termindato);
        assertThat(termin.getUtstedtdato()).isEqualTo(terminbekreftelseDato);
        assertThat(termin.getAntallBarn()).isEqualTo(antallBarn);
    }

    @Test
    public void test_mapAnnenForelder() {
        final LocalDate omsorgsovertakelsesdato = LocalDate.now();
        final int antallBarn = 1;

        ManuellRegistreringEngangsstonadDto manuellRegistreringEngangsstonadDto = opprettOmsorgDto(FamilieHendelseType.OMSORG,
                omsorgsovertakelsesdato, RettigheterDto.OVERTA_FORELDREANSVARET_ALENE, antallBarn, LocalDate.now());

        manuellRegistreringEngangsstonadDto.setAnnenForelder(opprettAnnenForelderDto(true, true, true));
        AnnenForelder annenForelder = SøknadMapperFelles.mapAnnenForelder(manuellRegistreringEngangsstonadDto, personinfoAdapter);
        assertThat(annenForelder).isInstanceOf(UkjentForelder.class);
    }

    @Test
    public void testMapperMedlemskapFP_bareOppholdNorge() {

        ManuellRegistreringForeldrepengerDto registreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        registreringForeldrepengerDto.setMottattDato(LocalDate.now());
        registreringForeldrepengerDto.setHarFremtidigeOppholdUtenlands(false);
        registreringForeldrepengerDto.setHarTidligereOppholdUtenlands(false);
        registreringForeldrepengerDto.setOppholdINorge(true);

        no.nav.vedtak.felles.xml.soeknad.felles.v3.Medlemskap medlemskap = SøknadMapperFelles.mapMedlemskap(registreringForeldrepengerDto);
        assertThat(medlemskap.getOppholdNorge()).as("Forventer at vi skal ha opphold norge når vi ikke har utenlandsopphold.").hasSize(2);
    }

    @Test
    public void testMapperMedlemskapFP_utenOppholdNorge() {

        ManuellRegistreringForeldrepengerDto registreringForeldrepengerDto = new ManuellRegistreringForeldrepengerDto();
        registreringForeldrepengerDto.setHarFremtidigeOppholdUtenlands(true);
        registreringForeldrepengerDto.setHarTidligereOppholdUtenlands(true);
        registreringForeldrepengerDto.setOppholdINorge(true);

        no.nav.vedtak.felles.xml.soeknad.felles.v3.Medlemskap medlemskap = SøknadMapperFelles.mapMedlemskap(registreringForeldrepengerDto);
        assertThat(medlemskap.getOppholdNorge()).as("Forventer at vi ikke har opphold norge når vi har utenlandsopphold.").isEmpty();
    }

    @Test
    public void testMapperMedlemskapFP_med_FremtidigUtenlandsopphold() {

        String land = "FRA";
        LocalDate periodeFom = LocalDate.now().plusMonths(2), periodeTom = LocalDate.now().plusMonths(5);

        ManuellRegistreringEngangsstonadDto registreringEngangsstonadDto = new ManuellRegistreringEngangsstonadDto();
        registreringEngangsstonadDto.setMottattDato(LocalDate.now());
        registreringEngangsstonadDto.setHarFremtidigeOppholdUtenlands(true);
        registreringEngangsstonadDto.setHarTidligereOppholdUtenlands(false);
        registreringEngangsstonadDto.setOppholdINorge(true);
        UtenlandsoppholdDto utenlandsoppholdDto = new UtenlandsoppholdDto();
        utenlandsoppholdDto.setPeriodeFom(periodeFom);
        utenlandsoppholdDto.setPeriodeTom(periodeTom);
        utenlandsoppholdDto.setLand(land);
        registreringEngangsstonadDto.setFremtidigeOppholdUtenlands(singletonList(utenlandsoppholdDto));

        no.nav.vedtak.felles.xml.soeknad.felles.v3.Medlemskap medlemskap = SøknadMapperFelles.mapMedlemskap(registreringEngangsstonadDto);

        // Assert tidligere opphold i norge(siden vi ikke har tidligere
        // utenlandsopphold.)
        List<OppholdNorge> oppholdNorgeListe = medlemskap.getOppholdNorge();
        assertThat(oppholdNorgeListe).isNotNull();
        assertThat(oppholdNorgeListe).hasSize(1);

        List<OppholdUtlandet> alleOppholdUtlandet = medlemskap.getOppholdUtlandet();
        assertThat(alleOppholdUtlandet).isNotNull();
        assertThat(alleOppholdUtlandet).hasSize(1);

        OppholdUtlandet oppholdUtlandet = alleOppholdUtlandet.get(0);
        assertThat(oppholdUtlandet.getLand()).isNotNull();
        assertThat(oppholdUtlandet.getLand().getKode()).isEqualTo(land);
        assertThat(oppholdUtlandet.getPeriode()).isNotNull();
        assertThat(oppholdUtlandet.getPeriode().getFom()).isEqualTo(periodeFom);
        assertThat(oppholdUtlandet.getPeriode().getTom()).isEqualTo(periodeTom);
    }

    @Test
    public void testMapperMedlemskapFP_med_TidligereUtenlandsopphold() {

        final String land = "FRA";
        LocalDate periodeFom = LocalDate.now().minusMonths(6), periodeTom = LocalDate.now().minusMonths(3);

        ManuellRegistreringEngangsstonadDto registreringEngangsstonadDto = new ManuellRegistreringEngangsstonadDto();
        registreringEngangsstonadDto.setMottattDato(LocalDate.now());
        registreringEngangsstonadDto.setHarFremtidigeOppholdUtenlands(false); // Ikke fremtidige utenlandsopphold, så da får vi fremtidg opphold i
                                                                              // norge
        registreringEngangsstonadDto.setHarTidligereOppholdUtenlands(true);
        registreringEngangsstonadDto.setOppholdINorge(true);
        UtenlandsoppholdDto utenlandsoppholdDto = new UtenlandsoppholdDto();
        utenlandsoppholdDto.setPeriodeFom(periodeFom);
        utenlandsoppholdDto.setPeriodeTom(periodeTom);
        utenlandsoppholdDto.setLand(land);
        registreringEngangsstonadDto.setTidligereOppholdUtenlands(singletonList(utenlandsoppholdDto));

        no.nav.vedtak.felles.xml.soeknad.felles.v3.Medlemskap medlemskap = SøknadMapperFelles.mapMedlemskap(registreringEngangsstonadDto);

        // Assert fremtidg opphold i norge(siden vi ikke har fremtidig utenlandsopphold.
        List<OppholdNorge> oppholdNorgeListe = medlemskap.getOppholdNorge();
        assertThat(oppholdNorgeListe).isNotNull();
        assertThat(oppholdNorgeListe).hasSize(1);

        List<OppholdUtlandet> oppholdUtenlandsListe = medlemskap.getOppholdUtlandet();
        assertThat(oppholdUtenlandsListe).isNotNull();
        assertThat(oppholdUtenlandsListe).hasSize(1);
        OppholdUtlandet utenlandsopphold = oppholdUtenlandsListe.get(0);
        assertThat(utenlandsopphold.getLand()).isNotNull();
        assertThat(utenlandsopphold.getLand().getKode()).isEqualTo(land);
        assertThat(utenlandsopphold.getPeriode()).isNotNull();
        assertThat(utenlandsopphold.getPeriode().getFom()).isEqualTo(periodeFom);
        assertThat(utenlandsopphold.getPeriode().getTom()).isEqualTo(periodeTom);
    }

    @Test
    public void test_mapOpptjening_andreYtelser() {
        List<AnnenOpptjening> annenOpptjenings = SøknadMapperFelles.mapAndreYtelser(opprettTestdataForAndreYtelser());
        assertThat(annenOpptjenings).as("Forventer en opptjening pr angitt ytelse.").hasSize(3);
    }

    @Test
    public void test_mapOpptjening_andreYtelser_null() {
        List<AnnenOpptjening> annenOpptjenings = SøknadMapperFelles.mapAndreYtelser(null);
        assertThat(annenOpptjenings).as("Forventer en opptjening pr angitt ytelse.").isEmpty();
    }

    @Test
    public void test_mapEgenNæring() {
        VirksomhetDto virksomhetDto = opprettNorskVirksomhetMedEndringUtenRegnskapsfører();
        EgenNaering egenNaering = SøknadMapperFelles.mapEgenNæring(virksomhetDto, virksomhetTjeneste);
        assertThat(egenNaering).isNotNull();
        assertThat(egenNaering).isInstanceOf(NorskOrganisasjon.class);
        assertThat(egenNaering.getVirksomhetstype().get(0).getKode()).isEqualTo(VirksomhetType.ANNEN.getKode());
    }

    @Test
    public void test_mapUtenlandskOrgEgenNæring() {
        VirksomhetDto virksomhetDto = opprettUtenlandskVirksomhetMedEndringUtenRegnskapsfører();
        EgenNaering egenNaering = SøknadMapperFelles.mapEgenNæring(virksomhetDto, virksomhetTjeneste);
        assertThat(egenNaering).isNotNull();
        assertThat(egenNaering).isInstanceOf(UtenlandskOrganisasjon.class);
        assertThat(egenNaering.getVirksomhetstype().get(0).getKode()).isEqualTo(VirksomhetType.ANNEN.getKode());
        assertThat(((UtenlandskOrganisasjon) egenNaering).getRegistrertILand()).isNotNull();
        assertThat((egenNaering).getPeriode().getFom()).isNotNull();
        assertThat((egenNaering).getPeriode().getFom()).isEqualTo(LocalDate.now());
        assertThat((egenNaering).getPeriode().getTom()).isNotNull();
        assertThat((egenNaering).getPeriode().getTom()).isEqualTo(Tid.TIDENES_ENDE);
    }

    @Test
    public void test_mapUtenlandskArbeidsforhold() {

        LocalDate periodeFom = LocalDate.now();
        LocalDate periodeTom = periodeFom.plusWeeks(10);
        ArbeidsforholdDto arbeidsforholdDto = opprettUtenlandskArbeidsforholdDto("arbg. navn", "FIN", periodeFom, periodeTom);
        List<UtenlandskArbeidsforhold> arbeidsforhold = SøknadMapperFelles
                .mapAlleUtenlandskeArbeidsforhold(Collections.singletonList(arbeidsforholdDto));
        assertThat(arbeidsforhold).isNotNull();
        assertThat(arbeidsforhold).anySatisfy(arbForhold -> assertThat(arbForhold).isInstanceOf(UtenlandskArbeidsforhold.class));
        assertThat(arbeidsforhold).anySatisfy(arbForhold -> assertThat(arbForhold.getArbeidsgiversnavn()).isEqualTo("arbg. navn"));
        assertThat(arbeidsforhold).anySatisfy(arbForhold -> assertThat(arbForhold.getArbeidsland().getKode()).isEqualTo("FIN"));
    }

    @Test
    public void test_mapUtenlandskArbeidsforhold_null_liste() {
        List<UtenlandskArbeidsforhold> arbeidsforhold = SøknadMapperFelles.mapAlleUtenlandskeArbeidsforhold(null);
        assertThat(arbeidsforhold).isEmpty();
    }

    @Test
    public void test_mapUtenlandskArbeidsforhold_null_element_i_liste() {
        ArbeidsforholdDto arbeidsforholdDto = new ArbeidsforholdDto();
        List<UtenlandskArbeidsforhold> arbeidsforhold = SøknadMapperFelles
                .mapAlleUtenlandskeArbeidsforhold(Collections.singletonList(arbeidsforholdDto));
        assertThat(arbeidsforhold).isEmpty();
    }
}
