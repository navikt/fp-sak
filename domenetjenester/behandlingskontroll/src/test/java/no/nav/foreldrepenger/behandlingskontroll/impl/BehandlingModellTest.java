package no.nav.foreldrepenger.behandlingskontroll.impl;

import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettForAksjonspunkt;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettForAksjonspunktMedFrist;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingskontroll.testutilities.TestScenario;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.JpaExtension;

@ExtendWith(JpaExtension.class)
class BehandlingModellTest {

    private static final LocalDateTime FRIST_TID = LocalDateTime.now().plusWeeks(4).withNano(0);

    private final BehandlingType behandlingType = BehandlingType.FØRSTEGANGSSØKNAD;
    private final FagsakYtelseType fagsakYtelseType = FagsakYtelseType.ENGANGSTØNAD;

    private static final BehandlingStegType STEG_1 = BehandlingStegType.INNHENT_REGISTEROPP;
    private static final BehandlingStegType STEG_2 = BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT;
    private static final BehandlingStegType STEG_3 = BehandlingStegType.SØKERS_RELASJON_TIL_BARN;
    private static final BehandlingStegType STEG_4 = BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR;

    private BehandlingskontrollTjeneste kontrollTjeneste;
    private BehandlingskontrollServiceProvider serviceProvider;

    private final DummySteg nullSteg = new DummySteg();
    private final DummyVenterSteg nullVenterSteg = new DummyVenterSteg();
    private final DummySteg aksjonspunktSteg = new DummySteg(
            opprettForAksjonspunkt(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL));
    private final DummySteg aksjonspunktModifisererSteg = new DummySteg(
            opprettForAksjonspunktMedFrist(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL, Venteårsak.AVV_DOK, FRIST_TID));

    @BeforeEach
    void setUp(EntityManager entityManager) {
        serviceProvider = new BehandlingskontrollServiceProvider(entityManager, null);
        kontrollTjeneste = new BehandlingskontrollTjenesteImpl(serviceProvider);
    }

