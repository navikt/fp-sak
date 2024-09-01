package no.nav.foreldrepenger.domene.uttak.input;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;

/** Inputstruktur for uttak tjenester. */
public class UttakInput {

    private final BehandlingReferanse behandlingReferanse;
    private final Skjæringstidspunkt skjæringstidspunkt;
    private final InntektArbeidYtelseGrunnlag iayGrunnlag;
    private final YtelsespesifiktGrunnlag ytelsespesifiktGrunnlag;
    private Set<BeregningsgrunnlagStatus> beregningsgrunnlagStatuser = Set.of();
    private LocalDateTime søknadOpprettetTidspunkt;
    private LocalDate medlemskapOpphørsdato;
    private Set<BehandlingÅrsakType> behandlingÅrsaker = Set.of();
    private boolean behandlingManueltOpprettet;
    private boolean finnesAndelerMedGraderingUtenBeregningsgrunnlag;

    public UttakInput(BehandlingReferanse behandlingReferanse,
                      Skjæringstidspunkt skjæringstidspunkt,
                      InntektArbeidYtelseGrunnlag iayGrunnlag,
                      YtelsespesifiktGrunnlag ytelsespesifiktGrunnlag) {
        this.behandlingReferanse = Objects.requireNonNull(behandlingReferanse, "behandlingReferanse");
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.iayGrunnlag = iayGrunnlag;
        this.ytelsespesifiktGrunnlag = ytelsespesifiktGrunnlag;
    }

    private UttakInput(UttakInput input) {
        this(input.getBehandlingReferanse(), input.getSkjæringstidspunkt(), input.getIayGrunnlag(), input.getYtelsespesifiktGrunnlag());
        this.beregningsgrunnlagStatuser = Set.copyOf(input.beregningsgrunnlagStatuser);
        this.søknadOpprettetTidspunkt = input.søknadOpprettetTidspunkt;
        this.medlemskapOpphørsdato = input.medlemskapOpphørsdato;
        this.behandlingÅrsaker = input.behandlingÅrsaker;
        this.behandlingManueltOpprettet = input.behandlingManueltOpprettet;
        this.finnesAndelerMedGraderingUtenBeregningsgrunnlag = input.finnesAndelerMedGraderingUtenBeregningsgrunnlag;
    }

    public BehandlingReferanse getBehandlingReferanse() {
        return behandlingReferanse;
    }

    public Skjæringstidspunkt getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public Set<BeregningsgrunnlagStatus> getBeregningsgrunnlagStatuser() {
        return beregningsgrunnlagStatuser;
    }

    public FagsakYtelseType getFagsakYtelseType() {
        return behandlingReferanse.fagsakYtelseType();
    }

    public InntektArbeidYtelseGrunnlag getIayGrunnlag() {
        return iayGrunnlag;
    }

    /** Sjekk fagsakytelsetype før denne kalles. */
    @SuppressWarnings("unchecked")
    public <V extends YtelsespesifiktGrunnlag> V getYtelsespesifiktGrunnlag() {
        return (V) ytelsespesifiktGrunnlag;
    }

    public UttakYrkesaktiviteter getYrkesaktiviteter() {
        return new UttakYrkesaktiviteter(this);
    }

    public Optional<LocalDate> getMedlemskapOpphørsdato() {
        return Optional.ofNullable(medlemskapOpphørsdato);
    }

    public boolean harBehandlingÅrsak(BehandlingÅrsakType behandlingÅrsakType) {
        return behandlingÅrsaker.stream().anyMatch(årsak -> årsak.equals(behandlingÅrsakType));
    }

    public boolean isBehandlingManueltOpprettet() {
        return behandlingManueltOpprettet;
    }

    public LocalDateTime getSøknadOpprettetTidspunkt() {
        return søknadOpprettetTidspunkt;
    }

    public boolean finnesAndelerMedGraderingUtenBeregningsgrunnlag() {
        return finnesAndelerMedGraderingUtenBeregningsgrunnlag;
    }

    public Set<BehandlingÅrsakType> getBehandlingÅrsaker() {
        return behandlingÅrsaker;
    }

    public UttakInput medBeregningsgrunnlagStatuser(Set<BeregningsgrunnlagStatus> statuser) {
        var newInput = new UttakInput(this);
        newInput.beregningsgrunnlagStatuser = Set.copyOf(statuser);
        return newInput;
    }

    public UttakInput medSøknadOpprettetTidspunkt(LocalDateTime tidspunkt) {
        var newInput = new UttakInput(this);
        newInput.søknadOpprettetTidspunkt = tidspunkt;
        return newInput;
    }

    public UttakInput medMedlemskapOpphørsdato(LocalDate opphørsdato) {
        var newInput = new UttakInput(this);
        newInput.medlemskapOpphørsdato = opphørsdato;
        return newInput;
    }

    public UttakInput medBehandlingÅrsaker(Set<BehandlingÅrsakType> behandlingÅrsaker) {
        var newInput = new UttakInput(this);
        newInput.behandlingÅrsaker = behandlingÅrsaker;
        return newInput;
    }

    public UttakInput medBehandlingManueltOpprettet(boolean behandlingManueltOpprettet) {
        var newInput = new UttakInput(this);
        newInput.behandlingManueltOpprettet = behandlingManueltOpprettet;
        return newInput;
    }

    public UttakInput medFinnesAndelerMedGraderingUtenBeregningsgrunnlag(boolean finnesAndelerMedGraderingUtenBeregningsgrunnlag) {
        var newInput = new UttakInput(this);
        newInput.finnesAndelerMedGraderingUtenBeregningsgrunnlag = finnesAndelerMedGraderingUtenBeregningsgrunnlag;
        return newInput;
    }
}
