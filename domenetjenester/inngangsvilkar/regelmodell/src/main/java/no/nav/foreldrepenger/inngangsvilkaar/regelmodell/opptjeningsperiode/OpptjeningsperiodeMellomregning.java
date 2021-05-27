package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode;

import java.time.LocalDate;

public class OpptjeningsperiodeMellomregning {

    // Input til regel
    private OpptjeningsperiodeGrunnlag grunnlag;
    private OpptjeningsperiodevilkårParametre regelParametre;

    // Settes i løpet av regelevaluering
    private LocalDate skjæringsdatoOpptjening;
    private LocalDate opptjeningsperiodeFom;
    private LocalDate opptjeningsperiodeTom;

    public OpptjeningsperiodeMellomregning() {
    }

    public OpptjeningsperiodeMellomregning(OpptjeningsperiodeGrunnlag grunnlag,
                                           OpptjeningsperiodevilkårParametre regelParametre) {
        this.grunnlag = grunnlag;
        this.regelParametre = regelParametre;
    }

    public OpptjeningsperiodeGrunnlag getGrunnlag() {
        return grunnlag;
    }

    public OpptjeningsperiodevilkårParametre getRegelParametre() {
        return regelParametre;
    }

    public LocalDate getSkjæringsdatoOpptjening() {
        return skjæringsdatoOpptjening;
    }

    public LocalDate getOpptjeningsperiodeFom() {
        return opptjeningsperiodeFom;
    }

    public LocalDate getOpptjeningsperiodeTom() {
        return opptjeningsperiodeTom;
    }

    public void setSkjæringsdatoOpptjening(LocalDate skjæringsdatoOpptjening) {
        this.skjæringsdatoOpptjening = skjæringsdatoOpptjening;
    }

    public void setOpptjeningsperiodeFom(LocalDate opptjeningsperiodeFom) {
        this.opptjeningsperiodeFom = opptjeningsperiodeFom;
    }

    public void setOpptjeningsperiodeTom(LocalDate opptjeningsperiodeTom) {
        this.opptjeningsperiodeTom = opptjeningsperiodeTom;
    }
}


