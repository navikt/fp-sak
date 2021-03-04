package no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdrag110;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;

public class KodeFagområdeTjenesteTest {

    private Oppdrag110 oppdrag110 = mock(Oppdrag110.class);
    private KodeFagområdeTjeneste tjenesteFP = KodeFagområdeTjeneste.forForeldrepenger();
    private KodeFagområdeTjeneste tjenesteSVP = KodeFagområdeTjeneste.forSvangerskapspenger();

    @Test
    public void skal_finne_kode_for_fagområde_når_ytelse_type_er_FP_og_oppdragsmottaker_er_bruker() {
        KodeFagområde value = tjenesteFP.finn(true);
        assertThat(value).isEqualTo(KodeFagområde.FORELDREPENGER_BRUKER);
    }

    @Test
    public void skal_finne_kode_for_fagområde_når_ytelse_type_er_FP_og_oppdragsmottaker_er_arbeidsgiver() {
        KodeFagområde value = tjenesteFP.finn(false);
        assertThat(value).isEqualTo(KodeFagområde.FORELDREPENGER_ARBEIDSGIVER);
    }

    @Test
    public void skal_finne_kode_for_fagområde_når_ytelse_type_er_SVP_og_oppdragsmottaker_er_bruker() {
        KodeFagområde value = tjenesteSVP.finn(true);
        assertThat(value).isEqualTo(KodeFagområde.SVANGERSKAPSPENGER_BRUKER);
    }

    @Test
    public void skal_finne_kode_for_fagområde_når_ytelse_type_er_SVP_og_oppdragsmottaker_er_arbeidsgiver() {
        KodeFagområde value = tjenesteSVP.finn(false);
        assertThat(value).isEqualTo(KodeFagområde.SVANGERSKAPSPENGER_ARBEIDSGIVER);
    }

    @Test
    public void skal_finne_oppdragsmottaker_fra_oppdrag_110_når_ytelse_type_er_FP() {
        when(oppdrag110.getKodeFagomrade()).thenReturn(KodeFagområde.FORELDREPENGER_BRUKER);
        boolean value = tjenesteFP.gjelderBruker(oppdrag110);
        assertThat(value).isTrue();
    }

    @Test
    public void skal_finne_oppdragsmottaker_fra_oppdrag_110_når_ytelse_type_er_SVP() {
        when(oppdrag110.getKodeFagomrade()).thenReturn(KodeFagområde.SVANGERSKAPSPENGER_BRUKER);
        boolean value = tjenesteSVP.gjelderBruker(oppdrag110);
        assertThat(value).isTrue();
    }
}
