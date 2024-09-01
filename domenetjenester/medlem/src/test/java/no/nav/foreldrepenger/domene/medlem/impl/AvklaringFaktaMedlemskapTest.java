package no.nav.foreldrepenger.domene.medlem.impl;

import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.medlem.MedlemskapPerioderTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.konfig.Tid;

class AvklaringFaktaMedlemskapTest extends EntityManagerAwareTest {

    private static final LocalDate SKJÆRINGSDATO_FØDSEL = LocalDate.now().plusDays(1);

    private BehandlingRepositoryProvider provider;

    private InntektArbeidYtelseTjeneste iayTjeneste;

    private AvklaringFaktaMedlemskap tjeneste;

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        provider = new BehandlingRepositoryProvider(entityManager);
        var medlemskapPerioderTjeneste = new MedlemskapPerioderTjeneste();
        iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        var personopplysningTjeneste = new PersonopplysningTjeneste(new PersonopplysningRepository(entityManager));
        this.tjeneste = new AvklaringFaktaMedlemskap(provider, medlemskapPerioderTjeneste, personopplysningTjeneste, iayTjeneste);
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_ved_gyldig_medlems_periode() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        scenario.medSøknadHendelse().medFødselsDato(SKJÆRINGSDATO_FØDSEL);

        var gyldigPeriode = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.FTL_2_7_A) // hjemlet i bokstav a
            .medMedlemskapType(MedlemskapType.ENDELIG)
            .medErMedlem(true)
            .medPeriode(SKJÆRINGSDATO_FØDSEL, SKJÆRINGSDATO_FØDSEL)
            .build();
        scenario.leggTilMedlemskapPeriode(gyldigPeriode);

        leggTilSøker(scenario);
        var behandling = lagre(scenario);
        var ref = lagRef(behandling);

        // Act
        var resultat = tjeneste.utled(ref, lagStp(), behandling, SKJÆRINGSDATO_FØDSEL);

        // Assert
        assertThat(resultat).isEmpty();
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }

    private Skjæringstidspunkt lagStp() {
        return Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(SKJÆRINGSDATO_FØDSEL)
            .medUttaksintervall(new LocalDateInterval(SKJÆRINGSDATO_FØDSEL.minusWeeks(4), SKJÆRINGSDATO_FØDSEL.plusWeeks(4)))
            .medFørsteUttaksdato(SKJÆRINGSDATO_FØDSEL).build();
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_ved_dekningsgrad_lik_ikke_medlem() {
        // Arrange
        var gyldigPeriode = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.FTL_2_9_1_B)
            .medMedlemskapType(MedlemskapType.ENDELIG)
            .medErMedlem(true)
            .medPeriode(SKJÆRINGSDATO_FØDSEL, SKJÆRINGSDATO_FØDSEL)
            .build();

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilMedlemskapPeriode(gyldigPeriode);
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        scenario.medSøknadHendelse().medFødselsDato(SKJÆRINGSDATO_FØDSEL);
        leggTilSøker(scenario);
        var behandling = lagre(scenario);
        var ref = lagRef(behandling);

        // Act
        var resultat = tjeneste.utled(ref, lagStp(), behandling, SKJÆRINGSDATO_FØDSEL);


        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_få_aksjonspunkt_når_dekningsrad_er_av_type_uavklart() {
        // Arrange
        var gyldigPeriode = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.OPPHOR)
            .medMedlemskapType(MedlemskapType.ENDELIG)
            .medErMedlem(true)
            .medPeriode(SKJÆRINGSDATO_FØDSEL, SKJÆRINGSDATO_FØDSEL)
            .build();

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilMedlemskapPeriode(gyldigPeriode);
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        scenario.medSøknadHendelse().medFødselsDato(SKJÆRINGSDATO_FØDSEL);

        leggTilSøker(scenario);
        var behandling = lagre(scenario);
        var ref = lagRef(behandling);

        // Act
        var resultat = tjeneste.utled(ref, lagStp(), behandling, SKJÆRINGSDATO_FØDSEL);


        // Assert
        assertThat(resultat).contains(MedlemResultat.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE);
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_dersom_dekningsgrad_unntatt_og_person_bosatt_og_statsborgerskap_ulik_usa() {
        // Arrange
        var medlemskapPeriodeForUnntak = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.UNNTATT) // unntak FT §2-13
            .medMedlemskapType(MedlemskapType.ENDELIG)
            .medErMedlem(true)
            .medPeriode(SKJÆRINGSDATO_FØDSEL, SKJÆRINGSDATO_FØDSEL)
            .build();

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilMedlemskapPeriode(medlemskapPeriodeForUnntak);
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        scenario.medSøknadHendelse().medFødselsDato(SKJÆRINGSDATO_FØDSEL);
        leggTilSøker(scenario);
        var behandling = lagre(scenario);
        var ref = lagRef(behandling);

        // Act
        var resultat = tjeneste.utled(ref, lagStp(), behandling, SKJÆRINGSDATO_FØDSEL);


        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_dersom_dekningsgrad_unntatt_og_person_utvandret() {
        // Arrange
        var medlemskapPeriodeForUnntak = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.UNNTATT) // unntak FT §2-13
            .medMedlemskapType(MedlemskapType.ENDELIG)
            .medErMedlem(true)
            .medPeriode(SKJÆRINGSDATO_FØDSEL, SKJÆRINGSDATO_FØDSEL)
            .build();

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilMedlemskapPeriode(medlemskapPeriodeForUnntak);
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        scenario.medSøknadHendelse().medFødselsDato(SKJÆRINGSDATO_FØDSEL);
        leggTilSøker(scenario, Landkoder.USA, PersonstatusType.UTVA);
        var behandling = lagre(scenario);
        var ref = lagRef(behandling);

        // Act
        var resultat = tjeneste.utled(ref, lagStp(), behandling, SKJÆRINGSDATO_FØDSEL);


        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_opprette_aksjonspunkt_dersom_dekningsgrad_unntatt_og_person_bosatt_og_statsborgerskap_lik_usa() {
        // Arrange
        var medlemskapPeriodeForUnntak = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.UNNTATT)
            .medMedlemskapType(MedlemskapType.ENDELIG)
            .medErMedlem(true)
            .medPeriode(SKJÆRINGSDATO_FØDSEL, SKJÆRINGSDATO_FØDSEL)
            .build();

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilMedlemskapPeriode(medlemskapPeriodeForUnntak);
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        scenario.medSøknadHendelse().medFødselsDato(SKJÆRINGSDATO_FØDSEL);
        leggTilSøker(scenario, Landkoder.USA, PersonstatusType.BOSA);
        var behandling = lagre(scenario);
        var ref = lagRef(behandling);

        // Act
        var resultat = tjeneste.utled(ref, lagStp(), behandling, SKJÆRINGSDATO_FØDSEL);


        // Assert
        assertThat(resultat).contains(MedlemResultat.AVKLAR_LOVLIG_OPPHOLD);
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_ved_ikke_gyldig_periode_og_status_utvandret() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        scenario.medSøknadHendelse().medFødselsDato(SKJÆRINGSDATO_FØDSEL);
        leggTilSøker(scenario, Landkoder.NOR, PersonstatusType.UTVA);
        var behandling = lagre(scenario);
        var ref = lagRef(behandling);

        // Act
        var resultat = tjeneste.utled(ref, lagStp(), behandling, SKJÆRINGSDATO_FØDSEL);


        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_ved_ikke_gyldig_periode_og_ikke_utvandret_og_region_nordisk() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        scenario.medSøknadHendelse().medFødselsDato(SKJÆRINGSDATO_FØDSEL);
        leggTilSøker(scenario, Landkoder.SWE, PersonstatusType.BOSA);
        var behandling = lagre(scenario);
        var ref = lagRef(behandling);

        // Act
        var resultat = tjeneste.utled(ref, lagStp(), behandling, SKJÆRINGSDATO_FØDSEL);


        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_ved_ikke_gyldig_periode_og_ikke_utvandret_og_region_eøs_og_inntekt_siste_3mnd() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        scenario.medSøknadHendelse().medFødselsDato(SKJÆRINGSDATO_FØDSEL);
        var aktørId = leggTilSøker(scenario, Landkoder.BEL, PersonstatusType.BOSA);
        var behandling = lagre(scenario);

        // Arrange - inntekt
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER);
        var aktørInntekt = builder.getAktørInntektBuilder(aktørId);
        aktørInntekt.leggTilInntekt(InntektBuilder.oppdatere(empty())
            .medInntektsKilde(InntektsKilde.INNTEKT_OPPTJENING)
            .leggTilInntektspost(InntektspostBuilder.ny()
                .medBeløp(BigDecimal.TEN)
                .medInntektspostType(InntektspostType.LØNN)
                .medPeriode(SKJÆRINGSDATO_FØDSEL.minusMonths(2), SKJÆRINGSDATO_FØDSEL.minusMonths(1))));
        builder.leggTilAktørInntekt(aktørInntekt);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        var ref = lagRef(behandling);

        // Act
        var resultat = tjeneste.utled(ref, lagStp(), behandling, SKJÆRINGSDATO_FØDSEL);


        // Assert
        assertThat(resultat).isEmpty();
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(provider);
    }

    @Test
    void skal_opprette_aksjonspunkt_ved_ikke_gyldig_periode_og_ikke_utvandret_og_region_eøs_og_ikke_inntekt_siste_3mnd() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        scenario.medSøknadHendelse().medFødselsDato(SKJÆRINGSDATO_FØDSEL);
        leggTilSøker(scenario, Landkoder.BEL, PersonstatusType.BOSA);
        var behandling = lagre(scenario);
        var ref = lagRef(behandling);

        // Act
        var resultat = tjeneste.utled(ref, lagStp(), behandling, SKJÆRINGSDATO_FØDSEL);


        // Assert
        assertThat(resultat).contains(MedlemResultat.AVKLAR_OPPHOLDSRETT);
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_ved_ikke_gyldig_periode_og_ikke_utvandret_og_region_eøs_og_vurdering_etter_stp() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        scenario.medSøknadHendelse().medFødselsDato(SKJÆRINGSDATO_FØDSEL);
        leggTilSøker(scenario, Landkoder.BEL, PersonstatusType.BOSA);
        var behandling = lagre(scenario);
        var ref = lagRef(behandling);

        // Act
        var resultat = tjeneste.utled(ref, lagStp(), behandling, SKJÆRINGSDATO_FØDSEL.plusMonths(1));


        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_opprette_aksjonspunkt_ved_ikke_gyldig_periode_og_ikke_utvandret_og_region_annen() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        scenario.medSøknadHendelse().medFødselsDato(SKJÆRINGSDATO_FØDSEL);
        leggTilSøker(scenario, Landkoder.UDEFINERT, PersonstatusType.BOSA);
        var behandling = lagre(scenario);
        var ref = lagRef(behandling);

        // Act
        var resultat = tjeneste.utled(ref, lagStp(), behandling, SKJÆRINGSDATO_FØDSEL);


        // Assert
        assertThat(resultat).contains(MedlemResultat.AVKLAR_LOVLIG_OPPHOLD);
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_ved_region_eøs_med_opphold() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        scenario.medSøknadHendelse().medFødselsDato(SKJÆRINGSDATO_FØDSEL);
        leggTilSøker(scenario, Landkoder.BEL, PersonstatusType.BOSA, OppholdstillatelseType.MIDLERTIDIG, SKJÆRINGSDATO_FØDSEL.minusYears(1), SKJÆRINGSDATO_FØDSEL.plusYears(1));
        var behandling = lagre(scenario);
        var ref = lagRef(behandling);

        // Act
        var resultat = tjeneste.utled(ref, lagStp(), behandling, SKJÆRINGSDATO_FØDSEL);


        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_ved_region_3land_med_opphold() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        scenario.medSøknadHendelse().medFødselsDato(SKJÆRINGSDATO_FØDSEL);
        leggTilSøker(scenario, Landkoder.ARG, PersonstatusType.BOSA, OppholdstillatelseType.PERMANENT, SKJÆRINGSDATO_FØDSEL.minusYears(1), Tid.TIDENES_ENDE);
        var behandling = lagre(scenario);
        var ref = lagRef(behandling);

        // Act
        var resultat = tjeneste.utled(ref, lagStp(), behandling, SKJÆRINGSDATO_FØDSEL);


        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_opprette_aksjonspunkt_ved_region_3land_med_delvis_opphold() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        scenario.medSøknadHendelse().medFødselsDato(SKJÆRINGSDATO_FØDSEL);
        leggTilSøker(scenario, Landkoder.USA, PersonstatusType.BOSA, OppholdstillatelseType.MIDLERTIDIG, SKJÆRINGSDATO_FØDSEL.minusYears(1), SKJÆRINGSDATO_FØDSEL);
        var behandling = lagre(scenario);
        var ref = lagRef(behandling);

        // Act
        var resultat = tjeneste.utled(ref, lagStp(), behandling, SKJÆRINGSDATO_FØDSEL);


        // Assert
        assertThat(resultat).contains(MedlemResultat.AVKLAR_LOVLIG_OPPHOLD);
    }

    private void leggTilSøker(AbstractTestScenario<?> scenario) {
        leggTilSøker(scenario, Landkoder.NOR, PersonstatusType.BOSA);
    }

    private AktørId leggTilSøker(AbstractTestScenario<?> scenario, Landkoder statsborgerskap, PersonstatusType personstatus) {
        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();
        var søker = builderForRegisteropplysninger
            .medPersonas()
            .kvinne(søkerAktørId, SivilstandType.UOPPGITT)
            .personstatus(personstatus)
            .statsborgerskap(statsborgerskap)
            .build();
        scenario.medRegisterOpplysninger(søker);
        return søkerAktørId;
    }

    private AktørId leggTilSøker(AbstractTestScenario<?> scenario, Landkoder statsborgerskap, PersonstatusType personstatus,
                                 OppholdstillatelseType opphold, LocalDate oppholdFom, LocalDate oppholdTom) {
        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();
        var søker = builderForRegisteropplysninger
            .medPersonas()
            .kvinne(søkerAktørId, SivilstandType.UOPPGITT)
            .personstatus(personstatus)
            .statsborgerskap(statsborgerskap)
            .opphold(opphold, oppholdFom, oppholdTom)
            .build();
        scenario.medRegisterOpplysninger(søker);
        return søkerAktørId;
    }

}
