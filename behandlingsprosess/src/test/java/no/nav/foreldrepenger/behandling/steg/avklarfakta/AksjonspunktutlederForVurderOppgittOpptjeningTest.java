package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Spy;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittUtenlandskVirksomhet;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.AksjonspunktutlederForVurderOppgittOpptjening;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.util.FPDateUtil;

public class AksjonspunktutlederForVurderOppgittOpptjeningTest {
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private OpptjeningRepository opptjeningRepository;

    private Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(FPDateUtil.iDag()).build();

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private VirksomhetTjeneste virksomhetTjeneste = mock(VirksomhetTjeneste.class);

    @Spy
    private AksjonspunktutlederForVurderOppgittOpptjening utleder = new AksjonspunktutlederForVurderOppgittOpptjening(
        repositoryProvider.getOpptjeningRepository(), iayTjeneste, virksomhetTjeneste);

    @Before
    public void oppsett() {
        initMocks(this);
        opptjeningRepository = repositoryProvider.getOpptjeningRepository();
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunktet_5051() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);
        Behandling behandling = lagre(scenario);
        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    private AksjonspunktUtlederInput lagRef(Behandling behandling) {
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, skjæringstidspunkt));
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_bruker_har_hatt_vartpenger() {
        // Arrange
        Behandling behandling = opprettBehandling(ArbeidType.VENTELØNN_VARTPENGER);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_bruker_har_oppgitt_frilansperiode() {
        // Arrange
        Behandling behandling = opprettBehandling(ArbeidType.FRILANSER);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_bruker_har_hatt_ventelønn() {
        // Arrange
        Behandling behandling = opprettBehandling(ArbeidType.VENTELØNN_VARTPENGER);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_bruker_har_hatt_militær_eller_siviltjeneste() {
        // Arrange
        Behandling behandling = opprettBehandling(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_bruker_har_hatt_etterlønn_sluttvederlag() {
        // Arrange
        Behandling behandling = opprettBehandling(ArbeidType.ETTERLØNN_SLUTTPAKKE);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_bruker_har_hatt_videre_og_etterutdanning() {
        // Arrange
        Behandling behandling = opprettBehandling(ArbeidType.LØNN_UNDER_UTDANNING);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_bruker_er_selvstendig_næringsdrivende_og_ikke_hatt_næringsinntekt_eller_registrert_næringen_senere() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        Behandling behandling = opprettOppgittOpptjening(aktørId, false);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_bruker_har_utenlandsforhold() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        Behandling behandling = opprettUtenlandskArbeidsforhold(aktørId);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_om_bruker_er_selvstendig_næringsdrivende_og_ikke_hatt_næringsinntekt_og_registrert_næringen_senere() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        LocalDate fraOgMed = LocalDate.now().minusMonths(1);
        LocalDate tilOgMed = LocalDate.now().plusMonths(1);
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);

        Behandling behandling = lagre(scenario);

        OppgittOpptjeningBuilder.EgenNæringBuilder egenNæringBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny();
        OppgittUtenlandskVirksomhet svenska_stat = new OppgittUtenlandskVirksomhet(Landkoder.SWE, "Svenska Stat");
        egenNæringBuilder
            .medPeriode(periode)
            .medUtenlandskVirksomhet(svenska_stat)
            .medBegrunnelse("Vet ikke")
            .medBruttoInntekt(BigDecimal.valueOf(100000))
            .medRegnskapsførerNavn("Jacob")
            .medRegnskapsførerTlf("+46678456345")
            .medVirksomhetType(VirksomhetType.FISKE)
            .medVirksomhet(KUNSTIG_ORG);
        OppgittOpptjeningBuilder oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        oppgittOpptjeningBuilder
            .leggTilEgneNæringer(Collections.singletonList(egenNæringBuilder));

        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_om_bruker_er_selvstendig_næringsdrivende_og_hatt_næringsinntekt() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        Behandling behandling = opprettOppgittOpptjening(aktørId, true);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    private Behandling opprettUtenlandskArbeidsforhold(AktørId aktørId) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId);
        Behandling behandling = lagre(scenario);

        LocalDate fraOgMed = LocalDate.now().minusMonths(1);
        LocalDate tilOgMed = LocalDate.now().plusMonths(1);
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);
        OppgittUtenlandskVirksomhet svenska_stat = new OppgittUtenlandskVirksomhet(Landkoder.SWE, "Svenska Stat");
        OppgittOpptjeningBuilder oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        oppgittOpptjeningBuilder
            .leggTilOppgittArbeidsforhold(OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.ny().medUtenlandskVirksomhet(svenska_stat).medPeriode(periode)
                .medErUtenlandskInntekt(true).medArbeidType(ArbeidType.UTENLANDSK_ARBEIDSFORHOLD));

        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);

        lagreOpptjeningsPeriode(behandling, tilOgMed);
        return behandling;
    }

    private Behandling opprettOppgittOpptjening(AktørId aktørId, boolean medNæring) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId);

        LocalDate fraOgMed = LocalDate.now().minusMonths(1);
        LocalDate tilOgMed = LocalDate.now().plusMonths(1);
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);

        OppgittOpptjeningBuilder.EgenNæringBuilder egenNæringBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny();
        OppgittUtenlandskVirksomhet svenska_stat = new OppgittUtenlandskVirksomhet(Landkoder.SWE, "Svenska Stat");
        egenNæringBuilder
            .medPeriode(periode)
            .medUtenlandskVirksomhet(svenska_stat)
            .medBegrunnelse("Vet ikke")
            .medBruttoInntekt(BigDecimal.valueOf(100000))
            .medRegnskapsførerNavn("Jacob")
            .medRegnskapsførerTlf("+46678456345")
            .medVirksomhetType(VirksomhetType.FISKE);
        OppgittOpptjeningBuilder oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        oppgittOpptjeningBuilder
            .leggTilEgneNæringer(Collections.singletonList(egenNæringBuilder));

        Behandling behandling = lagre(scenario);
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);

        if (medNæring) {
            var iayAggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
            var aktørInntektBuilder = iayAggregatBuilder.getAktørInntektBuilder(aktørId);
            var inntektBuilder = aktørInntektBuilder
                .getInntektBuilder(InntektsKilde.SIGRUN, new Opptjeningsnøkkel(null, null, aktørId.getId()));
            var inntektspost = inntektBuilder.getInntektspostBuilder()
                .medBeløp(BigDecimal.TEN)
                .medPeriode(LocalDate.now().minusYears(2L), LocalDate.now().minusYears(1L))
                .medInntektspostType(InntektspostType.SELVSTENDIG_NÆRINGSDRIVENDE);

            inntektBuilder.leggTilInntektspost(inntektspost);
            aktørInntektBuilder.leggTilInntekt(inntektBuilder);
            iayAggregatBuilder.leggTilAktørInntekt(aktørInntektBuilder);

            iayTjeneste.lagreIayAggregat(behandling.getId(), iayAggregatBuilder);
        }


        lagreOpptjeningsPeriode(behandling, tilOgMed);
        return behandling;
    }

    private Behandling opprettBehandling(ArbeidType annenOpptjeningType) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        LocalDate fraOgMed = LocalDate.now().minusMonths(1);
        LocalDate tilOgMed = LocalDate.now().plusMonths(1);
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);

        OppgittOpptjeningBuilder oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        oppgittOpptjeningBuilder
            .leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode, annenOpptjeningType));

        Behandling behandling = lagre(scenario);

        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);

        lagreOpptjeningsPeriode(behandling, tilOgMed);
        return behandling;
    }

    private void lagreOpptjeningsPeriode(Behandling behandling, LocalDate opptjeningTom) {
        opptjeningRepository.lagreOpptjeningsperiode(behandling, opptjeningTom.minusMonths(10), opptjeningTom, false);
    }
}
