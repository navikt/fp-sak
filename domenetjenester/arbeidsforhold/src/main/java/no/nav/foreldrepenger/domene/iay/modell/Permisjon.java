package no.nav.foreldrepenger.domene.iay.modell;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

public class Permisjon extends BaseEntitet implements IndexKey {

    private Yrkesaktivitet yrkesaktivitet;

    private PermisjonsbeskrivelseType permisjonsbeskrivelseType;

    private DatoIntervallEntitet periode;

    @ChangeTracked
    private Stillingsprosent prosentsats;

    public Permisjon() {
        // hibernate
    }

    /**
     * Deep copy ctor
     */
    Permisjon(Permisjon permisjon) {
        this.permisjonsbeskrivelseType = permisjon.getPermisjonsbeskrivelseType();
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(permisjon.getFraOgMed(), permisjon.getTilOgMed());
        this.prosentsats = permisjon.getProsentsats();
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(periode, getPermisjonsbeskrivelseType());
    }

    /**
     * Beskrivelse av permisjonen
     *
     * @return {@link PermisjonsbeskrivelseType}
     */
    public PermisjonsbeskrivelseType getPermisjonsbeskrivelseType() {
        return permisjonsbeskrivelseType;
    }

    void setPermisjonsbeskrivelseType(PermisjonsbeskrivelseType permisjonsbeskrivelseType) {
        this.permisjonsbeskrivelseType = permisjonsbeskrivelseType;
    }

    void setPeriode(LocalDate fraOgMed, LocalDate tilOgMed) {
        if (tilOgMed != null) {
            this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);
        } else {
            this.periode = DatoIntervallEntitet.fraOgMed(fraOgMed);
        }
    }

    public LocalDate getFraOgMed() {
        return periode.getFomDato();
    }

    public LocalDate getTilOgMed() {
        return periode.getTomDato();
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    /**
     * Prosentsats som aktøren er permitert fra arbeidet
     *
     * @return prosentsats
     */
    public Stillingsprosent getProsentsats() {
        return prosentsats;
    }

    void setProsentsats(Stillingsprosent prosentsats) {
        this.prosentsats = prosentsats;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof Permisjon)) {
            return false;
        }
        Permisjon other = (Permisjon) obj;
        return Objects.equals(this.getPermisjonsbeskrivelseType(), other.getPermisjonsbeskrivelseType())
                && Objects.equals(this.getFraOgMed(), other.getFraOgMed())
                && Objects.equals(this.getTilOgMed(), other.getTilOgMed());
    }

    @Override
    public int hashCode() {
        return Objects.hash(permisjonsbeskrivelseType, periode);
    }

    @Override
    public String toString() {
        return "Adopsjon{" + //$NON-NLS-1$
                "permisjonsbeskrivelseType=" + permisjonsbeskrivelseType + //$NON-NLS-1$
                ", fraOgMed=" + periode.getFomDato() + //$NON-NLS-1$
                ", tilOgMed=" + periode.getTomDato() + //$NON-NLS-1$
                ", v=" + prosentsats + //$NON-NLS-1$
                '}';
    }

    public Yrkesaktivitet getYrkesaktivitet() {
        return yrkesaktivitet;
    }

    void setYrkesaktivitet(Yrkesaktivitet yrkesaktivitet) {
        this.yrkesaktivitet = yrkesaktivitet;
    }

    boolean hasValues() {
        return (permisjonsbeskrivelseType != null) || (periode.getFomDato() != null) || (prosentsats != null);
    }
}
