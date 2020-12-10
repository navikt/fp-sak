package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

@Entity
@Table(name = "YF_AKTIVITETSKRAV_PERIODE")
public class AktivitetskravPeriodeEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_YF_AKTIVITETSKRAV_PERIODE")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "yf_aktivitetskrav_perioder_id", nullable = false, updatable = false)
    private AktivitetskravPerioderEntitet perioder;

    @Column(name = "avklaring")
    private KontrollerAktivitetskravAvklaring avklaring;

    @Column(name = "begrunnelse")
    private String begrunnelse;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet tidsperiode;


    public AktivitetskravPeriodeEntitet() {
        // For hibernate
    }

    public AktivitetskravPeriodeEntitet(LocalDate fom,
                                        LocalDate tom,
                                        KontrollerAktivitetskravAvklaring avklaring,
                                        String begrunnelse) {
        this.tidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        this.avklaring = avklaring;
        this.begrunnelse = begrunnelse;
    }

    public AktivitetskravPeriodeEntitet(AktivitetskravPeriodeEntitet periode) {
        this(periode.tidsperiode.getFomDato(), periode.tidsperiode.getTomDato(),
            periode.avklaring, periode.begrunnelse);
    }

    void setPerioder(AktivitetskravPerioderEntitet perioder) {
        this.perioder = perioder;
    }

    public KontrollerAktivitetskravAvklaring getAvklaring() {
        return avklaring;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public DatoIntervallEntitet getTidsperiode() {
        return tidsperiode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AktivitetskravPeriodeEntitet that = (AktivitetskravPeriodeEntitet) o;
        return Objects.equals(id, that.id) && avklaring == that.avklaring && Objects.equals(tidsperiode,
            that.tidsperiode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, avklaring, tidsperiode);
    }

    @Override
    public String toString() {
        return "AktivitetskravPeriodeEntitet{" + "avklaring=" + avklaring + ", tidsperiode=" + tidsperiode + '}';
    }
}
