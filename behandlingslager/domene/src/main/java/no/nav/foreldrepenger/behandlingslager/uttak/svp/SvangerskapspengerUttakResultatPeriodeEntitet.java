package no.nav.foreldrepenger.behandlingslager.uttak.svp;

import static no.nav.foreldrepenger.behandlingslager.uttak.svp.PeriodeIkkeOppfyltÅrsak._8306;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

@Entity
@Table(name = "SVP_UTTAK_PERIODE")
public class SvangerskapspengerUttakResultatPeriodeEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SVP_UTTAK_PERIODE")
    private Long id;

    @Embedded
    @AttributeOverride(name = "verdi", column = @Column(name = "utbetalingsgrad", nullable = false))
    private Utbetalingsgrad utbetalingsgrad;

    @Convert(converter = PeriodeResultatType.KodeverdiConverter.class)
    @Column(name = "periode_resultat_type", nullable = false)
    private PeriodeResultatType periodeResultatType;

    @Convert(converter = PeriodeIkkeOppfyltÅrsak.KodeverdiConverter.class)
    @Column(name = "periode_resultat_aarsak", nullable = false)
    private PeriodeIkkeOppfyltÅrsak periodeIkkeOppfyltÅrsak = PeriodeIkkeOppfyltÅrsak.INGEN;

    @Embedded
    @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false))
    @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    private DatoIntervallEntitet tidsperiode;

    @Lob
    @Column(name = "regel_input", nullable = false, updatable = false)
    private String regelInput;

    @Lob
    @Column(name = "regel_evaluering", nullable = false, updatable = false)
    private String regelEvaluering;

    @Column(name = "regel_versjon")
    private String regelVersjon;

    @Override
    public String toString() {
        return "SvangerskapspengerUttakResultatPeriodeEntitet{" +
            "tidsperiode=" + tidsperiode +
            ", utbetalingsgrad=" + utbetalingsgrad +
            ", periodeResultatType=" + periodeResultatType +
            ", periodeIkkeOppfyltÅrsak=" + periodeIkkeOppfyltÅrsak +
            '}';
    }

    public Long getId() {
        return id;
    }

    public LocalDate getFom() {
        return tidsperiode.getFomDato();
    }

    public LocalDate getTom() {
        return tidsperiode.getTomDato();
    }

    public DatoIntervallEntitet getTidsperiode() {
        return tidsperiode;
    }

    public Utbetalingsgrad getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public PeriodeResultatType getPeriodeResultatType() {
        return periodeResultatType;
    }

    public PeriodeIkkeOppfyltÅrsak getPeriodeIkkeOppfyltÅrsak() {
        return periodeIkkeOppfyltÅrsak;
    }

    public String getRegelInput() {
        return regelInput;
    }

    public String getRegelEvaluering() {
        return regelEvaluering;
    }

    public String getRegelVersjon() {
        return regelVersjon;
    }

    public boolean isInnvilget() {
        return Objects.equals(getPeriodeResultatType(), PeriodeResultatType.INNVILGET);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        var that = (SvangerskapspengerUttakResultatPeriodeEntitet) o;
        return Objects.equals(tidsperiode, that.tidsperiode) &&
            Objects.equals(utbetalingsgrad, that.utbetalingsgrad) &&
            Objects.equals(periodeResultatType, that.periodeResultatType) &&
            Objects.equals(periodeIkkeOppfyltÅrsak, that.periodeIkkeOppfyltÅrsak);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tidsperiode, utbetalingsgrad, periodeResultatType, periodeIkkeOppfyltÅrsak);
    }

    public boolean harAvslagPgaMedlemskap() {
        return _8306.equals(getPeriodeIkkeOppfyltÅrsak());
    }

    public static class Builder {
        private SvangerskapspengerUttakResultatPeriodeEntitet kladd;

        public Builder(LocalDate fom, LocalDate tom) {
            this.kladd = new SvangerskapspengerUttakResultatPeriodeEntitet();
            this.kladd.tidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        }

        public Builder medUtbetalingsgrad(Utbetalingsgrad utbetalingsgrad) {
            kladd.utbetalingsgrad = utbetalingsgrad;
            return this;
        }

        public Builder medPeriodeResultatType(PeriodeResultatType periodeResultatType) {
            kladd.periodeResultatType = periodeResultatType;
            return this;
        }

        public Builder medPeriodeIkkeOppfyltÅrsak(PeriodeIkkeOppfyltÅrsak periodeIkkeOppfyltÅrsak) {
            kladd.periodeIkkeOppfyltÅrsak = periodeIkkeOppfyltÅrsak;
            return this;
        }

        public Builder medRegelInput(String regelInput) {
            kladd.regelInput = regelInput;
            return this;
        }

        public Builder medRegelEvaluering(String regelEvaluering) {
            kladd.regelEvaluering = regelEvaluering;
            return this;
        }

        public Builder medRegelVersjon(String regelVersjon) {
            kladd.regelVersjon = regelVersjon;
            return this;
        }

        public SvangerskapspengerUttakResultatPeriodeEntitet build() {
            Objects.requireNonNull(kladd.tidsperiode, "tidsperiode");
            Objects.requireNonNull(kladd.periodeResultatType, "periodeResultatType");

            return kladd;
        }

    }
}
