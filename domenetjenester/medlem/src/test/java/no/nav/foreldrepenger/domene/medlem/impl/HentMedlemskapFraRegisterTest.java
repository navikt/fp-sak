package no.nav.foreldrepenger.domene.medlem.impl;

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
import no.nav.foreldrepenger.domene.medlem.api.Medlemskapsperiode;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.integrasjon.medl2.Medlemskap;
import no.nav.vedtak.felles.integrasjon.medl2.Medlemskapsunntak;

public class HentMedlemskapFraRegisterTest {

    private static final AktørId AKTØR_ID = AktørId.dummy();

    private Medlemskap restKlient = mock(Medlemskap.class);
    private HentMedlemskapFraRegister medlemTjeneste;

    private static final long MEDL_ID_1 = 2663947L;

    @BeforeEach
    public void before() {
        medlemTjeneste = new HentMedlemskapFraRegister(restKlient);
    }

    @Test
    public void skal_hente_medlemsperioder_og_logge_dem_til_saksopplysningslageret() throws Exception {
        // Arrange
        var unntak = mock(Medlemskapsunntak.class);
        when(unntak.getUnntakId()).thenReturn(MEDL_ID_1);
        when(unntak.getFraOgMed()).thenReturn(LocalDate.of(2019, 8, 1));
        when(unntak.getTilOgMed()).thenReturn(LocalDate.of(2019, 12, 31));
        when(unntak.getDekning()).thenReturn("Full");
        when(unntak.getLovvalg()).thenReturn("ENDL");
        when(unntak.getLovvalgsland()).thenReturn("UZB");
        when(unntak.getKilde()).thenReturn("AVGSYS");
        when(unntak.getBesluttet()).thenReturn(LocalDate.of(2020, 5, 26));
        when(unntak.getStudieland()).thenReturn("VUT");
        when(unntak.isMedlem()).thenReturn(true);

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
