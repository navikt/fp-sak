package no.nav.foreldrepenger.familiehendelse.kontrollerfakta.fødsel;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENT_PÅ_FØDSELREGISTRERING;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.SJEKK_TERMINBEKREFTELSE;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

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

class AksjonspunktUtlederForForeldrepengerFødselNårHovedsøkerErFarMedmorTest extends EntityManagerAwareTest {

    private static final AktørId GITT_AKTØR_ID = AktørId.dummy();
    private static final String ORG_NR = "55555555";

    private BehandlingRepositoryProvider repositoryProvider;

    private FamilieHendelseTjeneste familieHendelseTjeneste;

    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    @BeforeEach
    void oppsett() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        familieHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
    }

    @Test
    void ingen_akjsonspunkter_dersom_fødsel_registrert_i_TPS_og_antall_barn_stemmer_med_søknad() {
        // Oppsett
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var testObjekt = Mockito.spy(new AksjonspunktUtlederForForeldrepengerFødsel(iayTjeneste, familieHendelseTjeneste,
            ytelsesFordelingRepository));

        var behandling = opprettBehandlingFarSøkerFødselRegistrertIPDL(LocalDate.now(), 1, 1);
        var utledeteAksjonspunkter = testObjekt.utledAksjonspunkterFor(lagInput(behandling, LocalDate.now()));

        assertThat(utledeteAksjonspunkter).isEmpty();
        verify(testObjekt).samsvarerAntallBarnISøknadMedAntallBarnIPDL(any(FamilieHendelseGrunnlagEntitet.class));
    }

    private AksjonspunktUtlederInput lagInput(Behandling behandling, LocalDate stp) {
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling), Skjæringstidspunkt.builder().medUtenMinsterett(true).medUtledetSkjæringstidspunkt(
            stp).build());
    }

    private AksjonspunktUtlederInput lagInputMedMinsterettFar(Behandling behandling, LocalDate stp) {
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling), Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).build());
    }

    @Test
    void sjekk_manglende_fødsel_dersom_fødsel_registrert_i_TPS_og_antall_barn_ikke_stemmer_med_søknad() {
        // Oppsett
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var testObjekt = Mockito.spy(new AksjonspunktUtlederForForeldrepengerFødsel(iayTjeneste, familieHendelseTjeneste,
            ytelsesFordelingRepository));

        var behandling = opprettBehandlingFarSøkerFødselRegistrertIPDL(LocalDate.now(), 2, 1);
        var utledeteAksjonspunkter = testObjekt.utledAksjonspunkterFor(lagInput(behandling, LocalDate.now()));

        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktUtlederResultat.opprettForAksjonspunkt(SJEKK_MANGLENDE_FØDSEL));
        verify(testObjekt).samsvarerAntallBarnISøknadMedAntallBarnIPDL(any(FamilieHendelseGrunnlagEntitet.class));
    }

    @Test
    void sjekk_aksjonspunkter_når_søker_oppgir_termindato() {
        var behandling = opprettBehandlingMedOppgittTerminOgBehandlingType(LocalDate.now());
        var apUtleder = Mockito.spy(new AksjonspunktUtlederForForeldrepengerFødsel(iayTjeneste, familieHendelseTjeneste,
            repositoryProvider.getYtelsesFordelingRepository()));
        var utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(lagInput(behandling, LocalDate.now()));

        assertThat(utledeteAksjonspunkter).hasSize(1);
        assertThat(utledeteAksjonspunkter.getFirst().aksjonspunktDefinisjon()).isEqualTo(SJEKK_TERMINBEKREFTELSE);;
    }

    @Test
    void sjekk_aksjonspunkter_når_søker_oppgir_termindato_wlb() {
        var behandling = opprettBehandlingMedOppgittTerminOgBehandlingType(LocalDate.now());
        var apUtleder = Mockito.spy(new AksjonspunktUtlederForForeldrepengerFødsel(iayTjeneste, familieHendelseTjeneste,
            repositoryProvider.getYtelsesFordelingRepository()));
        var utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(lagInputMedMinsterettFar(behandling, LocalDate.now()));

        assertThat(utledeteAksjonspunkter).hasSize(1);
        assertThat(utledeteAksjonspunkter.getFirst().aksjonspunktDefinisjon()).isEqualTo(SJEKK_TERMINBEKREFTELSE);
    }

    @Test
    void ingen_aksjonspunkter_dersom_løpende_arbeid() {
        //Arrange
        var behandling = opprettBehandlingMedOppgittTerminOgArbeidsForhold(LocalDate.now(), Tid.TIDENES_ENDE, OppgittRettighetEntitet.beggeRett());
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var apUtleder = Mockito.spy(new AksjonspunktUtlederForForeldrepengerFødsel(iayTjeneste, familieHendelseTjeneste, ytelsesFordelingRepository));
        //Act
        var param = lagInputMedMinsterettFar(behandling, LocalDate.now());
        var utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(param);
        //Assert
        assertThat(utledeteAksjonspunkter).isEmpty();
        verify(apUtleder).erSøkerRegistrertArbeidstakerMedLøpendeArbeidsforholdIAARegisteret(param);
    }

    @Test
    void sjekk_manglende_fødsel_dersom_fødsel_og_mer_enn_14_dager_siden_fødsel() {
        var behandling = opprettBehandlingMedOppgittFødsel(LocalDate.now().minusDays(27));
        var apUtleder = Mockito.spy(new AksjonspunktUtlederForForeldrepengerFødsel(iayTjeneste, familieHendelseTjeneste,
            repositoryProvider.getYtelsesFordelingRepository()));
        var utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(lagInput(behandling, LocalDate.now()));

        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktUtlederResultat.opprettForAksjonspunkt(SJEKK_MANGLENDE_FØDSEL));
        verify(apUtleder).erFristForRegistreringAvFødselPassert(any(FamilieHendelseGrunnlagEntitet.class));
    }

    @Test
    void sjekk_autopunkt_vent_på_fødsel_dersom_fødsel_og_mindre_enn_8_dager_siden_fødsel() {
        var termindato = LocalDate.now();
        var behandling = opprettBehandlingMedOppgittFødsel(termindato.minusDays(1));
        var apUtleder = Mockito.spy(new AksjonspunktUtlederForForeldrepengerFødsel(iayTjeneste, familieHendelseTjeneste,
            repositoryProvider.getYtelsesFordelingRepository()));
        var utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(lagInput(behandling, termindato));

        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktUtlederResultat.opprettForAksjonspunkt(AUTO_VENT_PÅ_FØDSELREGISTRERING));
        assertThat(utledeteAksjonspunkter.getFirst().frist()).isNotNull();
        assertThat(utledeteAksjonspunkter.getFirst().venteårsak()).isNotNull();
        verify(apUtleder).erFristForRegistreringAvFødselPassert(any(FamilieHendelseGrunnlagEntitet.class));
    }

    @Test
    void aksjonspunkt_terminbekreftelse_dersom_bfhr_med_løpende_arbeid() {
        var termindato = LocalDate.now();
        var behandling = opprettBehandlingMedOppgittTerminOgArbeidsForhold(termindato, Tid.TIDENES_ENDE, OppgittRettighetEntitet.bareSøkerRett());
        var param = lagInput(behandling, termindato);
        var apUtleder = Mockito.spy(new AksjonspunktUtlederForForeldrepengerFødsel(iayTjeneste, familieHendelseTjeneste,
            repositoryProvider.getYtelsesFordelingRepository()));
        var utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(param);
        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktUtlederResultat.opprettForAksjonspunkt(SJEKK_TERMINBEKREFTELSE));
    }

    @Test
    void aksjonspunkt_terminbekreftelse_dersom_far_aleneomsorg_med_løpende_arbeid() {
        var termindato = LocalDate.now();
        var behandling = opprettBehandlingMedOppgittTerminOgArbeidsForhold(termindato, Tid.TIDENES_ENDE, OppgittRettighetEntitet.aleneomsorg());
        var param = lagInput(behandling, termindato);
        var apUtleder = Mockito.spy(new AksjonspunktUtlederForForeldrepengerFødsel(iayTjeneste, familieHendelseTjeneste,
            repositoryProvider.getYtelsesFordelingRepository()));
        var utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(param);
        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktUtlederResultat.opprettForAksjonspunkt(SJEKK_TERMINBEKREFTELSE));
    }

    private Behandling opprettBehandlingFarSøkerFødselRegistrertIPDL(LocalDate fødseldato, int antallBarnSøknad, int antallBarnPDL) {
        var scenario = ScenarioFarSøkerForeldrepenger
            .forFødsel();
        scenario.medSøknadHendelse()
            .medFødselsDato(fødseldato, antallBarnSøknad)
            .medAntallBarn(antallBarnSøknad);
        leggTilSøker(scenario, NavBrukerKjønn.MANN);
        scenario.medBekreftetHendelse().medFødselsDato(fødseldato, antallBarnPDL).medAntallBarn(antallBarnPDL);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingMedOppgittTerminOgBehandlingType(LocalDate termindato) {
        var scenario = ScenarioFarSøkerForeldrepenger
            .forFødselMedGittAktørId(GITT_AKTØR_ID)
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medUtstedtDato(LocalDate.now())
            .medTermindato(termindato)
            .medNavnPå("LEGEN MIN"));
        leggTilSøker(scenario, NavBrukerKjønn.MANN);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingMedOppgittTerminOgArbeidsForhold(LocalDate termindato,
                                                                         LocalDate tilOgMed,
                                                                         OppgittRettighetEntitet oppgittRettighet) {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(GITT_AKTØR_ID)
            .medOppgittRettighet(oppgittRettighet);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medUtstedtDato(termindato.minusWeeks(8))
            .medTermindato(termindato)
            .medNavnPå("LEGEN MIN"));
        leggTilSøker(scenario, NavBrukerKjønn.KVINNE);
        var behandling = scenario.lagre(repositoryProvider);
        var inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        lagAktørArbeid(inntektArbeidYtelseAggregatBuilder, GITT_AKTØR_ID, ORG_NR, termindato, tilOgMed);
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

    private void lagAktørArbeid(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder,
                                AktørId aktørId,
                                String orgNr,
                                LocalDate termindato,
                                LocalDate tilOgMed) {
        var aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder.getAktørArbeidBuilder(aktørId);

        var yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(Opptjeningsnøkkel.forOrgnummer(orgNr),
            ArbeidType.FORENKLET_OPPGJØRSORDNING);
        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();

        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(aktivitetsAvtaleBuilder
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(termindato.minusMonths(50), tilOgMed)));

        yrkesaktivitetBuilder.medArbeidType(ArbeidType.FORENKLET_OPPGJØRSORDNING);

        yrkesaktivitetBuilder.medArbeidsgiver(Arbeidsgiver.virksomhet(ORG_NR));
        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
    }

}
