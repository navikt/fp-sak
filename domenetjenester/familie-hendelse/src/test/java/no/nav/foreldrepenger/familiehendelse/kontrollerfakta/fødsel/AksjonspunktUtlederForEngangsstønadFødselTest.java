package no.nav.foreldrepenger.familiehendelse.kontrollerfakta.fødsel;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType.FØRSTEGANGSSØKNAD;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENT_PÅ_FØDSELREGISTRERING;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon.Builder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

public class AksjonspunktUtlederForEngangsstønadFødselTest {

    private static final LocalDate FØDSELSDATO_NÅ = LocalDate.now();
    private static final LocalDate TERMINDATO_NÅ = LocalDate.now();
    private static final LocalDate FØDSELSDATO_16_SIDEN = LocalDate.now().minusDays(16);
    private static final LocalDate TERMINDATO_27_SIDEN = LocalDate.now().minusDays(27);

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private AksjonspunktUtlederForEngangsstønadFødsel apUtleder;
    private FamilieHendelseTjeneste familieHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());


    @Before
    public void oppsett() {
        apUtleder = Mockito.spy(new AksjonspunktUtlederForEngangsstønadFødsel(mock(InntektArbeidYtelseTjeneste.class), familieHendelseTjeneste));
    }

    @Test
    public void avklar_terminbekreftelse_dersom_termindato_nå() {
        //Arrange
        Behandling behandling = opprettBehandlingMedOppgittTermin(TERMINDATO_NÅ, FØRSTEGANGSSØKNAD);
        //Act
        List<AksjonspunktResultat> utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(lagInput(behandling));
        //Assert
        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktResultat.opprettForAksjonspunkt(AVKLAR_TERMINBEKREFTELSE));
        verify(apUtleder).utledAksjonspunkterForTerminbekreftelse(any());
    }

    @Test
    public void sjekk_manglende_fødsel_dersom_termindato_mer_enn_25_dager_siden() {
        //Arrange
        Behandling behandling = opprettBehandlingMedOppgittTermin(TERMINDATO_27_SIDEN, FØRSTEGANGSSØKNAD);
        //Act
        List<AksjonspunktResultat> utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(lagInput(behandling));
        //Assert
        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktResultat.opprettForAksjonspunkt(SJEKK_MANGLENDE_FØDSEL));
        verify(apUtleder).erFristForRegistreringAvFødselPassert(any(FamilieHendelseGrunnlagEntitet.class));
    }

    private AksjonspunktUtlederInput lagInput(Behandling behandling) {
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling));
    }

    @Test
    public void sjekk_manglende_fødsel_dersom_fødsel_og_mindre_enn_14_dager_siden_fødsel() {
        //Arrange
        Behandling behandling = opprettBehandlingMedOppgittFødsel(FØDSELSDATO_NÅ);
        //Act
        List<AksjonspunktResultat> utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(lagInput(behandling));
        //Assert
        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktResultat.opprettForAksjonspunkt(AUTO_VENT_PÅ_FØDSELREGISTRERING));
        assertThat(utledeteAksjonspunkter.get(0).getFrist()).isNotNull();
        //Usikker her
        verify(apUtleder).erFristForRegistreringAvFødselPassert(any(FamilieHendelseGrunnlagEntitet.class));
    }

    @Test
    public void autopunkt_vent_på_fødsel_dersom_fødsel_og_mer_enn_14_dager_siden_fødsel() {
        //Arrange
        Behandling behandling = opprettBehandlingMedOppgittFødsel(FØDSELSDATO_16_SIDEN);
        //Act
        List<AksjonspunktResultat> utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(lagInput(behandling));
        //Assert
        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktResultat.opprettForAksjonspunkt(SJEKK_MANGLENDE_FØDSEL));
        //Usikker her
        verify(apUtleder).erFristForRegistreringAvFødselPassert(any(FamilieHendelseGrunnlagEntitet.class));
    }

    @Test
    public void ingen_akjsonspunkter_dersom_fødsel_registrert_i_TPS_og_antall_barn_stemmer_med_søknad() {
        //Arrange
        Behandling behandling = opprettBehandlingForFødselRegistrertITps(FØDSELSDATO_NÅ,1, 1);
        //Act
        List<AksjonspunktResultat> utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(lagInput(behandling));
        //Assert
        assertThat(utledeteAksjonspunkter).isEmpty();
        verify(apUtleder).samsvarerAntallBarnISøknadMedAntallBarnITps(any(FamilieHendelseGrunnlagEntitet.class));
    }

    @Test
    public void ingen_akjsonspunkter_dersom_fødsel_overstyrt() {
        //Arrange
        Behandling behandling = opprettBehandlingForFødselOverstyrt(FØDSELSDATO_NÅ,1, 1);
        //Act
        List<AksjonspunktResultat> utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(lagInput(behandling));
        //Assert
        assertThat(utledeteAksjonspunkter).isEmpty();
        verify(apUtleder).finnesOverstyrtFødsel(any(FamilieHendelseGrunnlagEntitet.class));
    }

    @Test
    public void sjekk_manglende_fødsel_dersom_fødsel_registrert_i_TPS_og_antall_barn_ikke_stemmer_med_søknad() {
        //Arrange
        Behandling behandling = opprettBehandlingForFødselRegistrertITps(FØDSELSDATO_NÅ, 2, 1);
        //Act
        List<AksjonspunktResultat> utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(lagInput(behandling));
        //Assert
        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktResultat.opprettForAksjonspunkt(SJEKK_MANGLENDE_FØDSEL));
        verify(apUtleder).samsvarerAntallBarnISøknadMedAntallBarnITps(any(FamilieHendelseGrunnlagEntitet.class));
    }

    private Behandling opprettBehandlingMedOppgittTermin(LocalDate termindato, BehandlingType behandlingType) {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel()
            .medBehandlingType(behandlingType);
        leggTilSøker(scenario, NavBrukerKjønn.KVINNE);

        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medUtstedtDato(LocalDate.now())
            .medTermindato(termindato)
            .medNavnPå("LEGEN MIN"));
        return scenario.lagre(repositoryProvider);
    }


    private Behandling opprettBehandlingMedOppgittFødsel(LocalDate fødseldato) {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(fødseldato);

        leggTilSøker(scenario, NavBrukerKjønn.KVINNE);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingForFødselRegistrertITps(LocalDate fødseldato, int antallBarnSøknad, int antallBarnTps) {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel();
        leggTilSøker(scenario, NavBrukerKjønn.KVINNE);
        scenario.medSøknadHendelse()
            .medFødselsDato(fødseldato)
            .medAntallBarn(antallBarnSøknad);
        scenario.medBekreftetHendelse().medFødselsDato(fødseldato).medAntallBarn(antallBarnTps);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingForFødselOverstyrt(LocalDate fødseldato, int antallBarnSøknad, int antallBarnTps) {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel();
        leggTilSøker(scenario, NavBrukerKjønn.KVINNE);
        scenario.medSøknadHendelse()
            .medFødselsDato(fødseldato)
            .medAntallBarn(antallBarnSøknad);
        scenario.medOverstyrtHendelse().medFødselsDato(fødseldato).medAntallBarn(antallBarnTps);
        return scenario.lagre(repositoryProvider);
    }

    private void leggTilSøker(AbstractTestScenario<?> scenario, NavBrukerKjønn kjønn) {
        Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();
        PersonInformasjon søker = builderForRegisteropplysninger
            .medPersonas()
            .voksenPerson(søkerAktørId, SivilstandType.UOPPGITT, kjønn, Region.UDEFINERT)
            .build();
        scenario.medRegisterOpplysninger(søker);
    }

}
