package no.nav.foreldrepenger.domene.entiteter;

import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.fasterxml.jackson.annotation.JsonBackReference;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.Hjemmel;

@Entity(name = "BeregningsgrunnlagAktivitetStatus")
@Table(name = "BG_AKTIVITET_STATUS")
public class BeregningsgrunnlagAktivitetStatus extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_AKTIVITET_STATUS")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @JsonBackReference
    @ManyToOne(cascade = {CascadeType.PERSIST}, optional = false)
    @JoinColumn(name = "beregningsgrunnlag_id", nullable = false, updatable = false)
    private BeregningsgrunnlagEntitet beregningsgrunnlag;

    @Convert(converter= AktivitetStatus.KodeverdiConverter.class)
    @Column(name="aktivitet_status", nullable = false)
    private AktivitetStatus aktivitetStatus;

    @Convert(converter= Hjemmel.KodeverdiConverter.class)
    @Column(name="hjemmel", nullable = false)
    private Hjemmel hjemmel;

    public BeregningsgrunnlagAktivitetStatus(BeregningsgrunnlagAktivitetStatus beregningsgrunnlagAktivitetStatus) {
        this.aktivitetStatus = beregningsgrunnlagAktivitetStatus.getAktivitetStatus();
        this.hjemmel = beregningsgrunnlagAktivitetStatus.getHjemmel();
    }

    protected BeregningsgrunnlagAktivitetStatus() {
    }

    public Long getId() {
        return id;
    }

    public BeregningsgrunnlagEntitet getBeregningsgrunnlag() {
        return beregningsgrunnlag;
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public Hjemmel getHjemmel() {
        return hjemmel;
    }

    void setBeregningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        this.beregningsgrunnlag = beregningsgrunnlag;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BeregningsgrunnlagAktivitetStatus other)) {
            return false;
        }
        return Objects.equals(this.getAktivitetStatus(), other.getAktivitetStatus())
                && Objects.equals(this.getBeregningsgrunnlag(), other.getBeregningsgrunnlag());
    }

    @Override
    public int hashCode() {
        return Objects.hash(beregningsgrunnlag, aktivitetStatus);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
                "id=" + id + ", "
                + "beregningsgrunnlag=" + beregningsgrunnlag + ", "
                + "aktivitetStatus=" + aktivitetStatus + ", "
                + "hjemmel=" + hjemmel + ", "
                + ">";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BeregningsgrunnlagAktivitetStatus beregningsgrunnlagAktivitetStatusMal;

        public Builder() {
            beregningsgrunnlagAktivitetStatusMal = new BeregningsgrunnlagAktivitetStatus();
            beregningsgrunnlagAktivitetStatusMal.hjemmel = Hjemmel.UDEFINERT;
        }

        public Builder medAktivitetStatus(AktivitetStatus aktivitetStatus) {
            beregningsgrunnlagAktivitetStatusMal.aktivitetStatus = aktivitetStatus;
            return this;
        }

        public Builder medHjemmel(Hjemmel hjemmel) {
            beregningsgrunnlagAktivitetStatusMal.hjemmel = hjemmel;
            return this;
        }

        public BeregningsgrunnlagAktivitetStatus build(BeregningsgrunnlagEntitet beregningsgrunnlag) {
            verifyStateForBuild();
            beregningsgrunnlag.leggTilBeregningsgrunnlagAktivitetStatus(beregningsgrunnlagAktivitetStatusMal);
            return beregningsgrunnlagAktivitetStatusMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(beregningsgrunnlagAktivitetStatusMal.aktivitetStatus, "aktivitetStatus");
            Objects.requireNonNull(beregningsgrunnlagAktivitetStatusMal.getHjemmel(), "hjemmel");
        }
    }
}
