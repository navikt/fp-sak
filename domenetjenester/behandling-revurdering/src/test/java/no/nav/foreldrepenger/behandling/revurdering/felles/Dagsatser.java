package no.nav.foreldrepenger.behandling.revurdering.felles;

import static no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil.TOTAL_ANDEL_NORMAL;
import static no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil.TOTAL_ANDEL_OPPJUSTERT;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Dagsatser {
    private BigDecimal dagsatsBruker;
    private BigDecimal dagsatsArbeidstaker;

    Dagsatser(boolean medOppjustertDagsat, boolean skalDeleAndelMellomArbeidsgiverOgBruker) {
        var aktuellDagsats = medOppjustertDagsat ? TOTAL_ANDEL_OPPJUSTERT : TOTAL_ANDEL_NORMAL;
        this.dagsatsBruker = skalDeleAndelMellomArbeidsgiverOgBruker ? aktuellDagsats.divide(BigDecimal.valueOf(2), 0,
            RoundingMode.HALF_UP) : aktuellDagsats;
        this.dagsatsArbeidstaker = skalDeleAndelMellomArbeidsgiverOgBruker ? aktuellDagsats.divide(BigDecimal.valueOf(2), 0,
            RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public BigDecimal getDagsatsBruker() {
        return dagsatsBruker;
    }

    public BigDecimal getDagsatsArbeidstaker() {
        return dagsatsArbeidstaker;
    }
}
