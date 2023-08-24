package no.nav.foreldrepenger.behandling.steg.foreslåresultat.es;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.es.RevurderingEndringImpl;
import no.nav.foreldrepenger.behandlingslager.behandling.*;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.*;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class ForeslåBehandlingsresultatTjenesteTest extends EntityManagerAwareTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    private BehandlingRepositoryProvider repositoryProvider;

    private final RevurderingEndringImpl revurderingEndring = mock(RevurderingEndringImpl.class);
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private ForeslåBehandlingsresultatTjenesteImpl foreslåVedtaTjenesteES;

    @BeforeEach
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        doReturn(false).when(revurderingEndring).erRevurderingMedUendretUtfall(any(), any());
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        foreslåVedtaTjenesteES = new ForeslåBehandlingsresultatTjenesteImpl(
                repositoryProvider.getBehandlingsresultatRepository(),
                repositoryProvider.getBehandlingRepository(),
                revurderingEndring);
    }

    @Test
    void setter_behandlingsresultat_avslag_med_avslagsårsak() {
        // Arrange
        var behandling = vilkårOppsett(VilkårUtfallType.IKKE_OPPFYLT, VilkårResultatType.AVSLÅTT);

        // Act
        foreslåBehandlingsresultat(behandling);

        // Assert
        var behandlingsresultat = getBehandlingsresultat(behandling);
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
    void setter_behandlingsresultat_innvilget() {
        // Arrange
        var behandling = vilkårOppsett(VilkårUtfallType.OPPFYLT, VilkårResultatType.INNVILGET);

        // Act
        foreslåBehandlingsresultat(behandling);

        // Assert
        var behandlingsresultat = getBehandlingsresultat(behandling);
        assertThat(behandlingsresultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.INNVILGET);
        assertThat(behandlingsresultat.getAvslagsårsak()).isNull();
    }

    @Test
    void setter_konsekvens_for_ytelse_ingen_endring() {
        doReturn(true).when(revurderingEndring).erRevurderingMedUendretUtfall(any(), any());
        var behandling = vilkårOppsett(VilkårUtfallType.OPPFYLT, VilkårResultatType.INNVILGET);
        foreslåBehandlingsresultat(behandling);
        var konsekvenserForYtelsen = getBehandlingsresultat(behandling).getKonsekvenserForYtelsen();
        assertThat(konsekvenserForYtelsen.size()).isOne();
        assertThat(konsekvenserForYtelsen.get(0)).isEqualTo(KonsekvensForYtelsen.INGEN_ENDRING);
    }

    @Test
    void setter_ikke_konsekvens_for_ytelse() {
        var behandling = vilkårOppsett(VilkårUtfallType.OPPFYLT, VilkårResultatType.INNVILGET);
        foreslåBehandlingsresultat(behandling);
        var konsekvenserForYtelsen = getBehandlingsresultat(behandling).getKonsekvenserForYtelsen();
        assertThat(konsekvenserForYtelsen).isEmpty();
    }

    private Behandling vilkårOppsett(VilkårUtfallType vilkårUtfallType, VilkårResultatType resultatType) {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);

        var builder = VilkårResultat.builder();
        if (VilkårUtfallType.IKKE_OPPFYLT.equals(vilkårUtfallType)) {
            builder.manueltVilkår(VilkårType.SØKNADSFRISTVILKÅRET, VilkårUtfallType.IKKE_OPPFYLT, Avslagsårsak.SØKT_FOR_SENT);
        } else {
            builder.leggTilVilkårOppfylt(VilkårType.SØKNADSFRISTVILKÅRET);
        }
        var vilkårResultat = builder
                .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
                .medVilkårResultatType(resultatType)
                .buildFor(behandling);
        behandlingRepository.lagre(vilkårResultat, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }
}
