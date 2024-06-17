package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

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

    @Convert(converter = BeregningSatsType.KodeverdiConverter.class)
    @Column(name = "sats_type", nullable = false)
    private BeregningSatsType satsType = BeregningSatsType.UDEFINERT;

    @SuppressWarnings("unused")
    BeregningSats() {
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
        if (tom == null || !tom.isAfter(periode.getFomDato())) {
            throw new IllegalArgumentException("Feil tomdato " + tom);
        }
        periode = DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato(), tom);
    }

    public void setVerdi(long nySatsVerdi) {
        verdi = nySatsVerdi;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BeregningSats annen)) {
            return false;
        }

        return Objects.equals(this.getSatsType(), annen.getSatsType()) && Objects.equals(this.periode, annen.periode) && Objects.equals(this.verdi,
            annen.verdi);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSatsType(), periode, verdi);
    }

    private void setSatsType(BeregningSatsType satsType) {
        this.satsType = satsType == null ? BeregningSatsType.UDEFINERT : satsType;
    }
}
