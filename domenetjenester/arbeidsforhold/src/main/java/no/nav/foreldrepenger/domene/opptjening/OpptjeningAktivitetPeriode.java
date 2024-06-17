package no.nav.foreldrepenger.domene.opptjening;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

public class OpptjeningAktivitetPeriode {
    private OpptjeningAktivitetType opptjeningAktivitetType;
    private Opptjeningsnøkkel grupperingNøkkel;
    private String orgnr;
    private Stillingsprosent stillingsprosent;
    private DatoIntervallEntitet periode;
    private VurderingsStatus vurderingsStatus;
    private String begrunnelse;

    private OpptjeningAktivitetPeriode() {
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public OpptjeningAktivitetType getOpptjeningAktivitetType() {
        return opptjeningAktivitetType;
    }

    public Opptjeningsnøkkel getOpptjeningsnøkkel() {
        return grupperingNøkkel;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public Stillingsprosent getStillingsprosent() {
        return stillingsprosent;
    }

    public VurderingsStatus getVurderingsStatus() {
        return vurderingsStatus;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var other = (OpptjeningAktivitetPeriode) o;
        return Objects.equals(opptjeningAktivitetType, other.opptjeningAktivitetType) && Objects.equals(grupperingNøkkel, other.grupperingNøkkel)
            && Objects.equals(begrunnelse, other.begrunnelse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(opptjeningAktivitetType, grupperingNøkkel, begrunnelse);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + "opptjeningAktivitetType=" + opptjeningAktivitetType + ", periode=" + periode + (
            orgnr == null ? "" : ", orgnr=" + orgnr) + (grupperingNøkkel == null ? "" : ", grupperingNøkkel=" + grupperingNøkkel) + (
            vurderingsStatus == null ? "" : ", vurderingsStatus=" + vurderingsStatus) + ">";
    }

    public static class Builder {
        private OpptjeningAktivitetPeriode kladd;

        private Builder(OpptjeningAktivitetPeriode periode) {
            kladd = periode;
        }

        public static Builder ny() {
            return new Builder(new OpptjeningAktivitetPeriode());
        }

        public static Builder lagNyBasertPå(OpptjeningAktivitetPeriode kladd) {
            var periode = ny().medPeriode(kladd.getPeriode())
                .medOpptjeningAktivitetType(kladd.getOpptjeningAktivitetType())
                .medVurderingsStatus(kladd.getVurderingsStatus())
                .medOpptjeningsnøkkel(kladd.getOpptjeningsnøkkel())
                .medBegrunnelse(kladd.getBegrunnelse())
                .medOrgnr(kladd.getOrgnr())
                .medStillingsandel(kladd.getStillingsprosent())
                .build();
            return new Builder(periode);
        }

        public Builder medPeriode(DatoIntervallEntitet periode) {
            kladd.periode = periode;
            return this;
        }

        public Builder medOpptjeningAktivitetType(OpptjeningAktivitetType type) {
            Objects.requireNonNull(type, "opptjeningAktivitetType");
            kladd.opptjeningAktivitetType = type;
            return this;
        }

        public Builder medOpptjeningsnøkkel(Opptjeningsnøkkel opptjeningsnøkkel) {
            kladd.grupperingNøkkel = opptjeningsnøkkel;
            return this;
        }

        Builder medOrgnr(String orgnr) {
            kladd.orgnr = orgnr;
            return this;
        }

        Builder medStillingsandel(Stillingsprosent stillingsprosent) {
            kladd.stillingsprosent = stillingsprosent;
            return this;
        }

        public Builder medVurderingsStatus(VurderingsStatus status) {
            kladd.vurderingsStatus = status;
            return this;
        }

        Builder medBegrunnelse(String begrunnelse) {
            kladd.begrunnelse = begrunnelse;
            return this;
        }

        public OpptjeningAktivitetPeriode build() {
            valider();
            return kladd;
        }

        private void valider() {
            // Opptjeningsperiode av typen arbeid krever alltid arbeidsgiver
            if (kladd.opptjeningAktivitetType == OpptjeningAktivitetType.ARBEID && (kladd.grupperingNøkkel == null
                || kladd.grupperingNøkkel.getArbeidsgiverType() == null)) {
                throw new IllegalStateException("Validering feilet for" + kladd.toString());
            }
        }
    }
}
