package no.nav.foreldrepenger.domene.medlem.medl2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapKildeType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.typer.AktørId;

class HentMedlemskapFraRegisterTest {

    private static final AktørId AKTØR_ID = AktørId.dummy();

    private Medlemskap restKlient = mock(Medlemskap.class);
    private HentMedlemskapFraRegister medlemTjeneste;

    private static final long MEDL_ID_1 = 2663947L;

    @BeforeEach
    public void before() {
        medlemTjeneste = new HentMedlemskapFraRegister(restKlient);
    }

    @Test
    void skal_hente_medlemsperioder_og_logge_dem_til_saksopplysningslageret() throws Exception {
        // Arrange
        var unntak = mock(Medlemskapsunntak.class);
        when(unntak.unntakId()).thenReturn(MEDL_ID_1);
        when(unntak.fraOgMed()).thenReturn(LocalDate.of(2019, 8, 1));
        when(unntak.tilOgMed()).thenReturn(LocalDate.of(2019, 12, 31));
        when(unntak.dekning()).thenReturn("Full");
        when(unntak.lovvalg()).thenReturn("ENDL");
        when(unntak.lovvalgsland()).thenReturn("UZB");
        when(unntak.getKilde()).thenReturn("AVGSYS");
        when(unntak.getBesluttet()).thenReturn(LocalDate.of(2020, 5, 26));
        when(unntak.getStudieland()).thenReturn("VUT");
        when(unntak.medlem()).thenReturn(true);

        when(restKlient.finnMedlemsunntak(eq(AKTØR_ID.getId()), any(), any())).thenReturn(List.of(unntak));

        // Act
        var medlemskapsperioder = medlemTjeneste.finnMedlemskapPerioder(AKTØR_ID, LocalDate.now().minusYears(1),
                LocalDate.now().plusYears(1));

        // Assert
        assertThat(medlemskapsperioder).hasSize(1);

        var medlemskapsperiode1 = new Medlemskapsperiode.Builder()
                .medFom(LocalDate.of(2010, 8, 1))
                .medTom(LocalDate.of(2010, 12, 31))
                .medDatoBesluttet(LocalDate.of(2012, 5, 26))
                .medErMedlem(true)
                .medDekning(MedlemskapDekningType.FULL)
                .medLovvalg(MedlemskapType.ENDELIG)
                .medLovvalgsland(Landkoder.UZB)
                .medKilde(MedlemskapKildeType.AVGSYS)
                .medStudieland(Landkoder.VUT)
                .medMedlId(MEDL_ID_1)
                .build();
    }
}
