package no.nav.foreldrepenger.domene.iay.modell;

import java.math.BigDecimal;
import java.time.LocalDate;

import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

public class YtelseGrunnlagBuilder {
    private final YtelseGrunnlag ytelseGrunnlag;

    YtelseGrunnlagBuilder(YtelseGrunnlag ytelseGrunnlag) {
        this.ytelseGrunnlag = ytelseGrunnlag;
    }

    static YtelseGrunnlagBuilder ny() {
        return new YtelseGrunnlagBuilder(new YtelseGrunnlag());
    }

    public YtelseGrunnlagBuilder medArbeidskategori(Arbeidskategori arbeidskategori) {
        this.ytelseGrunnlag.setArbeidskategori(arbeidskategori);
        return this;
    }

    public YtelseGrunnlagBuilder medDekningsgradProsent(BigDecimal prosent) {
        this.ytelseGrunnlag.setDekningsgradProsent(prosent == null ? null: new Stillingsprosent(prosent));
        return this;
    }

    public YtelseGrunnlagBuilder medGraderingProsent(BigDecimal prosent) {
        this.ytelseGrunnlag.setGraderingProsent(prosent == null ? null: new Stillingsprosent(prosent));
        return this;
    }

    public YtelseGrunnlagBuilder medInntektsgrunnlagProsent(BigDecimal prosent) {
        this.ytelseGrunnlag.setInntektsgrunnlagProsent(prosent == null ? null: new Stillingsprosent(prosent));
        return this;
    }

    public YtelseGrunnlagBuilder medOpprinneligIdentdato(LocalDate dato) {
        this.ytelseGrunnlag.setOpprinneligIdentdato(dato);
        return this;
    }

    public YtelseGrunnlagBuilder medYtelseStørrelse(YtelseStørrelse ytelseStørrelse) {
        this.ytelseGrunnlag.leggTilYtelseStørrelse(ytelseStørrelse);
        return this;
    }

    public YtelseGrunnlagBuilder medVedtaksDagsats(BigDecimal vedtaksDagsats) {
        this.ytelseGrunnlag.setVedtaksDagsats(new Beløp(vedtaksDagsats));
        return this;
    }

    public YtelseGrunnlagBuilder medVedtaksDagsats(Beløp vedtaksDagsats) {
        this.ytelseGrunnlag.setVedtaksDagsats(vedtaksDagsats);
        return this;
    }

    public void tilbakestillStørrelse() {
        this.ytelseGrunnlag.tilbakestillStørrelse();
    }

    public YtelseGrunnlag build() {
        return ytelseGrunnlag;
    }

    public YtelseStørrelseBuilder getStørrelseBuilder() {
        return YtelseStørrelseBuilder.ny();
    }
}
