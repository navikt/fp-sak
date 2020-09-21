package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.oppdrag110;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;

public class KodeFagområdeTjenesteTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private Oppdrag110 oppdrag110 = mock(Oppdrag110.class);
    private KodeFagområdeTjeneste tjenesteFP = KodeFagområdeTjeneste.forForeldrepenger();
    private KodeFagområdeTjeneste tjenesteSVP = KodeFagområdeTjeneste.forSvangerskapspenger();

    @Test
    public void skal_finne_kode_for_fagområde_når_ytelse_type_er_FP_og_oppdragsmottaker_er_bruker() {
        //Act
        String value = tjenesteFP.finn(true);

        //Assert
        assertThat(value).isEqualTo(ØkonomiKodeFagområde.FP.name());
    }

    @Test
    public void skal_finne_kode_for_fagområde_når_ytelse_type_er_FP_og_oppdragsmottaker_er_arbeidsgiver() {
        //Act
        String value = tjenesteFP.finn(false);

        //Assert
        assertThat(value).isEqualTo(ØkonomiKodeFagområde.FPREF.name());
    }

    @Test
    public void skal_finne_kode_for_fagområde_når_ytelse_type_er_SVP_og_oppdragsmottaker_er_bruker() {
        //Act
        String value = tjenesteSVP.finn(true);

        //Assert
        assertThat(value).isEqualTo(ØkonomiKodeFagområde.SVP.name());
    }

    @Test
    public void skal_finne_kode_for_fagområde_når_ytelse_type_er_SVP_og_oppdragsmottaker_er_arbeidsgiver() {
        //Act
        String value = tjenesteSVP.finn(false);

        //Assert
        assertThat(value).isEqualTo(ØkonomiKodeFagområde.SVPREF.name());
    }

    @Test
    public void skal_finne_oppdragsmottaker_fra_oppdrag_110_når_ytelse_type_er_FP() {
        //Arrange
        when(oppdrag110.getKodeFagomrade()).thenReturn(ØkonomiKodeFagområde.FP.name());

        //Act
        boolean value = tjenesteFP.gjelderBruker(oppdrag110);

        //Assert
        assertThat(value).isTrue();
    }

    @Test
    public void skal_finne_oppdragsmottaker_fra_oppdrag_110_når_ytelse_type_er_SVP() {
        //Arrange
        when(oppdrag110.getKodeFagomrade()).thenReturn(ØkonomiKodeFagområde.SVP.name());

        //Act
        boolean value = tjenesteSVP.gjelderBruker(oppdrag110);

        //Assert
        assertThat(value).isTrue();
    }

    @Test
    public void skal_kaste_exception_når_det_gjelder_fagområde_av_ES() {
        //Arrange
        when(oppdrag110.getKodeFagomrade()).thenReturn(ØkonomiKodeFagområde.REFUTG.name());

        //Assert
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Utvikler feil: ikke støtett fagområde: ");

        //Act
        tjenesteFP.gjelderBruker(oppdrag110);
    }

    @Test
    public void skal_kaste_exception_når_det_gjelder_ugyldig_fagområde() {
        //Arrange
        when(oppdrag110.getKodeFagomrade()).thenReturn(null);

        //Assert
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Utvikler feil: ikke støtett fagområde: ");

        //Act
        tjenesteFP.gjelderBruker(oppdrag110);
    }
}
