package no.nav.foreldrepenger.behandling;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import no.nav.fpsak.tidsserie.LocalDateInterval;

/**
 * Inneholder relevante tidspunkter for en behandling
 */
public class Skjæringstidspunkt {
    private LocalDate utledetSkjæringstidspunkt;
    private LocalDate skjæringstidspunktOpptjening;
    private LocalDate skjæringstidspunktBeregning;
    private LocalDate førsteUttaksdato;
    private LocalDate førsteUttaksdatoGrunnbeløp;
    private LocalDate førsteUttaksdatoSøknad;
    private LocalDateInterval uttaksintervall;
    private FamilieHendelseDato familieHendelseDato;
    private boolean kreverSammenhengendeUttak;
    private boolean utenMinsterett;
    private boolean uttakSkalJusteresTilFødselsdato;

    private Skjæringstidspunkt() {
        // hide constructor
    }

    private Skjæringstidspunkt(Skjæringstidspunkt other) {
        this.utledetSkjæringstidspunkt = other.utledetSkjæringstidspunkt;
        this.skjæringstidspunktOpptjening = other.skjæringstidspunktOpptjening;
        this.skjæringstidspunktBeregning = other.skjæringstidspunktBeregning;
        this.førsteUttaksdato = other.førsteUttaksdato;
        this.førsteUttaksdatoGrunnbeløp = other.førsteUttaksdatoGrunnbeløp;
        this.førsteUttaksdatoSøknad = other.førsteUttaksdatoSøknad;
        this.uttaksintervall = other.uttaksintervall;
        this.familieHendelseDato = other.familieHendelseDato;
        this.kreverSammenhengendeUttak = other.kreverSammenhengendeUttak;
        this.utenMinsterett = other.utenMinsterett;
        this.uttakSkalJusteresTilFødselsdato = other.uttakSkalJusteresTilFødselsdato;
    }

    public Optional<LocalDate> getSkjæringstidspunktHvisUtledet() {
        return Optional.ofNullable(utledetSkjæringstidspunkt);
    }

    public LocalDate getUtledetSkjæringstidspunkt() {
        Objects.requireNonNull(utledetSkjæringstidspunkt,
                "Utvikler-feil: utledetSkjæringstidspunkt er ikke satt. Sørg for at det er satt ifht. anvendelse");
        return utledetSkjæringstidspunkt;
    }

    /**
     * Skjæringstidspunkt for opptjening er definert som dagen etter slutt av
     * opptjeningsperiode.
     */
    public LocalDate getSkjæringstidspunktOpptjening() {
        Objects.requireNonNull(skjæringstidspunktOpptjening,
                "Utvikler-feil: skjæringstidspunktOpptjening er ikke satt. Sørg for at det er satt ifht. anvendelse");
        return skjæringstidspunktOpptjening;
    }

    /**
     * Skjæringstidspunkt for beregning er definert som dagen etter siste dag med
     * godkjente aktiviteter.
     */
    public LocalDate getSkjæringstidspunktBeregningForKopieringTilKalkulus() {
        return skjæringstidspunktBeregning;
    }

    /** Første uttaksdato er første dag stønadsperioden løper - dvs min(innvilget eller avslag søknadsfrist). Uten hensyn til tidlig fødsel */
    public LocalDate getFørsteUttaksdato() {
        Objects.requireNonNull(førsteUttaksdato, "Utvikler-feil: førsteUttaksdato er ikke satt. Sørg for at det er satt ifht. anvendelse");
        return førsteUttaksdato;
    }

    public Optional<LocalDate> getFørsteUttaksdatoHvisFinnes() {
        return Optional.ofNullable(førsteUttaksdato);
    }

    /** Grunnbeløpdato er første dag med innvilget uttak/utsettelse/overføring. */
    public LocalDate getFørsteUttaksdatoGrunnbeløp() {
        Objects.requireNonNull(førsteUttaksdatoGrunnbeløp, "Utvikler-feil: grunnbeløpdato er ikke satt. Sørg for at det er satt ifht. anvendelse");
        return førsteUttaksdatoGrunnbeløp;
    }

    /** Grunnbeløpdato er første dag med innvilget uttak/utsettelse/overføring. */
    public Optional<LocalDate> getFørsteUttaksdatoSøknad() {
        return Optional.ofNullable(førsteUttaksdatoSøknad);
    }

