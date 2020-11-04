package no.nav.foreldrepenger.behandling.steg.avklarfakta.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.YtelserKonsolidertTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class AksjonspunktUtlederForTidligereMottattYtelseTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private AksjonspunktUtlederForTidligereMottattYtelse utleder;

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        utleder = new AksjonspunktUtlederForTidligereMottattYtelse(
            iayTjeneste, new YtelserKonsolidertTjeneste(repositoryProvider));
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_om_søker_ikke_har_mottatt_foreldrepenger_før() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandling = byggBehandling(scenario, aktørId);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    private AksjonspunktUtlederInput lagRef(Behandling behandling) {
        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, skjæringstidspunkt));
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_om_søker_har_mottatt_andre_ytelser_før() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandling = byggBehandling(scenario, aktørId);
        leggTilYtelseForAktør(behandling, RelatertYtelseType.SYKEPENGER, LocalDate.now().minusMonths(1), LocalDate.now());

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_soker_har_mottatt_foreldrepenger_før() {
        // Arrange
        AktørId aktørId = AktørId.dummy();

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandling = byggBehandling(scenario, aktørId);
        leggTilYtelseForAktør(behandling, RelatertYtelseType.FORELDREPENGER, LocalDate.now().minusMonths(8), LocalDate.now().minusMonths(1));

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon())
            .isEqualTo(AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE);
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_soker_har_es_sak_under_behandling() {
        // Arrange
        AktørId aktørId = AktørId.dummy();

        var scenarioES = ScenarioMorSøkerEngangsstønad.forFødsel();
        byggBehandling(scenarioES, aktørId);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandling = byggBehandling(scenario, aktørId);

        leggTilYtelseForAktør(behandling, RelatertYtelseType.SYKEPENGER, LocalDate.now().minusMonths(5), LocalDate.now().minusMonths(4)); // For å

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon())
            .isEqualTo(AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE);
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_soker_har__foreldrepenger_med_framtidig_start() {
        // Arrange
        AktørId aktørId = AktørId.dummy();

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandling = byggBehandling(scenario, aktørId);

        leggTilYtelseForAktør(behandling, RelatertYtelseType.FORELDREPENGER, LocalDate.now().plusMonths(4), LocalDate.now().plusMonths(7));

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon())
            .isEqualTo(AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE);
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_soker_har_mottatt_engangsstønad_etter_fødsel_før_dagens_dato() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        LocalDate fødselsdato = LocalDate.now().minusWeeks(6);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadDato(LocalDate.now());
        Behandling behandling = byggBehandling(scenario, aktørId);

        leggTilYtelseForAktør(behandling, RelatertYtelseType.ENGANGSSTØNAD, fødselsdato.plusWeeks(3), fødselsdato.plusWeeks(3));

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon())
            .isEqualTo(AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_om_soker_har_mottatt_foreldrepenger_for_lenge_siden() {
        // Arrange
        AktørId aktørId = AktørId.dummy();

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandling = byggBehandling(scenario, aktørId);

        leggTilYtelseForAktør(behandling, RelatertYtelseType.FORELDREPENGER, LocalDate.now().minusMonths(15), LocalDate.now().minusMonths(5));

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_hvis_behandling_har_type_REVURDERING() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        Behandling behandling = byggBehandling(scenario, aktørId);

        Behandling.Builder builder = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING);
        Behandling revurdering = builder.build();

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(revurdering));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    private Behandling byggBehandling(AbstractTestScenario<?> scenario, AktørId aktørId) {
        scenario.medBruker(aktørId)
            .medSøknad().medMottattDato(LocalDate.now().minusWeeks(2));
        return lagre(scenario);
    }

    private void leggTilYtelseForAktør(Behandling behandling, RelatertYtelseType relatertYtelseType, LocalDate fom, LocalDate tom) {

        AktørId aktørId = behandling.getAktørId();

        var aggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        aggregatBuilder.leggTilAktørYtelse(AktørYtelseBuilder.oppdatere(Optional.empty()).medAktørId(aktørId)
            .leggTilYtelse(YtelseBuilder.oppdatere(Optional.empty())
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
                .medStatus(RelatertYtelseTilstand.AVSLUTTET)
                .medYtelseType(relatertYtelseType)));
        iayTjeneste.lagreIayAggregat(behandling.getId(), aggregatBuilder);
    }

}
