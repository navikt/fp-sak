package no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseCreateableEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

@Entity(name= "AktivitetskravArbeidPeriodeEntitet")
@Table(name = "AKTIVITETSKRAV_ARBEID_PERIODE")
public class AktivitetskravArbeidPeriodeEntitet extends BaseCreateableEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_AKTIVITETSKRAV_ARBEID_PERIODE")
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "aktivitetskrav_arbeid_perioder_id", nullable = false, updatable = false, unique = true)
    private AktivitetskravArbeidPerioderEntitet aktivitetskravArbeidPerioder;

    @ChangeTracked
    private DatoIntervallEntitet periode;

    @ChangeTracked
    @Column(name = "org_nr")
    private String organisasjonsnummer;

    @ChangeTracked
    @Column(name = "sum_stillingsprosent")
    private BigDecimal sumStillingsprosent;


    @ChangeTracked
    @Column(name = "sum_permisjonsprosent")
    private BigDecimal sumPermisjonsprosent;
    public AktivitetskravArbeidPeriodeEntitet() {
        //CDI
    }

    public AktivitetskravArbeidPerioderEntitet getAktivitetskravArbeidPerioder() {
        return aktivitetskravArbeidPerioder;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    public BigDecimal getSumStillingsprosent() {
        return sumStillingsprosent;
    }

    public BigDecimal getSumPermisjonsprosent() {
        return sumPermisjonsprosent;
    }

    public void setAktivitetskravArbeidPerioder(AktivitetskravArbeidPerioderEntitet aktivitetskravArbeidPerioder) {
        this.aktivitetskravArbeidPerioder = aktivitetskravArbeidPerioder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AktivitetskravArbeidPeriodeEntitet that = (AktivitetskravArbeidPeriodeEntitet) o;
        return Objects.equals(periode, that.periode) && Objects.equals(organisasjonsnummer, that.organisasjonsnummer) && Objects.equals(
            sumStillingsprosent, that.sumStillingsprosent) && Objects.equals(sumPermisjonsprosent, that.sumPermisjonsprosent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, organisasjonsnummer, sumStillingsprosent, sumPermisjonsprosent);
    }

    public static class Builder{
        AktivitetskravArbeidPeriodeEntitet kladd;

        public Builder() {
            this.kladd = new AktivitetskravArbeidPeriodeEntitet();
        }

        public Builder medPeriode(LocalDate fra, LocalDate til) {
            this.kladd.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fra, til);
            return this;
        }

        public Builder medOrganisasjonsnummer(String organisasjonsnummer) {
            this.kladd.organisasjonsnummer = organisasjonsnummer;
            return this;
        }

        public Builder medSumStillingsprosent(BigDecimal stillingsprosent) {
            this.kladd.sumStillingsprosent = stillingsprosent;
            return this;
        }

        public Builder medSumPermisjonsprosent(BigDecimal permisjonsprosent) {
            this.kladd.sumPermisjonsprosent = permisjonsprosent;
            return this;
        }

        public AktivitetskravArbeidPeriodeEntitet build() {
            return this.kladd;
        }
    }
}
