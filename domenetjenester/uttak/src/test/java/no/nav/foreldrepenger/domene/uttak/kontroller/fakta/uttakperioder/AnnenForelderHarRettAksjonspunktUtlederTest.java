package no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.Annenpart;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class AnnenForelderHarRettAksjonspunktUtlederTest extends EntityManagerAwareTest {

    private static final AktørId AKTØR_ID_MOR = AktørId.dummy();
    private static final AktørId AKTØR_ID_FAR = AktørId.dummy();

    private UttakRepositoryProvider repositoryProvider;
    private AnnenForelderHarRettAksjonspunktUtleder aksjonspunktUtleder;

    @BeforeEach
    public void before() {
        var entityManager = getEntityManager();
        repositoryProvider = new UttakRepositoryProvider(entityManager);
        PersonopplysningTjeneste personopplysningTjeneste = new PersonopplysningTjeneste(new PersonopplysningRepository(entityManager));
        ForeldrepengerUttakTjeneste uttakTjeneste = new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository());
        aksjonspunktUtleder = new AnnenForelderHarRettAksjonspunktUtleder(repositoryProvider, personopplysningTjeneste,
            uttakTjeneste);
    }

    @Test
    public void aksjonspunkt_dersom_mor_søker_førstegangssøknad_og_annenforelder_har_ikke_rett() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR)
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(false, true, false);
        scenario.medOppgittRettighet(rettighet);
        scenario.medSøknadAnnenPart().medAktørId(AKTØR_ID_FAR);

        Behandling behandling = scenario.lagre(repositoryProvider);
        var familieHendelse = FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(new Barn()), 1);
        FamilieHendelser familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(aksjonspunktResultater).containsExactly(AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT);

    }

    private UttakInput lagInput(Behandling behandling, FamilieHendelser familieHendelser) {
        return lagInput(behandling, familieHendelser, null);
    }

    private UttakInput lagInput(Behandling behandling, FamilieHendelser familieHendelser, Annenpart annenpart) {
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(familieHendelser)
            .medAnnenpart(annenpart);
        return new UttakInput(BehandlingReferanse.fra(behandling, lagSkjæringstidspunkt(LocalDate.now())), null, ytelsespesifiktGrunnlag);
    }

    private Skjæringstidspunkt lagSkjæringstidspunkt(LocalDate dato) {
        return Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(dato).build();
    }

    @Test
    public void aksjonspunkt_dersom_far_søker_førstegangssøknad_og_annenforelder_har_ikke_rett() {
        ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_FAR)
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(false, true, false);
        scenario.medOppgittRettighet(rettighet);
        scenario.medSøknadAnnenPart().medAktørId(AKTØR_ID_MOR);

        Behandling behandling = scenario.lagre(repositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(null, LocalDate.now(),
            List.of(new Barn()), 1);
        FamilieHendelser familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(aksjonspunktResultater).containsExactly(AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT);

    }

    @Test
    public void ingen_aksjonspunkt_dersom_far_søker_førstegangssøknad_og_annenforelder_har_ikke_rett_men_har_ES() {
        LocalDate fødselsdato = LocalDate.now().minusMonths(1);
        ScenarioMorSøkerEngangsstønad mores = ScenarioMorSøkerEngangsstønad.forFødsel(AKTØR_ID_MOR).medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);

        Behandling morEngang = mores.lagre(repositoryProvider);
        ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_FAR)
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(false, true, false);
        scenario.medOppgittRettighet(rettighet);
        scenario.medSøknadAnnenPart().medAktørId(AKTØR_ID_MOR);

        Behandling behandling = scenario.lagre(repositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1);
        FamilieHendelser familieHendelser = new FamilieHendelser()
            .medSøknadHendelse(familieHendelse)
            .medBekreftetHendelse(familieHendelse);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser,
            new Annenpart(true, morEngang.getId())));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void aksjonspunkt_dersom_far_søker_førstegangssøknad_og_annenforelder_har_ikke_rett_men_har_ES_for_annet_barn() {
        LocalDate fødselsdato = LocalDate.now().minusMonths(1);
        ScenarioMorSøkerEngangsstønad mores = ScenarioMorSøkerEngangsstønad.forFødsel(AKTØR_ID_MOR).medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);

        Behandling morEngang = mores.lagre(repositoryProvider);
        ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_FAR)
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(false, true, false);
        scenario.medOppgittRettighet(rettighet);
        scenario.medSøknadAnnenPart().medAktørId(AKTØR_ID_MOR);

        Behandling behandling = scenario.lagre(repositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1);
        FamilieHendelser familieHendelser = new FamilieHendelser()
            .medSøknadHendelse(familieHendelse)
            .medBekreftetHendelse(familieHendelse);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser,
            new Annenpart(false, morEngang.getId())));

        // Assert
        assertThat(aksjonspunktResultater).containsExactly(AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT);
    }

    @Test
    public void ingen_aksjonspunkt_dersom_mor_søker_førstegangssøknad_og_annenforelder_har_rett() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR)
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(true, true, false);
        scenario.medOppgittRettighet(rettighet);

        Behandling behandling = scenario.lagre(repositoryProvider);
        var familieHendelse = FamilieHendelse.forFødsel(null, LocalDate.now(),
            List.of(new Barn()), 1);
        FamilieHendelser familieHendelser = new FamilieHendelser()
            .medSøknadHendelse(familieHendelse);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void ingen_aksjonspunkt_dersom_revurdering() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR)
            .medBehandlingType(BehandlingType.REVURDERING);
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(true, true, false);
        scenario.medOppgittRettighet(rettighet);
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        var familieHendelse = FamilieHendelse.forFødsel(null, LocalDate.now(),
            List.of(new Barn()), 1);
        FamilieHendelser familieHendelser = new FamilieHendelser()
            .medSøknadHendelse(familieHendelse);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void ikke_aksjonspunkt_dersom_mor_søker_førstegangssøknad_og_annenforelder_har_ikke_rett_og_søker_har_aleneomsorg() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR)
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(false, true, true);
        scenario.medOppgittRettighet(rettighet);
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        var familieHendelse = FamilieHendelse.forFødsel(null, LocalDate.now(),
            List.of(new Barn()), 1);
        FamilieHendelser familieHendelser = new FamilieHendelser()
            .medSøknadHendelse(familieHendelse);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void ingen_aksjonspunkt_dersom_har_ikke_oppgitt_annenpart() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR)
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(false, true, true);
        scenario.medOppgittRettighet(rettighet);
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        var familieHendelse = FamilieHendelse.forFødsel(null, LocalDate.now(),
            List.of(new Barn()), 1);
        FamilieHendelser familieHendelser = new FamilieHendelser()
            .medSøknadHendelse(familieHendelse);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

}
