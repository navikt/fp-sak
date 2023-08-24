package no.nav.foreldrepenger.behandlingslager.behandling.totrinn;

import jakarta.persistence.*;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;

import java.util.Objects;


/**
 * Tilbakemelding fra beslutter for å be saksbehandler vurdere et aksjonspunkt på nytt.
 */
@Entity(name = "VurderÅrsakTotrinnsvurdering")
@Table(name = "VURDER_AARSAK_TTVURDERING")
public class VurderÅrsakTotrinnsvurdering extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_VURDER_AARSAK_TTVURDERING")
    private Long id;

    @Convert(converter = VurderÅrsak.KodeverdiConverter.class)
    @Column(name="aarsak_type", nullable=false, updatable=false)
    private VurderÅrsak årsaksType;

    @ManyToOne(optional = false)
    @JoinColumn(name = "totrinnsvurdering_id", nullable = false, updatable=false)
    private Totrinnsvurdering totrinnsvurdering;

    VurderÅrsakTotrinnsvurdering() {
        // for Hibernate
    }

    public VurderÅrsakTotrinnsvurdering(VurderÅrsak type, Totrinnsvurdering totrinnsvurdering) {
        this.totrinnsvurdering = totrinnsvurdering;
        this.årsaksType = type;
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(årsaksType);
    }

    public VurderÅrsak getÅrsaksType() {
        return årsaksType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VurderÅrsakTotrinnsvurdering that)) {
            return false;
        }
        return Objects.equals(getÅrsaksType(), that.getÅrsaksType()) &&
            Objects.equals(totrinnsvurdering, that.totrinnsvurdering);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getÅrsaksType(), totrinnsvurdering);
    }
}
