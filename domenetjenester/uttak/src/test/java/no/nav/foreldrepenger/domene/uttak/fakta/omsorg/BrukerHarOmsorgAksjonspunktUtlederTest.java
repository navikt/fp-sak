package no.nav.foreldrepenger.domene.uttak.fakta.omsorg;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.PersonopplysningerForUttak;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

public class BrukerHarOmsorgAksjonspunktUtlederTest {

    private static final LocalDate TERMINDATO = LocalDate.now().plusMonths(3);
    private static final LocalDate FØDSELSDATO = LocalDate.now();

    private static final AktørId GITT_MOR_AKTØR_ID = AktørId.dummy();
    private static final AktørId GITT_FAR_AKTØR_ID = AktørId.dummy();

    private UttakRepositoryProvider repositoryProvider;

    private BrukerHarOmsorgAksjonspunktUtleder aksjonspunktUtleder;
    private PersonopplysningerForUttak personopplysninger;

    @BeforeEach
    void setUp() {
        repositoryProvider = new UttakRepositoryStubProvider();
        personopplysninger = mock(PersonopplysningerForUttak.class);
        aksjonspunktUtleder = new BrukerHarOmsorgAksjonspunktUtleder(personopplysninger);
    }

    @Test
    public void ingen_aksjonspunkt_dersom_bruker_oppgitt_omsorg_til_barnet_men_barnet_er_ikke_født() {
        // Arrange
        var behandling = opprettBehandling(TERMINDATO);
        var familieHendelse = FamilieHendelse.forFødsel(TERMINDATO, null, List.of(new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);
        // Act
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void aksjonspunkt_dersom_mor_søker_og_oppgitt_omsorg_til_barnet_og_fødsel_men_barn_har_ikke_sammebosted() {
        // Arrange
        var behandling = opprettBehandling(FØDSELSDATO);
        var familieHendelser = fødselSøknadOgBekreftetStemmer();

        // Act
        var utledeteAksjonspunkter = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));
        // Assert
        assertThat(utledeteAksjonspunkter).containsExactly(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG);
    }

    private FamilieHendelser fødselSøknadOgBekreftetStemmer() {
        var familieHendelse = FamilieHendelse.forFødsel(null, FØDSELSDATO, List.of(new Barn()), 1);
        return new FamilieHendelser().medSøknadHendelse(familieHendelse).medBekreftetHendelse(familieHendelse);
    }

