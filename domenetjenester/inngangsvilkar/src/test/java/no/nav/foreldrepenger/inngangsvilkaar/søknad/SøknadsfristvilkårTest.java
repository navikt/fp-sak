package no.nav.foreldrepenger.inngangsvilkaar.søknad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SøknadsperiodeFristTjenesteImpl;

class SøknadsfristvilkårTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider);
    }

    @Test
    void skal_vurdere_vilkår_som_oppfylt_når_elektronisk_søknad_og_søknad_mottat_innen_6_mnd_fra_skjæringstidspunkt() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknad().medElektroniskRegistrert(true);
        scenario.medSøknad().medMottattDato(LocalDate.now().plusMonths(6));
        scenario.medBekreftetHendelse()
            .medAdopsjon(scenario.medBekreftetHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(LocalDate.now()));
        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var data = new InngangsvilkårEngangsstønadSøknadsfrist(new SøknadsperiodeFristTjenesteImpl(repositoryProvider)).vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(data.vilkårType()).isEqualTo(VilkårType.SØKNADSFRISTVILKÅRET);
        assertThat(data.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
    }

    @Test
    void skal_vurdere_vilkår_som_ikke_vurdert_når_elektronisk_søknad_og_søknad_ikke_mottat_innen_6_mnd_fra_skjæringstidspunkt() {
        var ANTALL_DAGER_SOKNAD_LEVERT_FOR_SENT = 100;

        // Arrange
        var behandling = mockBehandling(true, LocalDate.now().plusMonths(6).plusDays(ANTALL_DAGER_SOKNAD_LEVERT_FOR_SENT),
            LocalDate.now());

        // Act
        var data = new InngangsvilkårEngangsstønadSøknadsfrist(new SøknadsperiodeFristTjenesteImpl(repositoryProvider)).vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(data.vilkårType()).isEqualTo(VilkårType.SØKNADSFRISTVILKÅRET);
        assertThat(data.utfallType()).isEqualTo(VilkårUtfallType.IKKE_VURDERT);

        assertThat(data.aksjonspunktDefinisjoner()).contains(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET);
    }

    @Test
    void skal_vurdere_vilkår_som_oppfylt_når_papirsøknad_og_søknad_mottat_innen_6_mnd_og_2_dager_fra_skjæringstidspunkt() {
        // Arrange
        var behandling = mockBehandling(false, LocalDate.now().minusMonths(6), LocalDate.now());

        // Act
        var data = new InngangsvilkårEngangsstønadSøknadsfrist(new SøknadsperiodeFristTjenesteImpl(repositoryProvider)).vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(data.vilkårType()).isEqualTo(VilkårType.SØKNADSFRISTVILKÅRET);
        assertThat(data.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
    }

    private Behandling mockBehandling(boolean elektronisk, LocalDate mottakDato, LocalDate omsorgsovertakelsesDato) {
        var scenario = ScenarioMorSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknad().medElektroniskRegistrert(elektronisk);
        scenario.medSøknad().medMottattDato(mottakDato);
        scenario.medBekreftetHendelse()
            .medAdopsjon(scenario.medBekreftetHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(omsorgsovertakelsesDato));
        return scenario.lagre(repositoryProvider);
    }

    @Test
    void skal_vurdere_vilkår_for_papirsøknad_med_original_frist_lørdag_pluss_2_virkedager() {

        var mottattMandag = LocalDate.of(2017, 9, 4);
        var mottattTirsdag = mottattMandag.plusDays(1);
        var mottattOnsdag = mottattMandag.plusDays(2);
        var mottattTorsdag = mottattMandag.plusDays(3);
        var mottattFredag = mottattMandag.plusDays(4);
        var mottattLørdag = mottattMandag.plusDays(5);
        var mottattSøndag = mottattMandag.plusDays(6);

        var skjæringstidspunktMedOrginalFristLørdag = mottattMandag.minusDays(2).minusMonths(6);

        // Act + assert
        assertOppfylt(mockPapirSøknad(mottattMandag, skjæringstidspunktMedOrginalFristLørdag));
        assertOppfylt(mockPapirSøknad(mottattTirsdag, skjæringstidspunktMedOrginalFristLørdag));
        assertIkkeVurdertForSent(mockPapirSøknad(mottattOnsdag, skjæringstidspunktMedOrginalFristLørdag), 1);
        assertIkkeVurdertForSent(mockPapirSøknad(mottattTorsdag, skjæringstidspunktMedOrginalFristLørdag), 2);
        assertIkkeVurdertForSent(mockPapirSøknad(mottattFredag, skjæringstidspunktMedOrginalFristLørdag), 3);
        assertIkkeVurdertForSent(mockPapirSøknad(mottattLørdag, skjæringstidspunktMedOrginalFristLørdag), 4);
        assertIkkeVurdertForSent(mockPapirSøknad(mottattSøndag, skjæringstidspunktMedOrginalFristLørdag), 5);

    }

    @Test
    void skal_vurdere_vilkår_for_papirsøknad_med_original_frist_søndag_pluss_2_virkedager() {

        var mottattMandag = LocalDate.of(2017, 9, 4);
        var mottattTirsdag = mottattMandag.plusDays(1);
        var mottattOnsdag = mottattMandag.plusDays(2);
        var mottattTorsdag = mottattMandag.plusDays(3);
        var mottattFredag = mottattMandag.plusDays(4);
        var mottattLørdag = mottattMandag.plusDays(5);
        var mottattSøndag = mottattMandag.plusDays(6);

        var skjæringstidspunktMedOrginalFristSøndag = mottattMandag.minusDays(1).minusMonths(6);

        // Act + assert
        assertOppfylt(mockPapirSøknad(mottattMandag, skjæringstidspunktMedOrginalFristSøndag));
        assertOppfylt(mockPapirSøknad(mottattTirsdag, skjæringstidspunktMedOrginalFristSøndag));
        assertIkkeVurdertForSent(mockPapirSøknad(mottattOnsdag, skjæringstidspunktMedOrginalFristSøndag), 1);
        assertIkkeVurdertForSent(mockPapirSøknad(mottattTorsdag, skjæringstidspunktMedOrginalFristSøndag), 2);
        assertIkkeVurdertForSent(mockPapirSøknad(mottattFredag, skjæringstidspunktMedOrginalFristSøndag), 3);
        assertIkkeVurdertForSent(mockPapirSøknad(mottattLørdag, skjæringstidspunktMedOrginalFristSøndag), 4);
        assertIkkeVurdertForSent(mockPapirSøknad(mottattSøndag, skjæringstidspunktMedOrginalFristSøndag), 5);

    }

    @Test
    void skal_vurdere_vilkår_for_papirsøknad_med_original_frist_fredag_pluss_2_virkedager() {

        var mottattMandag = LocalDate.of(2017, 9, 4);
        var mottattTirsdag = mottattMandag.plusDays(1);
        var mottattOnsdag = mottattMandag.plusDays(2);
        var mottattTorsdag = mottattMandag.plusDays(3);
        var mottattFredag = mottattMandag.plusDays(4);
        var mottattLørdag = mottattMandag.plusDays(5);
        var mottattSøndag = mottattMandag.plusDays(6);

        var skjæringstidspunktMedOrginalFristFredag = mottattMandag.minusDays(3).minusMonths(6);

        // Act + assert
        assertOppfylt(mockPapirSøknad(mottattMandag, skjæringstidspunktMedOrginalFristFredag));
        assertOppfylt(mockPapirSøknad(mottattTirsdag, skjæringstidspunktMedOrginalFristFredag));
        assertIkkeVurdertForSent(mockPapirSøknad(mottattOnsdag, skjæringstidspunktMedOrginalFristFredag), 1);
        assertIkkeVurdertForSent(mockPapirSøknad(mottattTorsdag, skjæringstidspunktMedOrginalFristFredag), 2);
        assertIkkeVurdertForSent(mockPapirSøknad(mottattFredag, skjæringstidspunktMedOrginalFristFredag), 3);
        assertIkkeVurdertForSent(mockPapirSøknad(mottattLørdag, skjæringstidspunktMedOrginalFristFredag), 4);
        assertIkkeVurdertForSent(mockPapirSøknad(mottattSøndag, skjæringstidspunktMedOrginalFristFredag), 5);

    }

    @Test
    void skal_vurdere_vilkår_for_papirsøknad_med_original_frist_torsdag_pluss_2_virkedager_og_her_treffer_månedsslutt() {

        var mottattMandag = LocalDate.of(2017, 9, 11);
        var mottattTirsdag = mottattMandag.plusDays(1);
        var mottattOnsdag = mottattMandag.plusDays(2);
        var mottattTorsdag = mottattMandag.plusDays(3);
        var mottattFredag = mottattMandag.plusDays(4);
        var mottattLørdag = mottattMandag.plusDays(5);
        var mottattSøndag = mottattMandag.plusDays(6);

        var skjæringstidspunktMedOrginalFristTorsdag = mottattMandag.minusDays(4).minusMonths(6);

        // Act + assert
        assertOppfylt(mockPapirSøknad(mottattMandag, skjæringstidspunktMedOrginalFristTorsdag));
        assertIkkeVurdertForSent(mockPapirSøknad(mottattTirsdag, skjæringstidspunktMedOrginalFristTorsdag), 1);
        assertIkkeVurdertForSent(mockPapirSøknad(mottattOnsdag, skjæringstidspunktMedOrginalFristTorsdag), 2);
        assertIkkeVurdertForSent(mockPapirSøknad(mottattTorsdag, skjæringstidspunktMedOrginalFristTorsdag), 3);
        assertIkkeVurdertForSent(mockPapirSøknad(mottattFredag, skjæringstidspunktMedOrginalFristTorsdag), 4);
        assertIkkeVurdertForSent(mockPapirSøknad(mottattLørdag, skjæringstidspunktMedOrginalFristTorsdag), 5);
        assertIkkeVurdertForSent(mockPapirSøknad(mottattSøndag, skjæringstidspunktMedOrginalFristTorsdag), 6);

    }

    private Behandling mockPapirSøknad(LocalDate mottattDag, LocalDate omsorgDato) {
        return mockBehandling(false, mottattDag, omsorgDato);
    }

    private void assertOppfylt(Behandling behandling) {
        var data = new InngangsvilkårEngangsstønadSøknadsfrist(new SøknadsperiodeFristTjenesteImpl(repositoryProvider)).vurderVilkår(lagRef(behandling));
        assertThat(data.vilkårType()).isEqualTo(VilkårType.SØKNADSFRISTVILKÅRET);
        assertThat(data.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        assertThat(data.aksjonspunktDefinisjoner()).isEmpty();
    }

    private void assertIkkeVurdertForSent(Behandling behandling, int dagerForSent) {
        var data = new InngangsvilkårEngangsstønadSøknadsfrist(new SøknadsperiodeFristTjenesteImpl(repositoryProvider)).vurderVilkår(lagRef(behandling));
        assertThat(data.vilkårType()).isEqualTo(VilkårType.SØKNADSFRISTVILKÅRET);
        assertThat(data.utfallType()).isEqualTo(VilkårUtfallType.IKKE_VURDERT);

        assertThat(data.aksjonspunktDefinisjoner()).contains(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET);
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }


}
