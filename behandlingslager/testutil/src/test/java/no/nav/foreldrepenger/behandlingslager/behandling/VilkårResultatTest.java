package no.nav.foreldrepenger.behandlingslager.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;

public class VilkårResultatTest extends EntityManagerAwareTest {

    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void setup() {
        var entityManager = getEntityManager();
        behandlingRepository = new BehandlingRepository(entityManager);
    }

    @Test
    public void skal_gjenbruke_vilkårresultat_i_ny_behandling_når_det_ikke_er_endret() {
        // Arrange
        var behandling = lagBehandling();
        lagreBehandling(behandling);

        Behandlingsresultat.builderForInngangsvilkår().buildFor(behandling);
        var behandlingsresultat1 = lagreOgGjenopphenteBehandlingsresultat(behandling, VilkårResultatType.IKKE_FASTSATT);

        var id01 = behandlingsresultat1.getBehandlingId();

        // Act
        var behandling2 = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medKopiAvForrigeBehandlingsresultat()
            .build();
        lagreBehandling(behandling2);
        var behandlingsresultat2 = lagreOgGjenopphenteBehandlingsresultat(behandling2, VilkårResultatType.IKKE_FASTSATT);

        // Assert
        assertThat(getBehandlingsresultat(behandling2)).isNotSameAs(getBehandlingsresultat(behandling));
        assertThat(getBehandlingsresultat(behandling2).getVilkårResultat())
                .isSameAs(getBehandlingsresultat(behandling).getVilkårResultat());
        assertThat(getBehandlingsresultat(behandling2).getVilkårResultat())
                .isEqualTo(getBehandlingsresultat(behandling).getVilkårResultat());

        var id02 = behandlingsresultat2.getBehandlingId();
        assertThat(id02).isNotEqualTo(id01);
    }

    private Behandling lagBehandling() {
        var fagsak = FagsakBuilder.nyEngangstønadForMor().build();
        new FagsakRepository(getEntityManager()).opprettNy(fagsak);
        return Behandling.forFørstegangssøknad(fagsak).build();
    }

    @Test
    public void skal_opprette_nytt_vilkårresultat_i_ny_behandling_når_det_endrer_vilkårresultat() {
        // Arrange
        var behandling = lagBehandling();
        lagreBehandling(behandling);

        Behandlingsresultat.builderForInngangsvilkår().buildFor(behandling);
        var behandlingsresultat1 = lagreOgGjenopphenteBehandlingsresultat(behandling, VilkårResultatType.IKKE_FASTSATT);

        var id01 = behandlingsresultat1.getBehandlingId();

        // Act
        var builder = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medKopiAvForrigeBehandlingsresultat();
        var behandling2 = builder.build();
        lagreBehandling(behandling2);

        // legg til et nytt vilkårsresultat
        VilkårResultat.builderFraEksisterende(getBehandlingsresultat(behandling2).getVilkårResultat())
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .medVilkårResultatType(VilkårResultatType.INNVILGET)
                .buildFor(behandling2);

        var behandlingsresultat2 = lagreOgGjenopphenteBehandlingsresultat(behandling2, VilkårResultatType.INNVILGET);
        // Assert
        assertThat(getBehandlingsresultat(behandling2)).isNotSameAs(getBehandlingsresultat(behandling));
        assertThat(getBehandlingsresultat(behandling2).getVilkårResultat())
                .isNotEqualTo(getBehandlingsresultat(behandling).getVilkårResultat());

        var id02 = behandlingsresultat2.getBehandlingId();
        assertThat(id02).isNotEqualTo(id01);
    }

    @Test
    public void skal_lagre_og_hente_vilkår_med_avslagsårsak() {
        // Arrange
        var behandling = lagBehandling();
        lagreBehandling(behandling);
        var vilkårResultatBuilder = VilkårResultat.builder()
                .medVilkårResultatType(VilkårResultatType.AVSLÅTT)
                .leggTilVilkår(VilkårType.OMSORGSVILKÅRET, VilkårUtfallType.IKKE_OPPFYLT, Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_O);
        var behandlingsresultatBuilder = new Behandlingsresultat.Builder(vilkårResultatBuilder);
        var behandlingsresultat1 = behandlingsresultatBuilder.buildFor(behandling);

        // Act
        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandlingsresultat1.getVilkårResultat(), lås);
        lagreBehandling(behandling);
        var lagretBehandling = getEntityManager().find(Behandling.class, behandling.getId());

