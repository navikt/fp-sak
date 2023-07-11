package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BeregningsresultatMedUttaksplanDto {
    private final boolean sokerErMor;
    private LocalDate opphoersdato;
    private final BeregningsresultatPeriodeDto[] perioder;
    private final BeregningsresultatPeriodeDto[] utbetPerioder;
    private final Boolean skalHindreTilbaketrekk;

    private BeregningsresultatMedUttaksplanDto(Builder builder) {
        this.sokerErMor = builder.sokerErMor;
        this.opphoersdato = builder.opphoersdato;
        this.perioder = builder.perioder.toArray(BeregningsresultatPeriodeDto[]::new);
        this.utbetPerioder = builder.utbetPerioder == null ? null : builder.utbetPerioder.toArray(BeregningsresultatPeriodeDto[]::new);
        this.skalHindreTilbaketrekk = builder.skalHindreTilbaketrekk;
    }

    public boolean getSokerErMor() {
        return sokerErMor;
    }

    public LocalDate getOpphoersdato() {
        return opphoersdato;
    }

    public BeregningsresultatPeriodeDto[] getPerioder() {
        return Arrays.copyOf(perioder, perioder.length);
    }

    public BeregningsresultatPeriodeDto[] getUtbetPerioder() {
        return utbetPerioder;
    }

    public Boolean getSkalHindreTilbaketrekk() {
        return skalHindreTilbaketrekk;
    }

    public static Builder build() {
        return new Builder();
    }

    public static class Builder {
        private boolean sokerErMor;
        private LocalDate opphoersdato;
        private List<BeregningsresultatPeriodeDto> perioder;
        private List<BeregningsresultatPeriodeDto> utbetPerioder;
        private Boolean skalHindreTilbaketrekk;

        private Builder() {
            perioder = new ArrayList<>();
        }

        public Builder medSokerErMor(boolean sokerErMor) {
            this.sokerErMor = sokerErMor;
            return this;
        }

        public Builder medOpphoersdato(LocalDate opphoersdato) {
            this.opphoersdato = opphoersdato;
            return this;
        }

        public Builder medSkalHindreTilbaketrekk(Boolean skalHindreTilbaketrekk) {
            this.skalHindreTilbaketrekk = skalHindreTilbaketrekk;
            return this;
        }

        public Builder medPerioder(List<BeregningsresultatPeriodeDto> perioder) {
            this.perioder = perioder;
            return this;
        }

        public Builder medUtbetPerioder(List<BeregningsresultatPeriodeDto> utbetPerioder) {
            this.utbetPerioder = utbetPerioder;
            return this;
        }

        public BeregningsresultatMedUttaksplanDto create() {
            return new BeregningsresultatMedUttaksplanDto(this);
        }
    }
}
