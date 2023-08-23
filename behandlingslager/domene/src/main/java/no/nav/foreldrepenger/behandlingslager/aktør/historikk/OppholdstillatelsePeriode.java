package no.nav.foreldrepenger.behandlingslager.aktør.historikk;

import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;

import java.util.Objects;

public class OppholdstillatelsePeriode {

    private Gyldighetsperiode gyldighetsperiode;
    private OppholdstillatelseType tillatelse;

    public OppholdstillatelsePeriode(Gyldighetsperiode gyldighetsperiode, OppholdstillatelseType tillatelse) {
        this.gyldighetsperiode = gyldighetsperiode;
        this.tillatelse = tillatelse;
    }

    public Gyldighetsperiode getGyldighetsperiode() {
        return this.gyldighetsperiode;
    }

    public OppholdstillatelseType getTillatelse() {
        return tillatelse;
    }

    @Override
    public String toString() {
        return "OppholdstillatelsePeriode{" +
            "gyldighetsperiode=" + gyldighetsperiode +
            ", tillatelse=" + tillatelse +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (OppholdstillatelsePeriode) o;
        return Objects.equals(gyldighetsperiode, that.gyldighetsperiode) && tillatelse == that.tillatelse;
    }

    @Override
    public int hashCode() {
        return Objects.hash(gyldighetsperiode, tillatelse);
    }
}
