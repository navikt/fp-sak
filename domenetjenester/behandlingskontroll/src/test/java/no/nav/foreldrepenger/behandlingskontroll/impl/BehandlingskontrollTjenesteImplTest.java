package no.nav.foreldrepenger.behandlingskontroll.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingModellVisitor;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegUtfall;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingskontroll.testutilities.TestScenario;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;

public class BehandlingskontrollTjenesteImplTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private EntityManager em = repoRule.getEntityManager();
    private BehandlingskontrollTjenesteImpl kontrollTjeneste;
    private Behandling behandling;
    private BehandlingskontrollKontekst kontekst;
    private BehandlingskontrollEventPublisererForTest eventPubliserer = new BehandlingskontrollEventPublisererForTest();
    private BehandlingModellRepository behandlingModellRepository = new BehandlingModellRepository();
    private BehandlingskontrollServiceProvider serviceProvider = new BehandlingskontrollServiceProvider(em, behandlingModellRepository, eventPubliserer);
    private InternalManipulerBehandling manipulerInternBehandling = new InternalManipulerBehandling();

    private BehandlingStegType steg2;
    private BehandlingStegType steg3;
    private BehandlingStegType steg4;
    private BehandlingStegType steg5;

    private BehandlingModell modell;

    private String steg2InngangAksjonspunkt;

    private String steg2UtgangAksjonspunkt;

    @Before
    public void setup() {
        TestScenario scenario = TestScenario.forEngangsstønad();
        behandling = scenario.lagre(serviceProvider);
        modell = serviceProvider.getBehandlingModellRepository().getModell(behandling.getType(), behandling.getFagsakYtelseType());

        steg2 = modell.getAlleBehandlingStegTyper().get(5);
        steg3 = modell.finnNesteSteg(steg2).getBehandlingStegType();
        steg4 = modell.finnNesteSteg(steg3).getBehandlingStegType();
        steg5 = modell.finnNesteSteg(steg4).getBehandlingStegType();
        manipulerInternBehandling.forceOppdaterBehandlingSteg(behandling, steg3);

        steg2InngangAksjonspunkt = modell.finnAksjonspunktDefinisjonerInngang(steg2).iterator().next();
        steg2UtgangAksjonspunkt = modell.finnAksjonspunktDefinisjonerUtgang(steg2).iterator().next();

        initBehandlingskontrollTjeneste();

        kontekst = Mockito.mock(BehandlingskontrollKontekst.class);
        Mockito.when(kontekst.getBehandlingId()).thenReturn(behandling.getId());
        Mockito.when(kontekst.getFagsakId()).thenReturn(behandling.getFagsakId());
    }

    @Test
    public void skal_rykke_tilbake_til_inngang_vurderingspunkt_av_steg() {

        BehandlingStegType steg = steg2;
        String inngangAksjonspunkt = steg2InngangAksjonspunkt;

        kontrollTjeneste.behandlingTilbakeføringTilTidligsteAksjonspunkt(kontekst, List.of(inngangAksjonspunkt));

        assertThat(behandling.getAktivtBehandlingSteg()).isEqualTo(steg);
        assertThat(behandling.getStatus()).isEqualTo(BehandlingStatus.UTREDES);
        assertThat(behandling.getBehandlingStegStatus()).isEqualTo(BehandlingStegStatus.INNGANG);
        assertThat(behandling.getBehandlingStegTilstand()).isNotNull();
        assertThat(behandling.getBehandlingStegTilstandHistorikk()).hasSize(2);

        sjekkBehandlingStegTilstandHistorikk(behandling, steg3,
            BehandlingStegStatus.TILBAKEFØRT);
        sjekkBehandlingStegTilstandHistorikk(behandling, steg2,
            BehandlingStegStatus.INNGANG);

    }

    @Test
    public void skal_rykke_tilbake_til_utgang_vurderingspunkt_av_steg() {

        kontrollTjeneste.behandlingTilbakeføringTilTidligsteAksjonspunkt(kontekst, List.of(steg2UtgangAksjonspunkt));

        assertThat(behandling.getAktivtBehandlingSteg()).isEqualTo(steg2);
        assertThat(behandling.getStatus()).isEqualTo(BehandlingStatus.UTREDES);
        assertThat(behandling.getBehandlingStegStatus()).isEqualTo(BehandlingStegStatus.UTGANG);
        assertThat(behandling.getBehandlingStegTilstand()).isNotNull();

        assertThat(behandling.getBehandlingStegTilstand(steg2)).isPresent();
        assertThat(behandling.getBehandlingStegTilstandHistorikk()).hasSize(2);

        sjekkBehandlingStegTilstandHistorikk(behandling, steg3,
            BehandlingStegStatus.TILBAKEFØRT);
        sjekkBehandlingStegTilstandHistorikk(behandling, steg2,
            BehandlingStegStatus.UTGANG);

    }

    @Test
    public void skal_rykke_tilbake_til_start_av_tidligere_steg_ved_tilbakeføring() {

        kontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, steg2);

        assertThat(behandling.getAktivtBehandlingSteg()).isEqualTo(steg2);
        assertThat(behandling.getStatus()).isEqualTo(BehandlingStatus.UTREDES);
        assertThat(behandling.getBehandlingStegStatus()).isEqualTo(BehandlingStegStatus.INNGANG);
        assertThat(behandling.getBehandlingStegTilstand()).isNotNull();

        assertThat(behandling.getBehandlingStegTilstand(steg2)).isPresent();
        assertThat(behandling.getBehandlingStegTilstandHistorikk()).hasSize(2);

        sjekkBehandlingStegTilstandHistorikk(behandling, steg3,
            BehandlingStegStatus.TILBAKEFØRT);
        sjekkBehandlingStegTilstandHistorikk(behandling, steg2,
            BehandlingStegStatus.INNGANG);

    }

    @Test
    public void skal_tolerere_tilbakehopp_til_senere_steg_enn_inneværende() {

        kontrollTjeneste.behandlingTilbakeføringHvisTidligereBehandlingSteg(kontekst, steg4);

        assertThat(behandling.getAktivtBehandlingSteg()).isEqualTo(steg3);
        assertThat(behandling.getStatus()).isEqualTo(BehandlingStatus.UTREDES);
        assertThat(behandling.getBehandlingStegStatus()).isNull();
        assertThat(behandling.getBehandlingStegTilstand()).isNotNull();

        assertThat(behandling.getBehandlingStegTilstand(steg3)).isPresent();
        assertThat(behandling.getBehandlingStegTilstand(steg4)).isNotPresent();
        assertThat(behandling.getBehandlingStegTilstandHistorikk()).hasSize(1);
    }

    @Test
    public void skal_flytte_til__inngang_av_senere_steg_ved_framføring() {

        kontrollTjeneste.behandlingFramføringTilSenereBehandlingSteg(kontekst, steg5);

        assertThat(behandling.getAktivtBehandlingSteg()).isEqualTo(steg5);
        assertThat(behandling.getStatus()).isEqualTo(BehandlingStatus.UTREDES);
        assertThat(behandling.getBehandlingStegStatus()).isEqualTo(BehandlingStegStatus.INNGANG);
        assertThat(behandling.getBehandlingStegTilstand()).isNotNull();

        assertThat(behandling.getBehandlingStegTilstand(steg5)).isPresent();
        assertThat(behandling.getBehandlingStegTilstandHistorikk()).hasSize(2);

        sjekkBehandlingStegTilstandHistorikk(behandling, steg3,
            BehandlingStegStatus.AVBRUTT);

        // NB: skipper STEP_4
        sjekkBehandlingStegTilstandHistorikk(behandling, steg4);

        sjekkBehandlingStegTilstandHistorikk(behandling, steg5,
            BehandlingStegStatus.INNGANG);

    }

    @Test
    public void skal_kaste_exception_dersom_tilbakeføring_til_senere_steg() {
        // Assert
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Kan ikke angi steg ");
        expectedException.expectMessage("som er etter");

        // Act
        kontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, steg4);
    }

    @Test
    public void skal_kaste_exception_dersom_ugyldig_tilbakeføring_fra_iverks() {
        // Assert
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Kan ikke tilbakeføre fra");

        // Arrange
        BehandlingStegType iverksettSteg = BehandlingStegType.IVERKSETT_VEDTAK;
        BehandlingStegType forrigeSteg = modell.finnForrigeSteg(iverksettSteg).getBehandlingStegType();
        manipulerInternBehandling.forceOppdaterBehandlingSteg(behandling, iverksettSteg);

        // Act
        kontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, forrigeSteg);
    }

    @Test
    public void skal_rykke_tilbake_til_inngang_vurderingspunkt_av_samme_steg() {

        // Arrange
        var steg = steg2;
        manipulerInternBehandling.forceOppdaterBehandlingSteg(behandling, steg, BehandlingStegStatus.UTGANG, BehandlingStegStatus.AVBRUTT);

        assertThat(behandling.getAktivtBehandlingSteg()).isEqualTo(steg);
        assertThat(behandling.getBehandlingStegStatus()).isEqualTo(BehandlingStegStatus.UTGANG);
        assertThat(behandling.getStatus()).isEqualTo(BehandlingStatus.UTREDES);

        // Act
        kontrollTjeneste.behandlingTilbakeføringTilTidligsteAksjonspunkt(kontekst, List.of(steg2InngangAksjonspunkt));

        // Assert
        assertThat(behandling.getAktivtBehandlingSteg()).isEqualTo(steg);
        assertThat(behandling.getStatus()).isEqualTo(BehandlingStatus.UTREDES);
        assertThat(behandling.getBehandlingStegStatus()).isEqualTo(BehandlingStegStatus.INNGANG);
        assertThat(behandling.getBehandlingStegTilstand()).isNotNull();

        assertThat(behandling.getBehandlingStegTilstandHistorikk()).hasSize(2);

        sjekkBehandlingStegTilstandHistorikk(behandling, steg, BehandlingStegStatus.INNGANG);

        assertThat(behandling.getBehandlingStegTilstand(steg).get().getBehandlingStegStatus()).isEqualTo(BehandlingStegStatus.INNGANG);

    }

    @Test
    public void skal_ha_guard_mot_nøstet_behandlingskontroll_ved_prossesering_tilbakeføring_og_framføring() throws Exception {

        this.kontrollTjeneste = new BehandlingskontrollTjenesteImpl(serviceProvider) {
            @Override
            protected BehandlingStegUtfall doProsesserBehandling(BehandlingskontrollKontekst kontekst, BehandlingModell modell,
                                                                 BehandlingModellVisitor visitor) {
                kontrollTjeneste.prosesserBehandling(kontekst);
                return null;
            }
        };

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Støtter ikke nøstet prosessering");

        this.kontrollTjeneste.prosesserBehandling(kontekst);
    }

    @Test
    public void skal_returnere_true_når_aksjonspunktet_skal_løses_etter_angitt_steg() {
        assertThat(kontrollTjeneste.skalAksjonspunktLøsesIEllerEtterSteg(behandling.getFagsakYtelseType(), behandling.getType(), steg3, AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT)).isTrue();
    }

    @Test
    public void skal_returnere_true_når_aksjonspunktet_skal_løses_i_angitt_steg() {
        assertThat(kontrollTjeneste.skalAksjonspunktLøsesIEllerEtterSteg(behandling.getFagsakYtelseType(), behandling.getType(), steg2, AksjonspunktDefinisjon.AVKLAR_TILLEGGSOPPLYSNINGER))
            .isTrue();
    }

    @Test
    public void skal_returnere_false_når_aksjonspunktet_skal_løses_før_angitt_steg() {
        assertThat(kontrollTjeneste.skalAksjonspunktLøsesIEllerEtterSteg(behandling.getFagsakYtelseType(), behandling.getType(), steg4, AksjonspunktDefinisjon.AVKLAR_FAKTA_FOR_PERSONSTATUS))
            .isFalse();
    }

    private void sjekkBehandlingStegTilstandHistorikk(Behandling behandling, BehandlingStegType stegType,
                                                      BehandlingStegStatus... stegStatuser) {
        assertThat(
            behandling.getBehandlingStegTilstandHistorikk()
                .filter(bst -> stegType == null || Objects.equals(bst.getBehandlingSteg(), stegType))
                .map(bst -> bst.getBehandlingStegStatus()))
                    .containsExactly(stegStatuser);
    }

    private void initBehandlingskontrollTjeneste() {
        this.kontrollTjeneste = new BehandlingskontrollTjenesteImpl(serviceProvider);
    }


    private final class BehandlingskontrollEventPublisererForTest extends BehandlingskontrollEventPubliserer {
        private List<BehandlingEvent> events = new ArrayList<>();

        @Override
        protected void doFireEvent(BehandlingEvent event) {
            events.add(event);
        }
    }

}
