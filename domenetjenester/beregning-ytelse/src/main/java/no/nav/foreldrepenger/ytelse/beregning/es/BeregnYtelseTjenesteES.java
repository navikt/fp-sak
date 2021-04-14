package no.nav.foreldrepenger.ytelse.beregning.es;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;

@ApplicationScoped
public class BeregnYtelseTjenesteES {

    private LegacyESBeregningRepository beregningRepository;

    BeregnYtelseTjenesteES() {
        // for CDI proxy
    }

    @Inject
    public BeregnYtelseTjenesteES(LegacyESBeregningRepository beregningRepository) {
        this.beregningRepository = beregningRepository;
    }

    /** Overstyr tilkjent engangsytelse (for Engangsstønad). */
    public void overstyrTilkjentYtelseForEngangsstønad(Behandling behandling, Long tilkjentYtelse) {
        var beregningOptional = beregningRepository.getSisteBeregning(behandling.getId());
        if (beregningOptional.isPresent()) {
            var forrigeBeregning = beregningOptional.get();

            new TilkjentYtelseForEngangsstønad(beregningRepository)
                    .overstyrTilkjentYtelse(behandling.getId(), forrigeBeregning, tilkjentYtelse);

        }
    }

}
