package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;

public class UttakDto {
    @NotNull private final String stonadskontoType;
    private final String periodeResultatType;
    @NotNull private final boolean gradering;

    private UttakDto(String stonadskontoType, String periodeResultatType, boolean gradering) {
        this.stonadskontoType = stonadskontoType;
        this.periodeResultatType = periodeResultatType;
        this.gradering = gradering;
    }

    public String getStonadskontoType() {
        return stonadskontoType;
    }

    public String getPeriodeResultatType() {
        return periodeResultatType;
    }

    public boolean isGradering() {
        return gradering;
    }

    public static Builder build() {
        return new Builder();
    }

    public static class Builder {
        private UttakPeriodeType stønadskontoType;
        private PeriodeResultatType periodeResultatType;
        private boolean gradering;

        private Builder() {
        }

        public Builder medStønadskontoType(UttakPeriodeType stønadskontoType) {
            this.stønadskontoType = stønadskontoType;
            return this;
        }

        public Builder medPeriodeResultatType(PeriodeResultatType periodeResultatType) {
            this.periodeResultatType = periodeResultatType;
            return this;
        }

        public Builder medGradering(boolean gradering) {
            this.gradering = gradering;
            return this;
        }

        public UttakDto create() {
            var stonadskontoTypeString = stønadskontoType == null ? null : stønadskontoType.getKode();
            var periodeResultatTypeString = periodeResultatType == null ? null : periodeResultatType.getKode();
            return new UttakDto(stonadskontoTypeString, periodeResultatTypeString, gradering);
        }
    }
}
