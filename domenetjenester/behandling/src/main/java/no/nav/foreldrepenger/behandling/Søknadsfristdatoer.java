package no.nav.foreldrepenger.behandling;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.fpsak.tidsserie.LocalDateInterval;

/**
 * Inneholder relevante fristrelaterte datoer for en behandling
 */
public class Søknadsfristdatoer {

    private LocalDateInterval søknadGjelderPeriode;
    private LocalDate utledetSøknadsfrist;
    private LocalDate søknadMottattDato;
    private Long dagerOversittetFrist;

    public LocalDateInterval getSøknadGjelderPeriode() {
        return søknadGjelderPeriode;
    }

    public LocalDate getUtledetSøknadsfrist() {
        return utledetSøknadsfrist;
    }

    public LocalDate getSøknadMottattDato() {
        return søknadMottattDato;
    }

    public Long getDagerOversittetFrist() {
        return dagerOversittetFrist;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Søknadsfristdatoer))
            return false;
        var that = (Søknadsfristdatoer) o;
        return Objects.equals(søknadGjelderPeriode, that.søknadGjelderPeriode)
            && Objects.equals(søknadMottattDato, that.søknadMottattDato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(søknadGjelderPeriode, søknadMottattDato);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + utledetSøknadsfrist + ">";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Søknadsfristdatoer kladd;

        private Builder() {
            this.kladd = new Søknadsfristdatoer();
        }

        public Builder medSøknadGjelderPeriode(LocalDateInterval datointervall) {
            kladd.søknadGjelderPeriode = datointervall;
            return this;
        }

        public Builder medUtledetSøknadsfrist(LocalDate dato) {
            kladd.utledetSøknadsfrist = dato;
            return this;
        }

        public Builder medSøknadMottattDato(LocalDate dato) {
            kladd.søknadMottattDato = dato;
            return this;
        }

        public Builder medDagerOversittetFrist(Long dager) {
            kladd.dagerOversittetFrist = dager;
            return this;
        }

        public Søknadsfristdatoer build() {
            return kladd;
        }
    }
}
