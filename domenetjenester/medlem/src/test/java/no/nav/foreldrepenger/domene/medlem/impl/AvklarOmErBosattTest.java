package no.nav.foreldrepenger.domene.medlem.impl;

import static java.util.Collections.singletonList;
import static no.nav.foreldrepenger.domene.medlem.impl.MedlemResultat.AVKLAR_OM_ER_BOSATT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonAdresse;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.medlem.MedlemskapPerioderTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;

@CdiDbAwareTest
public class AvklarOmErBosattTest {

    @Inject
    private BehandlingRepositoryProvider provider;

    @Inject
    private MedlemskapPerioderTjeneste medlemskapPerioderTjeneste;

    @Inject
    private PersonopplysningTjeneste personopplysningTjeneste;

    private AvklarOmErBosatt avklarOmErBosatt;

    @BeforeEach
    public void setUp() {
        this.avklarOmErBosatt = new AvklarOmErBosatt(provider, medlemskapPerioderTjeneste, personopplysningTjeneste);
    }

    @Test
    public void skal_få_aksjonspunkt_ved_udefinert_personstatus() {
        // Arrange
        var fødselsDato = LocalDate.now();
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medDefaultOppgittTilknytning();
        scenario.medSøknadHendelse().medFødselsDato(fødselsDato).medAntallBarn(1);
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.SWE, PersonstatusType.UDEFINERT);
        var behandling = scenario.lagre(provider);

