package no.nav.foreldrepenger.domene.medlem.api;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

/**
 * Endringsresultat i personopplysninger for medlemskap
 */
public class EndringsresultatPersonopplysningerForMedlemskap {

    private Optional<LocalDate> gjeldendeFra;
    private List<Endring> endringer = new ArrayList<>();

    private EndringsresultatPersonopplysningerForMedlemskap() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<Endring> getEndredeAttributter() {
        return endringer.stream().filter(Endring::isEndret).toList();
    }

    /**
     * @return er satt hvis det er endringer
     */
    public Optional<LocalDate> getGjeldendeFra() {
        return gjeldendeFra;
    }

    private static final class Endring {
        private final boolean endret;
        private final DatoIntervallEntitet periode;

        private Endring(DatoIntervallEntitet periode, String endretFra, String endretTil) {
            Objects.requireNonNull(endretFra);
            Objects.requireNonNull(endretTil);
            Objects.requireNonNull(periode);

            this.periode = periode;
            this.endret = !endretFra.trim().equalsIgnoreCase(endretTil.trim());
        }

        private boolean isEndret() {
            return endret;
        }

        private DatoIntervallEntitet getPeriode() {
            return periode;
        }
    }

    public static final class Builder {
        EndringsresultatPersonopplysningerForMedlemskap kladd = new EndringsresultatPersonopplysningerForMedlemskap();

        private Builder() {
        }

        public EndringsresultatPersonopplysningerForMedlemskap build() {
            this.kladd.gjeldendeFra = kladd.getEndredeAttributter().stream()
                .map(e -> e.getPeriode().getFomDato())
                .min(LocalDate::compareTo);
            return kladd;
        }

        public Builder leggTilEndring(DatoIntervallEntitet periode, String endretFra, String endretTil) {
            var endring = new Endring(periode, endretFra, endretTil);
            kladd.endringer.add(endring);
            return this;
        }
    }
}
