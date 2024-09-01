package no.nav.foreldrepenger.domene.registerinnhenting.svp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.registerinnhenting.EndringsresultatSjekker;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktUtleder;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

class StartpunktTjenesteImplTest {

    private StartpunktTjenesteImpl startpunktTjenesteSvp;
    private EndringsresultatSjekker endringsresultatSjekker;

    @BeforeEach
    public void before() {
        endringsresultatSjekker = mock(EndringsresultatSjekker.class);

        // Mock startpunktutlederprovider
        var utledere = new UnitTestLookupInstanceImpl<StartpunktUtleder>(
            (behandling, stp, grunnlagId1, grunnlagId2) -> StartpunktType.BEREGNING);

        startpunktTjenesteSvp = new StartpunktTjenesteImpl(utledere, endringsresultatSjekker);
    }

    @Test
    void skal_utlede_startpunkt_opplysningsplikt_når_det_er_endring() {
        // Arrange
        var endringsresultatDiff = opprettEndringsresultat(1L, 2L);// Forskjellig ID indikerer endring

        // Act
        var startpunktType = startpunktTjenesteSvp.utledStartpunktForDiffBehandlingsgrunnlag(null, null, endringsresultatDiff);

        // Assert
        assertThat(startpunktType).isEqualTo(StartpunktType.BEREGNING);
    }

    @Test
    void skal_utlede_startpunkt_udefinert_når_det_ikke_er_endring() {
        // Arrange
        var endringsresultatDiff = opprettEndringsresultat(1L, 1L);// Lik ID indikerer ingen endring

        // Act
        var startpunktType = startpunktTjenesteSvp.utledStartpunktForDiffBehandlingsgrunnlag(null, null, endringsresultatDiff);

        // Assert
        assertThat(startpunktType).isEqualTo(StartpunktType.UDEFINERT);
    }

    private EndringsresultatDiff opprettEndringsresultat(Long grunnlagId1, Long grunnlagId2) {

        var endringsresultat = EndringsresultatDiff.opprett();
        var diffResult = mock(DiffResult.class);
        when(diffResult.isEmpty()).thenReturn(false); // Indikerer at det finnes diff
        endringsresultat.leggTilSporetEndring(EndringsresultatDiff.medDiff(InntektArbeidYtelseGrunnlag.class, grunnlagId1, grunnlagId2), () -> diffResult);

        return endringsresultat;
    }
}
