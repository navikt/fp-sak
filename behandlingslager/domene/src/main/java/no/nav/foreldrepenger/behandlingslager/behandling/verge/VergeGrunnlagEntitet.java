package no.nav.foreldrepenger.behandlingslager.behandling.verge;

import jakarta.persistence.*;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

import java.util.Objects;

@Entity(name = "VergeGrunnlag")
@Table(name = "GR_VERGE")
public class VergeGrunnlagEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_VERGE")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @OneToOne
    @JoinColumn(name = "verge_id", updatable = false, unique = true)
    private VergeEntitet verge;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public VergeGrunnlagEntitet() {
        // defualt tom entitet
    }

    VergeGrunnlagEntitet(Long behandlingId, VergeEntitet verge) {
        this.behandlingId = behandlingId;
        this.verge = verge;
    }

    VergeEntitet getVerge() {
        return verge;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof VergeGrunnlagEntitet that)) {
            return false;
        }

        return Objects.equals(this.behandlingId, that.behandlingId)
            && Objects.equals(this.verge, that.verge);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.behandlingId, this.verge);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<id=" + this.id
            + ", verge=" + this.verge
            + ">";
    }

    VergeAggregat tilAggregat() {
        return new VergeAggregat(this.verge);
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    void setBehandling(Long behandlingId) {
        this.behandlingId = behandlingId;
    }
}
