package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;

public class InntektskategoriKlassekodeMapperTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void skal_map_til_klassekode_når_det_gjelder_fødsel() {
        //Act
        String klassekode = InntektskategoriKlassekodeMapper.mapTilKlassekode(Inntektskategori.ARBEIDSTAKER, FamilieYtelseType.FØDSEL);

        //Assert
        assertThat(klassekode).isEqualTo("FPATORD");
    }

    @Test
    public void skal_map_til_klassekode_når_det_gjelder_adopsjon() {
        //Act
        String klassekode = InntektskategoriKlassekodeMapper.mapTilKlassekode(Inntektskategori.ARBEIDSTAKER, FamilieYtelseType.ADOPSJON);

        //Assert
        assertThat(klassekode).isEqualTo("FPADATORD");
    }

    @Test
    public void skal_map_til_klassekode_når_det_gjelder_svangerskapspenger() {
        //Act
        String klassekode = InntektskategoriKlassekodeMapper.mapTilKlassekode(Inntektskategori.ARBEIDSTAKER, FamilieYtelseType.SVANGERSKAPSPENGER);

        //Assert
        assertThat(klassekode).isEqualTo("FPSVATORD");
    }

    @Test
    public void skal_kaste_exception_hvis_inntektskategori_er_udefinert_eller_ikke_finnes() {
        //Assert
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Utvikler feil: Mangler mapping for inntektskategori " + Inntektskategori.UDEFINERT);

        //Act
        InntektskategoriKlassekodeMapper.mapTilKlassekode(Inntektskategori.UDEFINERT, FamilieYtelseType.FØDSEL);
    }
}
