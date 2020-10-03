package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.oppdrag110;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;

public class KodeFagområdeTjenesteTest {

    private Oppdrag110 oppdrag110 = mock(Oppdrag110.class);
    private KodeFagområdeTjeneste tjenesteFP = KodeFagområdeTjeneste.forForeldrepenger();
    private KodeFagområdeTjeneste tjenesteSVP = KodeFagområdeTjeneste.forSvangerskapspenger();

    @Test
    public void skal_finne_kode_for_fagområde_når_ytelse_type_er_FP_og_oppdragsmottaker_er_bruker() {
        String value = tjenesteFP.finn(true);
        assertThat(value).isEqualTo(ØkonomiKodeFagområde.FP.name());
    }

    @Test
    public void skal_finne_kode_for_fagområde_når_ytelse_type_er_FP_og_oppdragsmottaker_er_arbeidsgiver() {
        String value = tjenesteFP.finn(false);
        assertThat(value).isEqualTo(ØkonomiKodeFagområde.FPREF.name());
    }

    @Test
    public void skal_finne_kode_for_fagområde_når_ytelse_type_er_SVP_og_oppdragsmottaker_er_bruker() {
        String value = tjenesteSVP.finn(true);
        assertThat(value).isEqualTo(ØkonomiKodeFagområde.SVP.name());
    }

    @Test
    public void skal_finne_kode_for_fagområde_når_ytelse_type_er_SVP_og_oppdragsmottaker_er_arbeidsgiver() {
        String value = tjenesteSVP.finn(false);
        assertThat(value).isEqualTo(ØkonomiKodeFagområde.SVPREF.name());
    }

    @Test
    public void skal_finne_oppdragsmottaker_fra_oppdrag_110_når_ytelse_type_er_FP() {
        when(oppdrag110.getKodeFagomrade()).thenReturn(ØkonomiKodeFagområde.FP.name());
        boolean value = tjenesteFP.gjelderBruker(oppdrag110);
        assertThat(value).isTrue();
    }

    @Test
    public void skal_finne_oppdragsmottaker_fra_oppdrag_110_når_ytelse_type_er_SVP() {
        when(oppdrag110.getKodeFagomrade()).thenReturn(ØkonomiKodeFagområde.SVP.name());
        boolean value = tjenesteSVP.gjelderBruker(oppdrag110);
        assertThat(value).isTrue();
    }

    @Test
    public void skal_kaste_exception_når_det_gjelder_fagområde_av_ES() {
        when(oppdrag110.getKodeFagomrade()).thenReturn(ØkonomiKodeFagområde.REFUTG.name());
        assertThrows(IllegalArgumentException.class, () -> tjenesteFP.gjelderBruker(oppdrag110));
    }

    @Test
    public void skal_kaste_exception_når_det_gjelder_ugyldig_fagområde() {
        when(oppdrag110.getKodeFagomrade()).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> tjenesteFP.gjelderBruker(oppdrag110));
    }
}