    private UttakInput lagInput(Behandling behandling, FamilieHendelser familieHendelser) {
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(FØDSELSDATO).build();
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        return new UttakInput(ref, null, new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser));
    }

    @Test
    public void aksjonspunkt_dersom_far_søker_og_oppgitt_omsorg_til_barnet_og_fødsel_og_barn_har_sammebosted_med_mor_ikke_far() {
        // Arrange
        var behandling = opprettBehandlingForFødselOgBarnBorSammenMedMorIkkeFarOgFarSøker();
        var familieHendelser = fødselSøknadOgBekreftetStemmer();

        // Act
        var utledeteAksjonspunkter = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));
        // Assert
        assertThat(utledeteAksjonspunkter).containsExactly(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG);
    }

    @Test
    public void aksjonspunkt_dersom_mor_søker_og_oppgitt_omsorg_til_barnet_og_fødsel_og_barn_har_sammebosted_med_far_ikke_mor() {
        // Arrange
        var behandling = opprettBehandlingForFødselOgBarnBorSammenMedFarIkkeMorOgMorSøker(FØDSELSDATO);
        var familieHendelser = fødselSøknadOgBekreftetStemmer();

        // Act
        var utledeteAksjonspunkter = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));
        // Assert
        assertThat(utledeteAksjonspunkter).containsExactly(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG);
    }

    @Test
    public void ingen_aksjonspunkt_dersom_mor_søker_og_oppgitt_omsorg_til_barnet_og_fødsel_og_barn_har_sammebosted_med_mor_ikke_far() {
        // Arrange
        var behandling = opprettBehandlingForFødselOgBarnBorSammenMedMorIkkeFarOgMorSøker(FØDSELSDATO);
        var familieHendelser = fødselSøknadOgBekreftetStemmer();

        // Act
        var utledeteAksjonspunkter = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));
        // Assert
        assertThat(utledeteAksjonspunkter).isEmpty();
    }

    @Test
    public void aksjonspunkt_dersom_mor_søker_og_ikke_oppgitt_omsorg_til_barnet_med_lengre_søknadsperioden() {
        // Arrange
        var periode1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(FØDSELSDATO, FØDSELSDATO.plusWeeks(6))
            .build();

        var periode2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(FØDSELSDATO.plusWeeks(6).plusDays(1), FØDSELSDATO.plusWeeks(10))
            .build();
        var behandling = opprettBehandlingForBekreftetFødselMedSøknadsperioder(List.of(periode1, periode2));
        // Act
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, fødselSøknadOgBekreftetStemmer()));

        // Assert
        assertThat(aksjonspunktResultater).containsExactly(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG);
    }

    @Test
    public void ingen_aksjonspunkt_dersom_barn_er_død_selvom_de_ikke_har_samme_adresse() {
        // Arrange
        var behandling = opprettBehandling(FØDSELSDATO);
        var familieHendelser = new FamilieHendelser()
            .medSøknadHendelse(FamilieHendelse.forFødsel(null, FØDSELSDATO, List.of(new Barn()), 1))
            .medBekreftetHendelse(FamilieHendelse.forFødsel(null, FØDSELSDATO, List.of(new Barn(FØDSELSDATO)), 1));

        // Act
        var utledeteAksjonspunkter = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(utledeteAksjonspunkter).isEmpty();
    }

    @Test
    public void aksjonspunkt_dersom_ikke_alle_barn_er_død_fordi_det_levende_barnet_ikke_har_samme_bostedsadresse() {
        // Arrange
        var behandling = opprettBehandling(FØDSELSDATO);
        var familieHendelse = FamilieHendelse.forFødsel(null, FØDSELSDATO, List.of(new Barn(FØDSELSDATO), new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medBekreftetHendelse(familieHendelse);

        // Act
        var utledeteAksjonspunkter = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(utledeteAksjonspunkter).containsExactly(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG);
    }

    @Test
    public void ikke_aksjonspunkt_dersom_ett_barn_døde_og_ikke_har_samme_adresse_fordi_det_andre_barnet_lever_og_har_samme_bosted() {
        // Arrange
        var behandling = opprettBehandlingForFødselSammeBosted(FØDSELSDATO);
        var familieHendelse = FamilieHendelse.forFødsel(null, FØDSELSDATO, List.of(new Barn(FØDSELSDATO), new Barn()), 2);
        var familieHendelser = new FamilieHendelser().medBekreftetHendelse(familieHendelse);

        // Act
        var utledeteAksjonspunkter = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(utledeteAksjonspunkter).isEmpty();
    }

    @Test
    public void ikke_aksjonspunkt_dersom_ett_barn_døde_og_ikke_har_samme_adresse_fordi_det_andre_barnet_lever_og_har_samme_bostedsadresse() {
        // Arrange
        var behandling = opprettBehandlingForFødselMedLikBostedsadresse(FØDSELSDATO);
        var familieHendelser = new FamilieHendelser()
            .medSøknadHendelse(FamilieHendelse.forFødsel(null, FØDSELSDATO, List.of( new Barn(), new Barn()), 2))
            .medBekreftetHendelse(FamilieHendelse.forFødsel(null, FØDSELSDATO, List.of( new Barn(FØDSELSDATO), new Barn()), 2));

        // Act
        var utledeteAksjonspunkter = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(utledeteAksjonspunkter).isEmpty();
    }

    private Behandling opprettBehandling(LocalDate førsteUttaksdato) {
        var scenario = ScenarioMorSøkerForeldrepenger
            .forFødselMedGittAktørId(GITT_MOR_AKTØR_ID);
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(førsteUttaksdato).build());

        var rettighet = new OppgittRettighetEntitet(true, false, false, false);
        scenario.medOppgittRettighet(rettighet);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingForFødselSammeBosted(LocalDate fødselsdato) {
        var behandling = opprettBehandling(fødselsdato);

        when(personopplysninger.barnHarSammeBosted(BehandlingReferanse.fra(behandling))).thenReturn(true);
        return behandling;
    }

    private Behandling opprettBehandlingForFødselMedLikBostedsadresse(LocalDate fødselsdato) {
       return opprettBehandlingForFødselSammeBosted(fødselsdato);
    }

    private Behandling opprettBehandlingForFødselOgBarnBorSammenMedMorIkkeFarOgFarSøker() {
        var scenario = ScenarioMorSøkerForeldrepenger
            .forFødselMedGittAktørId(GITT_FAR_AKTØR_ID);
        var rettighet = new OppgittRettighetEntitet(true, false, false, false);
        scenario.medOppgittRettighet(rettighet);

        var behandling = scenario.lagre(repositoryProvider);

        when(personopplysninger.barnHarSammeBosted(BehandlingReferanse.fra(behandling))).thenReturn(false);

        return behandling;
    }

    private Behandling opprettBehandlingForFødselOgBarnBorSammenMedFarIkkeMorOgMorSøker(LocalDate fødselsdato) {
        var scenario = ScenarioMorSøkerForeldrepenger
            .forFødselMedGittAktørId(GITT_MOR_AKTØR_ID);

        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(fødselsdato).build());

        var rettighet = new OppgittRettighetEntitet(true, false, false, false);
        scenario.medOppgittRettighet(rettighet);

        var behandling = scenario.lagre(repositoryProvider);

        when(personopplysninger.barnHarSammeBosted(BehandlingReferanse.fra(behandling))).thenReturn(false);

        return behandling;
    }

    private Behandling opprettBehandlingForFødselOgBarnBorSammenMedMorIkkeFarOgMorSøker(LocalDate fødselsdato) {
        var scenario = ScenarioMorSøkerForeldrepenger
            .forFødselMedGittAktørId(GITT_MOR_AKTØR_ID);

        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(fødselsdato).build());

        var rettighet = new OppgittRettighetEntitet(true, false, false, false);
        scenario.medOppgittRettighet(rettighet);

        var behandling = scenario.lagre(repositoryProvider);

        when(personopplysninger.barnHarSammeBosted(BehandlingReferanse.fra(behandling))).thenReturn(true);

        return behandling;
    }

    private Behandling opprettBehandlingForBekreftetFødselMedSøknadsperioder(List<OppgittPeriodeEntitet> søknadsPerioder) {
        var scenario = ScenarioMorSøkerForeldrepenger
            .forFødselMedGittAktørId(GITT_MOR_AKTØR_ID);


        var rettighet = new OppgittRettighetEntitet(true, false, false, false);
        scenario.medOppgittRettighet(rettighet);

        var fordeling = new OppgittFordelingEntitet(søknadsPerioder, true);
        scenario.medFordeling(fordeling);

        return scenario.lagre(repositoryProvider);
    }

}
