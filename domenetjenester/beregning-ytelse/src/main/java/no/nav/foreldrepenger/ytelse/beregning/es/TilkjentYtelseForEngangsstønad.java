package no.nav.foreldrepenger.ytelse.beregning.es;

import java.time.LocalDateTime;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;

class TilkjentYtelseForEngangsstønad {



    private LegacyESBeregningRepository beregningRepository;

    TilkjentYtelseForEngangsstønad(LegacyESBeregningRepository beregningRepository) {
        this.beregningRepository = beregningRepository;
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

    void overstyrTilkjentYtelse(Long behandlingId, LegacyESBeregning forrigeBeregning, Long tilkjentYtelse) {

        LegacyESBeregning overstyrtBeregning = new LegacyESBeregning(forrigeBeregning.getSatsVerdi(),
                forrigeBeregning.getAntallBarn(),
                tilkjentYtelse,
                LocalDateTime.now(),
                true,
                finnOpprinneligBeløp(forrigeBeregning));

        beregningRepository.lagreBeregning(behandlingId, overstyrtBeregning);
    }
}
