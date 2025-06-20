package no.nav.foreldrepenger.behandlingslager.behandling.eøs;

import java.util.List;
import java.util.Objects;

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
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "EøsUttak")
@Table(name = "GR_EOS_UTTAK")
public class EøsUttakGrunnlagEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_EOS_UTTAK")
    private Long id;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @ManyToOne
    @JoinColumn(name = "saksbehandler_perioder_id", updatable = false)
    private EøsUttaksperioderEntitet saksbehandlerPerioder;

    EøsUttakGrunnlagEntitet() {
        // Hibernate
    }

    EøsUttakGrunnlagEntitet(EøsUttakGrunnlagEntitet eøsUttakGrunnlagEntitet) {
        this.aktiv = eøsUttakGrunnlagEntitet.aktiv;
        this.behandlingId = eøsUttakGrunnlagEntitet.behandlingId;
        this.saksbehandlerPerioder = eøsUttakGrunnlagEntitet.saksbehandlerPerioder;
    }


    public boolean isAktiv() {
        return aktiv;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public EøsUttaksperioderEntitet getSaksbehandlerPerioderEntitet() {
        return saksbehandlerPerioder;
    }

    public List<EøsUttaksperiodeEntitet> getPerioder() {
        return saksbehandlerPerioder.getPerioder();
    }

    public void deaktiver() {
        this.aktiv = false;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        EøsUttakGrunnlagEntitet that = (EøsUttakGrunnlagEntitet) o;
        return aktiv == that.aktiv && Objects.equals(behandlingId, that.behandlingId) && Objects.equals(saksbehandlerPerioder,
            that.saksbehandlerPerioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktiv, behandlingId, saksbehandlerPerioder);
    }

    public static class Builder {
        private EøsUttakGrunnlagEntitet kladd;

        private Builder(EøsUttakGrunnlagEntitet kladd) {
            this.kladd = kladd;
        }

        public static Builder ny() {
            return new EøsUttakGrunnlagEntitet.Builder(new EøsUttakGrunnlagEntitet());
        }

        public Builder medBehandlingId(Long behandlingId) {
            this.kladd.behandlingId = behandlingId;
            return this;
        }

        public Builder medEøsUttaksperioder(EøsUttaksperioderEntitet saksbehandlerPerioder) {
            this.kladd.saksbehandlerPerioder = saksbehandlerPerioder;
            return this;
        }

        public EøsUttakGrunnlagEntitet build() {
            if (this.kladd.behandlingId == null) {
                throw new IllegalStateException("Behandling ID må settes før EøsUttakEntitet kan bygges");
            }
            if (this.kladd.saksbehandlerPerioder == null) {
                throw new IllegalStateException("Uttaksperioder må settes før EøsUttakEntitet kan bygges");
            }
            return this.kladd;
        }
    }

}
