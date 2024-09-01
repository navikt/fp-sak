package no.nav.foreldrepenger.inngangsvilkaar.medlemskap;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapManuellVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.AvklarMedlemskapUtleder;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;

@CdiDbAwareTest
class MedlemskapsvilkårTest {

    public static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    @Inject
    private PersonopplysningTjeneste personopplysningTjeneste;


    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private InngangsvilkårMedlemskap vurderMedlemskapsvilkarEngangsstonad;
    private YrkesaktivitetBuilder yrkesaktivitetBuilder;

    @Inject
    private AvklarMedlemskapUtleder avklarMedlemskapUtleder;

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @BeforeEach
    public void before() {
        var skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider);
        var oversetter = new MedlemsvilkårOversetter(repositoryProvider, personopplysningTjeneste, iayTjeneste);
        this.vurderMedlemskapsvilkarEngangsstonad = new InngangsvilkårMedlemskap(oversetter, repositoryProvider.getBehandlingRepository(),
            avklarMedlemskapUtleder, skjæringstidspunktTjeneste);
    }

    /**
     * Input: - bruker manuelt avklart som ikke medlem (FP VK 2.13) = JA
     * <p>
     * Forventet: Ikke oppfylt, avslagsid 1020
     */
    @Test
    void skal_vurdere_manuell_avklart_ikke_medlem_som_vilkår_ikke_oppfylt() {
        // Arrange
        var scenario = lagTestScenario(MedlemskapDekningType.FTL_2_7_A, Landkoder.NOR, PersonstatusType.BOSA);
        scenario.medMedlemskap().medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.UNNTAK);
        var behandling = lagre(scenario);

        // Act
        var vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(vilkårData.vilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(vilkårData.vilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1020);
    }

    /**
     * Input: - bruker registrert som ikke medlem (FP VK 2.13) = JA
     * <p>
     * Forventet: Ikke oppfylt, avslagsid 1020
     */
    @Test
    void skal_vurdere_maskinelt_avklart_ikke_medlem_som_vilkår_ikke_oppfylt() {
        // Arrange
        var scenario = lagTestScenario(MedlemskapDekningType.FTL_2_6, Landkoder.NOR, PersonstatusType.BOSA);
        var behandling = lagre(scenario);

        // Act
        var vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(vilkårData.vilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(vilkårData.vilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1020);
    }

    /**
     * Input: - bruker registrert som ikke medlem (FP VK 2.13) = NEI - bruker
     * avklart som pliktig eller frivillig medlem (FP VK 2.2) = JA
     * <p>
     * Forventet: Oppfylt
     *
     */
    @Test
    void skal_vurdere_avklart_pliktig_medlem_som_vilkår_oppfylt() {
        // Arrange
        var scenario = lagTestScenario(MedlemskapDekningType.FTL_2_7_A, Landkoder.NOR, PersonstatusType.BOSA);
        scenario.medMedlemskap().medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.MEDLEM);
        leggTilSøker(scenario, PersonstatusType.BOSA, Landkoder.SWE);
        var behandling = lagre(scenario);

        // Act
        var vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling));

        var jsonNode = StandardJsonConfig.fromJsonAsTree(vilkårData.regelInput());
        var personStatusType = jsonNode.get("personStatusType").asText();

        // Assert
        assertThat(vilkårData.vilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(vilkårData.regelInput()).isNotEmpty();
        assertThat(personStatusType).isEqualTo("BOSA");
    }

    /**
     * Input: - bruker registrert som ikke medlem (FP VK 2.13) = NEI - bruker
     * avklart som pliktig eller frivillig medlem (FP VK 2.2) = NEI - bruker
     * registrert som utvandret (FP VK 2.1) = JA - bruker har relevant
     * arbeidsforhold og inntekt som dekker skjæringstidspunkt (FP_VK_2.2.1) = NEI
     * <p>
     * Forventet: Ikke oppfylt, avslagsid 1025
     */
    @Test
    void skal_vurdere_utvandret_som_vilkår_ikke_oppfylt_ingen_relevant_arbeid_og_inntekt() {
        // Arrange
        var scenario = lagTestScenario(MedlemskapDekningType.UNNTATT, Landkoder.NOR, PersonstatusType.UTVA);
        scenario.medMedlemskap().medBosattVurdering(false).medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.IKKE_RELEVANT);
        var behandling = lagre(scenario);

        // Act
        var vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(vilkårData.vilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(vilkårData.vilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1025);
    }

    /**
     * Input: - bruker registrert som ikke medlem (FP VK 2.13) = NEI - bruker
     * avklart som pliktig eller frivillig medlem (FP VK 2.2) = NEI - bruker
     * registrert som utvandret (FP VK 2.1) = JA - bruker har relevant
     * arbeidsforhold og inntekt som dekker skjæringstidspunkt (FP_VK_2.2.1) = JA
     * <p>
     * Forventet: oppfylt
     */
    @Test
    void skal_vurdere_utvandret_som_vilkår_oppfylt_når_relevant_arbeid_og_inntekt_finnes() {
        // Arrange
        var scenario = lagTestScenario(MedlemskapDekningType.UNNTATT, Landkoder.NOR, PersonstatusType.UTVA);
        scenario.medMedlemskap().medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.IKKE_RELEVANT);
        var behandling = lagre(scenario);

        opprettArbeidOgInntektForBehandling(behandling, SKJÆRINGSTIDSPUNKT.minusMonths(5), SKJÆRINGSTIDSPUNKT.plusDays(2));
        // Act
        var vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(vilkårData.vilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
    }

    /**
     * Input: - bruker registrert som ikke medlem (FP VK 2.13) = NEI - bruker
     * avklart som pliktig eller frivillig medlem (FP VK 2.2) = NEI - bruker
     * registrert som utvandret (FP VK 2.1) = NEI - bruker avklart som ikke bosatt =
     * JA - bruker har relevant arbeidsforhold og inntekt som dekker
     * skjæringstidspunkt (FP_VK_2.2.1) = JA
     * <p>
     * Forventet: Ikke oppfylt, avslagsid 1025
     */
    @Test
    void skal_vurdere_avklart_ikke_bosatt_som_vilkår_når_bruker_har_relevant_arbeid_og_inntekt() {
        // Arrange
        var landkode = Landkoder.POL;
        var scenario = lagTestScenario(MedlemskapDekningType.UNNTATT, landkode, PersonstatusType.BOSA);
        scenario.medMedlemskap().medBosattVurdering(false).medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.IKKE_RELEVANT);
        var behandling = lagre(scenario);
        opprettArbeidOgInntektForBehandling(behandling, SKJÆRINGSTIDSPUNKT.minusMonths(5), SKJÆRINGSTIDSPUNKT.plusDays(2));

        // Act
        var vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(vilkårData.vilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
    }

    /**
     * Input: - bruker registrert som ikke medlem (FP VK 2.13) = NEI - bruker
     * avklart som pliktig eller frivillig medlem (FP VK 2.2) = NEI - bruker
     * registrert som utvandret (FP VK 2.1) = NEI - bruker avklart som ikke bosatt =
     * JA - bruker har relevant arbeidsforhold og inntekt som dekker
     * skjæringstidspunkt (FP_VK_2.2.1) = NEI
     * <p>
     * Forventet: Ikke oppfylt, avslagsid 1025
     */
    @Test
    void skal_vurdere_avklart_ikke_bosatt_som_vilkår_når_bruker_har_ingen_relevant_arbeid_og_inntekt() {
        // Arrange
        var landkode = Landkoder.POL;
        var scenario = lagTestScenario(MedlemskapDekningType.UNNTATT, landkode, PersonstatusType.BOSA);
        scenario.medMedlemskap().medBosattVurdering(false).medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.IKKE_RELEVANT);
        var behandling = lagre(scenario);

        // Act
        var vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(vilkårData.vilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(vilkårData.vilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1025);
    }

    /**
     * Input: - bruker registrert som ikke medlem (FP VK 2.13) = NEI - bruker
     * avklart som pliktig eller frivillig medlem (FP VK 2.2) = NEI - bruker
     * registrert som utvandret (FP VK 2.1) = NEI - bruker avklart som ikke bosatt =
     * NEI - bruker oppgir opphold i norge (FP VK 2.3) = JA - bruker oppgir opphold
     * norge minst 12 mnd (FP VK 2.5) = JA - bruker norsk/nordisk statsborger i PDL
     * (FP VK 2.11) = JA
     * <p>
     * Forventet: oppfylt
     */
    @Test
    void skal_vurdere_norsk_nordisk_statsborger_som_vilkår_oppfylt() {
        // Arrange
        var scenario = lagTestScenario(MedlemskapDekningType.UDEFINERT, Landkoder.NOR, PersonstatusType.BOSA);
        leggTilSøker(scenario, PersonstatusType.BOSA, Landkoder.NOR);
        scenario.medMedlemskap().medBosattVurdering(true);
        var behandling = lagre(scenario);

        // Act
        var vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(vilkårData.vilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
    }

    /**
     * Input: - bruker registrert som ikke medlem (FP VK 2.13) = NEI - bruker
     * avklart som pliktig eller frivillig medlem (FP VK 2.2) = NEI - bruker
     * registrert som utvandret (FP VK 2.1) = NEI - bruker avklart som ikke bosatt =
     * NEI - bruker oppgir opphold i norge (FP VK 2.3) = JA - bruker oppgir opphold
     * norge minst 12 mnd (FP VK 2.5) = JA - bruker norsk/nordisk statsborger i PDL
     * (FP VK 2.11) = NEI - bruker EU/EØS statsborger = JA - bruker har avklart
     * oppholdsrett (FP VK 2.12) = JA
     * <p>
     * Forventet: oppfylt
     */
    @Test
    void skal_vurdere_eøs_statsborger_med_oppholdsrett_som_vilkår_oppfylt() {
        // Arrange
        var scenario = lagTestScenario(Landkoder.NOR, PersonstatusType.BOSA);
        leggTilSøker(scenario, PersonstatusType.BOSA, Landkoder.SWE);
        scenario.medMedlemskap().medBosattVurdering(true).medOppholdsrettVurdering(true);
        var behandling = lagre(scenario);

        // Act
        var vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(vilkårData.vilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
    }

    /**
     * Input: - bruker registrert som ikke medlem (FP VK 2.13) = NEI - bruker
     * avklart som pliktig eller frivillig medlem (FP VK 2.2) = NEI - bruker
     * registrert som utvandret (FP VK 2.1) = NEI - bruker avklart som ikke bosatt =
     * NEI - bruker oppgir opphold i norge (FP VK 2.3) = JA - bruker oppgir opphold
     * norge minst 12 mnd (FP VK 2.5) = JA - bruker norsk/nordisk statsborger i PDL
     * (FP VK 2.11) = NEI - bruker EU/EØS statsborger = JA - bruker har avklart
     * oppholdsrett (FP VK 2.12) = NEI
     * <p>
     * Forventet: Ikke oppfylt, avslagsid 1024
     */
    @Test
    void skal_vurdere_eøs_statsborger_uten_oppholdsrett_som_vilkår_ikke_oppfylt() {
        // Arrange
        var landkodeEOS = Landkoder.POL;
        var scenario = lagTestScenario(landkodeEOS, PersonstatusType.BOSA);
        scenario.medMedlemskap().medBosattVurdering(true).medOppholdsrettVurdering(false);
        var behandling = lagre(scenario);

        // Act
        var vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(vilkårData.vilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(vilkårData.vilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1024);
    }

    /**
     * Input: - bruker registrert som ikke medlem (FP VK 2.13) = NEI - bruker
     * avklart som pliktig eller frivillig medlem (FP VK 2.2) = NEI - bruker
     * registrert som utvandret (FP VK 2.1) = NEI - bruker avklart som ikke bosatt =
     * NEI - bruker oppgir opphold i norge (FP VK 2.3) = JA - bruker oppgir opphold
     * norge minst 12 mnd (FP VK 2.5) = JA - bruker norsk/nordisk statsborger i PDL
     * (FP VK 2.11) = NEI - bruker EU/EØS statsborger = NEI - bruker har avklart
     * lovlig opphold (FP VK 2.12) = NEI
     * <p>
     * Forventet: Ikke oppfylt, avslagsid 1023
     */
    @Test
    void skal_vurdere_annen_statsborger_uten_lovlig_opphold_som_vilkår_ikke_oppfylt() {
        // Arrange
        var land = Landkoder.ARG;
        var scenario = lagTestScenario(MedlemskapDekningType.UNNTATT, land, PersonstatusType.BOSA);
        scenario.medMedlemskap().medBosattVurdering(true).medLovligOppholdVurdering(false)
                .medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.IKKE_RELEVANT);
        var behandling = lagre(scenario);

        // Act
        var vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(vilkårData.vilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(vilkårData.vilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1023);
    }

    /**
     * Input: - bruker registrert som ikke medlem (FP VK 2.13) = NEI - bruker
     * avklart som pliktig eller frivillig medlem (FP VK 2.2) = NEI - bruker
     * registrert som utvandret (FP VK 2.1) = NEI - bruker avklart som ikke bosatt =
     * NEI - bruker oppgir opphold i norge (FP VK 2.3) = JA - bruker oppgir opphold
     * norge minst 12 mnd (FP VK 2.5) = JA - bruker norsk/nordisk statsborger i PDL
     * (FP VK 2.11) = NEI - bruker EU/EØS statsborger = NEI - bruker har avklart
     * lovlig opphold (FP VK 2.12) = NEI
     * <p>
     * Forventet: Ikke oppfylt, avslagsid 1023
     */
    @Test
    void skal_vurdere_annen_statsborger_med_oppholdstillatelse_som_vilkår_oppfylt() {
        // Arrange
        var scenario = lagTestScenario(Landkoder.NOR, PersonstatusType.BOSA);
        leggTilSøker(scenario, PersonstatusType.BOSA, Landkoder.ARG,
            OppholdstillatelseType.MIDLERTIDIG, SKJÆRINGSTIDSPUNKT.minusYears(1), SKJÆRINGSTIDSPUNKT.plusYears(1));
        scenario.medMedlemskap().medBosattVurdering(true)
            .medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.IKKE_RELEVANT);
        var behandling = lagre(scenario);

        // Act
        var vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(vilkårData.vilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
    }

    /**
     * Input: - bruker registrert som ikke medlem (FP VK 2.13) = NEI - bruker
     * avklart som pliktig eller frivillig medlem (FP VK 2.2) = NEI - bruker
     * registrert som utvandret (FP VK 2.1) = NEI - bruker avklart som ikke bosatt =
     * NEI - bruker oppgir opphold i norge (FP VK 2.3) = JA - bruker oppgir opphold
     * norge minst 12 mnd (FP VK 2.5) = JA - bruker norsk/nordisk statsborger i PDL
     * (FP VK 2.11) = NEI - bruker EU/EØS statsborger = NEI - bruker har avklart
     * lovlig opphold (FP VK 2.12) = JA
     * <p>
     * Forventet: oppfylt
     */
    @Test
    void skal_vurdere_annen_statsborger_med_lovlig_opphold_som_vilkår_oppfylt() {
        // Arrange
        var scenario = lagTestScenario(Landkoder.NOR, PersonstatusType.BOSA);
        leggTilSøker(scenario, PersonstatusType.BOSA, Landkoder.USA);
        scenario.medMedlemskap().medBosattVurdering(true).medLovligOppholdVurdering(true);
        var behandling = lagre(scenario);

        // Act
        var vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(vilkårData.vilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
    }

    /**
     * - bruker har relevant arbeidsforhold og inntekt som dekker skjæringstidspunkt
     * (FP_VK_2.2.1) = NEI
     */
    @Test
    void skal_få_medlemskapsvilkåret_satt_til_ikke_oppfylt_når_utva_og_ingen_relevant_arbeid_og_inntekt() {
        // Arrange

        var scenario = lagTestScenario(MedlemskapDekningType.FTL_2_9_1_C, Landkoder.NOR, PersonstatusType.UTVA);
        scenario.medMedlemskap().medBosattVurdering(false).medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.IKKE_RELEVANT);
        leggTilSøker(scenario, PersonstatusType.UREG, Landkoder.SWE);

        var behandling = lagre(scenario);

        // Act
        var vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(vilkårData.vilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(vilkårData.vilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1025);
    }

    /**
     * - bruker har relevant arbeidsforhold og inntekt som dekker skjæringstidspunkt
     * (FP_VK_2.2.1) = JA
     */
    @Test
    void skal_få_medlemskapsvilkåret_satt_til_ikke_oppfylt_når_ureg_og_relevant_arbeid_og_inntekt_finnes() {
        // Arrange

        var scenario = lagTestScenario(MedlemskapDekningType.FTL_2_9_1_C, Landkoder.NOR, PersonstatusType.UREG);
        scenario.medMedlemskap().medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.IKKE_RELEVANT);

        leggTilSøker(scenario, PersonstatusType.UREG, Landkoder.SWE);

        var behandling = lagre(scenario);

        opprettArbeidOgInntektForBehandling(behandling, SKJÆRINGSTIDSPUNKT.minusMonths(5), SKJÆRINGSTIDSPUNKT.plusDays(2));


        // Act
        var vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(vilkårData.vilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
    }

    /**
     * Lager minimalt testscenario med en medlemsperiode som indikerer om søker er
     * medlem eller ikke.
     */
    private ScenarioMorSøkerEngangsstønad lagTestScenario(MedlemskapDekningType dekningType, Landkoder statsborgerskap,
                                                          PersonstatusType personstatusType) {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
                .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                        .medTermindato(SKJÆRINGSTIDSPUNKT)
                        .medNavnPå("navn navnesen")
                        .medUtstedtDato(LocalDate.now()));
        if (dekningType != null) {
            scenario.leggTilMedlemskapPeriode(new MedlemskapPerioderBuilder()
                    .medDekningType(dekningType)
                    .medMedlemskapType(MedlemskapType.ENDELIG)
                    .medPeriode(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
                    .build());
        }

        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();

        var søker = builderForRegisteropplysninger
                .medPersonas()
                .kvinne(søkerAktørId, SivilstandType.GIFT)
                .personstatus(personstatusType)
                .statsborgerskap(statsborgerskap)
                .build();
        scenario.medRegisterOpplysninger(søker);
        return scenario;
    }

    private void leggTilSøker(ScenarioMorSøkerEngangsstønad scenario, PersonstatusType personstatus, Landkoder statsborgerskapLand) {
        leggTilSøker(scenario, personstatus, statsborgerskapLand, null, null, null);
    }

    private void leggTilSøker(ScenarioMorSøkerEngangsstønad scenario, PersonstatusType personstatus, Landkoder statsborgerskapLand,
                              OppholdstillatelseType opphold, LocalDate oppholdFom, LocalDate oppholdTom) {
        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var barnAktørId = AktørId.dummy();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();

        var fødtBarn = builderForRegisteropplysninger
                .medPersonas()
                .fødtBarn(barnAktørId, LocalDate.now().plusDays(7))
                .relasjonTil(søkerAktørId, RelasjonsRolleType.MORA, null)
                .build();

        var søker = builderForRegisteropplysninger
                .medPersonas()
                .kvinne(søkerAktørId, SivilstandType.GIFT)
                .statsborgerskap(statsborgerskapLand)
                .personstatus(personstatus)
                .relasjonTil(barnAktørId, RelasjonsRolleType.BARN, null);
        if (opphold != null) søker.opphold(opphold, oppholdFom, oppholdTom);
        scenario.medRegisterOpplysninger(søker.build());
        scenario.medRegisterOpplysninger(fødtBarn);
    }

    private ScenarioMorSøkerEngangsstønad lagTestScenario(Landkoder statsborgerskap, PersonstatusType personstatusType) {
        return lagTestScenario(null, statsborgerskap, personstatusType);
    }

    private void opprettArbeidOgInntektForBehandling(Behandling behandling, LocalDate fom, LocalDate tom) {

        var virksomhetOrgnr = "42";
        var aktørId = behandling.getAktørId();
        var aggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);

        lagAktørArbeid(aggregatBuilder, aktørId, virksomhetOrgnr, fom, tom, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, Optional.empty());
        for (var dt = fom; dt.isBefore(tom); dt = dt.plusMonths(1)) {
            lagInntekt(aggregatBuilder, aktørId, virksomhetOrgnr, dt, dt.plusMonths(1));
        }

        iayTjeneste.lagreIayAggregat(behandling.getId(), aggregatBuilder);
    }

    private AktørArbeid lagAktørArbeid(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder, AktørId aktørId, String virksomhetOrgnr,
            LocalDate fom, LocalDate tom, ArbeidType arbeidType, Optional<InternArbeidsforholdRef> arbeidsforholdRef) {
        var aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder
                .getAktørArbeidBuilder(aktørId);

        Opptjeningsnøkkel opptjeningsnøkkel;
        var arbeidsgiver = Arbeidsgiver.virksomhet(virksomhetOrgnr);
        if (arbeidsforholdRef.isPresent()) {
            opptjeningsnøkkel = new Opptjeningsnøkkel(arbeidsforholdRef.get(), arbeidsgiver);
        } else {
            opptjeningsnøkkel = Opptjeningsnøkkel.forOrgnummer(virksomhetOrgnr);
        }

        yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(opptjeningsnøkkel, arbeidType);
        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();

        var aktivitetsAvtale = aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));

        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(aktivitetsAvtale)
                .medArbeidType(arbeidType)
                .medArbeidsgiver(arbeidsgiver);

        yrkesaktivitetBuilder.medArbeidsforholdId(arbeidsforholdRef.orElse(null));

        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
        return aktørArbeidBuilder.build();
    }

    private void lagInntekt(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder, AktørId aktørId, String virksomhetOrgnr,
            LocalDate fom, LocalDate tom) {
        var opptjeningsnøkkel = Opptjeningsnøkkel.forOrgnummer(virksomhetOrgnr);

        var aktørInntektBuilder = inntektArbeidYtelseAggregatBuilder.getAktørInntektBuilder(aktørId);

        Stream.of(InntektsKilde.INNTEKT_BEREGNING, InntektsKilde.INNTEKT_SAMMENLIGNING, InntektsKilde.INNTEKT_OPPTJENING).forEach(kilde -> {
            var inntektBuilder = aktørInntektBuilder.getInntektBuilder(kilde, opptjeningsnøkkel);
            var inntektspost = InntektspostBuilder.ny()
                    .medBeløp(BigDecimal.valueOf(35000))
                    .medPeriode(fom, tom)
                    .medInntektspostType(InntektspostType.LØNN);
            inntektBuilder.leggTilInntektspost(inntektspost).medArbeidsgiver(yrkesaktivitetBuilder.build().getArbeidsgiver());
            aktørInntektBuilder.leggTilInntekt(inntektBuilder);
            inntektArbeidYtelseAggregatBuilder.leggTilAktørInntekt(aktørInntektBuilder);
        });
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }

}
