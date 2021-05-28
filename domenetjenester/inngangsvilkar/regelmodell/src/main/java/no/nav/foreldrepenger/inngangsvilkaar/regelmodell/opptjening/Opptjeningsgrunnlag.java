package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.VilkårGrunnlag;
import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;
import no.nav.fpsak.tidsserie.LocalDateInterval;

/**
 * Input for vurdering av opptjeningsvilkår. Består av sett av AktivitetPeriodeInput for ulike aktivitetet og eventuelt
 * ulike arbeidsgivere.
 */
@RuleDocumentationGrunnlag
public class Opptjeningsgrunnlag implements VilkårGrunnlag {

    /** Input med aktiviteter som skal inngå i vurderingen. */
    @JsonProperty("aktivitetPerioder")
    private final List<AktivitetPeriode> aktivitetPerioder = new ArrayList<>();

    /** Behandlingstidspunkt - normalt dagens dato. */
    @JsonProperty("behandlingsDato")
    private LocalDate behandlingsDato;

    /** Første dato med opptjening som teller med. (fra-og-med) */
    @JsonProperty("forsteDatoOpptjening")
    private LocalDate førsteDatoOpptjening;

    /** Skjæringstidspunkt (siste punkt med opptjening). (til-og-med) */
    @JsonProperty("sisteDatoForOpptjening")
    private LocalDate sisteDatoForOpptjening;

    /** Input med inntekter som benyttes i vurderingen for å avsjekke Arbeidsforhold (fra AAReg). */
    @JsonProperty("inntektPerioder")
    private final List<InntektPeriode> inntektPerioder = new ArrayList<>();

    @JsonCreator
    protected Opptjeningsgrunnlag() {
    }

    public Opptjeningsgrunnlag(LocalDate behandlingstidspunkt,
                               LocalDate startDato,
                               LocalDate skjæringstidspunkt) {
        this.behandlingsDato = behandlingstidspunkt;
        this.førsteDatoOpptjening = startDato;
        this.sisteDatoForOpptjening = skjæringstidspunkt;
    }

    public List<AktivitetPeriode> getAktivitetPerioder() {
        return Collections.unmodifiableList(new ArrayList<>(aktivitetPerioder));
    }

    public LocalDate getBehandlingsTidspunkt() {
        return behandlingsDato;
    }

    public LocalDate getFørsteDatoIOpptjening() {
        return førsteDatoOpptjening;
    }

    public List<InntektPeriode> getInntektPerioder() {
        return Collections.unmodifiableList(new ArrayList<>(inntektPerioder));
    }

    public LocalDateInterval getOpptjeningPeriode() {
        return new LocalDateInterval(førsteDatoOpptjening, sisteDatoForOpptjening);
    }


    public LocalDate getSisteDatoForOpptjening() {
        return sisteDatoForOpptjening;
    }

    public void leggTil(AktivitetPeriode input) {
        aktivitetPerioder.add(input);
    }

    public void leggTil(InntektPeriode input) {
        inntektPerioder.add(input);
    }

    /**
     * Legg til aktivitet for en angitt intervall
     *
     * @param datoIntervall
     *            - intervall
     * @param aktivitet
     *            - aktivitet
     */
    // TODO(OJR) fiks ved å endre testdekning
    public void leggTil(LocalDateInterval datoIntervall, Aktivitet aktivitet) {
        leggTil(new AktivitetPeriode(datoIntervall, aktivitet, AktivitetPeriode.VurderingsStatus.TIL_VURDERING));
    }

    public void leggTilRapportertInntekt(LocalDateInterval datoInterval, Aktivitet aktivitet, Long kronerInntekt) {
        leggTil(new InntektPeriode(datoInterval, aktivitet, kronerInntekt));
    }

}
