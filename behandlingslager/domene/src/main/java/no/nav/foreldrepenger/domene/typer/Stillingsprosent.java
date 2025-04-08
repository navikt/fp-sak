package no.nav.foreldrepenger.domene.typer;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.diff.TraverseValue;

/**
 * Stillingsprosent slik det er oppgitt i arbeidsavtalen
 */
@Embeddable
public class Stillingsprosent implements Serializable, IndexKey, TraverseValue, Comparable<Stillingsprosent> {
    private static final Logger LOG = LoggerFactory.getLogger(Stillingsprosent.class);

    private static final BigDecimal MAX_VERDI = new BigDecimal(500);

    public static final Stillingsprosent ZERO = new Stillingsprosent(0);

    public static final Stillingsprosent HUNDRED = new Stillingsprosent(100);

    @Column(name = "verdi", scale = 2, nullable = false)
    @ChangeTracked
    private BigDecimal verdi;

    protected Stillingsprosent() {
        // for hibernate
    }

    public Stillingsprosent(BigDecimal verdi) {
        this.verdi = verdi == null ? null : fiksNegativOgMax(verdi);
        validerRange(this.verdi);
    }

    // Beleilig å kunne opprette gjennom int
    public Stillingsprosent(Integer verdi) {
        this(new BigDecimal(verdi));
    }

    // Beleilig å kunne opprette gjennom string
    public Stillingsprosent(String verdi) {
        this(new BigDecimal(verdi));
    }

    @Override
    public String getIndexKey() {
        return Optional.ofNullable(skalertVerdi()).map(BigDecimal::toString).orElse("ingenVerdi");
    }

    public BigDecimal getVerdi() {
        return verdi;
    }

    private BigDecimal skalertVerdi() {
        return verdi != null ? verdi.setScale(2, RoundingMode.HALF_EVEN) : null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        var other = (Stillingsprosent) obj;
        var thisSkalert = this.skalertVerdi();
        var otherSkalert = other.skalertVerdi();
        return Objects.equals(thisSkalert, otherSkalert) || thisSkalert != null && otherSkalert != null && thisSkalert.compareTo(otherSkalert) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(skalertVerdi());
    }

    @Override
    public String toString() {
        return "Stillingsprosent{" +
            "verdi=" + verdi +
            ", skalertVerdi=" + skalertVerdi() +
            '}';
    }

    private static void validerRange(BigDecimal verdi) {
        if (verdi == null) {
            return;
        }
        if (verdi.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Prosent må være >= 0");
        }
    }

    private BigDecimal fiksNegativOgMax(BigDecimal verdi) {
        if (null != verdi && verdi.compareTo(BigDecimal.ZERO) < 0) {
            LOG.info("[IAY] Prosent (yrkesaktivitet, permisjon) kan ikke være mindre enn 0, absolutt verdi brukes isteden. Verdi fra Aa-reg: {}", verdi);
            verdi = verdi.abs();
        }
        if (null != verdi && verdi.compareTo(MAX_VERDI) > 0) {
            LOG.info("[IAY] Prosent (yrkesaktivitet, permisjon) kan ikke være mer enn 500, avkortet verdi brukes isteden. Verdi fra Aa-reg: {}", verdi);
            verdi = MAX_VERDI;
        }
        return verdi;
    }

    public boolean erNulltall() {
        return verdi != null && verdi.intValue() == 0;
    }

    public Stillingsprosent add(Stillingsprosent stillingsprosent) {
        return new Stillingsprosent(this.verdi.add(stillingsprosent.verdi));
    }

    @Override
    public int compareTo(Stillingsprosent o) {
        return this.verdi.compareTo(o.verdi);
    }

    public boolean merEllerLik(Stillingsprosent that) {
        return this.compareTo(that) >= 0;
    }

    public boolean merEnn0() {
        return this.compareTo(ZERO) > 0;
    }
}
