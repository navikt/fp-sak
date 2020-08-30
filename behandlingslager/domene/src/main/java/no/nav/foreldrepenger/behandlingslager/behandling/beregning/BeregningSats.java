package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

@Entity(name = "BeregningSats")
@Table(name = "BR_SATS")
public class BeregningSats extends BaseEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BR_SATS")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "verdi", nullable = false)
    private long verdi;

    @Embedded
    DatoIntervallEntitet periode;

    @Convert(converter= BeregningSatsType.KodeverdiConverter.class)
    @Column(name="sats_type", nullable = false)
    private BeregningSatsType satsType = BeregningSatsType.UDEFINERT;

    @SuppressWarnings("unused")
    private BeregningSats() {
        // For hibernate
    }

    public BeregningSats(BeregningSatsType satsType, DatoIntervallEntitet periode, Long verdi) {
        Objects.requireNonNull(satsType, "satsType må være satt");
        Objects.requireNonNull(periode, "periode må være satt");
        Objects.requireNonNull(verdi, "verdi  må være satt");
        this.setSatsType(satsType);
        this.periode = periode;
        this.verdi = verdi;
    }

    public long getVerdi() {
        return verdi;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public BeregningSatsType getSatsType() {
        return Objects.equals(BeregningSatsType.UDEFINERT, satsType) ? null : satsType;
    }

    public void setTomDato(LocalDate tom) {
        if (tom == null || !tom.isAfter(periode.getFomDato()))
            throw new IllegalArgumentException("Feil tomdato " + tom);
        periode = DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato(), tom);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BeregningSats)) {
            return false;
        }
        BeregningSats annen = (BeregningSats) o;

        return Objects.equals(this.getSatsType(), annen.getSatsType())
            && Objects.equals(this.periode, annen.periode)
            && Objects.equals(this.verdi, annen.verdi);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSatsType(), periode, verdi);
    }

    private void setSatsType(BeregningSatsType satsType) {
        this.satsType = satsType == null ? BeregningSatsType.UDEFINERT : satsType;
    }
}
