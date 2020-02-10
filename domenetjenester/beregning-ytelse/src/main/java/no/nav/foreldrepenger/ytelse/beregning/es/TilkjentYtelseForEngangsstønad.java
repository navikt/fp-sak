package no.nav.foreldrepenger.ytelse.beregning.es;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.vedtak.util.FPDateUtil;

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
                FPDateUtil.nå(),
                true,
                finnOpprinneligBeløp(forrigeBeregning));

        beregningRepository.lagreBeregning(behandlingId, overstyrtBeregning);
    }
}
