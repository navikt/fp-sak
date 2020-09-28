package no.nav.foreldrepenger.web.app.tjenester.registrering.es;

import static java.util.Collections.singletonList;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.oppdaterDtoForFødsel;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.opprettBruker;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapper;
import no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperFelles;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.UtenlandsoppholdDto;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Medlemskap;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.OppholdNorge;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.OppholdUtlandet;

@ExtendWith(MockitoExtension.class)
public class SøknadMapperTest {

    public static final AktørId STD_KVINNE_AKTØR_ID = AktørId.dummy();

    @Mock
    private TpsTjeneste tpsTjeneste;

    private SøknadMapper ytelseSøknadMapper;

    @BeforeEach
    public void setUp() {
        tpsTjeneste = mock(TpsTjeneste.class);
        ytelseSøknadMapper = new YtelseSøknadMapper(tpsTjeneste);
    }

    @Test
    public void test_mapEngangstønad() {
        ManuellRegistreringEngangsstonadDto registreringEngangsstonadDto = new ManuellRegistreringEngangsstonadDto();
        oppdaterDtoForFødsel(registreringEngangsstonadDto, true, LocalDate.now().minusWeeks(3), 1);
        ytelseSøknadMapper.mapSøknad(registreringEngangsstonadDto, opprettBruker());
    }

    @Test
    public void testMapperMedlemskapES_uten_utenlandsopphold() {

        ManuellRegistreringEngangsstonadDto registreringEngangsstonadDto = new ManuellRegistreringEngangsstonadDto();
        registreringEngangsstonadDto.setMottattDato(LocalDate.now());
        registreringEngangsstonadDto.setHarFremtidigeOppholdUtenlands(false);
        registreringEngangsstonadDto.setHarTidligereOppholdUtenlands(false);
        registreringEngangsstonadDto.setOppholdINorge(true);

        Medlemskap medlemskap = SøknadMapperFelles.mapMedlemskap(registreringEngangsstonadDto);
        assertThat(medlemskap.isINorgeVedFoedselstidspunkt()).isTrue();
        assertThat(medlemskap.getOppholdUtlandet()).isEmpty();
        assertThat(medlemskap.getOppholdNorge()).as("Forventer at vi skal ha opphold norge når vi ikke har utenlandsopphold.").hasSize(2);
    }

    @Test
    public void testMapperMedlemskapES_med_FremtidigUtenlandsopphold() throws Exception {

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

        Medlemskap medlemskap = SøknadMapperFelles.mapMedlemskap(registreringEngangsstonadDto);
        assertThat(medlemskap.isINorgeVedFoedselstidspunkt()).isTrue();

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
    public void testMapperMedlemskapES_med_TidligereUtenlandsopphold() throws Exception {

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

        Medlemskap medlemskap = SøknadMapperFelles.mapMedlemskap(registreringEngangsstonadDto);
        assertThat(medlemskap.isINorgeVedFoedselstidspunkt()).isTrue();

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
}
