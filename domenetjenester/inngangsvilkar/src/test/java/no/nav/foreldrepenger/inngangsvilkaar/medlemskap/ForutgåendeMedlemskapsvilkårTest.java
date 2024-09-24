package no.nav.foreldrepenger.inngangsvilkaar.medlemskap;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapManuellVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonAdresse;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.AvklarMedlemskapUtleder;

@CdiDbAwareTest
class ForutgåendeMedlemskapsvilkårTest {

    public static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    private InngangsvilkårMedlemskapForutgående vurderMedlemskapsvilkarEngangsstonad;
    @Inject
    private AvklarMedlemskapUtleder avklarMedlemskapUtleder;

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @BeforeEach
    public void before() {
        this.vurderMedlemskapsvilkarEngangsstonad = new InngangsvilkårMedlemskapForutgående(avklarMedlemskapUtleder);
    }

    @Test
    void skal_vurdere_manuell_avklart_ikke_medlem_som_vilkår_ikke_oppfylt() {
        // Arrange
        var scenario = lagTestScenario();
        scenario.medMedlemskap().medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.UNNTAK);
        var behandling = lagre(scenario);

        // Act
        var vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(vilkårData.vilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE);
        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(vilkårData.vilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1020);
    }

    /**
     * Lager minimalt testscenario med en medlemsperiode som indikerer om søker er
     * medlem eller ikke.
     */
    private ScenarioMorSøkerEngangsstønad lagTestScenario() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
                .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                        .medTermindato(SKJÆRINGSTIDSPUNKT)
                        .medNavnPå("navn navnesen")
                        .medUtstedtDato(LocalDate.now()));
        if (MedlemskapDekningType.FTL_2_7_A != null) {
            scenario.leggTilMedlemskapPeriode(new MedlemskapPerioderBuilder()
                    .medDekningType(MedlemskapDekningType.FTL_2_7_A)
                    .medMedlemskapType(MedlemskapType.ENDELIG)
                    .medPeriode(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
                    .build());
        }

        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();

        var søker = builderForRegisteropplysninger.medPersonas()
            .kvinne(søkerAktørId, SivilstandType.GIFT)
            .personstatus(PersonstatusType.BOSA)
            .statsborgerskap(Landkoder.NOR)
            .adresse(AdresseType.BOSTEDSADRESSE, PersonAdresse.builder()
                .land(Landkoder.NOR)
                .adresseType(AdresseType.BOSTEDSADRESSE)
                .periode(SKJÆRINGSTIDSPUNKT.minusYears(2), SKJÆRINGSTIDSPUNKT.plusYears(2)))
            .build();
        scenario.medRegisterOpplysninger(søker);
        return scenario;
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }

}
