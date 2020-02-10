package no.nav.foreldrepenger.behandlingskontroll.impl;

import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettForAksjonspunkt;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettForAksjonspunktMedFrist;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegUtfall;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.StegProsesseringResultat;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingskontroll.testutilities.TestScenario;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
@SuppressWarnings("resource")
@RunWith(CdiRunner.class)
public class BehandlingModellTest {

    private static final LocalDateTime FRIST_TID = LocalDateTime.now().plusWeeks(4).withNano(0);

    private final BehandlingType behandlingType = BehandlingType.FØRSTEGANGSSØKNAD;
    private final FagsakYtelseType fagsakYtelseType = FagsakYtelseType.ENGANGSTØNAD;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private static final BehandlingStegType STEG_1 = BehandlingStegType.INNHENT_REGISTEROPP;
    private static final BehandlingStegType STEG_2 = BehandlingStegType.KONTROLLER_FAKTA;
    private static final BehandlingStegType STEG_3 = BehandlingStegType.SØKERS_RELASJON_TIL_BARN;
    private static final BehandlingStegType STEG_4 = BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR;

    @Inject
    private BehandlingskontrollTjeneste kontrollTjeneste;

    @Inject
    private BehandlingskontrollServiceProvider serviceProvider;

    private final DummySteg nullSteg = new DummySteg();
    private final DummyVenterSteg nullVenterSteg = new DummyVenterSteg();
    private final DummySteg aksjonspunktSteg = new DummySteg(opprettForAksjonspunkt(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL));
    private final DummySteg aksjonspunktModifisererSteg = new DummySteg(opprettForAksjonspunktMedFrist(
        AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL, Venteårsak.AVV_DOK, FRIST_TID));

