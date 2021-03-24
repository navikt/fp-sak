package no.nav.foreldrepenger.behandling.steg.beregnytelse.fp;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.AksjonspunktutlederTilbaketrekk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@BehandlingStegRef(kode = "VURDER_TILBAKETREKK")
@BehandlingTypeRef("BT-004")
@FagsakYtelseTypeRef("FP")
@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class VurderTilbaketrekkSteg implements BehandlingSteg {
    private static final Logger LOGGER = LoggerFactory.getLogger(VurderTilbaketrekkSteg.class);

    private AksjonspunktutlederTilbaketrekk aksjonspunktutlederTilbaketrekk;
    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;

    VurderTilbaketrekkSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderTilbaketrekkSteg(AksjonspunktutlederTilbaketrekk aksjonspunktutlederTilbaketrekk,
                                  BehandlingRepository behandlingRepository,
                                  SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                  BeregningsresultatRepository beregningsresultatRepository) {
        this.aksjonspunktutlederTilbaketrekk = aksjonspunktutlederTilbaketrekk;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        Optional<Aksjonspunkt> apForVurderRefusjon = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VURDER_REFUSJON_BERGRUNN);
        boolean harVurdertStartdatoForTilkomneRefusjonskrav = apForVurderRefusjon.map(Aksjonspunkt::erUtført).orElse(false);
        if (harVurdertStartdatoForTilkomneRefusjonskrav) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
        List<AksjonspunktResultat> aksjonspunkter = aksjonspunktutlederTilbaketrekk.utledAksjonspunkterFor(new AksjonspunktUtlederInput(ref));

        if (aksjonspunkter.isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        if (bleLøstIForrigeBehandling(ref)) {
            // Kopierer valget som ble tatt sist og oppretter ikke aksjonspunkt
            kopierLøsningFraForrigeBehandling(ref);
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        // Hvis en sak ikke hadde aksjonspunktet i forrige behandling skal den ikke få det, da alle nye
        // tilfeller skal håndterers i beregning via aksjonspunkt 5059.
        // At det fortsatt utledes skyldes feil i aksjonspunktutlederTilbaketrekk.
        // Når alle saker som har hatt 5090 er avsluttet kan man avvikle alt relatert til tilbaketrekk.
        LOGGER.info("FP-584196: Saksnummer {}. Behandling med id {} fikk utledet aksjonspunkt 5090, " +
                "men forrige behandling med id {} gjorde ingen slik vurdering.", ref.getSaksnummer().getVerdi(),
            ref.getBehandlingId(), ref.getOriginalBehandlingId().orElse(null));
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void kopierLøsningFraForrigeBehandling(BehandlingReferanse ref) {
        Boolean originalBeslutning = ref.getOriginalBehandlingId()
            .flatMap(oid -> beregningsresultatRepository.hentBeregningsresultatAggregat(oid))
            .flatMap(BehandlingBeregningsresultatEntitet::skalHindreTilbaketrekk)
            .orElseThrow();
        LOGGER.info("FP-584197: Saksnummer {}. Behandling med id {} fikk utledet aksjonspunkt 5090, " +
                "kopierer valget som ble tatt i  forrige behandling med id {} der valget var {}.", ref.getSaksnummer().getVerdi(),
            ref.getBehandlingId(), ref.getOriginalBehandlingId().orElse(null), originalBeslutning);
        Behandling behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        beregningsresultatRepository.lagreMedTilbaketrekk(behandling, originalBeslutning);
    }

    private boolean bleLøstIForrigeBehandling(BehandlingReferanse ref) {
        Optional<Boolean> originalBeslutning = ref.getOriginalBehandlingId()
            .flatMap(oid -> beregningsresultatRepository.hentBeregningsresultatAggregat(oid))
            .flatMap(BehandlingBeregningsresultatEntitet::skalHindreTilbaketrekk);
        return originalBeslutning.isPresent();
    }
}
