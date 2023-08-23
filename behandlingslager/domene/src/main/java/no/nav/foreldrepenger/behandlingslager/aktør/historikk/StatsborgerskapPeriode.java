package no.nav.foreldrepenger.behandlingslager.aktør.historikk;


import no.nav.foreldrepenger.behandlingslager.aktør.Statsborgerskap;

import java.util.Objects;

public class StatsborgerskapPeriode {

    private Gyldighetsperiode gyldighetsperiode;
    private Statsborgerskap statsborgerskap;

    public StatsborgerskapPeriode(Gyldighetsperiode gyldighetsperiode, Statsborgerskap statsborgerskap) {
        this.gyldighetsperiode = gyldighetsperiode;
        this.statsborgerskap = statsborgerskap;
    }

    public Gyldighetsperiode getGyldighetsperiode() {
        return this.gyldighetsperiode;
    }

    public Statsborgerskap getStatsborgerskap() {
        return this.statsborgerskap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var periode = (StatsborgerskapPeriode) o;
        return Objects.equals(gyldighetsperiode, periode.gyldighetsperiode) &&
            Objects.equals(statsborgerskap, periode.statsborgerskap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gyldighetsperiode, statsborgerskap);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder("StatsborgerskapPeriode{");
        sb.append("gyldig=").append(gyldighetsperiode);
        sb.append(", land=").append(statsborgerskap != null ? statsborgerskap.getLandkode() : null);
        sb.append('}');
        return sb.toString();
    }
}
