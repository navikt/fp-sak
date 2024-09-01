package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

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
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.AksjonspunktutlederForVurderBekreftetOpptjening;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

class AksjonspunktutlederForVurderBekreftetOpptjeningTest extends EntityManagerAwareTest {

    private static final String NAV_ORGNR = "889640782";

    private OpptjeningRepository opptjeningRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private AksjonspunktutlederForVurderBekreftetOpptjening utleder;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        opptjeningRepository = new OpptjeningRepository(entityManager, new BehandlingRepository(entityManager));
        iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        utleder = new AksjonspunktutlederForVurderBekreftetOpptjening(opptjeningRepository, iayTjeneste);
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
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    private AksjonspunktUtlederInput lagInput(Behandling behandling) {
        var ref = BehandlingReferanse.fra(behandling);
        return new AksjonspunktUtlederInput(ref, Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build());
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_når_ingen_arbeidsavtaler_har_0_stillingsprosent() {
        // Arrange
        var aktørId1 = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);
        var behandling = lagre(scenario);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    void skal_opprette_aksjonspunkt_når_en_arbeidsavtale_har_0_stillingsprosent_for_forenklet_oppgjørsordning() {
        // Arrange
        var aktørId1 = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);
        var behandling = lagre(scenario);
        var tilOgMed = LocalDate.now().plusMonths(1);
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER);
        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId1);
        aktørArbeidBuilder.leggTilYrkesaktivitet(byggYrkesaktivitet(tilOgMed, ArbeidType.FORENKLET_OPPGJØRSORDNING, BigDecimal.ZERO));
        builder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        lagreOpptjeningsPeriode(behandling, tilOgMed);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    private YrkesaktivitetBuilder byggYrkesaktivitet(LocalDate tilOgMed, ArbeidType arbeidType, BigDecimal stillingsprosent) {
        var periode = DatoIntervallEntitet.fraOgMed(tilOgMed.minusMonths(10));
        return YrkesaktivitetBuilder.oppdatere(empty())
                .medArbeidType(arbeidType)
                .medArbeidsgiver(Arbeidsgiver.virksomhet(NAV_ORGNR))
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                        .medProsentsats(BigDecimal.ZERO)
                        .medPeriode(periode)
                        .medSisteLønnsendringsdato(periode.getFomDato()))
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                        .medProsentsats(stillingsprosent)
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(tilOgMed.minusMonths(10), tilOgMed)));
    }

    @Test
    void skal_opprette_aksjonspunkt_når_en_arbeidsavtale_har_0_stillingsprosent() {
        // Arrange
        var aktørId1 = AktørId.dummy();
        var tilOgMed = LocalDate.now().plusMonths(1);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);
        var behandling = lagre(scenario);

        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER);
        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId1);
        aktørArbeidBuilder.leggTilYrkesaktivitet(byggYrkesaktivitet(tilOgMed, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ZERO));
        builder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        lagreOpptjeningsPeriode(behandling, tilOgMed);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_når_en_arbeidsavtale_har_0_stillingsprosent_men_utfor_periode() {
        // Arrange
        var aktørId1 = AktørId.dummy();

        var tilOgMed = LocalDate.now().plusMonths(1);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);
        var behandling = lagre(scenario);

        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER);
        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId1);
        aktørArbeidBuilder.leggTilYrkesaktivitet(byggYrkesaktivitet(tilOgMed.minusMonths(11), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ZERO));
        builder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        lagreOpptjeningsPeriode(behandling, tilOgMed);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    private void lagreOpptjeningsPeriode(Behandling behandling, LocalDate opptjeningTom) {
        opptjeningRepository.lagreOpptjeningsperiode(behandling, opptjeningTom.minusMonths(10), opptjeningTom, false);
    }
}
