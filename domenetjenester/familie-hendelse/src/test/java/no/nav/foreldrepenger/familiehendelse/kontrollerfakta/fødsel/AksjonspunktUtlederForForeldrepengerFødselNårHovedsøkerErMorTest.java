package no.nav.foreldrepenger.familiehendelse.kontrollerfakta.fødsel;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENT_PÅ_FØDSELREGISTRERING;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.SJEKK_TERMINBEKREFTELSE;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederResultat;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
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
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.konfig.Tid;

class AksjonspunktUtlederForForeldrepengerFødselNårHovedsøkerErMorTest extends EntityManagerAwareTest {

    private static final LocalDate TERMINDAT0_NÅ = LocalDate.now();
    private static final LocalDate TERMINDATO_27_SIDEN = LocalDate.now().minusDays(27);
    private static final LocalDate FØDSEL_17_SIDEN = LocalDate.now().minusDays(27);
    private static final String ORG_NR = "55555555";
    private static final LocalDate AVSLUTTET_TIDSPUNKT = LocalDate.now().minusMonths(2);

    public static AktørId GITT_AKTØR_ID = AktørId.dummy();

    private BehandlingRepositoryProvider repositoryProvider;

    private AksjonspunktUtlederForForeldrepengerFødsel apUtleder;
    private final SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = Mockito.mock(SkjæringstidspunktTjeneste.class);
    private final Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    @BeforeEach
    public void oppsett() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        familieHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(Mockito.any())).thenReturn(skjæringstidspunkt);
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        apUtleder = Mockito.spy(new AksjonspunktUtlederForForeldrepengerFødsel(iayTjeneste, familieHendelseTjeneste, ytelsesFordelingRepository));
    }

    @Test
    void sjekk_manglende_fødsel_dersom_termindato_mer_enn_25_dager_siden() {
        var behandling = opprettBehandlingMedOppgittTerminOgBehandlingType(TERMINDATO_27_SIDEN);
        var utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(lagInput(behandling));

        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktUtlederResultat.opprettForAksjonspunkt(SJEKK_MANGLENDE_FØDSEL));
        verify(apUtleder).erFristForRegistreringAvFødselPassert(any(FamilieHendelseGrunnlagEntitet.class));
    }

    private AksjonspunktUtlederInput lagInput(Behandling behandling) {
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling), skjæringstidspunkt);
    }

    @Test
    void sjekk_manglende_fødsel_dersom_fødsel_og_mer_enn_14_dager_siden_fødsel() {
        var behandling = opprettBehandlingMedOppgittFødsel(FØDSEL_17_SIDEN);
        var utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(lagInput(behandling));

        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktUtlederResultat.opprettForAksjonspunkt(SJEKK_MANGLENDE_FØDSEL));
        verify(apUtleder).erFristForRegistreringAvFødselPassert(any(FamilieHendelseGrunnlagEntitet.class));
    }

    @Test
    void sjekk_autopunkt_vent_på_fødsel_dersom_fødsel_og_mindre_enn_8_dager_siden_fødsel() {
        var behandling = opprettBehandlingMedOppgittFødsel(LocalDate.now().minusDays(3));
        var utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(lagInput(behandling));

        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktUtlederResultat.opprettForAksjonspunkt(AUTO_VENT_PÅ_FØDSELREGISTRERING));
        assertThat(utledeteAksjonspunkter.getFirst().frist()).isNotNull();
        assertThat(utledeteAksjonspunkter.getFirst().venteårsak()).isNotNull();
        verify(apUtleder).erFristForRegistreringAvFødselPassert(any(FamilieHendelseGrunnlagEntitet.class));
    }

    @Test
    void ingen_akjsonspunkter_dersom_fødsel_registrert_i_TPS_og_antall_barn_stemmer_med_søknad() {

        // Oppsett
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var testObjekt = Mockito.spy(new AksjonspunktUtlederForForeldrepengerFødsel(mock(InntektArbeidYtelseTjeneste.class), familieHendelseTjeneste,
            ytelsesFordelingRepository));

        var behandling = opprettBehandlingForFødselRegistrertIPDL(LocalDate.now(), 1, 1);
        var utledeteAksjonspunkter = testObjekt.utledAksjonspunkterFor(lagInput(behandling));

        assertThat(utledeteAksjonspunkter).isEmpty();
        verify(testObjekt).samsvarerAntallBarnISøknadMedAntallBarnIPDL(any(FamilieHendelseGrunnlagEntitet.class));
    }

    @Test
    void sjekk_manglende_fødsel_dersom_fødsel_registrert_i_TPS_og_antall_barn_ikke_stemmer_med_søknad() {
        // Oppsett
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();

        var testObjekt = Mockito.spy(new AksjonspunktUtlederForForeldrepengerFødsel(mock(InntektArbeidYtelseTjeneste.class), familieHendelseTjeneste,
            ytelsesFordelingRepository));

        var behandling = opprettBehandlingForFødselRegistrertIPDL(LocalDate.now(), 2, 1);
        var utledeteAksjonspunkter = testObjekt.utledAksjonspunkterFor(lagInput(behandling));

        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktUtlederResultat.opprettForAksjonspunkt(SJEKK_MANGLENDE_FØDSEL));
        verify(testObjekt).samsvarerAntallBarnISøknadMedAntallBarnIPDL(any(FamilieHendelseGrunnlagEntitet.class));
    }

    @Test
    void avklar_terminbekreftelse_dersom_termindato_nå_og_mangler_løpende_arbeid() {
        //Arrange
        var behandling = opprettBehandlingMedOppgittTerminOgBehandlingType(TERMINDAT0_NÅ);
        //Act
        var param = lagInput(behandling);
        var utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(param);
        //Assert
        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktUtlederResultat.opprettForAksjonspunkt(SJEKK_TERMINBEKREFTELSE));
        verify(apUtleder).erSøkerRegistrertArbeidstakerMedLøpendeArbeidsforholdIAARegisteret(param);
    }

    @Test
    void ingen_aksjonspunkter_dersom_løpende_arbeid() {
        //Arrange
        var behandling = opprettBehandlingMedOppgittTerminOgArbeidsForhold(TERMINDAT0_NÅ, Tid.TIDENES_ENDE);
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();

        var apUtleder = Mockito.spy(new AksjonspunktUtlederForForeldrepengerFødsel(iayTjeneste, familieHendelseTjeneste, ytelsesFordelingRepository));
        //Act
        var param = lagInput(behandling);
        var utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(param);
        //Assert
        assertThat(utledeteAksjonspunkter).isEmpty();
        verify(apUtleder).erSøkerRegistrertArbeidstakerMedLøpendeArbeidsforholdIAARegisteret(param);
    }

    @Test
    void ingen_aksjonspunkter_dersom_tidsavgrenset_arbforhold_over_stp() {
        //Arrange
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();

        var behandling = opprettBehandlingMedOppgittTerminOgArbeidsForhold(TERMINDAT0_NÅ, LocalDate.now().plusMonths(6));
        var apUtleder = Mockito.spy(new AksjonspunktUtlederForForeldrepengerFødsel(iayTjeneste, familieHendelseTjeneste, ytelsesFordelingRepository));
        //Act
        var param = lagInput(behandling);
        var utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(param);
        //Assert
        assertThat(utledeteAksjonspunkter).isEmpty();
        verify(apUtleder).erSøkerRegistrertArbeidstakerMedLøpendeArbeidsforholdIAARegisteret(param);
    }

    @Test
    void avklar_terminbekreftelse_dersom_har_avsluttet_arbeidsforhold() {
        //Arrange
        var behandling = opprettBehandlingMedOppgittTerminOgArbeidsForhold(TERMINDAT0_NÅ, AVSLUTTET_TIDSPUNKT);
        //Act
        var param = lagInput(behandling);
        var utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(param);
        //Assert
        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktUtlederResultat.opprettForAksjonspunkt(SJEKK_TERMINBEKREFTELSE));
        verify(apUtleder).erSøkerRegistrertArbeidstakerMedLøpendeArbeidsforholdIAARegisteret(param);
    }

    private Behandling opprettBehandlingForFødselRegistrertIPDL(LocalDate fødseldato, int antallBarnSøknad, int antallBarnPDL) {
        var scenario = ScenarioMorSøkerForeldrepenger
            .forFødsel();
        scenario.medSøknadHendelse()
            .medFødselsDato(fødseldato, antallBarnSøknad)
            .medAntallBarn(antallBarnSøknad);
        scenario.medBekreftetHendelse().medFødselsDato(fødseldato, antallBarnPDL).medAntallBarn(antallBarnPDL);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingMedOppgittFødsel(LocalDate fødseldato) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(fødseldato);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingMedOppgittTerminOgArbeidsForhold(LocalDate termindato, LocalDate tilOgMed) {
        var scenario = ScenarioMorSøkerForeldrepenger
            .forFødselMedGittAktørId(GITT_AKTØR_ID)
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
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

    private Behandling opprettBehandlingMedOppgittTerminOgBehandlingType(LocalDate termindato) {
        var scenario = ScenarioMorSøkerForeldrepenger
            .forFødselMedGittAktørId(GITT_AKTØR_ID)
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medUtstedtDato(LocalDate.now())
            .medTermindato(termindato)
            .medNavnPå("LEGEN MIN"));

        return scenario.lagre(repositoryProvider);
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

    private void leggTilSøker(AbstractTestScenario<?> scenario, NavBrukerKjønn kjønn) {
        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();
        var søker = builderForRegisteropplysninger
            .medPersonas()
            .voksenPerson(søkerAktørId, SivilstandType.UOPPGITT, kjønn)
            .build();
        scenario.medRegisterOpplysninger(søker);
    }
}
