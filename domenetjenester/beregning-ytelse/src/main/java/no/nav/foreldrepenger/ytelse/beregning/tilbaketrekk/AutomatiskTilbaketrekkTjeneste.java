package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.Optional;

/**
 * Ønsker ikke lenger opprette dette aksjonspunktet, så hvis det oppstår igjen
 * kopierer vi valget som ble tatt i forrige behandling. Logges for analyse.
 * Om det ikke oppstod i forrige behandling skal det heller ikke oppstå igjen,
 * så vi logger da en melding for å kunne identifisere disse.
 * TFP-4129
 */
@ApplicationScoped
public class AutomatiskTilbaketrekkTjeneste {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutomatiskTilbaketrekkTjeneste.class);
    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingRepository behandlingRepository;

    public AutomatiskTilbaketrekkTjeneste() {
        // CDI
    }

    @Inject
    public AutomatiskTilbaketrekkTjeneste(BeregningsresultatRepository repository,
                                          BehandlingRepository behandlingRepository) {
        this.beregningsresultatRepository = repository;
        this.behandlingRepository = behandlingRepository;
    }

    public void kopierTilbaketrekksvurderingFraForrigeResultat(BehandlingReferanse ref) {
        Optional<BehandlingBeregningsresultatEntitet> originaltResultat = ref.getOriginalBehandlingId()
            .flatMap(oid -> beregningsresultatRepository.hentBeregningsresultatAggregat(oid));

        if (originaltResultat.isEmpty() || originaltResultat.get().skalHindreTilbaketrekk().isEmpty()) {
            LOGGER.info("FP-584196: Saksnummer {}. Behandling med id {} fikk utledet aksjonspunkt 5090, " +
                "men forrige behandling med id {} gjorde ingen slik vurdering.", ref.getSaksnummer().getVerdi(),
                ref.getBehandlingId(), ref.getOriginalBehandlingId().orElse(null));
        } else {
            Boolean originalBeslutning = originaltResultat.flatMap(BehandlingBeregningsresultatEntitet::skalHindreTilbaketrekk)
                .orElseThrow();
            LOGGER.info("FP-584197: Saksnummer {}. Behandling med id {} fikk utledet aksjonspunkt 5090, " +
                    "kopierer beslutning fra forrige behandling med id {} som hadde verdien {}.", ref.getSaksnummer().getVerdi(),
                ref.getBehandlingId(), ref.getOriginalBehandlingId().get(), originalBeslutning);
            Behandling behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
            beregningsresultatRepository.lagreMedTilbaketrekk(behandling, originalBeslutning);
        }

    }
}

