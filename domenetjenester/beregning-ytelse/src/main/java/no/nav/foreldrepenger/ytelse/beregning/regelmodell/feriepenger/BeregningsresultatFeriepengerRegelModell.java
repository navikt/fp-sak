package no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatFeriepengerGrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Dekningsgrad;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;
import no.nav.fpsak.tidsserie.LocalDateInterval;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RuleDocumentationGrunnlag
public class BeregningsresultatFeriepengerRegelModell {
    private BeregningsresultatFeriepengerGrunnlag grunnlag;
    private List<BeregningsresultatPeriode> beregningsresultatPerioder;
    private LocalDateInterval feriepengerPeriode;


    public BeregningsresultatFeriepengerRegelModell(BeregningsresultatFeriepengerGrunnlag grunnlag,
                                                     List<BeregningsresultatPeriode> beregningsresultatPerioder) {
        this.grunnlag = grunnlag;
        this.beregningsresultatPerioder = beregningsresultatPerioder;
    }

    public boolean erArbeidstakerVedSkjæringstidspunkt() {
        return grunnlag.erArbeidstakerVedSkjæringstidspunkt();
    }

    public Set<Inntektskategori> getInntektskategorier() {
        return grunnlag.getInntektskategorier();
    }

    public List<BeregningsresultatPeriode> getBeregningsresultatPerioder() {
        return beregningsresultatPerioder;
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
}
