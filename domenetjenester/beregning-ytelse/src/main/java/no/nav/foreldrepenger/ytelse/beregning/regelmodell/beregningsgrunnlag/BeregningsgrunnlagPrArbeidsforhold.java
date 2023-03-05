package no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record BeregningsgrunnlagPrArbeidsforhold(Arbeidsforhold arbeidsforhold,
                                                 BigDecimal redusertRefusjonPrÅr,
                                                 BigDecimal redusertBrukersAndelPrÅr,
                                                 Inntektskategori inntektskategori) {

    public Long getDagsatsBruker() {
        return redusertBrukersAndelPrÅr != null ? avrundTilDagsats(redusertBrukersAndelPrÅr) : null;
    }

    public Long getDagsatsArbeidsgiver() {
        return redusertRefusjonPrÅr != null ? avrundTilDagsats(redusertRefusjonPrÅr) : null;
    }

    public String getArbeidsgiverId() {
        return arbeidsforhold == null ? null: arbeidsforhold.identifikator();
    }

    private long avrundTilDagsats(BigDecimal verdi) {
        return verdi.divide(BigDecimal.valueOf(260), 0, RoundingMode.HALF_UP).longValue();
    }

    public static BeregningsgrunnlagPrArbeidsforhold opprett(Arbeidsforhold arbeidsforhold, Inntektskategori inntektskategori) {
        return new BeregningsgrunnlagPrArbeidsforhold(arbeidsforhold, null, null, inntektskategori);
    }

    public BeregningsgrunnlagPrArbeidsforhold medRedusertRefusjonPrÅr(BigDecimal redusertRefusjonPrÅr) {
        return new BeregningsgrunnlagPrArbeidsforhold(arbeidsforhold(), redusertRefusjonPrÅr, redusertBrukersAndelPrÅr(), inntektskategori());
    }

    public BeregningsgrunnlagPrArbeidsforhold medRedusertBrukersAndelPrÅr(BigDecimal redusertBrukersAndelPrÅr) {
        return new BeregningsgrunnlagPrArbeidsforhold(arbeidsforhold(), redusertRefusjonPrÅr(), redusertBrukersAndelPrÅr, inntektskategori());
    }

}
