package no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav;

import java.math.BigDecimal;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseCreateableEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

@Entity(name = "AktivitetskravArbeidPeriodeEntitet")
@Table(name = "AKTIVITETSKRAV_ARBEID_PERIODE")
public class AktivitetskravArbeidPeriodeEntitet extends BaseCreateableEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_AKTIVITETSKRAV_ARBEID_PERIODE")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "aktivitetskrav_arbeid_perioder_id", nullable = false, updatable = false, unique = true)
    private AktivitetskravArbeidPerioderEntitet aktivitetskravArbeidPerioder;

    @Embedded
    @ChangeTracked
    private DatoIntervallEntitet periode;

    @Embedded
    @ChangeTracked
    private OrgNummer orgNummer;

    @Embedded
    @ChangeTracked
    @AttributeOverride(name = "verdi", column = @Column(name = "sum_stillingsprosent"))
    private Stillingsprosent sumStillingsprosent;

    @Embedded
    @ChangeTracked
    @AttributeOverride(name = "verdi", column = @Column(name = "sum_permisjonsprosent"))
    private Stillingsprosent sumPermisjonsprosent;

    @ChangeTracked
    @Convert(converter = AktivitetskravPermisjonType.KodeverdiConverter.class)
    @Column(name = "permisjon_type", nullable = false, updatable = false)
    private AktivitetskravPermisjonType permisjonsbeskrivelseType = AktivitetskravPermisjonType.UDEFINERT;

    public AktivitetskravArbeidPeriodeEntitet() {
        //CDI
    }

    public AktivitetskravArbeidPerioderEntitet getAktivitetskravArbeidPerioder() {
        return aktivitetskravArbeidPerioder;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public OrgNummer getOrgNummer() {
        return orgNummer;
    }

    public Stillingsprosent getSumStillingsprosent() {
        return sumStillingsprosent;
    }

    public Stillingsprosent getSumPermisjonsprosent() {
        return sumPermisjonsprosent;
    }

    public AktivitetskravPermisjonType getPermisjonsbeskrivelseType() {
        return permisjonsbeskrivelseType;
    }

    public void setAktivitetskravArbeidPerioder(AktivitetskravArbeidPerioderEntitet aktivitetskravArbeidPerioder) {
        this.aktivitetskravArbeidPerioder = aktivitetskravArbeidPerioder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof AktivitetskravArbeidPeriodeEntitet that))
            return false;
        return Objects.equals(periode, that.periode) && Objects.equals(orgNummer, that.orgNummer) && Objects.equals(sumStillingsprosent,
            that.sumStillingsprosent) && Objects.equals(sumPermisjonsprosent, that.sumPermisjonsprosent)
            && permisjonsbeskrivelseType == that.permisjonsbeskrivelseType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, orgNummer, sumStillingsprosent, sumPermisjonsprosent, permisjonsbeskrivelseType);
    }

    public static class Builder {
        AktivitetskravArbeidPeriodeEntitet kladd;

        public Builder() {
            this.kladd = new AktivitetskravArbeidPeriodeEntitet();
        }

        public Builder medPeriode(LocalDate fra, LocalDate til) {
            this.kladd.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fra, til);
            return this;
        }

        public Builder medOrgNummer(String orgNummer) {
            this.kladd.orgNummer = new OrgNummer(orgNummer);
            return this;
        }

        public Builder medSumStillingsprosent(BigDecimal stillingsprosent) {
            this.kladd.sumStillingsprosent = new Stillingsprosent(stillingsprosent);
            return this;
        }


        public Builder medPermisjon(BigDecimal sumPermisjonsprosent, AktivitetskravPermisjonType type) {
            this.kladd.sumPermisjonsprosent = new Stillingsprosent(sumPermisjonsprosent);
            this.kladd.permisjonsbeskrivelseType = type == null ? AktivitetskravPermisjonType.UDEFINERT : type;
            return this;
        }

        public AktivitetskravArbeidPeriodeEntitet build() {
            return this.kladd;
        }
    }
}
