package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity
@Table(name = "UTTAK_RESULTAT_PERIODE_SOKNAD")
public class UttakResultatPeriodeSøknadEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UTTAK_RES_PER_SOKNAD")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "periode_type", updatable = false, nullable = false)
    @Convert(converter = UttakPeriodeType.KodeverdiConverter.class)
    private UttakPeriodeType uttakPeriodeType;

    @Column(name = "gradering_arbeidstidsprosent")
    private BigDecimal graderingArbeidsprosent;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "samtidig_uttak", nullable = false)
    private boolean samtidigUttak;

    @Column(name = "samtidig_uttaksprosent")
    private BigDecimal samtidigUttaksprosent;

    @Column(name = "mottatt_dato")
    private LocalDate mottattDato;

    @Column(name = "MORS_AKTIVITET", updatable = false, nullable = false)
    @Convert(converter = MorsAktivitet.KodeverdiConverter.class)
    private MorsAktivitet morsAktivitet = MorsAktivitet.UDEFINERT;

    public Long getId() {
        return id;
    }

    public UttakPeriodeType getUttakPeriodeType() {
        return uttakPeriodeType;
    }

    public BigDecimal getGraderingArbeidsprosent() {
        return graderingArbeidsprosent;
    }

    public boolean isSamtidigUttak() {
        return samtidigUttak;
    }

    public BigDecimal getSamtidigUttaksprosent() {
        return isSamtidigUttak() ? samtidigUttaksprosent : null;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public MorsAktivitet getMorsAktivitet() {
        return morsAktivitet;
    }


    @Override
    public String toString() {
        return "UttakResultatPeriodeSøknadEntitet{" +
            "id=" + id +
            ", uttakPeriodeType=" + uttakPeriodeType.getKode() +
            ", graderingArbeidsprosent=" + graderingArbeidsprosent +
            ", samtidigUttak=" + samtidigUttak +
            ", samtidigUttaksprosent=" + samtidigUttaksprosent +
            ", mottattDato=" + mottattDato +
            ", morsAktivitet=" + morsAktivitet +
            '}';
    }

    public static class Builder {

        private UttakResultatPeriodeSøknadEntitet kladd = new UttakResultatPeriodeSøknadEntitet();

        public Builder medUttakPeriodeType(UttakPeriodeType uttakPeriodeType) {
            kladd.uttakPeriodeType = uttakPeriodeType;
            return this;
        }

        public Builder medGraderingArbeidsprosent(BigDecimal graderingArbeidsprosent) {
            kladd.graderingArbeidsprosent = graderingArbeidsprosent;
            return this;
        }

        public Builder medSamtidigUttak(boolean samtidigUttak) {
            kladd.samtidigUttak = samtidigUttak;
            return this;
        }

        public Builder medSamtidigUttaksprosent(BigDecimal samtidigUttaksprosent) {
            kladd.samtidigUttaksprosent = samtidigUttaksprosent;
            return this;
        }

        public Builder medMottattDato(LocalDate mottattDato) {
            kladd.mottattDato = mottattDato;
            return this;
        }

        public Builder medMorsAktivitet(MorsAktivitet morsAktivitet) {
            kladd.morsAktivitet = morsAktivitet;
            return this;
        }

        public UttakResultatPeriodeSøknadEntitet build() {
            Objects.requireNonNull(kladd.uttakPeriodeType, "uttakPeriodeType");
            return kladd;
        }
    }
}
