package no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav;

import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "AktivitetskravGrunnlag")
@Table(name = "GR_AKTIVITETSKRAV_ARBEID")
public class AktivitetskravGrunnlagEnitet extends BaseEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_AKTIVITETSKRAV_ARBEID")
    private Long id;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @ChangeTracked
    @ManyToOne
    @JoinColumn(name = "aktivitetskrav_arbeid_perioder_id", updatable = false)
    private AktivitetskravArbeidPerioderEntitet perioderMedAktivitetskravArbeid;

    AktivitetskravGrunnlagEnitet() {
        //CDI
    }

    public AktivitetskravGrunnlagEnitet(AktivitetskravGrunnlagEnitet grunnlagEnitet) {
        grunnlagEnitet.getAktivitetskravPerioderMedArbeidEnitet().ifPresent(perioder -> this.perioderMedAktivitetskravArbeid = perioder);
    }

    public Optional<AktivitetskravArbeidPerioderEntitet> getAktivitetskravPerioderMedArbeidEnitet() {
        return Optional.ofNullable(perioderMedAktivitetskravArbeid);
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public void deaktiver() {
        this.aktiv = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AktivitetskravGrunnlagEnitet that = (AktivitetskravGrunnlagEnitet) o;
        return aktiv == that.aktiv && Objects.equals(id, that.id) && Objects.equals(behandlingId, that.behandlingId) && Objects.equals(
            perioderMedAktivitetskravArbeid, that.perioderMedAktivitetskravArbeid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, aktiv, behandlingId, perioderMedAktivitetskravArbeid);
    }

    public static class Builder {

        private AktivitetskravGrunnlagEnitet kladd;

        private Builder(AktivitetskravGrunnlagEnitet kladd) {
            this.kladd = kladd;
        }

        private static AktivitetskravGrunnlagEnitet.Builder nytt() {
            return new Builder(new AktivitetskravGrunnlagEnitet());
        }

        private static AktivitetskravGrunnlagEnitet.Builder oppdatere(AktivitetskravGrunnlagEnitet kladd) {
            return new Builder(new AktivitetskravGrunnlagEnitet(kladd));
        }

        public static AktivitetskravGrunnlagEnitet.Builder oppdatere(Optional<AktivitetskravGrunnlagEnitet> kladd) {
            return kladd.map(AktivitetskravGrunnlagEnitet.Builder::oppdatere).orElseGet(AktivitetskravGrunnlagEnitet.Builder::nytt);
        }

        public Builder medBehandlingId(Long behandlingId) {
            this.kladd.behandlingId = behandlingId;
            return this;
        }
        public Builder medPerioderMedAktivitetskravArbeid(AktivitetskravArbeidPerioderEntitet.Builder builder) {
            this.kladd.perioderMedAktivitetskravArbeid = builder.build();
            return this;
        }

        public AktivitetskravGrunnlagEnitet build() {
            return this.kladd;
        }
    }
}
