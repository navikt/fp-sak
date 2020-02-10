package no.nav.foreldrepenger.domene.typer;

import static no.nav.vedtak.util.Objects.check;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.diff.TraverseValue;

/**
 * Stillingsprosent slik det er oppgitt i arbeidsavtalen
 */
@Embeddable
public class Stillingsprosent implements Serializable, IndexKey, TraverseValue {
    private static final Logger log = LoggerFactory.getLogger(Stillingsprosent.class);

    private static final RoundingMode AVRUNDINGSMODUS = RoundingMode.HALF_EVEN;

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
        return skalertVerdi().toString();
    }

    public BigDecimal getVerdi() {
        return verdi;
    }

    private BigDecimal skalertVerdi() {
        return verdi.setScale(2, AVRUNDINGSMODUS);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        Stillingsprosent other = (Stillingsprosent) obj;
        return Objects.equals(skalertVerdi(), other.skalertVerdi());
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
        check(verdi.compareTo(BigDecimal.ZERO) >= 0, "Prosent må være >= 0"); //$NON-NLS-1$
    }

    private BigDecimal fiksNegativOgMax(BigDecimal verdi) {
        if (null != verdi && verdi.compareTo(BigDecimal.ZERO) < 0) {
            log.info("[IAY] Prosent (yrkesaktivitet, permisjon) kan ikke være mindre enn 0, absolutt verdi brukes isteden. Verdi fra AA-reg: {}", verdi);
            verdi = verdi.abs();
        }
        if (null != verdi && verdi.compareTo(MAX_VERDI) > 0) {
            log.info("[IAY] Prosent (yrkesaktivitet, permisjon) kan ikke være mer enn 500, avkortet verdi brukes isteden. Verdi fra AA-reg: {}", verdi);
            verdi = MAX_VERDI;
        }
        return verdi;
    }

    public boolean erNulltall() {
        return verdi != null && verdi.intValue() == 0;
    }
}
