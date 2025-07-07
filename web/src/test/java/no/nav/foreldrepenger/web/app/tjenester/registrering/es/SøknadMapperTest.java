package no.nav.foreldrepenger.web.app.tjenester.registrering.es;

import static java.util.Collections.singletonList;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.oppdaterDtoForFødsel;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.opprettBruker;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapper;
import no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperFelles;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.UtenlandsoppholdDto;

@ExtendWith(MockitoExtension.class)
class SøknadMapperTest {

    @Mock
    private PersoninfoAdapter personinfoAdapter;

    private SøknadMapper ytelseSøknadMapper;

    @BeforeEach
    void setUp() {
        personinfoAdapter = mock(PersoninfoAdapter.class);
        ytelseSøknadMapper = new YtelseSøknadMapper(personinfoAdapter);
    }

    @Test
    void test_mapEngangstønad() {
        var registreringEngangsstonadDto = new ManuellRegistreringEngangsstonadDto();
        oppdaterDtoForFødsel(registreringEngangsstonadDto, true, LocalDate.now().minusWeeks(3), 1);
        assertThatCode(() -> ytelseSøknadMapper.mapSøknad(registreringEngangsstonadDto, opprettBruker())).doesNotThrowAnyException();
    }

    @Test
    void testMapperMedlemskapES_uten_utenlandsopphold() {

        var registreringEngangsstonadDto = new ManuellRegistreringEngangsstonadDto();
        registreringEngangsstonadDto.setMottattDato(LocalDate.now());
        registreringEngangsstonadDto.setHarFremtidigeOppholdUtenlands(false);
        registreringEngangsstonadDto.setHarTidligereOppholdUtenlands(false);
        registreringEngangsstonadDto.setOppholdINorge(true);

        var medlemskap = SøknadMapperFelles.mapMedlemskap(registreringEngangsstonadDto);

        assertThat(medlemskap.isINorgeVedFoedselstidspunkt()).isTrue();
        assertThat(medlemskap.isBoddINorgeSiste12Mnd()).isTrue();
        assertThat(medlemskap.isBorINorgeNeste12Mnd()).isTrue();

        assertThat(medlemskap.getOppholdUtlandet()).isEmpty();
        assertThat(medlemskap.getOppholdNorge()).as("Forventer at vi skal ha opphold norge når vi ikke har utenlandsopphold.").isEmpty();
    }

    @Test
    void testMapperMedlemskapES_med_FremtidigUtenlandsopphold() {

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
        assertThat(medlemskap.isINorgeVedFoedselstidspunkt()).isTrue();
        assertThat(medlemskap.isBoddINorgeSiste12Mnd()).isTrue();
        assertThat(medlemskap.isBorINorgeNeste12Mnd()).isFalse();

        // Assert tidligere opphold i norge(siden vi ikke har tidligere
        // utenlandsopphold.)
        var oppholdNorgeListe = medlemskap.getOppholdNorge();
        assertThat(oppholdNorgeListe).isNotNull().isEmpty();

        var alleOppholdUtlandet = medlemskap.getOppholdUtlandet();
        assertThat(alleOppholdUtlandet).isNotNull().hasSize(1);

        var oppholdUtlandet = alleOppholdUtlandet.get(0);
        assertThat(oppholdUtlandet.getLand()).isNotNull();
        assertThat(oppholdUtlandet.getLand().getKode()).isEqualTo(land);
        assertThat(oppholdUtlandet.getPeriode()).isNotNull();
        assertThat(oppholdUtlandet.getPeriode().getFom()).isEqualTo(periodeFom);
        assertThat(oppholdUtlandet.getPeriode().getTom()).isEqualTo(periodeTom);
    }

    @Test
    void testMapperMedlemskapES_med_TidligereUtenlandsopphold() {

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
        assertThat(medlemskap.isINorgeVedFoedselstidspunkt()).isTrue();
        assertThat(medlemskap.isBoddINorgeSiste12Mnd()).isFalse();
        assertThat(medlemskap.isBorINorgeNeste12Mnd()).isTrue();

        // Assert fremtidg opphold i norge(siden vi ikke har fremtidig utenlandsopphold.
        var oppholdNorgeListe = medlemskap.getOppholdNorge();
        assertThat(oppholdNorgeListe).isNotNull().isEmpty();

        var oppholdUtenlandsListe = medlemskap.getOppholdUtlandet();
        assertThat(oppholdUtenlandsListe).isNotNull().hasSize(1);
        var utenlandsopphold = oppholdUtenlandsListe.get(0);
        assertThat(utenlandsopphold.getLand()).isNotNull();
        assertThat(utenlandsopphold.getLand().getKode()).isEqualTo(land);
        assertThat(utenlandsopphold.getPeriode()).isNotNull();
        assertThat(utenlandsopphold.getPeriode().getFom()).isEqualTo(periodeFom);
        assertThat(utenlandsopphold.getPeriode().getTom()).isEqualTo(periodeTom);
    }
}
