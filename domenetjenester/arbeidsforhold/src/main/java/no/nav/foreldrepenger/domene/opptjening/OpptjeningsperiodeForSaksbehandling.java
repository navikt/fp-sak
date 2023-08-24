package no.nav.foreldrepenger.domene.opptjening;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

import java.util.Objects;

public class OpptjeningsperiodeForSaksbehandling {

    private OpptjeningAktivitetType opptjeningAktivitetType;
    private Opptjeningsnøkkel grupperingNøkkel;
    private Arbeidsgiver arbeidsgiver;
    private Stillingsprosent stillingsprosent;
    private DatoIntervallEntitet periode;
    private VurderingsStatus vurderingsStatus;
    private Boolean erPeriodeEndret = false;
    private Boolean erManueltRegistrert = false;
    private String begrunnelse;
    private Boolean manueltBehandlet = false;
    private String arbeidsgiverUtlandNavn;

    private OpptjeningsperiodeForSaksbehandling() {
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

    public Stillingsprosent getStillingsprosent() {
        return stillingsprosent;
    }

    public VurderingsStatus getVurderingsStatus() {
        return vurderingsStatus;
    }

    public Boolean getErManueltRegistrert() {
        return erManueltRegistrert;
    }

    public Boolean getErPeriodeEndret() {
        return erPeriodeEndret;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public boolean erManueltBehandlet() {
        return manueltBehandlet;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public String getArbeidsgiverUtlandNavn() {
        return arbeidsgiverUtlandNavn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var other = (OpptjeningsperiodeForSaksbehandling) o;
        return Objects.equals(opptjeningAktivitetType, other.opptjeningAktivitetType) &&
                Objects.equals(grupperingNøkkel, other.grupperingNøkkel) &&
                Objects.equals(erPeriodeEndret, other.erPeriodeEndret) &&
                Objects.equals(erManueltRegistrert, other.erManueltRegistrert) &&
                Objects.equals(begrunnelse, other.begrunnelse);
    }

    @Override
    public int hashCode() {

        return Objects.hash(opptjeningAktivitetType, grupperingNøkkel, erPeriodeEndret, erManueltRegistrert, begrunnelse);
    }

    @Override
    public String toString() {
        return "OpptjeningsperiodeForSaksbehandling{" +
                "opptjeningAktivitetType=" + opptjeningAktivitetType +
                ", grupperingNøkkel=" + grupperingNøkkel +
                ", arbeidsgiver=" + arbeidsgiver +
                ", stillingsprosent=" + stillingsprosent +
                ", periode=" + periode +
                ", vurderingsStatus=" + vurderingsStatus +
                ", erPeriodeEndret=" + erPeriodeEndret +
                ", erManueltRegistrert=" + erManueltRegistrert +
                ", begrunnelse='" + begrunnelse + '\'' +
                ", manueltBehandlet=" + manueltBehandlet +
                ", arbeidsgiverUtlandNavn='" + arbeidsgiverUtlandNavn + '\'' +
                '}';
    }

    public static class Builder {
        private OpptjeningsperiodeForSaksbehandling kladd;

        private Builder(OpptjeningsperiodeForSaksbehandling periode) {
            kladd = periode;
        }

        public static Builder ny() {
            return new Builder(new OpptjeningsperiodeForSaksbehandling());
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

        public Builder medStillingsandel(Stillingsprosent stillingsprosent) {
            kladd.stillingsprosent = stillingsprosent;
            return this;
        }

        public Builder medVurderingsStatus(VurderingsStatus status) {
            kladd.vurderingsStatus = status;
            return this;
        }

        public Builder medErManueltRegistrert() {
            kladd.erManueltRegistrert = true;
            return this;
        }

        public Builder medErPeriodenEndret() {
            kladd.erPeriodeEndret = true;
            return this;
        }

        public Builder medBegrunnelse(String begrunnelse) {
            kladd.begrunnelse = begrunnelse;
            return this;
        }

        public Builder medErManueltBehandlet() {
            kladd.manueltBehandlet = true;
            return this;
        }

        public Builder medArbeidsgiverUtlandNavn(String arbeidsgiverNavn) {
            kladd.arbeidsgiverUtlandNavn = arbeidsgiverNavn;
            return this;
        }

        public Builder medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
            kladd.arbeidsgiver = arbeidsgiver;
            return this;
        }

        public OpptjeningsperiodeForSaksbehandling build() {
            valider();
            return kladd;
        }

        private void valider() {
            // Opptjeningsperiode av typen arbeid krever alltid arbeidsgiver
            if (kladd.opptjeningAktivitetType == OpptjeningAktivitetType.ARBEID && kladd.arbeidsgiver == null && (kladd.grupperingNøkkel == null
                || kladd.grupperingNøkkel.getArbeidsgiverType() == null)) {
                throw new IllegalStateException("Informasjon om arbeidsgiver mangler for " + kladd.toString());
            }
        }
    }
}
