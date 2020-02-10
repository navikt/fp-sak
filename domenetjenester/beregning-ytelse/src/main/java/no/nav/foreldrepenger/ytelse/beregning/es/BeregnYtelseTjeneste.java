package no.nav.foreldrepenger.ytelse.beregning.es;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;

@ApplicationScoped
public class BeregnYtelseTjeneste {

    private LegacyESBeregningRepository beregningRepository;

    BeregnYtelseTjeneste() {
        // for CDI proxy
    }

    @Inject
    public BeregnYtelseTjeneste(LegacyESBeregningRepository beregningRepository) {
        this.beregningRepository = beregningRepository;
    }

    /** Overstyr tilkjent engangsytelse (for Engangsstønad). */
    public void overstyrTilkjentYtelseForEngangsstønad(Behandling behandling, Long tilkjentYtelse) {
        Optional<LegacyESBeregning> beregningOptional = beregningRepository.getSisteBeregning(behandling.getId());
        if (beregningOptional.isPresent()) {
            LegacyESBeregning forrigeBeregning = beregningOptional.get();

            new TilkjentYtelseForEngangsstønad(beregningRepository)
                    .overstyrTilkjentYtelse(behandling.getId(), forrigeBeregning, tilkjentYtelse);

        }
    }

}
