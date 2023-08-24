package no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning;

import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

import java.time.LocalDate;


public final class Statsborgerskap {

    private AktørId aktørId;
    private DatoIntervallEntitet periode;
    private Landkoder statsborgerskap = Landkoder.UDEFINERT;

    public AktørId getAktørId() {
        return aktørId;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public Landkoder getStatsborgerskap() {
        return statsborgerskap;
    }

    private Statsborgerskap(Builder builder) {
        this.aktørId = builder.aktørId;
        this.periode = builder.periode;
        this.statsborgerskap = builder.statsborgerskap;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private AktørId aktørId;
        private DatoIntervallEntitet periode;
        private Landkoder statsborgerskap;

        private Builder() {
        }

        public Statsborgerskap build() {
            return new Statsborgerskap(this);
        }

        public Builder aktørId(AktørId aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder periode(LocalDate fom, LocalDate tom) {
            this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
            return this;
        }

        public Builder statsborgerskap(Landkoder statsborgerskap) {
            this.statsborgerskap = statsborgerskap;
            return this;
        }

    }
}