        // Assert
        assertThat(lagretBehandling).isEqualTo(behandling);
        assertThat(getBehandlingsresultat(lagretBehandling).getVilkårResultat().getVilkårene()).hasSize(1);
        var vilkår = getBehandlingsresultat(lagretBehandling).getVilkårResultat().getVilkårene().get(0);
        assertThat(vilkår.getAvslagsårsak()).isNotNull();
        assertThat(vilkår.getAvslagsårsak()).isEqualTo(Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_O);
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling lagretBehandling) {
        return lagretBehandling.getBehandlingsresultat();
    }

    @Test
    public void skal_legge_til_vilkår() {
        // Arrange
        var behandling = lagBehandling();
        var opprinneligVilkårResultat = VilkårResultat.builder()
            .medVilkårResultatType(VilkårResultatType.IKKE_FASTSATT)
            .leggTilVilkår(VilkårType.SØKNADSFRISTVILKÅRET, VilkårUtfallType.IKKE_VURDERT)
            .buildFor(behandling);

        // Act
        var oppdatertVilkårResultat = VilkårResultat.builderFraEksisterende(opprinneligVilkårResultat)
            .medVilkårResultatType(VilkårResultatType.AVSLÅTT)
            .manueltVilkår(VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD, VilkårUtfallType.IKKE_OPPFYLT, Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_F)
            .buildFor(behandling);

        // Assert
        assertThat(oppdatertVilkårResultat.getVilkårene()).hasSize(2);

        var vilkår1 = oppdatertVilkårResultat.getVilkårene().stream().filter(v -> VilkårType.SØKNADSFRISTVILKÅRET.equals(v.getVilkårType())).findFirst().orElse(null);
        assertThat(vilkår1).isNotNull();
        assertThat(vilkår1.getGjeldendeVilkårUtfall()).isEqualTo(VilkårUtfallType.IKKE_VURDERT);

        var vilkår2 = oppdatertVilkårResultat.getVilkårene().stream().filter(v -> VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD.equals(v.getVilkårType())).findFirst().orElse(null);
        assertThat(vilkår2).isNotNull();
        assertThat(vilkår2.getGjeldendeVilkårUtfall()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
    }

    @Test
    public void skal_oppdatere_vilkår_med_nytt_utfall() {
        // Arrange
        var behandling = lagBehandling();
        var opprinneligVilkårResultat = VilkårResultat.builder()
            .medVilkårResultatType(VilkårResultatType.AVSLÅTT)
            .manueltVilkår(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.IKKE_OPPFYLT, Avslagsårsak.SØKER_HAR_IKKE_FORELDREANSVAR)
            .buildFor(behandling);

        // Act
        var oppdatertVilkårResultat = VilkårResultat.builderFraEksisterende(opprinneligVilkårResultat)
            .medVilkårResultatType(VilkårResultatType.INNVILGET)
            .manueltVilkår(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.OPPFYLT, Avslagsårsak.UDEFINERT)
            .buildFor(behandling);

        // Assert
        assertThat(oppdatertVilkårResultat.getVilkårene()).hasSize(1);
        var vilkår = oppdatertVilkårResultat.getVilkårene().get(0);
        assertThat(vilkår.getVilkårType()).isEqualTo(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD);
        assertThat(vilkår.getAvslagsårsak()).isNull();
        assertThat(vilkår.getGjeldendeVilkårUtfall()).isEqualTo(VilkårUtfallType.OPPFYLT);
    }

    @Test
    public void skal_overstyre_vilkår() {
        // Arrange
        var behandling = lagBehandling();
        var opprinneligVilkårResultat = VilkårResultat.builder()
            .medVilkårResultatType(VilkårResultatType.INNVILGET)
            .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .buildFor(behandling);

        // Act 1: Ikke oppfylt (overstyrt)
        var oppdatertVilkårResultat = VilkårResultat.builderFraEksisterende(opprinneligVilkårResultat)
            .medVilkårResultatType(VilkårResultatType.AVSLÅTT)
            .overstyrVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.IKKE_OPPFYLT, Avslagsårsak.SØKER_ER_UTVANDRET)
            .buildFor(behandling);

        // Assert
        assertThat(oppdatertVilkårResultat.erOverstyrt()).isTrue();
        assertThat(oppdatertVilkårResultat.getVilkårene()).hasSize(1);
        var vilkår = oppdatertVilkårResultat.getVilkårene().get(0);
        assertThat(vilkår.getVilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkår.getAvslagsårsak()).isEqualTo(Avslagsårsak.SØKER_ER_UTVANDRET);
        assertThat(vilkår.getGjeldendeVilkårUtfall()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(vilkår.erOverstyrt()).isTrue();
        assertThat(vilkår.erManueltVurdert()).isTrue();

        // Act 2: Oppfylt
        oppdatertVilkårResultat = VilkårResultat.builderFraEksisterende(oppdatertVilkårResultat)
            .medVilkårResultatType(VilkårResultatType.INNVILGET)
            .overstyrVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT, Avslagsårsak.UDEFINERT)
            .buildFor(behandling);

        // Assert
        assertThat(oppdatertVilkårResultat.erOverstyrt()).isTrue();
        assertThat(oppdatertVilkårResultat.getVilkårene()).hasSize(1);
        vilkår = oppdatertVilkårResultat.getVilkårene().get(0);
        assertThat(vilkår.getVilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkår.getAvslagsårsak()).isNull();
        assertThat(vilkår.getGjeldendeVilkårUtfall()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(vilkår.erOverstyrt()).isTrue();
        assertThat(vilkår.erManueltVurdert()).isTrue();
    }

    @Test
    public void skal_beholde_tidligere_overstyring_inkl_avslagsårsak_når_manuell_vurdering_oppdateres() {
        // Arrange
        var behandling = lagBehandling();
        var opprinneligVilkårResultat = VilkårResultat.builder()
            .medVilkårResultatType(VilkårResultatType.INNVILGET)
            .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .buildFor(behandling);
        var overstyrtVilkårResultat = VilkårResultat.builderFraEksisterende(opprinneligVilkårResultat)
            .medVilkårResultatType(VilkårResultatType.AVSLÅTT)
            .overstyrVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.IKKE_OPPFYLT, Avslagsårsak.SØKER_ER_UTVANDRET)
            .buildFor(behandling);

        // Act
        var oppdatertVilkårResultat = VilkårResultat.builderFraEksisterende(overstyrtVilkårResultat)
            .medVilkårResultatType(VilkårResultatType.AVSLÅTT) // Overstyring teller
            .manueltVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT, Avslagsårsak.UDEFINERT)
            .buildFor(behandling);

        // Assert
        assertThat(oppdatertVilkårResultat.getVilkårene()).hasSize(1);
        var vilkår = oppdatertVilkårResultat.getVilkårene().get(0);
        assertThat(vilkår.getVilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkår.erOverstyrt()).isTrue();
        assertThat(vilkår.getAvslagsårsak()).isEqualTo(Avslagsårsak.SØKER_ER_UTVANDRET);
        assertThat(vilkår.getGjeldendeVilkårUtfall()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(vilkår.getVilkårUtfallManuelt()).isEqualTo(VilkårUtfallType.OPPFYLT);
    }

    @Test
    public void skal_fjerne_vilkår() {
        // Arrange
        var behandling = lagBehandling();
        var opprinneligVilkårResultat = VilkårResultat.builder()
            .medVilkårResultatType(VilkårResultatType.AVSLÅTT)
            .leggTilVilkår(VilkårType.SØKNADSFRISTVILKÅRET, VilkårUtfallType.IKKE_VURDERT)
            .manueltVilkår(VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD, VilkårUtfallType.IKKE_OPPFYLT, Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_F)
            .buildFor(behandling);

        // Act
        var oppdatertVilkårResultat = VilkårResultat.builderFraEksisterende(opprinneligVilkårResultat)
            .fjernVilkår(VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD)
            .medVilkårResultatType(VilkårResultatType.IKKE_FASTSATT)
            .buildFor(behandling);

        // Assert
        assertThat(oppdatertVilkårResultat.getVilkårene()).hasSize(1);
        var vilkår = oppdatertVilkårResultat.getVilkårene().get(0);
        assertThat(vilkår.getVilkårType()).isEqualTo(VilkårType.SØKNADSFRISTVILKÅRET);
        assertThat(vilkår.getGjeldendeVilkårUtfall()).isEqualTo(VilkårUtfallType.IKKE_VURDERT);
    }

    private Behandlingsresultat lagreOgGjenopphenteBehandlingsresultat(Behandling behandling, VilkårResultatType forventet) {
        var behandlingsresultat = getBehandlingsresultat(behandling);

        assertThat(behandlingsresultat.getBehandlingId()).isNotNull();
        assertThat(behandlingsresultat.getVilkårResultat().getOriginalBehandlingId()).isNotNull();
        assertThat(behandlingsresultat.getVilkårResultat().getVilkårResultatType()).isEqualTo(forventet);

        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandlingsresultat.getVilkårResultat(), lås);
        lagreBehandling(behandling);

        var id = behandling.getId();
        assertThat(id).isNotNull();

        var lagretBehandling = getEntityManager().find(Behandling.class, id);
        assertThat(lagretBehandling).isEqualTo(behandling);
        assertThat(getBehandlingsresultat(lagretBehandling)).isEqualTo(behandlingsresultat);

        return behandlingsresultat;
    }

    private void lagreBehandling(Behandling behandling) {
        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
    }

}
