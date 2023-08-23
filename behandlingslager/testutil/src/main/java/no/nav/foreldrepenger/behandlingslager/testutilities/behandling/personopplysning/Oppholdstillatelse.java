package no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning;

import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

import java.time.LocalDate;
import java.util.Objects;

public class Oppholdstillatelse  {

    private AktørId aktørId;
    private DatoIntervallEntitet periode;
    private OppholdstillatelseType tillatelse = OppholdstillatelseType.UDEFINERT;

    public Oppholdstillatelse() {
    }

    Oppholdstillatelse(Oppholdstillatelse personstatus) {
        this.aktørId = personstatus.getAktørId();
        this.periode = personstatus.getPeriode();
        this.tillatelse = personstatus.getTillatelse();
    }


    public OppholdstillatelseType getTillatelse() {
        return tillatelse;
    }

    public void setTillatelse(OppholdstillatelseType tillatelse) {
        this.tillatelse = tillatelse;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }


    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    void setPeriode(DatoIntervallEntitet gyldighetsperiode) {
        this.periode = gyldighetsperiode;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var entitet = (Oppholdstillatelse) o;
        return Objects.equals(aktørId, entitet.aktørId) &&
                Objects.equals(periode, entitet.periode) &&
                Objects.equals(tillatelse, entitet.tillatelse);
    }


    @Override
    public int hashCode() {
        return Objects.hash(aktørId, periode, tillatelse);
    }


    @Override
    public String toString() {
        return "OppholdstillatelseEntitet{" +
            "periode=" + periode +
            ", tillatelse=" + tillatelse +
            '}';
    }

    public static Oppholdstillatelse.OppholdstillatelseBuilder builder() {
        return new OppholdstillatelseBuilder(new Oppholdstillatelse());
    }

    public static final class OppholdstillatelseBuilder {

        private final Oppholdstillatelse kladd;

        private OppholdstillatelseBuilder(Oppholdstillatelse kladd) {
            this.kladd = kladd;
        }

        public Oppholdstillatelse.OppholdstillatelseBuilder medAktørId(AktørId aktørId) {
            kladd.setAktørId(aktørId);
            return this;
        }

        public Oppholdstillatelse.OppholdstillatelseBuilder medPeriode(LocalDate fom, LocalDate tom) {
            kladd.setPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
            return this;
        }

        public Oppholdstillatelse.OppholdstillatelseBuilder medOppholdstillatelse(OppholdstillatelseType tillatelse) {
            kladd.setTillatelse(tillatelse);
            return this;
        }

        public Oppholdstillatelse build() {
            return kladd;
        }

    }

}
