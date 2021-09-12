package no.nav.foreldrepenger.inngangsvilkaar.søknad;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.YtelseMaksdatoTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.RegisterInnhentingIntervall;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;

public class SøknadsfristvilkårTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private InngangsvilkårOversetter oversetter;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        var iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider, new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));
        var personopplysningTjeneste = new PersonopplysningTjeneste(
            repositoryProvider.getPersonopplysningRepository());
        oversetter = new InngangsvilkårOversetter(repositoryProvider,
            personopplysningTjeneste, new YtelseMaksdatoTjeneste(repositoryProvider, new RelatertBehandlingTjeneste(repositoryProvider)),
            iayTjeneste,
            null);
    }

    @Test
    public void skal_vurdere_vilkår_som_oppfylt_når_elektronisk_søknad_og_søknad_mottat_innen_6_mnd_fra_skjæringstidspunkt() throws JsonProcessingException, IOException {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknad().medElektroniskRegistrert(true);
        scenario.medSøknad().medMottattDato(LocalDate.now().plusMonths(6));
        scenario.medBekreftetHendelse()
            .medAdopsjon(scenario.medBekreftetHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(LocalDate.now()));
        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var data = new InngangsvilkårEngangsstønadSøknadsfrist(oversetter).vurderVilkår(lagRef(behandling));

        var jsonNode = StandardJsonConfig.fromJsonAsTree(data.regelInput());
        var elektroniskSoeknad = jsonNode.get("elektroniskSoeknad").asText();

        // Assert
        assertThat(data.vilkårType()).isEqualTo(VilkårType.SØKNADSFRISTVILKÅRET);
        assertThat(data.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(data.merknadParametere()).isEmpty();
        assertThat(data.regelInput()).isNotEmpty();
        assertThat(elektroniskSoeknad).isEqualTo("true");
    }

    @Test
    public void skal_vurdere_vilkår_som_ikke_vurdert_når_elektronisk_søknad_og_søknad_ikke_mottat_innen_6_mnd_fra_skjæringstidspunkt() {
        final var ANTALL_DAGER_SOKNAD_LEVERT_FOR_SENT = 100;

        // Arrange
        var behandling = mockBehandling(true, LocalDate.now().plusMonths(6).plusDays(ANTALL_DAGER_SOKNAD_LEVERT_FOR_SENT),
            LocalDate.now());

        // Act
        var data = new InngangsvilkårEngangsstønadSøknadsfrist(oversetter).vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(data.vilkårType()).isEqualTo(VilkårType.SØKNADSFRISTVILKÅRET);
        assertThat(data.utfallType()).isEqualTo(VilkårUtfallType.IKKE_VURDERT);

        assertThat(data.aksjonspunktDefinisjoner()).contains(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET);
        assertThat(data.merknadParametere())
            .containsOnlyKeys("antallDagerSoeknadLevertForSent")
            .containsEntry("antallDagerSoeknadLevertForSent", String.valueOf(ANTALL_DAGER_SOKNAD_LEVERT_FOR_SENT));

    }

    @Test
    public void skal_vurdere_vilkår_som_oppfylt_når_papirsøknad_og_søknad_mottat_innen_6_mnd_og_2_dager_fra_skjæringstidspunkt() {
        // Arrange
        var behandling = mockBehandling(false, LocalDate.now().minusMonths(6), LocalDate.now());

        // Act
        var data = new InngangsvilkårEngangsstønadSøknadsfrist(oversetter).vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(data.vilkårType()).isEqualTo(VilkårType.SØKNADSFRISTVILKÅRET);
        assertThat(data.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(data.merknadParametere()).isEmpty();
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
    public void skal_vurdere_vilkår_for_papirsøknad_med_original_frist_lørdag_pluss_2_virkedager() {

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
    public void skal_vurdere_vilkår_for_papirsøknad_med_original_frist_søndag_pluss_2_virkedager() {

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
    public void skal_vurdere_vilkår_for_papirsøknad_med_original_frist_fredag_pluss_2_virkedager() {

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
    public void skal_vurdere_vilkår_for_papirsøknad_med_original_frist_torsdag_pluss_2_virkedager_og_her_treffer_månedsslutt() {

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
        var behandling = mockBehandling(false, mottattDag, omsorgDato);
        return behandling;
    }

    private void assertOppfylt(Behandling behandling) {
        var data = new InngangsvilkårEngangsstønadSøknadsfrist(oversetter).vurderVilkår(lagRef(behandling));
        assertThat(data.vilkårType()).isEqualTo(VilkårType.SØKNADSFRISTVILKÅRET);
        assertThat(data.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        assertThat(data.aksjonspunktDefinisjoner()).isEmpty();
        assertThat(data.merknadParametere()).isEmpty();
    }

    private void assertIkkeVurdertForSent(Behandling behandling, int dagerForSent) {
        var data = new InngangsvilkårEngangsstønadSøknadsfrist(oversetter).vurderVilkår(lagRef(behandling));
        assertThat(data.vilkårType()).isEqualTo(VilkårType.SØKNADSFRISTVILKÅRET);
        assertThat(data.utfallType()).isEqualTo(VilkårUtfallType.IKKE_VURDERT);

        assertThat(data.aksjonspunktDefinisjoner()).contains(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET);
        assertThat(data.merknadParametere())
            .containsOnlyKeys("antallDagerSoeknadLevertForSent")
            .containsEntry("antallDagerSoeknadLevertForSent", String.valueOf(dagerForSent));
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
    }


}
