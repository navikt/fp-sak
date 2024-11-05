package no.nav.foreldrepenger.behandling.steg.foreslåresultat.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.es.RevurderingEndringImpl;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;

class ForeslåBehandlingsresultatTjenesteTest extends EntityManagerAwareTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    private BehandlingRepositoryProvider repositoryProvider;

    private final RevurderingEndringImpl revurderingEndring = mock(RevurderingEndringImpl.class);
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private ForeslåBehandlingsresultatTjenesteES foreslåVedtaTjenesteES;

    @BeforeEach
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        doReturn(false).when(revurderingEndring).erRevurderingMedUendretUtfall(any(), any());
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        foreslåVedtaTjenesteES = new ForeslåBehandlingsresultatTjenesteES(
                repositoryProvider.getBehandlingsresultatRepository(),
                repositoryProvider.getBehandlingRepository(),
                revurderingEndring);
    }

    @Test
    void setter_behandlingsresultat_avslag_med_avslagsårsak() {
        // Arrange
        var behandling = vilkårOppsett(VilkårUtfallType.IKKE_OPPFYLT);

        // Act
        foreslåBehandlingsresultat(behandling);

        // Assert
        var behandlingsresultat = getBehandlingsresultat(behandling);
        assertThat(behandlingsresultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.AVSLÅTT);
        assertThat(behandlingsresultat.getAvslagsårsak()).isEqualTo(Avslagsårsak.SØKT_FOR_SENT);
    }

    private void foreslåBehandlingsresultat(Behandling behandling) {
        var ref = BehandlingReferanse.fra(behandling);
        foreslåVedtaTjenesteES.foreslåBehandlingsresultat(ref);
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
    }

    @Test
    void setter_behandlingsresultat_innvilget() {
        // Arrange
        var behandling = vilkårOppsett(VilkårUtfallType.OPPFYLT);

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
        var behandling = vilkårOppsett(VilkårUtfallType.OPPFYLT);
        foreslåBehandlingsresultat(behandling);
        var konsekvenserForYtelsen = getBehandlingsresultat(behandling).getKonsekvenserForYtelsen();
        assertThat(konsekvenserForYtelsen.size()).isOne();
        assertThat(konsekvenserForYtelsen.get(0)).isEqualTo(KonsekvensForYtelsen.INGEN_ENDRING);
    }

    @Test
    void setter_ikke_konsekvens_for_ytelse() {
        var behandling = vilkårOppsett(VilkårUtfallType.OPPFYLT);
        foreslåBehandlingsresultat(behandling);
        var konsekvenserForYtelsen = getBehandlingsresultat(behandling).getKonsekvenserForYtelsen();
        assertThat(konsekvenserForYtelsen).isEmpty();
    }

    private Behandling vilkårOppsett(VilkårUtfallType vilkårUtfallType) {
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
                .buildFor(behandling);
        behandlingRepository.lagre(vilkårResultat, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }
}
