package no.nav.foreldrepenger.behandling.steg.beregnytelse.fp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.KopierUtbetResultatTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.VurderTilbaketrekkTjeneste;

@BehandlingStegRef(BehandlingStegType.VURDER_TILBAKETREKK)
@BehandlingTypeRef(BehandlingType.REVURDERING)
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class VurderTilbaketrekkSteg implements BehandlingSteg {
    private static final Logger LOGGER = LoggerFactory.getLogger(VurderTilbaketrekkSteg.class);

    private VurderTilbaketrekkTjeneste vurderTilbaketrekkTjeneste;
    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;
    private KopierUtbetResultatTjeneste kopierUtbetResultatTjeneste;

    VurderTilbaketrekkSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderTilbaketrekkSteg(VurderTilbaketrekkTjeneste vurderTilbaketrekkTjeneste,
                                  BehandlingRepository behandlingRepository,
                                  SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                  BeregningsresultatRepository beregningsresultatRepository,
                                  KopierUtbetResultatTjeneste kopierUtbetResultatTjeneste) {
        this.vurderTilbaketrekkTjeneste = vurderTilbaketrekkTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.kopierUtbetResultatTjeneste = kopierUtbetResultatTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var apForVurderRefusjon = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VURDER_REFUSJON_BERGRUNN);
        boolean harVurdertStartdatoForTilkomneRefusjonskrav = apForVurderRefusjon.map(Aksjonspunkt::erUtført).orElse(false);
        if (harVurdertStartdatoForTilkomneRefusjonskrav) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);
        var harBehovForTilbaketrekk = vurderTilbaketrekkTjeneste.skalVurdereTilbaketrekk(ref, skjæringstidspunkter);

        if (!harBehovForTilbaketrekk) {
            // I saker som er opprettet pga feriepenger må reberegnes kan det komme tilfeller der vi ikke kan omfordele igjen pga tilkommede arbeidsforhold,
            // i slike tilfeller må vi sjekke om foreslått resultat er likt og om det finnes et utbet. resultat vi kan kopiere, og isåfall kopiere dette
            // TFP-4279
            if (SpesialBehandling.erJusterFeriepenger(behandling)
                && kopierUtbetResultatTjeneste.kanKopiereForrigeUtbetResultat(ref)) {
                kopierUtbetResultatTjeneste.kopierOgLagreUtbetBeregningsresultat(ref);
            }
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
                "men forrige behandling med id {} gjorde ingen slik vurdering.", ref.saksnummer().getVerdi(),
            ref.behandlingId(), ref.getOriginalBehandlingId().orElse(null));
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void kopierLøsningFraForrigeBehandling(BehandlingReferanse ref) {
        var originalBeslutning = ref.getOriginalBehandlingId()
            .flatMap(oid -> beregningsresultatRepository.hentBeregningsresultatAggregat(oid))
            .flatMap(BehandlingBeregningsresultatEntitet::skalHindreTilbaketrekk)
            .orElseThrow();
        LOGGER.info("FP-584197: Saksnummer {}. Behandling med id {} fikk utledet aksjonspunkt 5090, " +
                "kopierer valget som ble tatt i  forrige behandling med id {} der valget var {}.", ref.saksnummer().getVerdi(),
            ref.behandlingId(), ref.getOriginalBehandlingId().orElse(null), originalBeslutning);
        var behandling = behandlingRepository.hentBehandling(ref.behandlingId());
        beregningsresultatRepository.lagreMedTilbaketrekk(behandling, originalBeslutning);
    }

    private boolean bleLøstIForrigeBehandling(BehandlingReferanse ref) {
        var originalBeslutning = ref.getOriginalBehandlingId()
            .flatMap(oid -> beregningsresultatRepository.hentBeregningsresultatAggregat(oid))
            .flatMap(BehandlingBeregningsresultatEntitet::skalHindreTilbaketrekk);
        return originalBeslutning.isPresent();
    }
}
