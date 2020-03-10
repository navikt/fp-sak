package no.nav.foreldrepenger.domene.registerinnhenting.fp;

import static no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType.OPPTJENING;
import static no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType.UDEFINERT;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.registerinnhenting.EndringsresultatSjekker;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktUtleder;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

@RunWith(CdiRunner.class)
public class StartpunktTjenesteImplTest {

    private StartpunktTjeneste tjeneste;

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private Behandling behandling;

    private EndringsresultatSjekker endringsresultatSjekker;

    @Before
    public void before() {

        behandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD).lagre(repositoryProvider);

        endringsresultatSjekker = mock(EndringsresultatSjekker.class);

        var familietjeneste = mock(FamilieHendelseTjeneste.class);

        // Mock startpunktutlederprovider
        var utledere = new UnitTestLookupInstanceImpl<StartpunktUtleder>(
            (behandling, grunnlagId1, grunnlagId2) -> StartpunktType.OPPTJENING);

        tjeneste = new StartpunktTjenesteImpl(utledere, endringsresultatSjekker, familietjeneste);
    }

    @Test
    public void skal_returnere_startpunkt_for_endret_aggregat() {
        // Arrange
        // To forskjellige id-er indikerer endring på grunnlag
        long grunnlagId1 = 1L, grunnlagId2 = 2L;
        EndringsresultatDiff endringsresultat = opprettEndringsresultat(grunnlagId1, grunnlagId2);

        when(endringsresultatSjekker.finnSporedeEndringerPåBehandlingsgrunnlag(Mockito.anyLong(), any(EndringsresultatSnapshot.class)))
            .thenReturn(endringsresultat);

        // Act/Assert
        assertThat(tjeneste.utledStartpunktForDiffBehandlingsgrunnlag(lagRef(behandling), endringsresultat)).isEqualTo(OPPTJENING);
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling, LocalDate.now());
    }

    @Test
    public void skal_gi_startpunkt_udefinert_dersom_ingen_endringer_på_aggregater() {
        // Arrange
        // To lik id-er indikerer ingen endring på grunnlag
        long grunnlagId1 = 1L, grunnlagId2 = grunnlagId1;
        EndringsresultatDiff endringsresultat = opprettEndringsresultat(grunnlagId1, grunnlagId2);
        when(endringsresultatSjekker.finnSporedeEndringerPåBehandlingsgrunnlag(Mockito.anyLong(), any(EndringsresultatSnapshot.class)))
            .thenReturn(endringsresultat);

        // Act/Assert
        assertThat(tjeneste.utledStartpunktForDiffBehandlingsgrunnlag(lagRef(behandling), endringsresultat)).isEqualTo(UDEFINERT);
    }

    @Test
    public void rotnode_skal_ikke_tas_med() {
        EndringsresultatDiff endringsresultat = EndringsresultatDiff.opprettForSporingsendringer();

        when(endringsresultatSjekker.finnSporedeEndringerPåBehandlingsgrunnlag(Mockito.anyLong(), any(EndringsresultatSnapshot.class))).thenReturn(endringsresultat);

        // Act/Assert
        assertThat(tjeneste.utledStartpunktForDiffBehandlingsgrunnlag(lagRef(behandling), endringsresultat)).isEqualTo(UDEFINERT);
    }

    private EndringsresultatDiff opprettEndringsresultat(Long grunnlagId1, Long grunnlagId2) {

        EndringsresultatDiff endringsresultat = EndringsresultatDiff.opprett();
        DiffResult diffResult = mock(DiffResult.class);
        when(diffResult.isEmpty()).thenReturn(false); // Indikerer at det finnes diff
        endringsresultat.leggTilSporetEndring(EndringsresultatDiff.medDiff(Object.class, grunnlagId1, grunnlagId2), () -> diffResult);

        return endringsresultat;
    }
}
