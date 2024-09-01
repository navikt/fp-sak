package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
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

class AksjonspunktutlederForVurderOppgittOpptjeningTest extends EntityManagerAwareTest {

    private final Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(LocalDate.now()).build();

    private OpptjeningRepository opptjeningRepository;

    private AksjonspunktutlederForVurderOppgittOpptjening utleder;
    private AbakusInMemoryInntektArbeidYtelseTjeneste iayTjeneste;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        opptjeningRepository = new OpptjeningRepository(entityManager, new BehandlingRepository(entityManager));
        iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        utleder = new AksjonspunktutlederForVurderOppgittOpptjening(opptjeningRepository, iayTjeneste, mock(VirksomhetTjeneste.class));
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(new BehandlingRepositoryProvider(getEntityManager()));
    }

    @Test
    void skal_ikke_opprette_aksjonspunktet_5051() {
        // Arrange
        var aktørId1 = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);
        var behandling = lagre(scenario);
        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    private AksjonspunktUtlederInput lagRef(Behandling behandling) {
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling), skjæringstidspunkt);
    }

    @Test
    void skal_opprette_aksjonspunkt_om_bruker_har_hatt_vartpenger() {
        // Arrange
        var behandling = opprettBehandling(ArbeidType.VENTELØNN_VARTPENGER);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    void skal_opprette_aksjonspunkt_om_bruker_har_oppgitt_frilansperiode() {
        // Arrange
        var behandling = opprettBehandling(ArbeidType.FRILANSER);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    void skal_opprette_aksjonspunkt_om_bruker_har_hatt_ventelønn() {
        // Arrange
        var behandling = opprettBehandling(ArbeidType.VENTELØNN_VARTPENGER);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    void skal_opprette_aksjonspunkt_om_bruker_har_hatt_militær_eller_siviltjeneste() {
        // Arrange
        var behandling = opprettBehandling(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    void skal_opprette_aksjonspunkt_om_bruker_har_hatt_etterlønn_sluttvederlag() {
        // Arrange
        var behandling = opprettBehandling(ArbeidType.ETTERLØNN_SLUTTPAKKE);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    void skal_opprette_aksjonspunkt_om_bruker_har_hatt_videre_og_etterutdanning() {
        // Arrange
        var behandling = opprettBehandling(ArbeidType.LØNN_UNDER_UTDANNING);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    void skal_opprette_aksjonspunkt_om_bruker_er_selvstendig_næringsdrivende_og_ikke_hatt_næringsinntekt_eller_registrert_næringen_senere() {
        // Arrange
        var aktørId = AktørId.dummy();
        var behandling = opprettOppgittOpptjening(aktørId, false);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    void skal_opprette_aksjonspunkt_om_bruker_har_utenlandsforhold() {
        // Arrange
        var aktørId = AktørId.dummy();
        var behandling = opprettUtenlandskArbeidsforhold(aktørId);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_om_bruker_er_selvstendig_næringsdrivende_og_ikke_hatt_næringsinntekt_og_registrert_næringen_senere() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        var fraOgMed = LocalDate.now().minusMonths(1);
        var tilOgMed = LocalDate.now().plusMonths(1);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);

        var behandling = lagre(scenario);

        var egenNæringBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny();
        var svenska_stat = new OppgittUtenlandskVirksomhet(Landkoder.SWE, "Svenska Stat");
        egenNæringBuilder
                .medPeriode(periode)
                .medUtenlandskVirksomhet(svenska_stat)
                .medBegrunnelse("Vet ikke")
                .medBruttoInntekt(BigDecimal.valueOf(100000))
                .medRegnskapsførerNavn("Jacob")
                .medRegnskapsførerTlf("TELEFON")
                .medVirksomhetType(VirksomhetType.FISKE)
                .medVirksomhet(KUNSTIG_ORG);
        var oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        oppgittOpptjeningBuilder
                .leggTilEgenNæring(Collections.singletonList(egenNæringBuilder));

        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_om_bruker_er_selvstendig_næringsdrivende_og_hatt_næringsinntekt() {
        // Arrange
        var aktørId = AktørId.dummy();
        var behandling = opprettOppgittOpptjening(aktørId, true);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    private Behandling opprettUtenlandskArbeidsforhold(AktørId aktørId) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId);
        var behandling = lagre(scenario);

        var fraOgMed = LocalDate.now().minusMonths(1);
        var tilOgMed = LocalDate.now().plusMonths(1);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);
        var svenska_stat = new OppgittUtenlandskVirksomhet(Landkoder.SWE, "Svenska Stat");
        var oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        oppgittOpptjeningBuilder
                .leggTilOppgittArbeidsforhold(
                        OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.ny().medUtenlandskVirksomhet(svenska_stat).medPeriode(periode)
                                .medErUtenlandskInntekt(true).medArbeidType(ArbeidType.UTENLANDSK_ARBEIDSFORHOLD).build());

        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);

        lagreOpptjeningsPeriode(behandling, tilOgMed);
        return behandling;
    }

    private Behandling opprettOppgittOpptjening(AktørId aktørId, boolean medNæring) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId);

        var fraOgMed = LocalDate.now().minusMonths(1);
        var tilOgMed = LocalDate.now().plusMonths(1);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);

        var egenNæringBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny();
        var svenska_stat = new OppgittUtenlandskVirksomhet(Landkoder.SWE, "Svenska Stat");
        egenNæringBuilder
                .medPeriode(periode)
                .medUtenlandskVirksomhet(svenska_stat)
                .medBegrunnelse("Vet ikke")
                .medBruttoInntekt(BigDecimal.valueOf(100000))
                .medRegnskapsførerNavn("Jacob")
                .medRegnskapsførerTlf("TELEFON")
                .medVirksomhetType(VirksomhetType.FISKE);
        var oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        oppgittOpptjeningBuilder
                .leggTilEgenNæring(Collections.singletonList(egenNæringBuilder));

        var behandling = lagre(scenario);
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

        var fraOgMed = LocalDate.now().minusMonths(1);
        var tilOgMed = LocalDate.now().plusMonths(1);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);

        var oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        oppgittOpptjeningBuilder
                .leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode, annenOpptjeningType));

        var behandling = lagre(scenario);

        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);

        lagreOpptjeningsPeriode(behandling, tilOgMed);
        return behandling;
    }

    private void lagreOpptjeningsPeriode(Behandling behandling, LocalDate opptjeningTom) {
        opptjeningRepository.lagreOpptjeningsperiode(behandling, opptjeningTom.minusMonths(10), opptjeningTom, false);
    }
}
