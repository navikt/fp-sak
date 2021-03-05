package no.nav.foreldrepenger.økonomistøtte.ny.domene;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Ytelse {

    public static final Ytelse EMPTY = Ytelse.builder().build();

    private List<YtelsePeriode> perioder;

    private Ytelse(List<YtelsePeriode> perioder) {
        this.perioder = perioder;
    }

    public List<YtelsePeriode> getPerioder() {
        return Collections.unmodifiableList(perioder);
    }

    public List<YtelsePeriode> getPerioderFraOgMed(LocalDate fom) {
        List<YtelsePeriode> resultat = new ArrayList<>();
        for (YtelsePeriode ytelsePeriode : perioder) {
            if (ytelsePeriode.getPeriode().getTom().isBefore(fom)) {
                continue;
            }

            if (ytelsePeriode.getPeriode().getFom().isBefore(fom) && ytelsePeriode.getSats().getSatsType() != SatsType.ENGANG) {
                    resultat.add(ytelsePeriode.fraOgMed(fom));
            } else {
                resultat.add(ytelsePeriode);
            }
        }
        return resultat;
    }

    public YtelseVerdi finnVerdiFor(LocalDate dato) {
        for (YtelsePeriode yp : perioder) {
            if (yp.getPeriode().overlapper(dato)) {
                return yp.getVerdi();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Ytelse{" +
            "perioder=" + perioder +
            '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public long summerYtelse() {
        return perioder.stream()
            .mapToLong(YtelsePeriode::summerYtelse)
            .sum();
    }

    public LocalDate getFørsteDato() {
        return perioder.isEmpty() ? null : perioder.get(0).getPeriode().getFom();
    }

    public boolean harVerdiPåEllerEtter(LocalDate dato) {
        return perioder.stream()
            .anyMatch(p -> !p.getPeriode().getTom().isBefore(dato));
    }

    public static class Builder {

        private ArrayList<YtelsePeriode> perioder = new ArrayList<>();

        private Builder() {
        }

        public Builder leggTilPeriode(YtelsePeriode periode) {
            perioder.add(periode);
            return this;
        }

        public Ytelse build() {
            return new Ytelse(perioder);
        }

        public void fjernAltEtter(LocalDate opphørFomDato) {
            ArrayList<YtelsePeriode> resultat = new ArrayList<>();
            for (YtelsePeriode yp : perioder) {
                Periode periode = yp.getPeriode();
                if (periode.getTom().isBefore(opphørFomDato)) {
                    resultat.add(yp);
                } else if (periode.getFom().isBefore(opphørFomDato)) {
                    Periode fortsattAktivPeriode = Periode.of(periode.getFom(), opphørFomDato.minusDays(1));
                    resultat.add(new YtelsePeriode(fortsattAktivPeriode, yp.getSats(), yp.getUtbetalingsgrad()));
                }
            }
            perioder = resultat;
        }
        public LocalDate sisteTidspunkt() {
            if (perioder.isEmpty()) {
                return null;
            }
            return perioder.get(perioder.size() - 1).getPeriode().getTom();
        }

        public boolean erTom() {
            return perioder.isEmpty();
        }
    }
}
