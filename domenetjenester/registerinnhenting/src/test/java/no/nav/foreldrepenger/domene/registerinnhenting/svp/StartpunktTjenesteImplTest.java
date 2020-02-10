package no.nav.foreldrepenger.domene.registerinnhenting.svp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class StartpunktTjenesteImplTest {

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();

    private StartpunktTjenesteImpl startpunktTjenesteSvp;

    @Before
    public void before() {
        startpunktTjenesteSvp = new StartpunktTjenesteImpl();
    }

    @Test
    public void skal_utlede_startpunkt_opplysningsplikt_når_det_er_endring() {
        // Arrange
        EndringsresultatDiff endringsresultatDiff = opprettEndringsresultat(1L, 2L);// Forskjellig ID indikerer endring

        // Act
        StartpunktType startpunktType = startpunktTjenesteSvp.utledStartpunktForDiffBehandlingsgrunnlag(null, endringsresultatDiff);

        // Assert
        assertThat(startpunktType).isEqualTo(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
    }

    @Test
    public void skal_utlede_startpunkt_udefinert_når_det_ikke_er_endring() {
        // Arrange
        EndringsresultatDiff endringsresultatDiff = opprettEndringsresultat(1L, 1L);// Lik ID indikerer ingen endring

        // Act
        StartpunktType startpunktType = startpunktTjenesteSvp.utledStartpunktForDiffBehandlingsgrunnlag(null, endringsresultatDiff);

        // Assert
        assertThat(startpunktType).isEqualTo(StartpunktType.UDEFINERT);
    }

    private EndringsresultatDiff opprettEndringsresultat(Long grunnlagId1, Long grunnlagId2) {
        EndringsresultatDiff endringsresultat = EndringsresultatDiff.opprett();
        DiffResult diffResult = mock(DiffResult.class);
        when(diffResult.isEmpty()).thenReturn(false); // Indikerer at det finnes diff
        endringsresultat.leggTilSporetEndring(EndringsresultatDiff.medDiff(Object.class, grunnlagId1, grunnlagId2), () -> diffResult);
        return endringsresultat;
    }
}
