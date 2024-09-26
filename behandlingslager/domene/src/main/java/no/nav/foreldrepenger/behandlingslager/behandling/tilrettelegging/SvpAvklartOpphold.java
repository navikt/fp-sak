package no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseCreateableEntitet;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;


@Entity
@Table(name = "SVP_AVKLART_OPPHOLD")
public class SvpAvklartOpphold extends BaseCreateableEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SVP_AVKLART_OPPHOLD")
    private Long id;

    @Embedded
    private DatoIntervallEntitet oppholdPeriode;

    @Column(name = "svp_opphold_arsak", nullable = false)
    private SvpOppholdÅrsak svpOppholdÅrsak;

    @Column(name = "svp_opphold_arsak_kilde")
    private SvpOppholdKilde svpOppholdKilde;

    public SvpAvklartOpphold() {
        //for hibernate
    }

    public Long getId() {
        return id;
    }

    public LocalDate getFom() {
        return oppholdPeriode.getFomDato();
    }

    public LocalDate getTom() {
        return oppholdPeriode.getTomDato();
    }

    public SvpOppholdÅrsak getOppholdÅrsak() {
        return svpOppholdÅrsak;
    }

    public SvpOppholdKilde getKilde() {
        return svpOppholdKilde == null ? SvpOppholdKilde.REGISTRERT_AV_SAKSBEHANDLER : svpOppholdKilde;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (SvpAvklartOpphold) o;
        return Objects.equals(oppholdPeriode.getFomDato(), that.oppholdPeriode.getFomDato()) &&
            Objects.equals(oppholdPeriode.getTomDato(), that.oppholdPeriode.getTomDato()) &&
            Objects.equals(svpOppholdÅrsak, that.svpOppholdÅrsak);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oppholdPeriode.getFomDato(), oppholdPeriode.getTomDato(), svpOppholdÅrsak);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            (id != null ? "id=" + id + ", " : "")
            + "fom=" + oppholdPeriode.getFomDato() + ", "
            + "tom=" + oppholdPeriode.getTomDato() + ", "
            + "svpOppholdÅrsak=" + svpOppholdÅrsak + ", "
            + ">";
    }

    public static class Builder {
    private final SvpAvklartOpphold kladd;

    private Builder(SvpAvklartOpphold kladd) {
        this.kladd = kladd;
    }

    private Builder() {
        kladd = new SvpAvklartOpphold();
    }

    public static Builder nytt() {
        return new Builder(new SvpAvklartOpphold());
    }

    public static Builder fraEksisterende(SvpAvklartOpphold eksisterende) {
        return new Builder()
            .medOppholdPeriode(eksisterende.oppholdPeriode.getFomDato(), eksisterende.oppholdPeriode.getTomDato())
            .medOppholdÅrsak(eksisterende.svpOppholdÅrsak);
    }

    public SvpAvklartOpphold.Builder medOppholdPeriode(LocalDate fom, LocalDate tom) {
            kladd.oppholdPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
            return this;
        }

        public SvpAvklartOpphold.Builder medOppholdÅrsak(SvpOppholdÅrsak svpOppholdÅrsak) {
            kladd.svpOppholdÅrsak = Objects.requireNonNull(svpOppholdÅrsak);
            return this;
        }

        public SvpAvklartOpphold.Builder medKilde(SvpOppholdKilde kilde) {
            kladd.svpOppholdKilde = Objects.requireNonNull(kilde);
            return this;
        }

        public SvpAvklartOpphold build() {
            Objects.requireNonNull(this.kladd.svpOppholdÅrsak, "Utviklerfeil:oppholdsårsak  skal være satt");
            if (kladd.svpOppholdKilde == null) {
                kladd.svpOppholdKilde = SvpOppholdKilde.REGISTRERT_AV_SAKSBEHANDLER;
            }
            return kladd;
        }
    }

}
