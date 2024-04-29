package no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "AktivitetskravGrunnlag")
@Table(name = "GR_AKTIVITETSKRAV_ARBEID")
public class AktivitetskravGrunnlagEntitet extends BaseEntitet {
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
    @ChangeTracked
    @Embedded
    @AttributeOverride(name = "fomDato", column = @Column(name = "periode_fom"))
    @AttributeOverride(name = "tomDato", column = @Column(name = "periode_tom"))
    DatoIntervallEntitet periode;

    AktivitetskravGrunnlagEntitet() {
        //CDI
    }

    public AktivitetskravGrunnlagEntitet(AktivitetskravGrunnlagEntitet grunnlagEnitet) {
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

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AktivitetskravGrunnlagEntitet that = (AktivitetskravGrunnlagEntitet) o;
        return aktiv == that.aktiv && Objects.equals(id, that.id) && Objects.equals(behandlingId, that.behandlingId) && Objects.equals(
            perioderMedAktivitetskravArbeid, that.perioderMedAktivitetskravArbeid) && Objects.equals(periode, that.periode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, aktiv, behandlingId, perioderMedAktivitetskravArbeid, periode);
    }

    public static class Builder {

        private AktivitetskravGrunnlagEntitet kladd;

        private Builder(AktivitetskravGrunnlagEntitet kladd) {
            this.kladd = kladd;
        }

        private static AktivitetskravGrunnlagEntitet.Builder nytt() {
            return new Builder(new AktivitetskravGrunnlagEntitet());
        }

        private static AktivitetskravGrunnlagEntitet.Builder oppdatere(AktivitetskravGrunnlagEntitet kladd) {
            return new Builder(new AktivitetskravGrunnlagEntitet(kladd));
        }

        public static AktivitetskravGrunnlagEntitet.Builder oppdatere(Optional<AktivitetskravGrunnlagEntitet> kladd) {
            return kladd.map(AktivitetskravGrunnlagEntitet.Builder::oppdatere).orElseGet(AktivitetskravGrunnlagEntitet.Builder::nytt);
        }

        public Builder medBehandlingId(Long behandlingId) {
            this.kladd.behandlingId = behandlingId;
            return this;
        }
        public Builder medPerioderMedAktivitetskravArbeid(AktivitetskravArbeidPerioderEntitet.Builder builder) {
            this.kladd.perioderMedAktivitetskravArbeid = builder.build();
            return this;
        }
        public Builder medPeriode(LocalDate fom, LocalDate tom) {
            this.kladd.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
            return this;
        }

        public AktivitetskravGrunnlagEntitet build() {
            return this.kladd;
        }
    }
}
