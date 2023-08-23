package no.nav.foreldrepenger.ytelse.beregning.es;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;

import java.time.LocalDateTime;

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

            overstyrTilkjentYtelse(behandling.getId(), forrigeBeregning, tilkjentYtelse);

        }
    }

    private void overstyrTilkjentYtelse(Long behandlingId, LegacyESBeregning forrigeBeregning, Long tilkjentYtelse) {

        var overstyrtBeregning = new LegacyESBeregning(forrigeBeregning.getSatsVerdi(),
            forrigeBeregning.getAntallBarn(),
            tilkjentYtelse,
            LocalDateTime.now(),
            true,
            finnOpprinneligBeløp(forrigeBeregning));

        beregningRepository.lagreBeregning(behandlingId, overstyrtBeregning);
    }

    private Long finnOpprinneligBeløp(LegacyESBeregning forrigeBeregning) {
        Long opprinneligBeløp;
        if (forrigeBeregning.isOverstyrt()) {
            opprinneligBeløp = forrigeBeregning.getOpprinneligBeregnetTilkjentYtelse();
        } else {
            opprinneligBeløp = forrigeBeregning.getBeregnetTilkjentYtelse();
        }
        return opprinneligBeløp;
    }



}
