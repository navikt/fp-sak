package no.nav.foreldrepenger.behandlingskontroll.impl;

import static no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling.forceOppdaterBehandlingSteg;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class BehandlingskontrollTjenesteImplTest {

    private BehandlingskontrollTjenesteImpl kontrollTjeneste;
    private final BehandlingskontrollEventPublisererForTest eventPubliserer = new BehandlingskontrollEventPublisererForTest();
    private BehandlingskontrollServiceProvider serviceProvider;

    private BehandlingStegType steg2;
    private BehandlingStegType steg3;
    private BehandlingStegType steg4;
    private BehandlingStegType steg5;

    private String steg2InngangAksjonspunkt;

    private String steg2UtgangAksjonspunkt;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        serviceProvider = new BehandlingskontrollServiceProvider(entityManager, new BehandlingModellRepository(), eventPubliserer);
        var modell = serviceProvider.getBehandlingModellRepository().getModell(BehandlingType.FØRSTEGANGSSØKNAD, FagsakYtelseType.ENGANGSTØNAD);

        steg2 = modell.getAlleBehandlingStegTyper().get(5);
        steg3 = modell.finnNesteSteg(steg2).getBehandlingStegType();
        steg4 = modell.finnNesteSteg(steg3).getBehandlingStegType();
        steg5 = modell.finnNesteSteg(steg4).getBehandlingStegType();

        steg2InngangAksjonspunkt = modell.finnAksjonspunktDefinisjonerInngang(steg2).iterator().next();
        steg2UtgangAksjonspunkt = modell.finnAksjonspunktDefinisjonerUtgang(steg2).iterator().next();

        initBehandlingskontrollTjeneste();
    }

    @Test
    public void skal_rykke_tilbake_til_inngang_vurderingspunkt_av_steg() {
        var behandling = TestScenario.forEngangsstønad()
                .lagre(serviceProvider);
        forceOppdaterBehandlingSteg(behandling, steg3);
        var kontekst = mock(BehandlingskontrollKontekst.class);
        when(kontekst.getBehandlingId()).thenReturn(behandling.getId());
        when(kontekst.getFagsakId()).thenReturn(behandling.getFagsakId());

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
        var behandling = TestScenario.forEngangsstønad()
                .lagre(serviceProvider);
        forceOppdaterBehandlingSteg(behandling, steg3);
        var kontekst = mock(BehandlingskontrollKontekst.class);
        when(kontekst.getBehandlingId()).thenReturn(behandling.getId());
        when(kontekst.getFagsakId()).thenReturn(behandling.getFagsakId());

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
        var behandling = TestScenario.forEngangsstønad()
                .lagre(serviceProvider);
        forceOppdaterBehandlingSteg(behandling, steg3);
        var kontekst = mock(BehandlingskontrollKontekst.class);
        when(kontekst.getBehandlingId()).thenReturn(behandling.getId());
        when(kontekst.getFagsakId()).thenReturn(behandling.getFagsakId());

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
        var behandling = TestScenario.forEngangsstønad()
                .lagre(serviceProvider);
        forceOppdaterBehandlingSteg(behandling, steg3);
        var kontekst = mock(BehandlingskontrollKontekst.class);
        when(kontekst.getBehandlingId()).thenReturn(behandling.getId());
        when(kontekst.getFagsakId()).thenReturn(behandling.getFagsakId());

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
        var behandling = TestScenario.forEngangsstønad()
                .lagre(serviceProvider);
        forceOppdaterBehandlingSteg(behandling, steg3);
        var kontekst = mock(BehandlingskontrollKontekst.class);
        when(kontekst.getBehandlingId()).thenReturn(behandling.getId());
        when(kontekst.getFagsakId()).thenReturn(behandling.getFagsakId());

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
        var behandling = TestScenario.forEngangsstønad()
                .lagre(serviceProvider);
        forceOppdaterBehandlingSteg(behandling, steg3);
        var kontekst = mock(BehandlingskontrollKontekst.class);
        when(kontekst.getBehandlingId()).thenReturn(behandling.getId());
        when(kontekst.getFagsakId()).thenReturn(behandling.getFagsakId());

        assertThatThrownBy(() -> kontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, steg4))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Kan ikke angi steg ")
                .hasMessageContaining("som er etter");
    }

    @Test
    public void skal_kaste_exception_dersom_ugyldig_tilbakeføring_fra_iverks() {
        var behandling = TestScenario.forEngangsstønad()
                .lagre(serviceProvider);
        forceOppdaterBehandlingSteg(behandling, steg3);
        var kontekst = mock(BehandlingskontrollKontekst.class);
        when(kontekst.getBehandlingId()).thenReturn(behandling.getId());
        when(kontekst.getFagsakId()).thenReturn(behandling.getFagsakId());
        var modell = serviceProvider.getBehandlingModellRepository().getModell(behandling.getType(), behandling.getFagsakYtelseType());
        // Arrange
        BehandlingStegType iverksettSteg = BehandlingStegType.IVERKSETT_VEDTAK;
        BehandlingStegType forrigeSteg = modell.finnForrigeSteg(iverksettSteg).getBehandlingStegType();
        forceOppdaterBehandlingSteg(behandling, iverksettSteg);

        // Act
        assertThatThrownBy(() -> kontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, forrigeSteg))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Kan ikke tilbakeføre fra");
    }

    @Test
    public void skal_rykke_tilbake_til_inngang_vurderingspunkt_av_samme_steg() {
        var behandling = TestScenario.forEngangsstønad()
                .lagre(serviceProvider);
        forceOppdaterBehandlingSteg(behandling, steg3);
        var kontekst = mock(BehandlingskontrollKontekst.class);
        when(kontekst.getBehandlingId()).thenReturn(behandling.getId());
        when(kontekst.getFagsakId()).thenReturn(behandling.getFagsakId());
        // Arrange
        var steg = steg2;
        forceOppdaterBehandlingSteg(behandling, steg, BehandlingStegStatus.UTGANG, BehandlingStegStatus.AVBRUTT);

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
    public void skal_ha_guard_mot_nøstet_behandlingskontroll_ved_prossesering_tilbakeføring_og_framføring() {

        this.kontrollTjeneste = new BehandlingskontrollTjenesteImpl(serviceProvider) {
            @Override
            protected BehandlingStegUtfall doProsesserBehandling(BehandlingskontrollKontekst kontekst, BehandlingModell modell,
                    BehandlingModellVisitor visitor) {
                kontrollTjeneste.prosesserBehandling(kontekst);
                return null;
            }
        };

        var behandling = TestScenario.forEngangsstønad()
                .lagre(serviceProvider);
        forceOppdaterBehandlingSteg(behandling, steg3);
        var kontekst = mock(BehandlingskontrollKontekst.class);
        when(kontekst.getBehandlingId()).thenReturn(behandling.getId());
        when(kontekst.getFagsakId()).thenReturn(behandling.getFagsakId());

        assertThatThrownBy(() -> this.kontrollTjeneste.prosesserBehandling(kontekst))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Støtter ikke nøstet prosessering");
    }

    @Test
    public void skal_returnere_true_når_aksjonspunktet_skal_løses_etter_angitt_steg() {
        var behandling = TestScenario.forEngangsstønad()
                .lagre(serviceProvider);
        forceOppdaterBehandlingSteg(behandling, steg3);
        var kontekst = mock(BehandlingskontrollKontekst.class);
        when(kontekst.getBehandlingId()).thenReturn(behandling.getId());
        when(kontekst.getFagsakId()).thenReturn(behandling.getFagsakId());
        assertThat(kontrollTjeneste.skalAksjonspunktLøsesIEllerEtterSteg(behandling.getFagsakYtelseType(),
                behandling.getType(), steg3, AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT)).isTrue();
    }

    @Test
    public void skal_returnere_true_når_aksjonspunktet_skal_løses_i_angitt_steg() {
        var behandling = TestScenario.forEngangsstønad()
                .lagre(serviceProvider);
        forceOppdaterBehandlingSteg(behandling, steg3);
        var kontekst = mock(BehandlingskontrollKontekst.class);
        when(kontekst.getBehandlingId()).thenReturn(behandling.getId());
        when(kontekst.getFagsakId()).thenReturn(behandling.getFagsakId());
        assertThat(kontrollTjeneste.skalAksjonspunktLøsesIEllerEtterSteg(behandling.getFagsakYtelseType(),
                behandling.getType(), steg2, AksjonspunktDefinisjon.AVKLAR_TILLEGGSOPPLYSNINGER))
                        .isTrue();
    }

    @Test
    public void skal_returnere_false_når_aksjonspunktet_skal_løses_før_angitt_steg() {
        var behandling = TestScenario.forEngangsstønad()
                .lagre(serviceProvider);
        forceOppdaterBehandlingSteg(behandling, steg3);
        var kontekst = mock(BehandlingskontrollKontekst.class);
        when(kontekst.getBehandlingId()).thenReturn(behandling.getId());
        when(kontekst.getFagsakId()).thenReturn(behandling.getFagsakId());
        assertThat(kontrollTjeneste.skalAksjonspunktLøsesIEllerEtterSteg(behandling.getFagsakYtelseType(),
                behandling.getType(), steg4, AksjonspunktDefinisjon.AVKLAR_VERGE))
                        .isFalse();
    }

    private void sjekkBehandlingStegTilstandHistorikk(Behandling behandling, BehandlingStegType stegType,
            BehandlingStegStatus... stegStatuser) {
        assertThat(
                behandling.getBehandlingStegTilstandHistorikk()
                        .filter(bst -> (stegType == null) || Objects.equals(bst.getBehandlingSteg(), stegType))
                        .map(bst -> bst.getBehandlingStegStatus()))
                                .containsExactly(stegStatuser);
    }

    private void initBehandlingskontrollTjeneste() {
        this.kontrollTjeneste = new BehandlingskontrollTjenesteImpl(serviceProvider);
    }

    private static final class BehandlingskontrollEventPublisererForTest extends BehandlingskontrollEventPubliserer {
        private List<BehandlingEvent> events = new ArrayList<>();

        @Override
        protected void doFireEvent(BehandlingEvent event) {
            events.add(event);
        }
    }

}