        // Act
        var resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).contains(AVKLAR_OM_ER_BOSATT);
    }

    @Test
    public void skal_få_aksjonspunkt_ved_utvandret_personstatus() {
        // Arrange
        var fødselsDato = LocalDate.now();
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medDefaultOppgittTilknytning();
        scenario.medSøknadHendelse().medFødselsDato(fødselsDato).medAntallBarn(1);
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.SWE, PersonstatusType.UTVA);
        var behandling = scenario.lagre(provider);

        // Act
        var resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).contains(AVKLAR_OM_ER_BOSATT);
    }

    @Test
    public void skal_gi_medlem_resultat_AVKLAR_OM_ER_BOSATT() {
        // Arrange
        var fødselsDato = LocalDate.now();
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medDefaultOppgittTilknytning();
        scenario.medSøknadHendelse().medFødselsDato(fødselsDato).medAntallBarn(1);
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.SWE, PersonstatusType.BOSA);
        var behandling = scenario.lagre(provider);

        // Act
        var resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).contains(AVKLAR_OM_ER_BOSATT);
    }

    @Test
    public void skal_ikke_gi_medlem_resultat_AVKLAR_OM_ER_BOSATT() {
        // Arrange
        var fødselsDato = LocalDate.now();
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medDefaultOppgittTilknytning();
        scenario.medSøknadHendelse().medFødselsDato(fødselsDato).medAntallBarn(1);

        leggTilSøker(scenario, AdresseType.BOSTEDSADRESSE, Landkoder.NOR, PersonstatusType.BOSA);
        var behandling = scenario.lagre(provider);

        // Act
        var resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_ikke_få_aksjonspunkt_når_bruker_har_utenlandsk_postadresse_og_dekningsgraden_er_frivillig_medlem() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medDefaultOppgittTilknytning();
        leggTilSøker(scenario, AdresseType.MIDLERTIDIG_POSTADRESSE_UTLAND, Landkoder.USA, PersonstatusType.BOSA);
        var fødselsDato = LocalDate.now();
        scenario.medSøknadHendelse().medFødselsDato(fødselsDato);
        var gyldigPeriodeUnderFødsel = new MedlemskapPerioderBuilder()
                .medDekningType(MedlemskapDekningType.FTL_2_9_2_a) // hjemlet i bokstav a
                .medMedlemskapType(MedlemskapType.ENDELIG) // gyldig
                .medPeriode(LocalDate.now(), LocalDate.now())
                .build();

        scenario.leggTilMedlemskapPeriode(gyldigPeriodeUnderFødsel);
        var behandling = scenario.lagre(provider);

        // Act
        var resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_ikke_få_aksjonspunkt_når_bruker_har_utenlandsk_postadresse_og_dekningsgraden_er_ikke_medlem() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medDefaultOppgittTilknytning();
        leggTilSøker(scenario, AdresseType.MIDLERTIDIG_POSTADRESSE_UTLAND, Landkoder.USA, PersonstatusType.BOSA);
        var fødselsDato = LocalDate.now();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        var gyldigPeriodeUnderFødsel = new MedlemskapPerioderBuilder()
                .medDekningType(MedlemskapDekningType.FTL_2_6) // hjemlet i bokstav a
                .medMedlemskapType(MedlemskapType.ENDELIG) // gyldig
                .medPeriode(LocalDate.now(), LocalDate.now())
                .build();

        scenario.leggTilMedlemskapPeriode(gyldigPeriodeUnderFødsel);
        var behandling = scenario.lagre(provider);

        // Act
        var resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_opprette_aksjonspunkt_dersom_minst_to_av_spørsmål_til_bruker_om_tilknytning_er_nei() {
        // Arrange
        var fødselsDato = LocalDate.now();
        var oppholdUtlandForrigePeriode = new MedlemskapOppgittLandOppholdEntitet.Builder()
                .erTidligereOpphold(true)
                .medLand(Landkoder.BEL)
                .medPeriode(LocalDate.now(), LocalDate.now().plusYears(1))
                .build();
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medDefaultOppgittTilknytning();
        leggTilSøker(scenario, AdresseType.BOSTEDSADRESSE, Landkoder.NOR, PersonstatusType.BOSA);
        scenario.medOppgittTilknytning().medOpphold(singletonList(oppholdUtlandForrigePeriode)).medOppholdNå(false);
        var behandling = scenario.lagre(provider);

        // Act
        var resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).contains(AVKLAR_OM_ER_BOSATT);
    }

    @Test
    public void skal_opprette_aksjonspunkt_dersom_søker_har_søkt_termin_og_ikke_skal_bo_i_norge_de_neste_12_månedene() {
        // Arrange
        var fødselsDato = LocalDate.now();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknad().medMottattDato(LocalDate.now());
        scenario.medDefaultOppgittTilknytning();
        scenario.medDefaultSøknadTerminbekreftelse();

        var fremtidigOppholdISverige = new MedlemskapOppgittLandOppholdEntitet.Builder()
                .erTidligereOpphold(false)
                .medLand(Landkoder.SWE)
                .medPeriode(LocalDate.now().plusDays(20), LocalDate.now().plusYears(2))
                .build();

        leggTilSøker(scenario, AdresseType.BOSTEDSADRESSE, Landkoder.NOR, PersonstatusType.BOSA);
        scenario.medOppgittTilknytning()
                .medOpphold(List.of(fremtidigOppholdISverige))
                .medOppholdNå(true);
        var behandling = scenario.lagre(provider);

        // Act
        var resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).contains(AVKLAR_OM_ER_BOSATT);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_dersom_søker_har_søkt_termin_og_skal_bo_i_mange_land_i_fremtiden_men_til_sammen_under_12_måneder() {
        // Arrange
        var fødselsDato = LocalDate.now();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknad().medMottattDato(LocalDate.now());
        scenario.medDefaultOppgittTilknytning();
        scenario.medDefaultSøknadTerminbekreftelse();

        var swe = new MedlemskapOppgittLandOppholdEntitet.Builder()
                .erTidligereOpphold(false)
                .medLand(Landkoder.SWE)
                .medPeriode(LocalDate.now().plusDays(0), LocalDate.now().plusMonths(2))
                .build();

        var usa = new MedlemskapOppgittLandOppholdEntitet.Builder()
                .erTidligereOpphold(false)
                .medLand(Landkoder.USA)
                .medPeriode(LocalDate.now().plusMonths(2), LocalDate.now().plusMonths(4))
                .build();

        var bel = new MedlemskapOppgittLandOppholdEntitet.Builder()
                .erTidligereOpphold(false)
                .medLand(Landkoder.BEL)
                .medPeriode(LocalDate.now().plusMonths(4), LocalDate.now().plusMonths(6))
                .build();

        var png = new MedlemskapOppgittLandOppholdEntitet.Builder()
                .erTidligereOpphold(false)
                .medLand(Landkoder.PNG)
                .medPeriode(LocalDate.now().plusMonths(6), LocalDate.now().plusMonths(8))
                .build();

        leggTilSøker(scenario, AdresseType.BOSTEDSADRESSE, Landkoder.NOR, PersonstatusType.BOSA);
        scenario.medOppgittTilknytning()
                .medOpphold(List.of(swe, usa, bel, png))
                .medOppholdNå(true);
        var behandling = scenario.lagre(provider);

        // Act
        var resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_opprette_aksjonspunkt_dersom_søker_har_søkt_termin_og_skal_bo_i_mange_land_i_fremtiden_men_til_sammen_mer_12_måneder() {
        // Arrange
        var fødselsDato = LocalDate.now();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknad().medMottattDato(LocalDate.now());
        scenario.medDefaultOppgittTilknytning();
        scenario.medDefaultSøknadTerminbekreftelse();

        var swe = new MedlemskapOppgittLandOppholdEntitet.Builder()
                .erTidligereOpphold(false)
                .medLand(Landkoder.SWE)
                .medPeriode(LocalDate.now().plusDays(0), LocalDate.now().plusMonths(2))
                .build();

        var usa = new MedlemskapOppgittLandOppholdEntitet.Builder()
                .erTidligereOpphold(false)
                .medLand(Landkoder.USA)
                .medPeriode(LocalDate.now().plusMonths(2), LocalDate.now().plusMonths(4))
                .build();

        var bel = new MedlemskapOppgittLandOppholdEntitet.Builder()
                .erTidligereOpphold(false)
                .medLand(Landkoder.BEL)
                .medPeriode(LocalDate.now().plusMonths(4), LocalDate.now().plusMonths(6))
                .build();

        var png = new MedlemskapOppgittLandOppholdEntitet.Builder()
                .erTidligereOpphold(false)
                .medLand(Landkoder.PNG)
                .medPeriode(LocalDate.now().plusMonths(6), LocalDate.now().plusMonths(15))
                .build();

        leggTilSøker(scenario, AdresseType.BOSTEDSADRESSE, Landkoder.NOR, PersonstatusType.BOSA);
        scenario.medOppgittTilknytning()
                .medOpphold(List.of(swe, usa, bel, png))
                .medOppholdNå(true);
        var behandling = scenario.lagre(provider);

        // Act
        var resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).contains(AVKLAR_OM_ER_BOSATT);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_dersom_søker_har_søkt_fødsel_og_ikke_skal_bo_i_norge_de_neste_12_månedene() {
        // Arrange
        var fødselsDato = LocalDate.now();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medDefaultOppgittTilknytning();

        var fremtidigOppholdISverige = new MedlemskapOppgittLandOppholdEntitet.Builder()
                .erTidligereOpphold(false)
                .medLand(Landkoder.SWE)
                .medPeriode(LocalDate.now().plusDays(20), LocalDate.now().plusYears(2))
                .build();

        leggTilSøker(scenario, AdresseType.BOSTEDSADRESSE, Landkoder.NOR, PersonstatusType.BOSA);
        scenario.medOppgittTilknytning()
                .medOpphold(List.of(fremtidigOppholdISverige))
                .medOppholdNå(true);
        var behandling = scenario.lagre(provider);

        // Act
        var resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_dersom_søker_har_søkt_termin_og_oppholder_seg_i_utland_i_under_12_fremtidige_måneder() {
        // Arrange
        var fødselsDato = LocalDate.now();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknad().medMottattDato(LocalDate.now());
        scenario.medDefaultOppgittTilknytning();
        scenario.medDefaultSøknadTerminbekreftelse();

        var fremtidigOppholdISverige = new MedlemskapOppgittLandOppholdEntitet.Builder()
                .erTidligereOpphold(false)
                .medLand(Landkoder.SWE)
                .medPeriode(LocalDate.now().plusDays(20), LocalDate.now().plusMonths(9))
                .build();

        leggTilSøker(scenario, AdresseType.BOSTEDSADRESSE, Landkoder.NOR, PersonstatusType.BOSA);
        scenario.medOppgittTilknytning()
                .medOpphold(List.of(fremtidigOppholdISverige))
                .medOppholdNå(true);
        var behandling = scenario.lagre(provider);

        // Act
        var resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_dersom_bare_ett_av_spørsmål_til_bruker_om_tilknytning_er_nei() {
        // Arrange
        var fødselsDato = LocalDate.now();

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medDefaultOppgittTilknytning();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now()).medAntallBarn(1);
        leggTilSøker(scenario, AdresseType.BOSTEDSADRESSE, Landkoder.NOR, PersonstatusType.BOSA);
        var behandling = scenario.lagre(provider);

        // Act
        var resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).isEmpty();
    }

    private void leggTilSøker(AbstractTestScenario<?> scenario, AdresseType adresseType, Landkoder adresseLand, PersonstatusType personstatus) {
        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();
        var persona = builderForRegisteropplysninger
                .medPersonas()
                .kvinne(søkerAktørId, SivilstandType.UOPPGITT, Region.UDEFINERT)
                .personstatus(personstatus)
                .statsborgerskap(adresseLand);

        var adresseBuilder = PersonAdresse.builder().adresselinje1("Portveien 2").land(adresseLand);
        persona.adresse(adresseType, adresseBuilder);
        var søker = persona.build();
        scenario.medRegisterOpplysninger(søker);
    }
}
