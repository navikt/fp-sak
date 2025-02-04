package no.nav.foreldrepenger.domene.uttak.fakta.omsorg;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;
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
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.PersonopplysningerForUttakStub;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

class AnnenForelderHarRettAksjonspunktUtlederTest {

    private static final AktørId AKTØR_ID_MOR = AktørId.dummy();
    private static final AktørId AKTØR_ID_FAR = AktørId.dummy();

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();
    private final AnnenForelderHarRettAksjonspunktUtleder aksjonspunktUtleder = new AnnenForelderHarRettAksjonspunktUtleder(
        repositoryProvider, new PersonopplysningerForUttakStub(),
        new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository()));

    @Test
    void aksjonspunkt_dersom_mor_søker_førstegangssøknad_og_annenforelder_har_ikke_rett() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR);
        scenario.medAvklarteUttakDatoer(
            new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        var rettighet = OppgittRettighetEntitet.bareSøkerRett();
        scenario.medOppgittRettighet(rettighet);

        //mockPersonopplysninger(AKTØR_ID_FAR);

        var behandling = scenario.lagre(repositoryProvider);
        var familieHendelse = FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(aksjonspunktResultater).containsExactly(AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT);

    }

    private UttakInput lagInput(Behandling behandling, FamilieHendelser familieHendelser) {
        return lagInput(behandling, familieHendelser, null, false);
    }

    private UttakInput lagInput(Behandling behandling, FamilieHendelser familieHendelser, Annenpart annenpart, boolean annenForelderES) {
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser)
            .medOppgittAnnenForelderHarEngangsstønadForSammeBarn(annenForelderES)
            .medAnnenpart(annenpart);
        return new UttakInput(BehandlingReferanse.fra(behandling), lagSkjæringstidspunkt(LocalDate.now()), null,
            ytelsespesifiktGrunnlag);
    }

    private Skjæringstidspunkt lagSkjæringstidspunkt(LocalDate dato) {
        return Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(dato).build();
    }

    @Test
    void aksjonspunkt_dersom_far_søker_førstegangssøknad_og_annenforelder_har_ikke_rett() {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_FAR);
        scenario.medAvklarteUttakDatoer(
            new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        var rettighet = OppgittRettighetEntitet.bareSøkerRett();
        scenario.medOppgittRettighet(rettighet);

        var behandling = scenario.lagre(repositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(aksjonspunktResultater).containsExactly(AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT);

    }

    @Test
    void ingen_aksjonspunkt_dersom_far_søker_førstegangssøknad_og_annenforelder_har_ikke_rett_men_har_ES() {
        var fødselsdato = LocalDate.now().minusMonths(1);
        var mores = ScenarioMorSøkerEngangsstønad.forFødsel(AKTØR_ID_MOR);
        mores.lagre(repositoryProvider);
        var scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_FAR);
        scenario.medAvklarteUttakDatoer(
            new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        var rettighet = OppgittRettighetEntitet.bareSøkerRett();
        scenario.medOppgittRettighet(rettighet);

        var behandling = scenario.lagre(repositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse)
            .medBekreftetHendelse(familieHendelse);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(
            lagInput(behandling, familieHendelser, null, true));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    void aksjonspunkt_dersom_far_søker_førstegangssøknad_og_annenforelder_har_ikke_rett_men_har_ES_for_annet_barn() {
        var fødselsdato = LocalDate.now().minusMonths(1);
        var mores = ScenarioMorSøkerEngangsstønad.forFødsel(AKTØR_ID_MOR);

        mores.lagre(repositoryProvider);
        var scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_FAR);
        scenario.medAvklarteUttakDatoer(
            new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        var rettighet = OppgittRettighetEntitet.bareSøkerRett();
        scenario.medOppgittRettighet(rettighet);

        var behandling = scenario.lagre(repositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse)
            .medBekreftetHendelse(familieHendelse);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(
            lagInput(behandling, familieHendelser,null, false));

        // Assert
        assertThat(aksjonspunktResultater).containsExactly(AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT);
    }

    @Test
    void ikke_aksjonspunkt_dersom_far_søker_førstegangssøknad_og_annenforelder_har_ikke_rett_men_har_ES_og_har_bekreftet_Uføretrygd() {
        var fødselsdato = LocalDate.now().minusMonths(1);
        var mores = ScenarioMorSøkerEngangsstønad.forFødsel(AKTØR_ID_MOR);

        mores.lagre(repositoryProvider);
        var scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_FAR);
        scenario.medAvklarteUttakDatoer(
            new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        var rettighet = OppgittRettighetEntitet.bareSøkerRett();
        scenario.medOppgittRettighet(rettighet);
        var overstyrt = new OppgittRettighetEntitet(null, null, true, null, null);
        scenario.medOverstyrtRettighet(overstyrt);

        var behandling = scenario.lagre(repositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse)
            .medBekreftetHendelse(familieHendelse);
        var uføreBuilder = UføretrygdGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medBehandlingId(behandling.getId()).medAktørIdUføretrygdet(AKTØR_ID_MOR)
            .medRegisterUføretrygd(false, null, null);

        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(
            lagInputUføre(behandling, familieHendelser, null, true, uføreBuilder));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    void aksjonspunkt_dersom_far_søker_førstegangssøknad_og_annenforelder_har_ikke_rett_men_har_ES_og_har_uavklart_Uføretrygd() {
        var fødselsdato = LocalDate.now().minusMonths(1);
        var mores = ScenarioMorSøkerEngangsstønad.forFødsel(AKTØR_ID_MOR);

        mores.lagre(repositoryProvider);
        var scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_FAR);
        scenario.medAvklarteUttakDatoer(
            new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        var rettighet = OppgittRettighetEntitet.bareSøkerRett();
        scenario.medOppgittRettighet(rettighet);

        var behandling = scenario.lagre(repositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse)
            .medBekreftetHendelse(familieHendelse);
        var uføreBuilder = UføretrygdGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medBehandlingId(behandling.getId()).medAktørIdUføretrygdet(AKTØR_ID_MOR)
            .medRegisterUføretrygd(false, null, null);

        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(
            lagInputUføre(behandling, familieHendelser, null, true, uføreBuilder));

        // Assert
        assertThat(aksjonspunktResultater).containsExactly(AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT);
    }


    private UttakInput lagInputUføre(Behandling behandling, FamilieHendelser familieHendelser, Annenpart annenpart, boolean annenForelderES,
                                     UføretrygdGrunnlagEntitet.Builder uføreBuilder) {
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser)
            .medOppgittAnnenForelderHarEngangsstønadForSammeBarn(annenForelderES)
            .medAnnenpart(null).medUføretrygdGrunnlag(uføreBuilder.build()).medAnnenpart(annenpart);
        return new UttakInput(BehandlingReferanse.fra(behandling), lagSkjæringstidspunkt(LocalDate.now()), null,
            ytelsespesifiktGrunnlag);
    }

    @Test
    void ingen_aksjonspunkt_dersom_mor_søker_førstegangssøknad_og_annenforelder_har_rett() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR);
        scenario.medAvklarteUttakDatoer(
            new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        var rettighet = OppgittRettighetEntitet.beggeRett();
        scenario.medOppgittRettighet(rettighet);

        var behandling = scenario.lagre(repositoryProvider);
        var familieHendelse = FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    void ingen_aksjonspunkt_dersom_revurdering() {
        var originalBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR)
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        scenario.medAvklarteUttakDatoer(
            new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        var rettighet = OppgittRettighetEntitet.beggeRett();
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
    void ikke_aksjonspunkt_dersom_mor_søker_førstegangssøknad_og_annenforelder_har_ikke_rett_og_søker_har_aleneomsorg() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR);
        scenario.medAvklarteUttakDatoer(
            new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        var rettighet = OppgittRettighetEntitet.aleneomsorg();
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
    void ingen_aksjonspunkt_dersom_har_ikke_oppgitt_annenpart() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR);
        scenario.medAvklarteUttakDatoer(
            new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now().minusWeeks(3)).build());
        var rettighet = OppgittRettighetEntitet.aleneomsorg();
        scenario.medOppgittRettighet(rettighet);
        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var familieHendelse = FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling, familieHendelser));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

}