    @Test
    void skal_finne_aksjonspunkter_som_ligger_etter_et_gitt_steg() {
        // Arrange - noen utvalge, tilfeldige aksjonspunkter
        var a0_0 = AksjonspunktDefinisjon.FORDEL_BEREGNINGSGRUNNLAG;
        var a0_1 = AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL;
        var a1_0 = AksjonspunktDefinisjon.AVKLAR_ADOPSJONSDOKUMENTAJON;
        var a1_1 = AksjonspunktDefinisjon.VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR;
        var a2_0 = AksjonspunktDefinisjon.FASTSETT_UTTAKPERIODER;
        var a2_1 = AksjonspunktDefinisjon.VURDER_PERMISJON_UTEN_SLUTTDATO;

        var steg = new DummySteg();
        var steg0 = new DummySteg();
        var steg1 = new DummySteg();
        var steg2 = new DummySteg();

        var modellData = List.of(
                new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, steg, ap(), ap()),
                new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, steg0, ap(a0_0), ap(a0_1)),
                new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, steg1, ap(a1_0), ap(a1_1)),
                new TestStegKonfig(STEG_4, behandlingType, fagsakYtelseType, steg2, ap(a2_0), ap(a2_1)));

        var modell = setupModell(modellData);

        var ads = modell.finnAksjonspunktDefinisjonerEtter(STEG_1);

        assertThat(ads).

                containsOnly(a0_0, a0_1, a1_0, a1_1, a2_0, a2_1);

        ads = modell.finnAksjonspunktDefinisjonerEtter(STEG_2);

        assertThat(ads).

                containsOnly(a1_0, a1_1, a2_0, a2_1);

        ads = modell.finnAksjonspunktDefinisjonerEtter(STEG_3);

        assertThat(ads).

                containsOnly(a2_0, a2_1);

        ads = modell.finnAksjonspunktDefinisjonerEtter(STEG_4);

        assertThat(ads).

                isEmpty();

    }

    @Test
    void skal_finne_aksjonspunkter_ved_inngang_eller_utgang_av_steg() {
        // Arrange - noen utvalge, tilfeldige aksjonspunkter
        var a0_0 = AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING;
        var a0_1 = AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL;
        var a1_0 = AksjonspunktDefinisjon.AVKLAR_ADOPSJONSDOKUMENTAJON;
        var a1_1 = AksjonspunktDefinisjon.VURDER_MEDLEMSKAPSVILKÅRET;

        var steg = new DummySteg();
        var steg0 = new DummySteg();
        var steg1 = new DummySteg();

        var modellData = List.of(
                new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, steg, ap(), ap()),
                new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, steg0, ap(a0_0), ap(a0_1)),
                new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, steg1, ap(a1_0), ap(a1_1)));

        var modell = setupModell(modellData);

        var ads = modell.finnAksjonspunktDefinisjonerInngang(STEG_1);
        assertThat(ads).isEmpty();

        ads = modell.finnAksjonspunktDefinisjonerInngang(STEG_2);
        assertThat(ads).containsOnly(a0_0);

        ads = modell.finnAksjonspunktDefinisjonerUtgang(STEG_3);
        assertThat(ads).containsOnly(a1_1);

    }

    @Test
    void skal_stoppe_på_steg_2_når_får_aksjonspunkt() {
        // Arrange
        var modellData = List.of(
                new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()),
                new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, aksjonspunktSteg, ap(), ap()),
                new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, nullSteg,
                        ap(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL), ap()),
                new TestStegKonfig(STEG_4, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()));
        var modell = setupModell(modellData);

        var scenario = TestScenario.forEngangsstønad();
        var behandling = scenario.lagre(serviceProvider);
        var visitor = lagVisitor(behandling);
        var siste = modell.prosesserFra(STEG_1, visitor);

        assertThat(siste.behandlingStegType()).isEqualTo(STEG_3);
        assertThat(visitor.kjørteSteg).isEqualTo(List.of(STEG_1, STEG_2, STEG_3));
    }

    public List<AksjonspunktDefinisjon> ap(AksjonspunktDefinisjon... aksjonspunktDefinisjoner) {
        return List.of(aksjonspunktDefinisjoner);
    }

    @Test
    void skal_kjøre_til_siste_når_ingen_gir_aksjonspunkt() {
        // Arrange
        var modellData = List.of(
                new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()),
                new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()),
                new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()));
        var modell = setupModell(modellData);

        var scenario = TestScenario.forEngangsstønad();
        var behandling = scenario.lagre(serviceProvider);
        var visitor = lagVisitor(behandling);
        var siste = modell.prosesserFra(STEG_1, visitor);

        assertThat(siste).isNull();
        assertThat(visitor.kjørteSteg).isEqualTo(List.of(STEG_1, STEG_2, STEG_3));
    }

    @Test
    void skal_stoppe_når_settes_på_vent_deretter_fortsette() {
        // Arrange
        var modellData = List.of(
                new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()),
                new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, nullVenterSteg, ap(), ap()),
                new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()));
        var modell = setupModell(modellData);

        var scenario = TestScenario.forEngangsstønad();
        var behandling = scenario.lagre(serviceProvider);

        // Act 1
        var visitor = lagVisitor(behandling);

        var første = modell.prosesserFra(STEG_1, visitor);

        assertThat(første).isNotNull();
        assertThat(første.behandlingStegType()).isEqualTo(STEG_2);
        assertThat(første.resultat()).isEqualTo(BehandlingStegStatus.STARTET);
        assertThat(visitor.kjørteSteg).isEqualTo(List.of(STEG_1, STEG_2));

        // Act 2
        var visitorNeste = lagVisitor(behandling);
        var neste = modell.prosesserFra(STEG_2, visitorNeste);

        assertThat(neste).isNotNull();
        assertThat(neste.behandlingStegType()).isEqualTo(STEG_2);
        assertThat(neste.resultat()).isEqualTo(BehandlingStegStatus.VENTER);
        assertThat(visitorNeste.kjørteSteg).isEqualTo(List.of(STEG_2));

        // Act 3
        var visitorNeste2 = lagVisitor(behandling);
        var neste2 = modell.prosesserFra(STEG_2, visitorNeste2);

        assertThat(neste2).isNotNull();
        assertThat(neste2.behandlingStegType()).isEqualTo(STEG_2);
        assertThat(neste2.resultat()).isEqualTo(BehandlingStegStatus.VENTER);
        assertThat(visitorNeste2.kjørteSteg).isEqualTo(List.of(STEG_2));

        // Act 4
        var gjenoppta = lagVisitorVenter(behandling);

        var fortsett = modell.prosesserFra(STEG_2, gjenoppta);
        assertThat(fortsett).isNull();
        assertThat(gjenoppta.kjørteSteg).isEqualTo(List.of(STEG_2, STEG_3));
    }

    @Test
    void skal_feile_ved_gjenopptak_vanlig_steg() {
        // Arrange
        var modellData = List.of(
                new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()),
                new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()),
                new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()));
        var modell = setupModell(modellData);

        var scenario = TestScenario.forEngangsstønad();
        var behandling = scenario.lagre(serviceProvider);

        // Act 1
        var visitor = lagVisitorVenter(behandling);
        assertThrows(IllegalStateException.class, () -> modell.prosesserFra(STEG_1, visitor));
    }

    @Test
    void tilbakefører_til_tidligste_steg_med_åpent_aksjonspunkt() {
        var aksjonspunktDefinisjon = STEG_2.getAksjonspunktDefinisjonerUtgang().get(0);
        var tilbakeføringssteg = new DummySteg(true, opprettForAksjonspunkt(aksjonspunktDefinisjon));
        // Arrange
        var modellData = List.of(
                new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, nullSteg, ap(aksjonspunktDefinisjon), ap()),
                new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()),
                new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, tilbakeføringssteg, ap(), ap()),
                new TestStegKonfig(STEG_4, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()));
        var modell = setupModell(modellData);

        var scenario = TestScenario.forEngangsstønad();
        var behandling = scenario.lagre(serviceProvider);
        var visitor = lagVisitor(behandling);

        var aksjonspunkt = serviceProvider.getAksjonspunktKontrollRepository()
                .leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon, STEG_1);
        serviceProvider.getAksjonspunktKontrollRepository().setReåpnet(aksjonspunkt);

        var siste = modell.prosesserFra(STEG_3, visitor);
        assertThat(siste.behandlingStegType()).isEqualTo(STEG_3);

        var beh = hentBehandling(behandling.getId());
        assertThat(beh.getAktivtBehandlingSteg()).isEqualTo(STEG_2);
    }

    @Test
    void finner_tidligste_steg_for_aksjonspunkter() {
        var aksjonspunktDefinisjon = STEG_3.getAksjonspunktDefinisjonerInngang().get(0);
        var modellData = List.of(
                new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, nullSteg, ap(aksjonspunktDefinisjon), ap()),
                new TestStegKonfig(STEG_4, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()));

        var modell = setupModell(modellData);
        Set<AksjonspunktDefinisjon> aksjonspunktDefinisjoner = new HashSet<>();
        aksjonspunktDefinisjoner.add(aksjonspunktDefinisjon);
        var behandlingStegModell = modell.finnTidligsteStegForAksjonspunktDefinisjon(aksjonspunktDefinisjoner);
        assertThat(behandlingStegModell.getBehandlingStegType()).isEqualTo(STEG_3);
    }

    @Test
    void skal_modifisere_aksjonspunktet_ved_å_kalle_funksjon_som_legger_til_frist() {
        // Arrange
        var modellData = List.of(
                new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, aksjonspunktModifisererSteg, ap(), ap()),
                new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()),
                new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, nullSteg,
                        ap(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL), ap()));
        var modell = setupModell(modellData);
        var scenario = TestScenario.forEngangsstønad();
        var behandling = scenario.lagre(serviceProvider);
        var visitor = lagVisitor(behandling);

        // Act
        modell.prosesserFra(STEG_1, visitor);

        // Assert
        var beh = hentBehandling(behandling.getId());
        assertThat(beh.getÅpneAksjonspunkter()).hasSize(1);
        assertThat(beh.getÅpneAksjonspunkter().get(0).getFristTid()).isEqualTo(FRIST_TID);
    }

    private Behandling hentBehandling(Long behandlingId) {
        return serviceProvider.hentBehandling(behandlingId);
    }

    private BehandlingModellImpl setupModell(List<TestStegKonfig> resolve) {
        return ModifiserbarBehandlingModell.setupModell(behandlingType, fagsakYtelseType, resolve);
    }

    private BehandlingStegVisitorUtenLagring lagVisitor(Behandling behandling) {
        var kontekst = kontrollTjeneste.initBehandlingskontroll(behandling, serviceProvider.taLås(behandling.getId()));
        var lokalServiceProvider = new BehandlingskontrollServiceProvider(serviceProvider.getEntityManager(), null);
        return new BehandlingStegVisitorUtenLagring(lokalServiceProvider, kontekst);
    }

    private BehandlingStegVisitorVenterUtenLagring lagVisitorVenter(Behandling behandling) {
        var kontekst = kontrollTjeneste.initBehandlingskontroll(behandling, serviceProvider.taLås(behandling.getId()));
        var lokalServiceProvider = new BehandlingskontrollServiceProvider(serviceProvider.getEntityManager(), null);
        return new BehandlingStegVisitorVenterUtenLagring(lokalServiceProvider, kontekst);
    }

    static class BehandlingStegVisitorUtenLagring extends TekniskBehandlingStegVisitor {
        List<BehandlingStegType> kjørteSteg = new ArrayList<>();

        BehandlingStegVisitorUtenLagring(BehandlingskontrollServiceProvider repositoryProvider,
                BehandlingskontrollKontekst kontekst) {
            super(repositoryProvider, kontekst);
        }

        @Override
        protected StegProsesseringResultat prosesserStegISavepoint(Behandling behandling,
                BehandlingStegVisitor stegVisitor) {
            // bypass savepoint
            this.kjørteSteg.add(stegVisitor.getStegModell().getBehandlingStegType());
            return super.prosesserSteg(stegVisitor);
        }
    }

    static class BehandlingStegVisitorVenterUtenLagring extends TekniskBehandlingStegVenterVisitor {
        List<BehandlingStegType> kjørteSteg = new ArrayList<>();

        BehandlingStegVisitorVenterUtenLagring(BehandlingskontrollServiceProvider repositoryProvider,
                BehandlingskontrollKontekst kontekst) {
            super(repositoryProvider, kontekst);
        }

        @Override
        protected StegProsesseringResultat prosesserStegISavepoint(Behandling behandling,
                BehandlingStegVisitor stegVisitor) {
            // bypass savepoint
            this.kjørteSteg.add(stegVisitor.getStegModell().getBehandlingStegType());
            return super.prosesserSteg(stegVisitor);
        }
    }
}
