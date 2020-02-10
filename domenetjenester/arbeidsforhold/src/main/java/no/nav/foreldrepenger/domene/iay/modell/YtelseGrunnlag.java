package no.nav.foreldrepenger.domene.iay.modell;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

public class YtelseGrunnlag extends BaseEntitet {

    @ChangeTracked
    private List<YtelseStørrelse> ytelseStørrelse = new ArrayList<>();

    private Arbeidskategori arbeidskategori = Arbeidskategori.UDEFINERT;

    @ChangeTracked
    private Stillingsprosent dekngradProsent;

    @ChangeTracked
    private Stillingsprosent graderingProsent;

    @ChangeTracked
    private Stillingsprosent inntektProsent;

    @ChangeTracked
    private LocalDate opprinneligIdentdato;

    @ChangeTracked
    private Beløp vedtaksDagsats;

    public YtelseGrunnlag() {
        // hibernate
    }

    public YtelseGrunnlag(YtelseGrunnlag ytelseGrunnlag) {
        this.arbeidskategori = ytelseGrunnlag.getArbeidskategori().orElse(null);
        this.dekngradProsent = ytelseGrunnlag.getDekningsgradProsent().orElse(null);
        this.graderingProsent = ytelseGrunnlag.getGraderingProsent().orElse(null);
        this.inntektProsent = ytelseGrunnlag.getInntektsgrunnlagProsent().orElse(null);
        this.opprinneligIdentdato = ytelseGrunnlag.getOpprinneligIdentdato().orElse(null);
        this.ytelseStørrelse = ytelseGrunnlag.getYtelseStørrelse().stream().map(ys -> {
            YtelseStørrelse ytelseStørrelse = new YtelseStørrelse(ys);
            return ytelseStørrelse;
        }).collect(Collectors.toList());
        this.vedtaksDagsats = ytelseGrunnlag.getVedtaksDagsats().orElse(null);
    }

    public Optional<Arbeidskategori> getArbeidskategori() {
        return Optional.ofNullable(arbeidskategori);
    }

    void setArbeidskategori(Arbeidskategori arbeidskategori) {
        this.arbeidskategori = arbeidskategori;
    }

    public Optional<Stillingsprosent> getDekningsgradProsent() {
        return Optional.ofNullable(dekngradProsent);
    }

    void setDekningsgradProsent(Stillingsprosent prosent) {
        this.dekngradProsent = prosent;
    }

    public Optional<Stillingsprosent> getGraderingProsent() {
        return Optional.ofNullable(graderingProsent);
    }

    void setGraderingProsent(Stillingsprosent prosent) {
        this.graderingProsent = prosent;
    }

    public Optional<Stillingsprosent> getInntektsgrunnlagProsent() {
        return Optional.ofNullable(inntektProsent);
    }

    void setInntektsgrunnlagProsent(Stillingsprosent prosent) {
        this.inntektProsent = prosent;
    }

    public Optional<LocalDate> getOpprinneligIdentdato() {
        return Optional.ofNullable(opprinneligIdentdato);
    }

    void setOpprinneligIdentdato(LocalDate dato) {
        this.opprinneligIdentdato = dato;
    }

    public List<YtelseStørrelse> getYtelseStørrelse() {
        return Collections.unmodifiableList(ytelseStørrelse);
    }

    void leggTilYtelseStørrelse(YtelseStørrelse ytelseStørrelse) {
        this.ytelseStørrelse.add(ytelseStørrelse);

    }

    void tilbakestillStørrelse() {
        ytelseStørrelse.clear();
    }

    void setVedtaksDagsats(Beløp vedtaksDagsats) {
        this.vedtaksDagsats = vedtaksDagsats;
    }

    public Optional<Beløp> getVedtaksDagsats() {
        return Optional.ofNullable(vedtaksDagsats);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof YtelseGrunnlag))
            return false;
        YtelseGrunnlag that = (YtelseGrunnlag) o;
        return Objects.equals(arbeidskategori, that.arbeidskategori) &&
            Objects.equals(dekngradProsent, that.dekngradProsent) &&
            Objects.equals(graderingProsent, that.graderingProsent) &&
            Objects.equals(inntektProsent, that.inntektProsent) &&
            Objects.equals(opprinneligIdentdato, that.opprinneligIdentdato) &&
            Objects.equals(vedtaksDagsats, that.vedtaksDagsats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidskategori, dekngradProsent, graderingProsent, inntektProsent, opprinneligIdentdato, vedtaksDagsats);
    }

    @Override
    public String toString() {
        return "YtelseGrunnlagEntitet{" +
            "arbeidskategori=" + arbeidskategori +
            ", dekngradProsent=" + dekngradProsent +
            ", graderingProsent=" + graderingProsent +
            ", inntektProsent=" + inntektProsent +
            ", opprinneligIdentdato=" + opprinneligIdentdato +
            ", vedtaksDagsats=" + vedtaksDagsats +
            '}';
    }
}
