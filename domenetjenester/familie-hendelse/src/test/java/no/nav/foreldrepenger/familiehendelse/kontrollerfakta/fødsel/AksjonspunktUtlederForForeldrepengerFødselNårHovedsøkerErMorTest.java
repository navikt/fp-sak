package no.nav.foreldrepenger.familiehendelse.kontrollerfakta.fødsel;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENT_PÅ_FØDSELREGISTRERING;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon.Builder;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.konfig.Tid;

public class AksjonspunktUtlederForForeldrepengerFødselNårHovedsøkerErMorTest extends EntityManagerAwareTest {

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
        apUtleder = Mockito.spy(new AksjonspunktUtlederForForeldrepengerFødsel(iayTjeneste, familieHendelseTjeneste));
    }

    @Test
    public void sjekk_manglende_fødsel_dersom_termindato_mer_enn_25_dager_siden() {
        Behandling behandling = opprettBehandlingMedOppgittTerminOgBehandlingType(TERMINDATO_27_SIDEN);
        List<AksjonspunktResultat> utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(lagInput(behandling));

        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktResultat.opprettForAksjonspunkt(SJEKK_MANGLENDE_FØDSEL));
        verify(apUtleder).erFristForRegistreringAvFødselPassert(any(FamilieHendelseGrunnlagEntitet.class));
    }

    private AksjonspunktUtlederInput lagInput(Behandling behandling) {
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, skjæringstidspunkt));
    }

    @Test
    public void sjekk_manglende_fødsel_dersom_fødsel_og_mer_enn_14_dager_siden_fødsel() {
        Behandling behandling = opprettBehandlingMedOppgittFødsel(FØDSEL_17_SIDEN);
        List<AksjonspunktResultat> utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(lagInput(behandling));

        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktResultat.opprettForAksjonspunkt(SJEKK_MANGLENDE_FØDSEL));
        verify(apUtleder).erFristForRegistreringAvFødselPassert(any(FamilieHendelseGrunnlagEntitet.class));
    }

    @Test
    public void sjekk_autopunkt_vent_på_fødsel_dersom_fødsel_og_mindre_enn_14_dager_siden_fødsel() {
        Behandling behandling = opprettBehandlingMedOppgittFødsel(LocalDate.now());
        List<AksjonspunktResultat> utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(lagInput(behandling));

        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktResultat.opprettForAksjonspunkt(AUTO_VENT_PÅ_FØDSELREGISTRERING));
        assertThat(utledeteAksjonspunkter.get(0).getFrist()).isNotNull();
        assertThat(utledeteAksjonspunkter.get(0).getVenteårsak()).isNotNull();
        verify(apUtleder).erFristForRegistreringAvFødselPassert(any(FamilieHendelseGrunnlagEntitet.class));
    }

    @Test
    public void ingen_akjsonspunkter_dersom_fødsel_registrert_i_TPS_og_antall_barn_stemmer_med_søknad() {

        // Oppsett
        AksjonspunktUtlederForForeldrepengerFødsel testObjekt = Mockito.spy(new AksjonspunktUtlederForForeldrepengerFødsel(mock(InntektArbeidYtelseTjeneste.class), familieHendelseTjeneste));

        Behandling behandling = opprettBehandlingForFødselRegistrertITps(LocalDate.now(), 1, 1);
        List<AksjonspunktResultat> utledeteAksjonspunkter = testObjekt.utledAksjonspunkterFor(lagInput(behandling));

        assertThat(utledeteAksjonspunkter).isEmpty();
        verify(testObjekt).samsvarerAntallBarnISøknadMedAntallBarnITps(any(FamilieHendelseGrunnlagEntitet.class));
    }

    @Test
    public void sjekk_manglende_fødsel_dersom_fødsel_registrert_i_TPS_og_antall_barn_ikke_stemmer_med_søknad() {
        // Oppsett

        AksjonspunktUtlederForForeldrepengerFødsel testObjekt = Mockito.spy(new AksjonspunktUtlederForForeldrepengerFødsel(mock(InntektArbeidYtelseTjeneste.class), familieHendelseTjeneste));

        Behandling behandling = opprettBehandlingForFødselRegistrertITps(LocalDate.now(), 2, 1);
        List<AksjonspunktResultat> utledeteAksjonspunkter = testObjekt.utledAksjonspunkterFor(lagInput(behandling));

        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktResultat.opprettForAksjonspunkt(SJEKK_MANGLENDE_FØDSEL));
        verify(testObjekt).samsvarerAntallBarnISøknadMedAntallBarnITps(any(FamilieHendelseGrunnlagEntitet.class));
    }

    @Test
    public void avklar_terminbekreftelse_dersom_termindato_nå_og_mangler_løpende_arbeid() {
        //Arrange
        Behandling behandling = opprettBehandlingMedOppgittTerminOgBehandlingType(TERMINDAT0_NÅ);
        //Act
        AksjonspunktUtlederInput param = lagInput(behandling);
        List<AksjonspunktResultat> utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(param);
        //Assert
        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktResultat.opprettForAksjonspunkt(AVKLAR_TERMINBEKREFTELSE));
        verify(apUtleder).erSøkerRegistrertArbeidstakerMedLøpendeArbeidsforholdIAARegisteret(param);
    }

    @Test
    public void ingen_aksjonspunkter_dersom_løpende_arbeid() {
        //Arrange
        Behandling behandling = opprettBehandlingMedOppgittTerminOgArbeidsForhold(TERMINDAT0_NÅ, Tid.TIDENES_ENDE);
        AksjonspunktUtlederForForeldrepengerFødsel apUtleder = Mockito.spy(new AksjonspunktUtlederForForeldrepengerFødsel(iayTjeneste, familieHendelseTjeneste));
        //Act
        AksjonspunktUtlederInput param = lagInput(behandling);
        List<AksjonspunktResultat> utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(param);
        //Assert
        assertThat(utledeteAksjonspunkter).isEmpty();
        verify(apUtleder).erSøkerRegistrertArbeidstakerMedLøpendeArbeidsforholdIAARegisteret(param);
    }

    @Test
    public void ingen_aksjonspunkter_dersom_tidsavgrenset_arbforhold_over_stp() {
        //Arrange
        Behandling behandling = opprettBehandlingMedOppgittTerminOgArbeidsForhold(TERMINDAT0_NÅ, LocalDate.now().plusMonths(6));
        AksjonspunktUtlederForForeldrepengerFødsel apUtleder = Mockito.spy(new AksjonspunktUtlederForForeldrepengerFødsel(iayTjeneste, familieHendelseTjeneste));
        //Act
        AksjonspunktUtlederInput param = lagInput(behandling);
        List<AksjonspunktResultat> utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(param);
        //Assert
        assertThat(utledeteAksjonspunkter).isEmpty();
        verify(apUtleder).erSøkerRegistrertArbeidstakerMedLøpendeArbeidsforholdIAARegisteret(param);
    }

    @Test
    public void avklar_terminbekreftelse_dersom_har_avsluttet_arbeidsforhold() {
        //Arrange
        Behandling behandling = opprettBehandlingMedOppgittTerminOgArbeidsForhold(TERMINDAT0_NÅ, AVSLUTTET_TIDSPUNKT);
        //Act
        AksjonspunktUtlederInput param = lagInput(behandling);
        List<AksjonspunktResultat> utledeteAksjonspunkter = apUtleder.utledAksjonspunkterFor(param);
        //Assert
        assertThat(utledeteAksjonspunkter).containsExactly(AksjonspunktResultat.opprettForAksjonspunkt(AVKLAR_TERMINBEKREFTELSE));
        verify(apUtleder).erSøkerRegistrertArbeidstakerMedLøpendeArbeidsforholdIAARegisteret(param);
    }

    private Behandling opprettBehandlingForFødselRegistrertITps(LocalDate fødseldato, int antallBarnSøknad, int antallBarnTps) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger
            .forFødsel();
        scenario.medSøknadHendelse()
            .medFødselsDato(fødseldato)
            .medAntallBarn(antallBarnSøknad);
        scenario.medBekreftetHendelse().medFødselsDato(fødseldato).medAntallBarn(antallBarnTps);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingMedOppgittFødsel(LocalDate fødseldato) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(fødseldato);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingMedOppgittTerminOgArbeidsForhold(LocalDate termindato, LocalDate tilOgMed) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger
            .forFødselMedGittAktørId(GITT_AKTØR_ID);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medUtstedtDato(LocalDate.now())
            .medTermindato(termindato)
            .medNavnPå("LEGEN MIN"));
        leggTilSøker(scenario, NavBrukerKjønn.KVINNE);
        Behandling behandling = scenario.lagre(repositoryProvider);
        final InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        lagAktørArbeid(inntektArbeidYtelseAggregatBuilder, GITT_AKTØR_ID, ORG_NR, tilOgMed);
        iayTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder);
        return behandling;
    }

    private Behandling opprettBehandlingMedOppgittTerminOgBehandlingType(LocalDate termindato) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger
            .forFødselMedGittAktørId(GITT_AKTØR_ID);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medUtstedtDato(LocalDate.now())
            .medTermindato(termindato)
            .medNavnPå("LEGEN MIN"));

        Behandling behandling = scenario.lagre(repositoryProvider);
        return behandling;
    }

    private void lagAktørArbeid(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder, AktørId aktørId, String orgNr, LocalDate tilOgMed) {
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder =
            inntektArbeidYtelseAggregatBuilder.getAktørArbeidBuilder(aktørId);

        YrkesaktivitetBuilder yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(Opptjeningsnøkkel.forOrgnummer(orgNr),
            ArbeidType.FORENKLET_OPPGJØRSORDNING);
        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();

        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(aktivitetsAvtaleBuilder
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(50), tilOgMed)));

        yrkesaktivitetBuilder.medArbeidType(ArbeidType.FORENKLET_OPPGJØRSORDNING);

        yrkesaktivitetBuilder.medArbeidsgiver(Arbeidsgiver.virksomhet(ORG_NR));
        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
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
