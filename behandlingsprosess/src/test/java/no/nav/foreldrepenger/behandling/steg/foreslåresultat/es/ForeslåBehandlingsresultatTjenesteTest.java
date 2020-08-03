package no.nav.foreldrepenger.behandling.steg.foreslåresultat.es;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.es.RevurderingEndringImpl;
import no.nav.foreldrepenger.behandling.steg.foreslåresultat.AvslagsårsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;

public class ForeslåBehandlingsresultatTjenesteTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private final EntityManager entityManager = repoRule.getEntityManager();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);

    private RevurderingEndringImpl revurderingEndring = Mockito.mock(RevurderingEndringImpl.class);
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private BehandlingsresultatRepository behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
    private AvslagsårsakTjeneste avslagsårsakTjeneste = new AvslagsårsakTjeneste();
    private ForeslåBehandlingsresultatTjenesteImpl foreslåVedtaTjenesteES = new ForeslåBehandlingsresultatTjenesteImpl(
        repositoryProvider.getBehandlingsresultatRepository(),
        repositoryProvider.getBehandlingRepository(),
        avslagsårsakTjeneste,revurderingEndring);

    @Before
    public void setup() {
        Mockito.doReturn(false).when(revurderingEndring).erRevurderingMedUendretUtfall(Mockito.any(), Mockito.any());
    }

    @Test
    public void setter_behandlingsresultat_avslag_med_avslagsårsak() {
        // Arrange
        Behandling behandling = vilkårOppsett(VilkårUtfallType.IKKE_OPPFYLT, VilkårResultatType.AVSLÅTT);

        // Act
        foreslåBehandlingsresultat(behandling);

        // Assert
        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(behandling);
        assertThat(behandlingsresultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.AVSLÅTT);
        assertThat(behandlingsresultat.getAvslagsårsak()).isEqualTo(Avslagsårsak.SØKT_FOR_SENT);
    }

    private void foreslåBehandlingsresultat(Behandling behandling) {
        var ref = BehandlingReferanse.fra(behandling,
            Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build());
        foreslåVedtaTjenesteES.foreslåBehandlingsresultat(ref);
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
    }

    @Test
    public void setter_behandlingsresultat_innvilget() {
        // Arrange
        Behandling behandling = vilkårOppsett(VilkårUtfallType.OPPFYLT, VilkårResultatType.INNVILGET);

        // Act
        foreslåBehandlingsresultat(behandling);

        // Assert
        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(behandling);
        assertThat(behandlingsresultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.INNVILGET);
        assertThat(behandlingsresultat.getAvslagsårsak()).isNull();
    }

    @Test
    public void setter_konsekvens_for_ytelse_ingen_endring() {
        Mockito.doReturn(true).when(revurderingEndring).erRevurderingMedUendretUtfall(Mockito.any(), Mockito.any());
        Behandling behandling = vilkårOppsett(VilkårUtfallType.OPPFYLT, VilkårResultatType.INNVILGET);
        foreslåBehandlingsresultat(behandling);
        List<KonsekvensForYtelsen> konsekvenserForYtelsen = getBehandlingsresultat(behandling).getKonsekvenserForYtelsen();
        assertThat(konsekvenserForYtelsen.size()).isOne();
        assertThat(konsekvenserForYtelsen.get(0)).isEqualTo(KonsekvensForYtelsen.INGEN_ENDRING);
    }


    @Test
    public void setter_ikke_konsekvens_for_ytelse() {
        Behandling behandling = vilkårOppsett(VilkårUtfallType.OPPFYLT, VilkårResultatType.INNVILGET);
        foreslåBehandlingsresultat(behandling);
        List<KonsekvensForYtelsen> konsekvenserForYtelsen = getBehandlingsresultat(behandling).getKonsekvenserForYtelsen();
        assertThat(konsekvenserForYtelsen).isEmpty();
    }

    private Behandling vilkårOppsett(VilkårUtfallType vilkårUtfallType, VilkårResultatType resultatType) {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        Behandling behandling = scenario.lagre(repositoryProvider);

        VilkårResultat vilkårResultat = VilkårResultat.builder()
            .leggTilVilkår(VilkårType.SØKNADSFRISTVILKÅRET, vilkårUtfallType)
            .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .medVilkårResultatType(resultatType)
            .buildFor(behandling);
        behandlingRepository.lagre(vilkårResultat, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }
}