    /** Periode fra skjæringstidspunkt til siste uttaksdag, inntil 3 år for foreldrepenger. Skal normalt være satt */
    public Optional<LocalDateInterval> getUttaksintervall() {
        return Optional.ofNullable(uttaksintervall);
    }

    /** Sak relatert til termin/fødsel - alternativet er adopsjon/omsorgsovertagelse */
    public boolean gjelderFødsel() {
        return familieHendelseDato == null || !familieHendelseDato.gjelderAdopsjonOmsorg();
    }

    /** Gjeldende dato for fødsel, termin, eller omsorgsovertagelse. Kan være empty dersom behandling før søknad */
    public Optional<FamilieHendelseDato> getFamilieHendelseDato() {
        return Optional.ofNullable(familieHendelseDato);
    }

    /** Skal behandles etter nytt regelverk for uttak anno 2021. True = gamle regler. V2 betyr eldre versjon av 14-6 opptjening */
    public boolean kreverSammenhengendeUttak() {
        return this.kreverSammenhengendeUttak;
    }


    /** Skal behandles etter nytt regelverk for balansert arbeids/familieliv 2022. True = gamle regler */
    public boolean utenMinsterett() {
        return utenMinsterett;
    }

    /** Søkt om justering av første uttaksdato fra termin til fødsel når fødsel blir registrert. Kan påvirke STP */
    public boolean uttakSkalJusteresTilFødselsdato() {
        return uttakSkalJusteresTilFødselsdato;
    }

    @Override
    public int hashCode() {
        return Objects.hash(utledetSkjæringstidspunkt, skjæringstidspunktBeregning, skjæringstidspunktOpptjening, førsteUttaksdato);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !obj.getClass().equals(this.getClass())) {
            return false;
        }
        var other = (Skjæringstidspunkt) obj;
        return Objects.equals(this.utledetSkjæringstidspunkt, other.utledetSkjæringstidspunkt)
                && Objects.equals(this.skjæringstidspunktBeregning, other.skjæringstidspunktBeregning)
                && Objects.equals(this.skjæringstidspunktOpptjening, other.skjæringstidspunktOpptjening)
                && Objects.equals(this.førsteUttaksdato, other.førsteUttaksdato);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + utledetSkjæringstidspunkt + ">";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(Skjæringstidspunkt other) {
        return new Builder(other);
    }

    public static class Builder {
        private final Skjæringstidspunkt kladd;

        private Builder() {
            this.kladd = new Skjæringstidspunkt();
        }

        private Builder(Skjæringstidspunkt other) {
            this.kladd = new Skjæringstidspunkt(other);
        }

        public Builder medUtledetSkjæringstidspunkt(LocalDate dato) {
            kladd.utledetSkjæringstidspunkt = dato;
            return this;
        }

        public Builder medSkjæringstidspunktOpptjening(LocalDate dato) {
            kladd.skjæringstidspunktOpptjening = dato;
            return this;
        }

        public Builder medSkjæringstidspunktBeregning(LocalDate dato) {
            kladd.skjæringstidspunktBeregning = dato;
            return this;
        }

        public Builder medFørsteUttaksdato(LocalDate dato) {
            kladd.førsteUttaksdato = dato;
            return this;
        }

        public Builder medFørsteUttaksdatoGrunnbeløp(LocalDate dato) {
            kladd.førsteUttaksdatoGrunnbeløp = dato;
            return this;
        }

        public Builder medFørsteUttaksdatoSøknad(LocalDate dato) {
            kladd.førsteUttaksdatoSøknad = dato;
            return this;
        }

        public Builder medUttaksintervall(LocalDateInterval uttaksperiode) {
            kladd.uttaksintervall = uttaksperiode;
            return this;
        }

        public Builder medFamilieHendelseDato(FamilieHendelseDato familieHendelseDato) {
            kladd.familieHendelseDato = familieHendelseDato;
            return this;
        }

        public Builder medKreverSammenhengendeUttak(boolean sammenhengendeUttak) {
            kladd.kreverSammenhengendeUttak = sammenhengendeUttak;
            return this;
        }

        public Builder medUtenMinsterett(boolean utenMinsterett) {
            kladd.utenMinsterett = utenMinsterett;
            return this;
        }

        public Builder medUttakSkalJusteresTilFødselsdato(boolean uttakSkalJusteresTilFødselsdato) {
            kladd.uttakSkalJusteresTilFødselsdato = uttakSkalJusteresTilFødselsdato;
            return this;
        }

        public Skjæringstidspunkt build() {
            return kladd;
        }
    }
}
