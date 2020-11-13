package no.nav.foreldrepenger.domene.uttak.kontroller.fakta.omsorg;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.personopplysning.PersonAdresse;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.personopplysning.PersonInformasjon.Builder;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class BrukerHarOmsorgAksjonspunktUtlederTest {

    private static final LocalDate TERMINDATO = LocalDate.now().plusMonths(3);
    private static final LocalDate FØDSELSDATO_NÅ = LocalDate.now();
    private static final Period FORBEHOLDT_MOR_ETTER_FØDSEL = Period.ofWeeks(6);

    private static final AktørId GITT_MOR_AKTØR_ID = AktørId.dummy();
    private static final AktørId GITT_BARN_ID = AktørId.dummy();
    private static final AktørId GITT_BARN_ID_2 = AktørId.dummy();
    private static final AktørId GITT_FAR_AKTØR_ID = AktørId.dummy();

    private UttakRepositoryProvider repositoryProvider;

    private BrukerHarOmsorgAksjonspunktUtleder aksjonspunktUtleder;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new UttakRepositoryProvider(entityManager);
        aksjonspunktUtleder = new BrukerHarOmsorgAksjonspunktUtleder(repositoryProvider,
            new PersonopplysningTjeneste(new PersonopplysningRepository(entityManager)), FORBEHOLDT_MOR_ETTER_FØDSEL);
    }

    @Test
    public void ingen_aksjonspunkt_dersom_bruker_oppgitt_omsorg_til_barnet_men_barnet_er_ikke_født() {
        // Arrange
        Behandling behandling = opprettBehandling(TERMINDATO);
        var familieHendelse = FamilieHendelse.forFødsel(TERMINDATO, null, List.of(new Barn()), 1);
        FamilieHendelser familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);
        // Act
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void aksjonspunkt_dersom_mor_søker_og_oppgitt_omsorg_til_barnet_og_fødsel_registrert_i_TPS_men_barn_har_ikke_sammebosted() {
        // Arrange
        Behandling behandling = opprettBehandlingForFødselRegistrertITps(FØDSELSDATO_NÅ);
        FamilieHendelser familieHendelser = fødselSøknadOgBekreftetStemmer();

        // Act
        var utledeteAksjonspunkter = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));
        // Assert
        assertThat(utledeteAksjonspunkter).containsExactly(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG);
    }

    private FamilieHendelser fødselSøknadOgBekreftetStemmer() {
        var familieHendelse = FamilieHendelse.forFødsel(null, FØDSELSDATO_NÅ, List.of(new Barn()), 1);
        return new FamilieHendelser().medSøknadHendelse(familieHendelse).medBekreftetHendelse(familieHendelse);
    }

    private UttakInput lagInput(Behandling behandling, FamilieHendelser familieHendelser) {
        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(FØDSELSDATO_NÅ).build();
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        return new UttakInput(ref, null, new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser));
    }

    @Test
    public void aksjonspunkt_dersom_far_søker_og_oppgitt_omsorg_til_barnet_og_fødsel_registrert_i_TPS_og_barn_har_sammebosted_med_mor_ikke_far() {
        // Arrange
        Behandling behandling = opprettBehandlingForFødselRegistrertITpsOgBarnBorSammenMedMorIkkeFarOgFarSøker(FØDSELSDATO_NÅ);
        var familieHendelser = fødselSøknadOgBekreftetStemmer();

        // Act
        var utledeteAksjonspunkter = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));
        // Assert
        assertThat(utledeteAksjonspunkter).containsExactly(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG);
    }

    @Test
    public void aksjonspunkt_dersom_mor_søker_og_oppgitt_omsorg_til_barnet_og_fødsel_registrert_i_TPS_og_barn_har_sammebosted_med_far_ikke_mor() {
        // Arrange
        Behandling behandling = opprettBehandlingForFødselRegistrertITpsOgBarnBorSammenMedFarIkkeMorOgMorSøker(FØDSELSDATO_NÅ);
        var familieHendelser = fødselSøknadOgBekreftetStemmer();

        // Act
        var utledeteAksjonspunkter = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));
        // Assert
        assertThat(utledeteAksjonspunkter).containsExactly(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG);
    }

    @Test
    public void ingen_aksjonspunkt_dersom_mor_søker_og_oppgitt_omsorg_til_barnet_og_fødsel_registrert_i_TPS_og_barn_har_sammebosted_med_mor_ikke_far() {
        // Arrange
        Behandling behandling = opprettBehandlingForFødselRegistrertITpsOgBarnBorSammenMedMorIkkeFarOgMorSøker(FØDSELSDATO_NÅ);
        var familieHendelser = fødselSøknadOgBekreftetStemmer();

        // Act
        var utledeteAksjonspunkter = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));
        // Assert
        assertThat(utledeteAksjonspunkter).isEmpty();
    }

    @Test
    public void aksjonspunkt_dersom_mor_søker_og_ikke_oppgitt_omsorg_til_barnet_med_lengre_søknadsperioden() {
        // Arrange
        OppgittPeriodeEntitet periode1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(FØDSELSDATO_NÅ, FØDSELSDATO_NÅ.plusWeeks(6))
            .build();

        OppgittPeriodeEntitet periode2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(FØDSELSDATO_NÅ.plusWeeks(6).plusDays(1), FØDSELSDATO_NÅ.plusWeeks(10))
            .build();
        Behandling behandling = opprettBehandlingForBekreftetFødselMedSøknadsperioder(FØDSELSDATO_NÅ, List.of(periode1, periode2));
        // Act
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, fødselSøknadOgBekreftetStemmer()));

        // Assert
        assertThat(aksjonspunktResultater).containsExactly(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG);
    }

    @Test
    public void ingen_aksjonspunkt_dersom_mor_søker_og_ikke_oppgitt_omsorg_til_barnet_med_kortere_søknadsperioden() {
        // Arrange
        OppgittPeriodeEntitet periode1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(FØDSELSDATO_NÅ, FØDSELSDATO_NÅ.plusWeeks(5))
            .build();

        Behandling behandling = opprettBehandlingForBekreftetFødselMedSøknadsperioder(FØDSELSDATO_NÅ, List.of(periode1));
        // Act
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, fødselSøknadOgBekreftetStemmer()));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void ingen_aksjonspunkt_dersom_barn_er_død_selvom_de_ikke_har_samme_adresse() {
        // Arrange
        Behandling behandling = opprettBehandlingForFødselOgDødRegistrertITpsEttBarn(FØDSELSDATO_NÅ, FØDSELSDATO_NÅ);
        FamilieHendelser familieHendelser = new FamilieHendelser()
            .medSøknadHendelse(FamilieHendelse.forFødsel(null, FØDSELSDATO_NÅ, List.of(new Barn()), 1))
            .medBekreftetHendelse(FamilieHendelse.forFødsel(null, FØDSELSDATO_NÅ, List.of(new Barn(FØDSELSDATO_NÅ)), 1));

        // Act
        var utledeteAksjonspunkter = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(utledeteAksjonspunkter).isEmpty();
    }

    @Test
    public void aksjonspunkt_dersom_ikke_alle_barn_er_død_fordi_det_levende_barnet_ikke_har_samme_bostedsadresse() {
        // Arrange
        Behandling behandling = opprettBehandlingForFødselOgDødRegistrertITpsFlereBarn(FØDSELSDATO_NÅ, FØDSELSDATO_NÅ);
        var familieHendelse = FamilieHendelse.forFødsel(null, FØDSELSDATO_NÅ, List.of(new Barn(FØDSELSDATO_NÅ), new Barn()), 1);
        FamilieHendelser familieHendelser = new FamilieHendelser().medBekreftetHendelse(familieHendelse);

        // Act
        var utledeteAksjonspunkter = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(utledeteAksjonspunkter).containsExactly(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG);
    }

    @Test
    public void ikke_aksjonspunkt_dersom_ett_barn_døde_og_ikke_har_samme_adresse_fordi_det_andre_barnet_lever_og_har_samme_bosted() {
        // Arrange
        Behandling behandling = opprettBehandlingForFødselOgDødRegistrertITpsSammeBosted(FØDSELSDATO_NÅ, FØDSELSDATO_NÅ);
        var familieHendelse = FamilieHendelse.forFødsel(null, FØDSELSDATO_NÅ, List.of(new Barn(FØDSELSDATO_NÅ), new Barn()), 2);
        FamilieHendelser familieHendelser = new FamilieHendelser().medBekreftetHendelse(familieHendelse);

        // Act
        var utledeteAksjonspunkter = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(utledeteAksjonspunkter).isEmpty();
    }

    @Test
    public void ikke_aksjonspunkt_dersom_ett_barn_døde_og_ikke_har_samme_adresse_fordi_det_andre_barnet_lever_og_har_samme_bostedsadresse() {
        // Arrange
        Behandling behandling = opprettBehandlingForFødselOgDødRegistrertITpsMedLikBostedsadresse(FØDSELSDATO_NÅ, FØDSELSDATO_NÅ);
        FamilieHendelser familieHendelser = new FamilieHendelser()
            .medSøknadHendelse(FamilieHendelse.forFødsel(null, FØDSELSDATO_NÅ, List.of( new Barn(), new Barn()), 2))
            .medBekreftetHendelse(FamilieHendelse.forFødsel(null, FØDSELSDATO_NÅ, List.of( new Barn(FØDSELSDATO_NÅ), new Barn()), 2));

        // Act
        var utledeteAksjonspunkter = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(utledeteAksjonspunkter).isEmpty();
    }

    private Behandling opprettBehandling(LocalDate førsteUttaksdato) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger
            .forFødselMedGittAktørId(GITT_MOR_AKTØR_ID);
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(førsteUttaksdato).build());

        PersonInformasjon søker = scenario.opprettBuilderForRegisteropplysninger()
        .medPersonas()
        .kvinne(GITT_MOR_AKTØR_ID, SivilstandType.GIFT, Region.NORDEN)
            .build();
        scenario.medRegisterOpplysninger(søker);

        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(true, true, false);
        scenario.medOppgittRettighet(rettighet);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingForFødselRegistrertITps(LocalDate fødselsdato) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger
            .forFødselMedGittAktørId(GITT_MOR_AKTØR_ID);

        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(fødselsdato).build());
        Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();

        PersonInformasjon fødtBarn = builderForRegisteropplysninger
            .medPersonas()
            .fødtBarn(GITT_BARN_ID, fødselsdato)
            .relasjonTil(GITT_MOR_AKTØR_ID, RelasjonsRolleType.MORA, false)
            .build();
        scenario.medRegisterOpplysninger(fødtBarn);

        PersonInformasjon søker = builderForRegisteropplysninger
        .medPersonas()
        .kvinne(GITT_MOR_AKTØR_ID, SivilstandType.GIFT, Region.NORDEN)
            .relasjonTil(GITT_BARN_ID, RelasjonsRolleType.BARN, false)
            .build();

        scenario.medRegisterOpplysninger(søker);

        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(true, true, false);
        scenario.medOppgittRettighet(rettighet);

        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingForFødselOgDødRegistrertITpsEttBarn(LocalDate fødselsdato, LocalDate dødsdato) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger
            .forFødselMedGittAktørId(GITT_MOR_AKTØR_ID);

        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(fødselsdato).build());
        Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();

        PersonInformasjon fødtBarn = builderForRegisteropplysninger
            .medPersonas()
            .fødtBarn(GITT_BARN_ID, fødselsdato)
            .dødsdato(dødsdato)
            .relasjonTil(GITT_MOR_AKTØR_ID, RelasjonsRolleType.MORA, false)
            .build();
        scenario.medRegisterOpplysninger(fødtBarn);

        PersonInformasjon søker = builderForRegisteropplysninger
        .medPersonas()
        .kvinne(GITT_MOR_AKTØR_ID, SivilstandType.GIFT, Region.NORDEN)
            .relasjonTil(GITT_BARN_ID, RelasjonsRolleType.BARN, false)
            .build();

        scenario.medRegisterOpplysninger(søker);

        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(true, true, false);
        scenario.medOppgittRettighet(rettighet);

        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingForFødselOgDødRegistrertITpsFlereBarn(LocalDate fødselsdato, LocalDate dødsdato) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger
            .forFødselMedGittAktørId(GITT_MOR_AKTØR_ID);

        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(fødselsdato).build());
        Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();

        PersonInformasjon fødtBarn = builderForRegisteropplysninger
            .medPersonas()
            .fødtBarn(GITT_BARN_ID, fødselsdato)
            .dødsdato(dødsdato)
            .relasjonTil(GITT_MOR_AKTØR_ID, RelasjonsRolleType.MORA, false)
            .build();
        scenario.medRegisterOpplysninger(fødtBarn);

        PersonInformasjon søker = builderForRegisteropplysninger
        .medPersonas()
        .kvinne(GITT_MOR_AKTØR_ID, SivilstandType.GIFT, Region.NORDEN)
            .relasjonTil(GITT_BARN_ID, RelasjonsRolleType.BARN, false)
            .build();

        scenario.medRegisterOpplysninger(søker);

        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(true, true, false);
        scenario.medOppgittRettighet(rettighet);

        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingForFødselOgDødRegistrertITpsSammeBosted(LocalDate fødselsdato, LocalDate dødsdato) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger
            .forFødselMedGittAktørId(GITT_MOR_AKTØR_ID);

        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(fødselsdato).build());
        Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();

        PersonAdresse.Builder bostedsadresse = PersonAdresse.builder().adresselinje1("Portveien 2").postnummer("7000").land(Landkoder.NOR);

        PersonInformasjon fødtBarn = builderForRegisteropplysninger
            .medPersonas()
            .dødsdato(dødsdato)
            .fødtBarn(GITT_BARN_ID, fødselsdato)
            .relasjonTil(GITT_MOR_AKTØR_ID, RelasjonsRolleType.MORA, false) //false
            .build();
        scenario.medRegisterOpplysninger(fødtBarn);

        PersonInformasjon fødtBarn2 = builderForRegisteropplysninger
            .medPersonas()
            .fødtBarn(GITT_BARN_ID_2, fødselsdato)
            .bostedsadresse(bostedsadresse)
            .relasjonTil(GITT_MOR_AKTØR_ID, RelasjonsRolleType.MORA, true) //true
            .build();
        scenario.medRegisterOpplysninger(fødtBarn2);

        PersonInformasjon søker = builderForRegisteropplysninger
        .medPersonas()
        .kvinne(GITT_MOR_AKTØR_ID, SivilstandType.GIFT, Region.NORDEN)
        .bostedsadresse(bostedsadresse)
            .relasjonTil(GITT_BARN_ID, RelasjonsRolleType.BARN, false) //false
            .relasjonTil(GITT_BARN_ID_2, RelasjonsRolleType.BARN, true) //true
            .build();

        scenario.medRegisterOpplysninger(søker);

        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(true, true, false);
        scenario.medOppgittRettighet(rettighet);

        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingForFødselOgDødRegistrertITpsMedLikBostedsadresse(LocalDate fødselsdato, LocalDate dødsdato) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger
            .forFødselMedGittAktørId(GITT_MOR_AKTØR_ID);

        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(fødselsdato).build());
        Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();

        PersonAdresse.Builder bostedsadresse = PersonAdresse.builder().adresselinje1("Portveien 2").postnummer("7000").land(Landkoder.NOR);


        PersonInformasjon fødtBarn = builderForRegisteropplysninger
            .medPersonas()
            .dødsdato(dødsdato)
            .fødtBarn(GITT_BARN_ID, fødselsdato)
            .relasjonTil(GITT_MOR_AKTØR_ID, RelasjonsRolleType.MORA, null)
            .build();
        scenario.medRegisterOpplysninger(fødtBarn);

        PersonInformasjon fødtBarn2 = builderForRegisteropplysninger
            .medPersonas()
            .fødtBarn(GITT_BARN_ID_2, fødselsdato)
            .bostedsadresse(bostedsadresse)
            .relasjonTil(GITT_MOR_AKTØR_ID, RelasjonsRolleType.MORA, null)
            .build();
        scenario.medRegisterOpplysninger(fødtBarn2);

        PersonInformasjon søker = builderForRegisteropplysninger
        .medPersonas()
        .kvinne(GITT_MOR_AKTØR_ID, SivilstandType.GIFT, Region.NORDEN)
        .bostedsadresse(bostedsadresse)
            .relasjonTil(GITT_BARN_ID, RelasjonsRolleType.BARN, null)
            .relasjonTil(GITT_BARN_ID_2, RelasjonsRolleType.BARN, null)
            .build();

        scenario.medRegisterOpplysninger(søker);

        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(true, true, false);
        scenario.medOppgittRettighet(rettighet);

        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingForFødselRegistrertITpsOgBarnBorSammenMedMorIkkeFarOgFarSøker(LocalDate fødselsdato) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger
            .forFødselMedGittAktørId(GITT_FAR_AKTØR_ID);

        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(fødselsdato).build());
        Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();

        PersonInformasjon fødtBarn = builderForRegisteropplysninger
            .medPersonas()
            .fødtBarn(GITT_BARN_ID, fødselsdato)
            .relasjonTil(GITT_MOR_AKTØR_ID, RelasjonsRolleType.MORA, true)
            .relasjonTil(GITT_FAR_AKTØR_ID, RelasjonsRolleType.FARA, false)
            .build();
        scenario.medRegisterOpplysninger(fødtBarn);

        PersonInformasjon søker = builderForRegisteropplysninger
        .medPersonas()
        .mann(GITT_FAR_AKTØR_ID, SivilstandType.GIFT, Region.NORDEN)
            .relasjonTil(GITT_MOR_AKTØR_ID, RelasjonsRolleType.EKTE, false)
            .relasjonTil(GITT_BARN_ID, RelasjonsRolleType.BARN, false)
            .build();

        scenario.medRegisterOpplysninger(søker);

        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(true, true, false);
        scenario.medOppgittRettighet(rettighet);

        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingForFødselRegistrertITpsOgBarnBorSammenMedFarIkkeMorOgMorSøker(LocalDate fødselsdato) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger
            .forFødselMedGittAktørId(GITT_MOR_AKTØR_ID);

        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(fødselsdato).build());
        Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();

        PersonInformasjon fødtBarn = builderForRegisteropplysninger
            .medPersonas()
            .fødtBarn(GITT_BARN_ID, fødselsdato)
            .relasjonTil(GITT_MOR_AKTØR_ID, RelasjonsRolleType.MORA, true)
            .relasjonTil(GITT_FAR_AKTØR_ID, RelasjonsRolleType.FARA, true)
            .build();
        scenario.medRegisterOpplysninger(fødtBarn);

        PersonInformasjon fødtBarn2 = builderForRegisteropplysninger
            .medPersonas()
            .fødtBarn(GITT_BARN_ID, fødselsdato)
            .dødsdato(fødselsdato)
            .relasjonTil(GITT_MOR_AKTØR_ID, RelasjonsRolleType.MORA, false)
            .relasjonTil(GITT_FAR_AKTØR_ID, RelasjonsRolleType.FARA, true)
            .build();
        scenario.medRegisterOpplysninger(fødtBarn2);

        PersonInformasjon søker = builderForRegisteropplysninger
        .medPersonas()
        .kvinne(GITT_MOR_AKTØR_ID, SivilstandType.GIFT, Region.NORDEN)
            .relasjonTil(GITT_FAR_AKTØR_ID, RelasjonsRolleType.EKTE, false)
            .relasjonTil(GITT_BARN_ID, RelasjonsRolleType.BARN, false)
            .build();

        scenario.medRegisterOpplysninger(søker);

        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(true, true, false);
        scenario.medOppgittRettighet(rettighet);

        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingForFødselRegistrertITpsOgBarnBorSammenMedMorIkkeFarOgMorSøker(LocalDate fødselsdato) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger
            .forFødselMedGittAktørId(GITT_MOR_AKTØR_ID);

        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(fødselsdato).build());
        Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();

        PersonInformasjon fødtBarn = builderForRegisteropplysninger
            .medPersonas()
            .fødtBarn(GITT_BARN_ID, fødselsdato)
            .relasjonTil(GITT_MOR_AKTØR_ID, RelasjonsRolleType.MORA, true)
            .relasjonTil(GITT_FAR_AKTØR_ID, RelasjonsRolleType.FARA, false)
            .build();
        scenario.medRegisterOpplysninger(fødtBarn);

        PersonInformasjon søker = builderForRegisteropplysninger
        .medPersonas()
        .kvinne(GITT_MOR_AKTØR_ID, SivilstandType.GIFT, Region.NORDEN)
            .relasjonTil(GITT_FAR_AKTØR_ID, RelasjonsRolleType.EKTE, false)
            .relasjonTil(GITT_BARN_ID, RelasjonsRolleType.BARN, true)
            .build();

        scenario.medRegisterOpplysninger(søker);

        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(true, true, false);
        scenario.medOppgittRettighet(rettighet);

        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingForBekreftetFødselMedSøknadsperioder(LocalDate fødselsdato, List<OppgittPeriodeEntitet> søknadsPerioder) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger
            .forFødselMedGittAktørId(GITT_MOR_AKTØR_ID);

        Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();

        PersonInformasjon fødtBarn = builderForRegisteropplysninger
            .medPersonas()
            .fødtBarn(GITT_BARN_ID, fødselsdato)
            .relasjonTil(GITT_MOR_AKTØR_ID, RelasjonsRolleType.MORA, false)
            .build();
        scenario.medRegisterOpplysninger(fødtBarn);

        PersonInformasjon søker = builderForRegisteropplysninger
        .medPersonas()
        .kvinne(GITT_MOR_AKTØR_ID, SivilstandType.GIFT, Region.NORDEN)
            .relasjonTil(GITT_BARN_ID, RelasjonsRolleType.BARN, false)
            .build();
        scenario.medRegisterOpplysninger(søker);

        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(true, false, false);
        scenario.medOppgittRettighet(rettighet);

        OppgittFordelingEntitet fordeling = new OppgittFordelingEntitet(søknadsPerioder, true);
        scenario.medFordeling(fordeling);

        return scenario.lagre(repositoryProvider);
    }

}
