package no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging;

import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;


@Entity
@Table(name = "SVP_AVKLART_OPPHOLD")
public class SvpAvklartOpphold extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SVP_AVKLART_OPPHOLD")
    private Long id;

    @Column(name = "fom", nullable = false)
    private LocalDate fom;

    @Column(name = "tom", nullable = false)
    private LocalDate tom;

    @Column(name = "svp_opphold_arsak", nullable = false)
    private SvpOppholdÅrsak svpOppholdÅrsak;

    public SvpAvklartOpphold() {
        //for hibernate
    }

    public Long getId() {
        return id;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public SvpOppholdÅrsak getOppholdÅrsak() {
        return svpOppholdÅrsak;
    }
    @Override
    public String getIndexKey() {
        return IndexKey.createKey(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (SvpAvklartOpphold) o;
        return Objects.equals(fom, that.fom) &&
            Objects.equals(tom, that.tom) &&
            Objects.equals(svpOppholdÅrsak, that.svpOppholdÅrsak);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fom, tom, svpOppholdÅrsak);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            (id != null ? "id=" + id + ", " : "")
            + "fom=" + fom + ", "
            + "tom=" + tom + ", "
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
            .medFom(eksisterende.fom)
            .medTom(eksisterende.tom)
            .medOppholdÅrsak(eksisterende.svpOppholdÅrsak);
    }

    public SvpAvklartOpphold.Builder medFom(LocalDate fom) {
            kladd.fom = Objects.requireNonNull(fom);
            return this;
        }

        public SvpAvklartOpphold.Builder medTom(LocalDate tom) {
            kladd.tom = Objects.requireNonNull(tom);
            return this;
        }

        public SvpAvklartOpphold.Builder medOppholdÅrsak(SvpOppholdÅrsak svpOppholdÅrsak) {
            kladd.svpOppholdÅrsak = Objects.requireNonNull(svpOppholdÅrsak);
            return this;
        }

        public SvpAvklartOpphold build() {
            Objects.requireNonNull(this.kladd.fom,  "Utviklerfeil:fra dato skal være satt");
            Objects.requireNonNull(this.kladd.tom, "Utviklerfeil:tom dato skal være satt");
            Objects.requireNonNull(this.kladd.svpOppholdÅrsak, "Utviklerfeil:oppholdsårsak  skal være satt");
            return kladd;
        }
    }
}
