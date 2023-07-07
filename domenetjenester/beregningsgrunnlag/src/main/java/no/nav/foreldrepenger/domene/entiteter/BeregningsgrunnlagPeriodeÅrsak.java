package no.nav.foreldrepenger.domene.entiteter;


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
import jakarta.persistence.Version;

import com.fasterxml.jackson.annotation.JsonBackReference;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.domene.modell.kodeverk.PeriodeÅrsak;

@Entity(name = "BeregningsgrunnlagPeriodeÅrsak")
@Table(name = "BG_PERIODE_AARSAK")
public class BeregningsgrunnlagPeriodeÅrsak extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_PERIODE_AARSAK")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @JsonBackReference
    @ManyToOne(optional = false)
    @JoinColumn(name = "bg_periode_id", nullable = false, updatable = false)
    private BeregningsgrunnlagPeriode beregningsgrunnlagPeriode;

    @Convert(converter = PeriodeÅrsak.KodeverdiConverter.class)
    @Column(name="periode_aarsak", nullable = false)
    private PeriodeÅrsak periodeÅrsak = PeriodeÅrsak.UDEFINERT;

    public BeregningsgrunnlagPeriodeÅrsak(BeregningsgrunnlagPeriodeÅrsak beregningsgrunnlagPeriodeÅrsak) {
        this.periodeÅrsak = beregningsgrunnlagPeriodeÅrsak.periodeÅrsak;
    }

    public BeregningsgrunnlagPeriodeÅrsak() {
    }

    public Long getId() {
        return id;
    }

    public BeregningsgrunnlagPeriode getBeregningsgrunnlagPeriode() {
        return beregningsgrunnlagPeriode;
    }

    public PeriodeÅrsak getPeriodeÅrsak() {
        return periodeÅrsak;
    }


    public void setBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
        this.beregningsgrunnlagPeriode = beregningsgrunnlagPeriode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(beregningsgrunnlagPeriode, periodeÅrsak);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BeregningsgrunnlagPeriodeÅrsak other)) {
            return false;
        }
        return Objects.equals(this.getBeregningsgrunnlagPeriode(), other.getBeregningsgrunnlagPeriode())
                && Objects.equals(this.getPeriodeÅrsak(), other.getPeriodeÅrsak());
    }

    public static class Builder {
        private BeregningsgrunnlagPeriodeÅrsak beregningsgrunnlagPeriodeÅrsakMal;

        public Builder() {
            beregningsgrunnlagPeriodeÅrsakMal = new BeregningsgrunnlagPeriodeÅrsak();
        }

        public Builder medPeriodeÅrsak(PeriodeÅrsak periodeÅrsak) {
            beregningsgrunnlagPeriodeÅrsakMal.periodeÅrsak = periodeÅrsak;
            return this;
        }

        public BeregningsgrunnlagPeriodeÅrsak build(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
            beregningsgrunnlagPeriode.addBeregningsgrunnlagPeriodeÅrsak(beregningsgrunnlagPeriodeÅrsakMal);
            return beregningsgrunnlagPeriodeÅrsakMal;
        }
    }
}