    @Test
    public void skal_finne_aksjonspunkter_som_ligger_etter_et_gitt_steg() {
        // Arrange - noen utvalge, tilfeldige aksjonspunkter
        AksjonspunktDefinisjon a0_0 = AksjonspunktDefinisjon.AVKLAR_OPPHOLDSRETT;
        AksjonspunktDefinisjon a0_1 = AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL;
        AksjonspunktDefinisjon a1_0 = AksjonspunktDefinisjon.AVKLAR_ADOPSJONSDOKUMENTAJON;
        AksjonspunktDefinisjon a1_1 = AksjonspunktDefinisjon.AVKLAR_FAKTA_FOR_PERSONSTATUS;
        AksjonspunktDefinisjon a2_0 = AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE;
        AksjonspunktDefinisjon a2_1 = AksjonspunktDefinisjon.AVKLAR_TILLEGGSOPPLYSNINGER;

        DummySteg steg = new DummySteg();
        DummySteg steg0 = new DummySteg();
        DummySteg steg1 = new DummySteg();
        DummySteg steg2 = new DummySteg();

        List<TestStegKonfig> modellData = List.of(
            new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, steg, ap(), ap()),
            new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, steg0, ap(a0_0), ap(a0_1)),
            new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, steg1, ap(a1_0), ap(a1_1)),
            new TestStegKonfig(STEG_4, behandlingType, fagsakYtelseType, steg2, ap(a2_0), ap(a2_1)));

        BehandlingModellImpl modell = setupModell(modellData);

        Set<String> ads = null;

        ads = modell.finnAksjonspunktDefinisjonerEtter(STEG_1);

        assertThat(ads).

            containsOnly(a0_0.getKode(), a0_1.

                getKode(), a1_0.

                    getKode(),
                a1_1.

                    getKode(),
                a2_0.

                    getKode(),
                a2_1.

                    getKode());

        ads = modell.finnAksjonspunktDefinisjonerEtter(STEG_2);

        assertThat(ads).

            containsOnly(a1_0.getKode(), a1_1.

                getKode(), a2_0.

                    getKode(),
                a2_1.

                    getKode());

        ads = modell.finnAksjonspunktDefinisjonerEtter(STEG_3);

        assertThat(ads).

            containsOnly(a2_0.getKode(), a2_1.

                getKode());

        ads = modell.finnAksjonspunktDefinisjonerEtter(STEG_4);

        assertThat(ads).

            isEmpty();

    }

    @Test
    public void skal_finne_aksjonspunkter_ved_inngang_eller_utgang_av_steg() {
        // Arrange - noen utvalge, tilfeldige aksjonspunkter
        AksjonspunktDefinisjon a0_0 = AksjonspunktDefinisjon.AVKLAR_OPPHOLDSRETT;
        AksjonspunktDefinisjon a0_1 = AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL;
        AksjonspunktDefinisjon a1_0 = AksjonspunktDefinisjon.AVKLAR_ADOPSJONSDOKUMENTAJON;
        AksjonspunktDefinisjon a1_1 = AksjonspunktDefinisjon.AVKLAR_FAKTA_FOR_PERSONSTATUS;

        DummySteg steg = new DummySteg();
        DummySteg steg0 = new DummySteg();
        DummySteg steg1 = new DummySteg();

        List<TestStegKonfig> modellData = List.of(
            new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, steg, ap(), ap()),
            new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, steg0, ap(a0_0), ap(a0_1)),
            new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, steg1, ap(a1_0), ap(a1_1)));

        BehandlingModellImpl modell = setupModell(modellData);

        Set<String> ads = null;

        ads = modell.finnAksjonspunktDefinisjonerInngang(STEG_1);
        assertThat(ads).isEmpty();

        ads = modell.finnAksjonspunktDefinisjonerInngang(STEG_2);
        assertThat(ads).containsOnly(a0_0.getKode());

        ads = modell.finnAksjonspunktDefinisjonerUtgang(STEG_3);
        assertThat(ads).containsOnly(a1_1.getKode());

    }

    @Test
    public void skal_stoppe_på_steg_2_når_får_aksjonspunkt() throws Exception {
        // Arrange
        List<TestStegKonfig> modellData = List.of(
            new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()),
            new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, aksjonspunktSteg, ap(), ap()),
            new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, nullSteg, ap(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL), ap()),
            new TestStegKonfig(STEG_4, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()));
        BehandlingModellImpl modell = setupModell(modellData);

        TestScenario scenario = TestScenario.forEngangsstønad();
        Behandling behandling = scenario.lagre(serviceProvider);
        BehandlingStegVisitorUtenLagring visitor = lagVisitor(behandling);
        BehandlingStegUtfall siste = modell.prosesserFra(STEG_1, visitor);

        assertThat(siste.getBehandlingStegType()).isEqualTo(STEG_3);
        assertThat(visitor.kjørteSteg).isEqualTo(List.of(STEG_1, STEG_2, STEG_3));
    }

    public List<AksjonspunktDefinisjon> ap(AksjonspunktDefinisjon... aksjonspunktDefinisjoner) {
        return List.of(aksjonspunktDefinisjoner);
    }

    @Test
    public void skal_kjøre_til_siste_når_ingen_gir_aksjonspunkt() throws Exception {
        // Arrange
        List<TestStegKonfig> modellData = List.of(
            new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()),
            new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()),
            new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()));
        BehandlingModellImpl modell = setupModell(modellData);

        TestScenario scenario = TestScenario.forEngangsstønad();
        Behandling behandling = scenario.lagre(serviceProvider);
        BehandlingStegVisitorUtenLagring visitor = lagVisitor(behandling);
        BehandlingStegUtfall siste = modell.prosesserFra(STEG_1, visitor);

        assertThat(siste).isNull();
        assertThat(visitor.kjørteSteg).isEqualTo(List.of(STEG_1, STEG_2, STEG_3));
    }

    @Test
    public void skal_stoppe_når_settes_på_vent_deretter_fortsette()  {
        // Arrange
        List<TestStegKonfig> modellData = List.of(
            new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()),
            new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, nullVenterSteg, ap(), ap()),
            new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()));
        BehandlingModellImpl modell = setupModell(modellData);

        TestScenario scenario = TestScenario.forEngangsstønad();
        Behandling behandling = scenario.lagre(serviceProvider);

        // Act 1
        BehandlingStegVisitorUtenLagring visitor = lagVisitor(behandling);

        BehandlingStegUtfall første = modell.prosesserFra(STEG_1, visitor);

        assertThat(første).isNotNull();
        assertThat(første.getBehandlingStegType()).isEqualTo(STEG_2);
        assertThat(første.getResultat()).isEqualTo(BehandlingStegStatus.STARTET);
        assertThat(visitor.kjørteSteg).isEqualTo(List.of(STEG_1, STEG_2));

        // Act 2
        BehandlingStegVisitorUtenLagring visitorNeste = lagVisitor(behandling);
        BehandlingStegUtfall neste = modell.prosesserFra(STEG_2, visitorNeste);

        assertThat(neste).isNotNull();
        assertThat(neste.getBehandlingStegType()).isEqualTo(STEG_2);
        assertThat(neste.getResultat()).isEqualTo(BehandlingStegStatus.VENTER);
        assertThat(visitorNeste.kjørteSteg).isEqualTo(List.of(STEG_2));

        // Act 3
        BehandlingStegVisitorUtenLagring visitorNeste2 = lagVisitor(behandling);
        BehandlingStegUtfall neste2 = modell.prosesserFra(STEG_2, visitorNeste2);

        assertThat(neste2).isNotNull();
        assertThat(neste2.getBehandlingStegType()).isEqualTo(STEG_2);
        assertThat(neste2.getResultat()).isEqualTo(BehandlingStegStatus.VENTER);
        assertThat(visitorNeste2.kjørteSteg).isEqualTo(List.of(STEG_2));

        // Act 4
        BehandlingStegVisitorVenterUtenLagring gjenoppta = lagVisitorVenter(behandling);

        BehandlingStegUtfall fortsett = modell.prosesserFra(STEG_2, gjenoppta);
        assertThat(fortsett).isNull();
        assertThat(gjenoppta.kjørteSteg).isEqualTo(List.of(STEG_2, STEG_3));
    }

    @Test(expected = IllegalStateException.class)
    public void skal_feile_ved_gjenopptak_vanlig_steg() {
        // Arrange
        List<TestStegKonfig> modellData = List.of(
            new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()),
            new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()),
            new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()));
        BehandlingModellImpl modell = setupModell(modellData);

        TestScenario scenario = TestScenario.forEngangsstønad();
        Behandling behandling = scenario.lagre(serviceProvider);

        // Act 1
        BehandlingStegVisitorVenterUtenLagring visitor = lagVisitorVenter(behandling);
        modell.prosesserFra(STEG_1, visitor);
    }

    @Test
    public void tilbakefører_til_tidligste_steg_med_åpent_aksjonspunkt() {
        AksjonspunktDefinisjon aksjonspunktDefinisjon = STEG_2.getAksjonspunktDefinisjonerUtgang().get(0);
        DummySteg tilbakeføringssteg = new DummySteg(true, opprettForAksjonspunkt(aksjonspunktDefinisjon));
        // Arrange
        List<TestStegKonfig> modellData = List.of(
            new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, nullSteg, ap(aksjonspunktDefinisjon), ap()),
            new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()),
            new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, tilbakeføringssteg, ap(), ap()),
            new TestStegKonfig(STEG_4, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()));
        BehandlingModellImpl modell = setupModell(modellData);

        TestScenario scenario = TestScenario.forEngangsstønad();
        Behandling behandling = scenario.lagre(serviceProvider);
        BehandlingStegVisitorUtenLagring visitor = lagVisitor(behandling);

        Aksjonspunkt aksjonspunkt = serviceProvider.getAksjonspunktKontrollRepository().leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon,
            STEG_1);
        serviceProvider.getAksjonspunktKontrollRepository().setReåpnet(aksjonspunkt);

        BehandlingStegUtfall siste = modell.prosesserFra(STEG_3, visitor);
        assertThat(siste.getBehandlingStegType()).isEqualTo(STEG_3);

        Behandling beh = hentBehandling(behandling.getId());
        assertThat(beh.getAktivtBehandlingSteg()).isEqualTo(STEG_2);
    }

    @Test
    public void finner_tidligste_steg_for_aksjonspunkter() {
        AksjonspunktDefinisjon aksjonspunktDefinisjon = STEG_2.getAksjonspunktDefinisjonerInngang().get(0);
        List<TestStegKonfig> modellData = List.of(
            new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, nullSteg, ap(aksjonspunktDefinisjon), ap()),
            new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()));

        BehandlingModellImpl modell = setupModell(modellData);
        Set<AksjonspunktDefinisjon> aksjonspunktDefinisjoner = new HashSet<>();
        aksjonspunktDefinisjoner.add(aksjonspunktDefinisjon);
        BehandlingStegModell behandlingStegModell = modell.finnTidligsteStegFor(aksjonspunktDefinisjoner);
        assertThat(behandlingStegModell.getBehandlingStegType()).isEqualTo(STEG_2);
    }

    @Test
    public void skal_modifisere_aksjonspunktet_ved_å_kalle_funksjon_som_legger_til_frist() throws Exception {
        // Arrange
        List<TestStegKonfig> modellData = List.of(
            new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, aksjonspunktModifisererSteg, ap(), ap()),
            new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, nullSteg, ap(), ap()),
            new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, nullSteg, ap(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL), ap()));
        BehandlingModellImpl modell = setupModell(modellData);
        TestScenario scenario = TestScenario.forEngangsstønad();
        Behandling behandling = scenario.lagre(serviceProvider);
        BehandlingStegVisitorUtenLagring visitor = lagVisitor(behandling);

        // Act
        modell.prosesserFra(STEG_1, visitor);

        // Assert
        Behandling beh = hentBehandling(behandling.getId());
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
        BehandlingskontrollKontekst kontekst = kontrollTjeneste.initBehandlingskontroll(behandling);
        var lokalServiceProvider = new BehandlingskontrollServiceProvider(serviceProvider.getEntityManager(), serviceProvider.getBehandlingModellRepository(), null);
        return new BehandlingStegVisitorUtenLagring(lokalServiceProvider, kontekst);
    }

    private BehandlingStegVisitorVenterUtenLagring lagVisitorVenter(Behandling behandling) {
        BehandlingskontrollKontekst kontekst = kontrollTjeneste.initBehandlingskontroll(behandling);
        var lokalServiceProvider = new BehandlingskontrollServiceProvider(serviceProvider.getEntityManager(), serviceProvider.getBehandlingModellRepository(), null);
        return new BehandlingStegVisitorVenterUtenLagring(lokalServiceProvider, kontekst);
    }


    static class BehandlingStegVisitorUtenLagring extends TekniskBehandlingStegVisitor {
        List<BehandlingStegType> kjørteSteg = new ArrayList<>();

        BehandlingStegVisitorUtenLagring(BehandlingskontrollServiceProvider repositoryProvider,
                                         BehandlingskontrollKontekst kontekst) {
            super(repositoryProvider, kontekst);
        }

        @Override
        protected StegProsesseringResultat prosesserStegISavepoint(Behandling behandling, BehandlingStegVisitor stegVisitor) {
            // bypass savepoint
            this.kjørteSteg.add(stegVisitor.getStegModell().getBehandlingStegType());
            return super.prosesserSteg(stegVisitor);
        }
    }

    static class BehandlingStegVisitorVenterUtenLagring extends TekniskBehandlingStegVenterVisitor {
        List<BehandlingStegType> kjørteSteg = new ArrayList<>();

        BehandlingStegVisitorVenterUtenLagring (BehandlingskontrollServiceProvider repositoryProvider,
                                         BehandlingskontrollKontekst kontekst) {
            super(repositoryProvider, kontekst);
        }

        @Override
        protected StegProsesseringResultat prosesserStegISavepoint(Behandling behandling, BehandlingStegVisitor stegVisitor) {
            // bypass savepoint
            this.kjørteSteg.add(stegVisitor.getStegModell().getBehandlingStegType());
            return super.prosesserSteg(stegVisitor);
        }
    }
}
