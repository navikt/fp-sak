package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;

public class InntektskategoriKlassekodeMapperTest {

    @Test
    public void skal_map_til_klassekode_når_det_gjelder_fødsel() {
        String klassekode = InntektskategoriKlassekodeMapper.mapTilKlassekode(Inntektskategori.ARBEIDSTAKER, FamilieYtelseType.FØDSEL);
        assertThat(klassekode).isEqualTo("FPATORD");
    }

    @Test
    public void skal_map_til_klassekode_når_det_gjelder_adopsjon() {
        String klassekode = InntektskategoriKlassekodeMapper.mapTilKlassekode(Inntektskategori.ARBEIDSTAKER, FamilieYtelseType.ADOPSJON);
        assertThat(klassekode).isEqualTo("FPADATORD");
    }

    @Test
    public void skal_map_til_klassekode_når_det_gjelder_svangerskapspenger() {
        String klassekode = InntektskategoriKlassekodeMapper.mapTilKlassekode(Inntektskategori.ARBEIDSTAKER, FamilieYtelseType.SVANGERSKAPSPENGER);
        assertThat(klassekode).isEqualTo("FPSVATORD");
    }

    @Test
    public void skal_kaste_exception_hvis_inntektskategori_er_udefinert_eller_ikke_finnes() {
        assertThrows(IllegalStateException.class,
                () -> InntektskategoriKlassekodeMapper.mapTilKlassekode(Inntektskategori.UDEFINERT, FamilieYtelseType.FØDSEL));
    }
}
