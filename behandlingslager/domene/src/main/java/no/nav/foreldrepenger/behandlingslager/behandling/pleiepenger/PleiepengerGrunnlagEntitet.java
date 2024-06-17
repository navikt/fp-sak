package no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger;

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

@Entity(name = "PleiepengerGrunnlag")
@Table(name = "GR_PLEIEPENGER")
public class PleiepengerGrunnlagEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_PLEIEPENGER")
    private Long id;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @ChangeTracked
    @ManyToOne
    @JoinColumn(name = "psb_perioder_id", updatable = false)
    private PleiepengerPerioderEntitet perioderMedInnleggelse;

    PleiepengerGrunnlagEntitet() {
    }

    PleiepengerGrunnlagEntitet(PleiepengerGrunnlagEntitet behandlingsgrunnlag) {
        behandlingsgrunnlag.getPerioderMedInnleggelse().ifPresent(perioder -> this.perioderMedInnleggelse = perioder);
    }

    Long getId() {
        return id;
    }

    public long getBehandlingId() {
        return behandlingId;
    }

    public Optional<PleiepengerPerioderEntitet> getPerioderMedInnleggelse() {
        return Optional.ofNullable(perioderMedInnleggelse);
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public void deaktiver() {
        this.aktiv = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (PleiepengerGrunnlagEntitet) o;
        return Objects.equals(behandlingId, that.behandlingId) && Objects.equals(perioderMedInnleggelse, that.perioderMedInnleggelse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, perioderMedInnleggelse);
    }

    public static class Builder {

        private PleiepengerGrunnlagEntitet kladd;

        private Builder(PleiepengerGrunnlagEntitet kladd) {
            this.kladd = kladd;
        }

        private static Builder nytt() {
            return new Builder(new PleiepengerGrunnlagEntitet());
        }

        private static Builder oppdatere(PleiepengerGrunnlagEntitet kladd) {
            return new Builder(new PleiepengerGrunnlagEntitet(kladd));
        }

        public static Builder oppdatere(Optional<PleiepengerGrunnlagEntitet> kladd) {
            return kladd.map(Builder::oppdatere).orElseGet(Builder::nytt);
        }

        public Builder medBehandlingId(Long behandlingId) {
            this.kladd.behandlingId = behandlingId;
            return this;
        }

        public Builder medInnleggelsePerioder(PleiepengerPerioderEntitet.Builder perioder) {
            this.kladd.perioderMedInnleggelse = perioder.build();
            return this;
        }

        public PleiepengerGrunnlagEntitet build() {
            return this.kladd;
        }
    }
}
