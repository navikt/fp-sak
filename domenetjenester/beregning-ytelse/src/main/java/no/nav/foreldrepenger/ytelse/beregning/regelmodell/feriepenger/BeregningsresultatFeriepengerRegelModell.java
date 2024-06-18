package no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatFeriepengerGrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Dekningsgrad;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@RuleDocumentationGrunnlag
public class BeregningsresultatFeriepengerRegelModell {
    private BeregningsresultatFeriepengerGrunnlag grunnlag;
    private List<BeregningsresultatFeriepengerPrÅr> feriepengerPrÅrListe = new ArrayList<>();
    private LocalDateInterval feriepengerPeriode;


    public BeregningsresultatFeriepengerRegelModell(BeregningsresultatFeriepengerGrunnlag grunnlag) {
        this.grunnlag = grunnlag;
    }

    public boolean erArbeidstakerVedSkjæringstidspunkt() {
        return grunnlag.erArbeidstakerVedSkjæringstidspunkt();
    }

    public Set<Inntektskategori> getInntektskategorier() {
        return grunnlag.getInntektskategorier();
    }

    public List<BeregningsresultatPeriode> getBeregningsresultatPerioder() {
        return grunnlag.getBeregningsresultatPerioder();
    }

    public List<BeregningsresultatPeriode> getAnnenPartsBeregningsresultatPerioder() {
        return grunnlag.getAnnenPartsBeregningsresultatPerioder();
    }

    public Set<Inntektskategori> getInntektskategorierAnnenPart() {
        return grunnlag.getInntektskategorierAnnenPart();
    }

    public Dekningsgrad getDekningsgrad() {
        return grunnlag.getDekningsgrad();
    }

    public boolean erForelder1() {
        return grunnlag.erForelder1();
    }

    public LocalDateInterval getFeriepengerPeriode() {
        return feriepengerPeriode;
    }

    public int getAntallDagerFeriepenger() {
        return grunnlag.getAntallDagerFeriepenger();
    }

    public void setFeriepengerPeriode(LocalDate feriepengePeriodeFom, LocalDate feriepengePeriodeTom) {
        this.feriepengerPeriode = new LocalDateInterval(feriepengePeriodeFom, feriepengePeriodeTom);
    }

    public List<BeregningsresultatFeriepengerPrÅr> getBeregningsresultatFeriepengerPrÅrListe() {
        return feriepengerPrÅrListe;
    }

    public void tømBeregningsresultatFeriepengerPrÅrListe() {
        feriepengerPrÅrListe.clear();
    }

    public void addBeregningsresultatFeriepengerPrÅr(BeregningsresultatFeriepengerPrÅr beregningsresultatFeriepengerPrÅr) {
        Objects.requireNonNull(beregningsresultatFeriepengerPrÅr, "beregningsresultatFeriepengerPrÅr");
        if (!feriepengerPrÅrListe.contains(beregningsresultatFeriepengerPrÅr)) {
            feriepengerPrÅrListe.add(beregningsresultatFeriepengerPrÅr);
        }
    }
}
