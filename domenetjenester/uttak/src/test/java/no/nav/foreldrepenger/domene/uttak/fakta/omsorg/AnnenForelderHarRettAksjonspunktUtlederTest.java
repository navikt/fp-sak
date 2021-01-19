package no.nav.foreldrepenger.domene.uttak.fakta.omsorg;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.Annenpart;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.PersonopplysningerForUttakForTest;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryProviderForTest;

public class AnnenForelderHarRettAksjonspunktUtlederTest {

    private static final AktørId AKTØR_ID_MOR = AktørId.dummy();
    private static final AktørId AKTØR_ID_FAR = AktørId.dummy();

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryProviderForTest();
    private final AnnenForelderHarRettAksjonspunktUtleder aksjonspunktUtleder = new AnnenForelderHarRettAksjonspunktUtleder(
        repositoryProvider, new PersonopplysningerForUttakForTest(),
        new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository()));

    @Test
    public void aksjonspunkt_dersom_mor_søker_førstegangssøknad_og_annenforelder_har_ikke_rett() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR);
        scenario.medAvklarteUttakDatoer(
            new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(false, true, false);
        scenario.medOppgittRettighet(rettighet);

        //mockPersonopplysninger(AKTØR_ID_FAR);

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
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser)
            .medAnnenpart(annenpart);
        return new UttakInput(BehandlingReferanse.fra(behandling, lagSkjæringstidspunkt(LocalDate.now())), null,
            ytelsespesifiktGrunnlag);
    }

    private Skjæringstidspunkt lagSkjæringstidspunkt(LocalDate dato) {
        return Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(dato).build();
    }

    @Test
    public void aksjonspunkt_dersom_far_søker_førstegangssøknad_og_annenforelder_har_ikke_rett() {
        ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_FAR);
        scenario.medAvklarteUttakDatoer(
            new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(false, true, false);
        scenario.medOppgittRettighet(rettighet);

        Behandling behandling = scenario.lagre(repositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(new Barn()), 1);
        FamilieHendelser familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(aksjonspunktResultater).containsExactly(AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT);

    }

    @Test
    public void ingen_aksjonspunkt_dersom_far_søker_førstegangssøknad_og_annenforelder_har_ikke_rett_men_har_ES() {
        LocalDate fødselsdato = LocalDate.now().minusMonths(1);
        ScenarioMorSøkerEngangsstønad mores = ScenarioMorSøkerEngangsstønad.forFødsel(AKTØR_ID_MOR);

        Behandling morEngang = mores.lagre(repositoryProvider);
        ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_FAR);
        scenario.medAvklarteUttakDatoer(
            new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(false, true, false);
        scenario.medOppgittRettighet(rettighet);

        Behandling behandling = scenario.lagre(repositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1);
        FamilieHendelser familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse)
            .medBekreftetHendelse(familieHendelse);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(
            lagInput(behandling, familieHendelser, new Annenpart(true, morEngang.getId())));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void aksjonspunkt_dersom_far_søker_førstegangssøknad_og_annenforelder_har_ikke_rett_men_har_ES_for_annet_barn() {
        LocalDate fødselsdato = LocalDate.now().minusMonths(1);
        ScenarioMorSøkerEngangsstønad mores = ScenarioMorSøkerEngangsstønad.forFødsel(AKTØR_ID_MOR);

        Behandling morEngang = mores.lagre(repositoryProvider);
        ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_FAR);
        scenario.medAvklarteUttakDatoer(
            new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(false, true, false);
        scenario.medOppgittRettighet(rettighet);

        Behandling behandling = scenario.lagre(repositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1);
        FamilieHendelser familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse)
            .medBekreftetHendelse(familieHendelse);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(
            lagInput(behandling, familieHendelser, new Annenpart(false, morEngang.getId())));

        // Assert
        assertThat(aksjonspunktResultater).containsExactly(AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT);
    }

    @Test
    public void ingen_aksjonspunkt_dersom_mor_søker_førstegangssøknad_og_annenforelder_har_rett() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR);
        scenario.medAvklarteUttakDatoer(
            new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(true, true, false);
        scenario.medOppgittRettighet(rettighet);

        Behandling behandling = scenario.lagre(repositoryProvider);
        var familieHendelse = FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(new Barn()), 1);
        FamilieHendelser familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void ingen_aksjonspunkt_dersom_revurdering() {
        var originalBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR)
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        scenario.medAvklarteUttakDatoer(
            new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        var rettighet = new OppgittRettighetEntitet(true, true, false);
        scenario.medOppgittRettighet(rettighet);
        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var familieHendelse = FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void ikke_aksjonspunkt_dersom_mor_søker_førstegangssøknad_og_annenforelder_har_ikke_rett_og_søker_har_aleneomsorg() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR);
        scenario.medAvklarteUttakDatoer(
            new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(false, true, true);
        scenario.medOppgittRettighet(rettighet);
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        var familieHendelse = FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(new Barn()), 1);
        FamilieHendelser familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void ingen_aksjonspunkt_dersom_har_ikke_oppgitt_annenpart() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR);
        scenario.medAvklarteUttakDatoer(
            new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(false, true, true);
        scenario.medOppgittRettighet(rettighet);
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        var familieHendelse = FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(new Barn()), 1);
        FamilieHendelser familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

}
