package no.nav.foreldrepenger.domene.iay.modell;

import java.math.BigDecimal;
import java.time.LocalDate;

import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.SkatteOgAvgiftsregelType;
import no.nav.foreldrepenger.domene.typer.Beløp;

public class InntektspostBuilder {
    private Inntektspost inntektspost;

    InntektspostBuilder(Inntektspost inntektspost) {
        this.inntektspost = inntektspost;
    }

    public static InntektspostBuilder ny() {
        return new InntektspostBuilder(new Inntektspost());
    }

    public InntektspostBuilder medInntektspostType(InntektspostType inntektspostType) {
        this.inntektspost.setInntektspostType(inntektspostType);
        return this;
    }

    public InntektspostBuilder medSkatteOgAvgiftsregelType(SkatteOgAvgiftsregelType skatteOgAvgiftsregelType) {
        this.inntektspost.setSkatteOgAvgiftsregelType(skatteOgAvgiftsregelType);
        return this;
    }

    public InntektspostBuilder medPeriode(LocalDate fraOgMed, LocalDate tilOgMed) {
        this.inntektspost.setPeriode(fraOgMed, tilOgMed);
        return this;
    }

    public InntektspostBuilder medBeløp(BigDecimal verdi) {
        this.inntektspost.setBeløp(new Beløp(verdi));
        return this;
    }

    public InntektspostBuilder medInntektYtelse(InntektYtelseType inntektYtelseType) {
        this.inntektspost.setInntektYtelseType(inntektYtelseType);
        return this;
    }

    public Inntektspost build() {
        if (inntektspost.hasValues()) {
            return inntektspost;
        }
        throw new IllegalStateException();
    }
}
