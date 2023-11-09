package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseCreateableEntitet;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.Alvorlighetsgrad;

@Entity(name = "OppdragKvittering")
@Table(name = "OPPDRAG_KVITTERING")
public class OppdragKvittering extends BaseCreateableEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OPPDRAG_KVITTERING")
    private Long id;

    @Convert(converter = Alvorlighetsgrad.KodeverdiConverter.class)
    @Column(name = "alvorlighetsgrad")
    private Alvorlighetsgrad alvorlighetsgrad;

    @Column(name = "beskr_melding")
    private String beskrMelding;

    @Column(name = "melding_kode")
    private String meldingKode;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oppdrag_110_id", nullable = false)
    private Oppdrag110 oppdrag110;

    protected OppdragKvittering() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Alvorlighetsgrad getAlvorlighetsgrad() {
        return alvorlighetsgrad;
    }

    public void setAlvorlighetsgrad(Alvorlighetsgrad alvorlighetsgrad) {
        this.alvorlighetsgrad = alvorlighetsgrad;
    }

    public String getBeskrMelding() {
        return beskrMelding;
    }

    public void setBeskrMelding(String beskrMelding) {
        this.beskrMelding = beskrMelding;
    }

    public String getMeldingKode() {
        return meldingKode;
    }

    public void setMeldingKode(String meldingKode) {
        this.meldingKode = meldingKode;
    }

    public Oppdrag110 getOppdrag110() {
        return oppdrag110;
    }

    public void setOppdrag110(Oppdrag110 oppdrag110) {
        this.oppdrag110 = oppdrag110;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof OppdragKvittering oppdragKvittering)) {
            return false;
        }
        return Objects.equals(alvorlighetsgrad, oppdragKvittering.getAlvorlighetsgrad())
            && Objects.equals(beskrMelding, oppdragKvittering.getBeskrMelding())
            && Objects.equals(meldingKode, oppdragKvittering.getMeldingKode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(alvorlighetsgrad, beskrMelding, meldingKode);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Alvorlighetsgrad alvorlighetsgrad;
        private String beskrMelding;
        private String meldingKode;
        private Oppdrag110 oppdrag110;

        public Builder medAlvorlighetsgrad(Alvorlighetsgrad alvorlighetsgrad) {
            this.alvorlighetsgrad = alvorlighetsgrad;
            return this;
        }

        public Builder medBeskrMelding(String beskrMelding) {
            this.beskrMelding = beskrMelding;
            return this;
        }

        public Builder medMeldingKode(String meldingKode) {
            this.meldingKode = meldingKode;
            return this;
        }

        public Builder medOppdrag110(Oppdrag110 oppdrag110) {
            this.oppdrag110 = oppdrag110;
            return this;
        }

        public OppdragKvittering build() {
            verifyStateForBuild();
            var oppdragKvittering = new OppdragKvittering();
            oppdragKvittering.alvorlighetsgrad = alvorlighetsgrad;
            oppdragKvittering.beskrMelding = beskrMelding;
            oppdragKvittering.meldingKode = meldingKode;
            oppdragKvittering.oppdrag110 = oppdrag110;
            oppdrag110.setOppdragKvittering(oppdragKvittering);
            return oppdragKvittering;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(oppdrag110, "oppdrag110");
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            (id != null ? "id=" + id + ", " : "")
            + "alvorlighetsgrad=" + alvorlighetsgrad + ", "
            + "beskrMelding=" + beskrMelding + ", "
            + "meldingKode=" + meldingKode + ", "
            + "opprettetTs=" + getOpprettetTidspunkt()
            + ">";
    }

}
