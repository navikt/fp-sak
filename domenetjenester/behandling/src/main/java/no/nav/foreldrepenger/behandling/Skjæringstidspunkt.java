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
    private LocalDate førsteUttaksdatoFødseljustert;
    private LocalDateInterval utledetMedlemsintervall;
    private boolean kvalifisertFriUtsettelse = false;

    private Skjæringstidspunkt() {
        // hide constructor
    }

    private Skjæringstidspunkt(Skjæringstidspunkt other) {
        this.utledetSkjæringstidspunkt = other.utledetSkjæringstidspunkt;
        this.skjæringstidspunktOpptjening = other.skjæringstidspunktOpptjening;
        this.skjæringstidspunktBeregning = other.skjæringstidspunktBeregning;
        this.førsteUttaksdato = other.førsteUttaksdato;
        this.førsteUttaksdatoGrunnbeløp = other.førsteUttaksdatoGrunnbeløp;
        this.førsteUttaksdatoFødseljustert = other.førsteUttaksdatoFødseljustert;
        this.kvalifisertFriUtsettelse = other.kvalifisertFriUtsettelse;
    }

    public Optional<LocalDate> getSkjæringstidspunktHvisUtledet() {
        return Optional.ofNullable(utledetSkjæringstidspunkt);
    }

    public LocalDateInterval getUtledetMedlemsintervall() {
        return utledetMedlemsintervall;
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
    public LocalDate getSkjæringstidspunktBeregning() {
        Objects.requireNonNull(skjæringstidspunktBeregning,
                "Utvikler-feil: skjæringstidspunktBeregning er ikke satt. Sørg for at det er satt ifht. anvendelse");
        return skjæringstidspunktBeregning;
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

    /** Grunnbeløpdato er første dag med innvilget uttak/utsettelse/overføring. */
    public LocalDate getFørsteUttaksdatoGrunnbeløp() {
        Objects.requireNonNull(førsteUttaksdatoGrunnbeløp, "Utvikler-feil: grunnbeløpdato er ikke satt. Sørg for at det er satt ifht. anvendelse");
        return førsteUttaksdatoGrunnbeløp;
    }

    /** Første uttaksdato er første dag stønadsperioden løper - før evaluering av opptjeningsperiode. */
    public LocalDate getFørsteUttaksdatoFødseljustert() {
        Objects.requireNonNull(førsteUttaksdatoFødseljustert, "Utvikler-feil: fødselsjustert uttaksdato er ikke satt. Sørg for at det er satt ifht. anvendelse");
        return førsteUttaksdatoFødseljustert;
    }

    /** Skal behandles etter nytt regelverk for uttak anno 2021. */
    public boolean isKvalifisertFriUtsettelse() {
        return kvalifisertFriUtsettelse;
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
        if ((obj == null) || !(obj.getClass().equals(this.getClass()))) {
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

        public Builder medUtledetMedlemsintervall(LocalDateInterval datointervall) {
            kladd.utledetMedlemsintervall = datointervall;
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

        public Builder medFørsteUttaksdatoFødseljustert(LocalDate dato) {
            kladd.førsteUttaksdatoFødseljustert = dato;
            return this;
        }

        public Builder medKvalifisertFriUtsettelse(boolean erKvalifisert) {
            kladd.kvalifisertFriUtsettelse = erKvalifisert;
            return this;
        }

        public Skjæringstidspunkt build() {
            return kladd;
        }
    }
}
