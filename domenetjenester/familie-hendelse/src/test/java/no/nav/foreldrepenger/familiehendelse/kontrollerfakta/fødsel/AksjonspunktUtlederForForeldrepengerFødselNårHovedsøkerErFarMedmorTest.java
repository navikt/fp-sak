package no.nav.foreldrepenger.familiehendelse.kontrollerfakta.fødsel;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.vedtak.konfig.Tid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AksjonspunktUtlederForForeldrepengerFødselNårHovedsøkerErFarMedmorTest extends EntityManagerAwareTest {

    private static final LocalDate TERMINDAT0_NÅ = LocalDate.now();
    private static final LocalDate FØDSEL_17_SIDEN = LocalDate.now().minusDays(27);

    private static final AktørId GITT_AKTØR_ID = AktørId.dummy();
    private static final String ORG_NR = "55555555";

    private BehandlingRepositoryProvider repositoryProvider;

    private AksjonspunktUtlederForForeldrepengerFødsel apUtleder;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    @BeforeEach
    public void oppsett() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        familieHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        apUtleder = Mockito.spy(new AksjonspunktUtlederForForeldrepengerFødsel(mock(InntektArbeidYtelseTjeneste.class), familieHendelseTjeneste));
    }

    @Test
    void ingen_akjsonspunkter_dersom_fødsel_registrert_i_TPS_og_antall_barn_stemmer_med_søknad() {
        // Oppsett
        var testObjekt = Mockito.spy(new AksjonspunktUtlederForForeldrepengerFødsel(mock(InntektArbeidYtelseTjeneste.class), familieHendelseTjeneste));

        var behandling = opprettBehandlingFarSøkerFødselRegistrertITps(LocalDate.now(), 1, 1);
        var utledeteAksjonspunkter = testObjekt.utledAksjonspunkterFor(lagInput(behandling));

        assertThat(utledeteAksjonspunkter).isEmpty();
        verify(testObjekt).samsvarerAntallBarnISøknadMedAntallBarnITps(any(FamilieHendelseGrunnlagEntitet.class));
    }

    private AksjonspunktUtlederInput lagInput(Behandling behandling) {
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, Skjæringstidspunkt.builder().medUtenMinsterett(true).medUtledetSkjæringstidspunkt(LocalDate.now()).build()));
    }

    private AksjonspunktUtlederInput lagInputMedMinsterettFar(Behandling behandling) {
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build()));
    }

    @Test
    void sjekk_manglende_fødsel_dersom_fødsel_registrert_i_TPS_og_antall_barn_ikke_stemmer_med_søknad() {
        // Oppsett
        var testObjekt = Mockito.spy(new AksjonspunktUtlederForForeldrepengerFødsel(mock(InntektArbeidYtelseTjeneste.class), familieHendelseTjeneste));

        var behandling = opprettBehandlingFarSøkerFødselRegistrertITps(LocalDate.now(), 2, 1);
        var utledeteAksjonspunkter = testObjekt.utledAksjonspunkterFor(lagInput(behandling));

        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktResultat.opprettForAksjonspunkt(SJEKK_MANGLENDE_FØDSEL));
        verify(testObjekt).samsvarerAntallBarnISøknadMedAntallBarnITps(any(FamilieHendelseGrunnlagEntitet.class));
    }

    @Test
    void sjekk_aksjonspunkter_når_søker_oppgir_termindato() {
        var behandling = opprettBehandlingMedOppgittTerminOgBehandlingType(TERMINDAT0_NÅ);
        var utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(lagInput(behandling));

        assertThat(utledeteAksjonspunkter).hasSize(1);
        assertThat(utledeteAksjonspunkter.get(0).getAksjonspunktDefinisjon()).isEqualTo(AVKLAR_TERMINBEKREFTELSE);;
    }

    @Test
    void sjekk_aksjonspunkter_når_søker_oppgir_termindato_wlb() {
        var behandling = opprettBehandlingMedOppgittTerminOgBehandlingType(TERMINDAT0_NÅ);
        var utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(lagInputMedMinsterettFar(behandling));

        assertThat(utledeteAksjonspunkter).hasSize(1);
        assertThat(utledeteAksjonspunkter.get(0).getAksjonspunktDefinisjon()).isEqualTo(AVKLAR_TERMINBEKREFTELSE);
    }

    @Test
    void ingen_aksjonspunkter_dersom_løpende_arbeid() {
        //Arrange
        var behandling = opprettBehandlingMedOppgittTerminOgArbeidsForhold(TERMINDAT0_NÅ, Tid.TIDENES_ENDE);
        var apUtleder = Mockito.spy(new AksjonspunktUtlederForForeldrepengerFødsel(iayTjeneste, familieHendelseTjeneste));
        //Act
        var param = lagInputMedMinsterettFar(behandling);
        var utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(param);
        //Assert
        assertThat(utledeteAksjonspunkter).isEmpty();
        verify(apUtleder).erSøkerRegistrertArbeidstakerMedLøpendeArbeidsforholdIAARegisteret(param);
    }

    @Test
    void sjekk_manglende_fødsel_dersom_fødsel_og_mer_enn_14_dager_siden_fødsel() {
        var behandling = opprettBehandlingMedOppgittFødsel(FØDSEL_17_SIDEN);
        var utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(lagInput(behandling));

        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktResultat.opprettForAksjonspunkt(SJEKK_MANGLENDE_FØDSEL));
        verify(apUtleder).erFristForRegistreringAvFødselPassert(any(FamilieHendelseGrunnlagEntitet.class));
    }

    @Test
    void sjekk_autopunkt_vent_på_fødsel_dersom_fødsel_og_mindre_enn_8_dager_siden_fødsel() {
        var behandling = opprettBehandlingMedOppgittFødsel(LocalDate.now().minusDays(1));
        var utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(lagInput(behandling));

        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktResultat.opprettForAksjonspunkt(AUTO_VENT_PÅ_FØDSELREGISTRERING));
        assertThat(utledeteAksjonspunkter.get(0).getFrist()).isNotNull();
        assertThat(utledeteAksjonspunkter.get(0).getVenteårsak()).isNotNull();
        verify(apUtleder).erFristForRegistreringAvFødselPassert(any(FamilieHendelseGrunnlagEntitet.class));
    }

    private Behandling opprettBehandlingFarSøkerFødselRegistrertITps(LocalDate fødseldato, int antallBarnSøknad, int antallBarnTps) {
        var scenario = ScenarioFarSøkerForeldrepenger
            .forFødsel();
        scenario.medSøknadHendelse()
            .medFødselsDato(fødseldato, antallBarnSøknad)
            .medAntallBarn(antallBarnSøknad);
        leggTilSøker(scenario, NavBrukerKjønn.MANN);
        scenario.medBekreftetHendelse().medFødselsDato(fødseldato, antallBarnTps).medAntallBarn(antallBarnTps);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingMedOppgittTerminOgBehandlingType(LocalDate termindato) {
        var scenario = ScenarioFarSøkerForeldrepenger
            .forFødselMedGittAktørId(GITT_AKTØR_ID);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medUtstedtDato(LocalDate.now())
            .medTermindato(termindato)
            .medNavnPå("LEGEN MIN"));
        leggTilSøker(scenario, NavBrukerKjønn.MANN);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingMedOppgittTerminOgArbeidsForhold(LocalDate termindato, LocalDate tilOgMed) {
        var scenario = ScenarioFarSøkerForeldrepenger
            .forFødselMedGittAktørId(GITT_AKTØR_ID);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medUtstedtDato(LocalDate.now())
            .medTermindato(termindato)
            .medNavnPå("LEGEN MIN"));
        leggTilSøker(scenario, NavBrukerKjønn.KVINNE);
        var behandling = scenario.lagre(repositoryProvider);
        var inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        lagAktørArbeid(inntektArbeidYtelseAggregatBuilder, GITT_AKTØR_ID, ORG_NR, tilOgMed);
        iayTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder);
        return behandling;
    }

    private Behandling opprettBehandlingMedOppgittFødsel(LocalDate fødseldato) {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(fødseldato);
        leggTilSøker(scenario, NavBrukerKjønn.MANN);
        return scenario.lagre(repositoryProvider);
    }

    private void leggTilSøker(AbstractTestScenario<?> scenario, NavBrukerKjønn kjønn) {
        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();
        var søker = builderForRegisteropplysninger
            .medPersonas()
            .voksenPerson(søkerAktørId, SivilstandType.UOPPGITT, kjønn)
            .build();
        scenario.medRegisterOpplysninger(søker);
    }

    private void lagAktørArbeid(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder, AktørId aktørId, String orgNr, LocalDate tilOgMed) {
        var aktørArbeidBuilder =
            inntektArbeidYtelseAggregatBuilder.getAktørArbeidBuilder(aktørId);

        var yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(Opptjeningsnøkkel.forOrgnummer(orgNr),
            ArbeidType.FORENKLET_OPPGJØRSORDNING);
        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();

        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(aktivitetsAvtaleBuilder
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(50), tilOgMed)));

        yrkesaktivitetBuilder.medArbeidType(ArbeidType.FORENKLET_OPPGJØRSORDNING);

        yrkesaktivitetBuilder.medArbeidsgiver(Arbeidsgiver.virksomhet(ORG_NR));
        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
    }

}
