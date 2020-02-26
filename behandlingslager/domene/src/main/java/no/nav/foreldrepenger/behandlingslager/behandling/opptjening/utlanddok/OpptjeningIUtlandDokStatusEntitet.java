package no.nav.foreldrepenger.behandlingslager.behandling.opptjening.utlanddok;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity
@Table(name = "GR_OPPTJ_UTLAND_DOK_VALG")
public class OpptjeningIUtlandDokStatusEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_OPPTJ_UTLAND_DOK_VALG")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Convert(converter = OpptjeningIUtlandDokStatus.KodeverdiConverter.class)
    @Column(name = "dok_status", nullable = false, updatable = false)
    private OpptjeningIUtlandDokStatus dokStatus;

    public OpptjeningIUtlandDokStatusEntitet(long behandlingId, OpptjeningIUtlandDokStatus dokStatus) {
        this.behandlingId = behandlingId;
        this.dokStatus = Objects.requireNonNull(dokStatus);
    }

    protected OpptjeningIUtlandDokStatusEntitet() {
        // Hibernate
    }

    public long getBehandlingId() {
        return behandlingId;
    }

    public OpptjeningIUtlandDokStatus getDokStatus() {
        return dokStatus;
    }

    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpptjeningIUtlandDokStatusEntitet that = (OpptjeningIUtlandDokStatusEntitet) o;
        return dokStatus == that.dokStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dokStatus);
    }
}
