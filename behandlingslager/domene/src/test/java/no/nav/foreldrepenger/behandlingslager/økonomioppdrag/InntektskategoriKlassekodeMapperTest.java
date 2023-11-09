package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;

class InntektskategoriKlassekodeMapperTest {

    @Test
    void skal_map_til_klassekode_når_det_gjelder_fødsel() {
        var klassekode = InntektskategoriKlassekodeMapper.mapTilKlassekode(Inntektskategori.ARBEIDSTAKER, FamilieYtelseType.FØDSEL);
        assertThat(klassekode.getKode()).isEqualTo("FPATORD");
    }

    @Test
    void skal_map_til_klassekode_når_det_gjelder_adopsjon() {
        var klassekode = InntektskategoriKlassekodeMapper.mapTilKlassekode(Inntektskategori.ARBEIDSTAKER, FamilieYtelseType.ADOPSJON);
        assertThat(klassekode.getKode()).isEqualTo("FPADATORD");
    }

    @Test
    void skal_map_til_klassekode_når_det_gjelder_svangerskapspenger() {
        var klassekode = InntektskategoriKlassekodeMapper.mapTilKlassekode(Inntektskategori.ARBEIDSTAKER, FamilieYtelseType.SVANGERSKAPSPENGER);
        assertThat(klassekode.getKode()).isEqualTo("FPSVATORD");
    }

    @Test
    void skal_kaste_exception_hvis_inntektskategori_er_udefinert_eller_ikke_finnes() {
        assertThrows(IllegalStateException.class,
                () -> InntektskategoriKlassekodeMapper.mapTilKlassekode(Inntektskategori.UDEFINERT, FamilieYtelseType.FØDSEL));
    }
}
