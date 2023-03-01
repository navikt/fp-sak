package no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag;

import java.math.BigDecimal;
import java.util.List;

public record BeregningsgrunnlagPrStatus(AktivitetStatus aktivitetStatus,
                                         List<BeregningsgrunnlagPrArbeidsforhold> arbeidsforhold,
                                         BigDecimal redusertBrukersAndelPrÅr,
                                         Inntektskategori inntektskategori) {
    public BeregningsgrunnlagPrStatus(AktivitetStatus aktivitetStatus, List<BeregningsgrunnlagPrArbeidsforhold> arbeidsforhold) {
        this(aktivitetStatus, arbeidsforhold != null ? arbeidsforhold : List.of(), null, null);
    }

    public BeregningsgrunnlagPrStatus(AktivitetStatus aktivitetStatus, BigDecimal redusertBrukersAndelPrÅr, Inntektskategori inntektskategori) {
        this(aktivitetStatus, List.of(), redusertBrukersAndelPrÅr, inntektskategori);
    }
}
