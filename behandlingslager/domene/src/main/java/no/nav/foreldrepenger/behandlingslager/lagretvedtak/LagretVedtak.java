package no.nav.foreldrepenger.behandlingslager.lagretvedtak;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

@SqlResultSetMapping(name = "LagretVedtakResult", classes = {
    @ConstructorResult(targetClass = LagretVedtakMedBehandlingType.class, columns = {
        @ColumnResult(name = "id"),
        @ColumnResult(name = "behandlingType"),
        @ColumnResult(name = "opprettetDato")
    })
})
@Entity(name = "LagretVedtak")
@Table(name = "LAGRET_VEDTAK")
public class LagretVedtak extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAGRET_VEDTAK")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "FAGSAK_ID", nullable = false, updatable = false)
    private Long fagsakId;

    @Column(name = "BEHANDLING_ID", nullable = false, updatable = false)
    private Long behandlingId;

    @Lob
    @Column(name = "XML_CLOB", nullable = false)
    private String xmlClob;

    private LagretVedtak() {
    }

    public Long getId() {
        return id;
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public String getXmlClob() {
        return xmlClob;
    }
    public void setXmlClob(String xmlClob) {
        this.xmlClob = xmlClob;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof LagretVedtak)) {
            return false;
        }
        LagretVedtak lagretVedtak = (LagretVedtak) object;
        return Objects.equals(fagsakId, lagretVedtak.getFagsakId())
            && Objects.equals(behandlingId, lagretVedtak.getBehandlingId())
            && Objects.equals(xmlClob, lagretVedtak.getXmlClob());
    }

    @Override
    public int hashCode() {
        return Objects.hash(fagsakId, behandlingId, xmlClob);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long fagsakId;
        private Long behandlingId;
        private String xmlClob;

        public Builder medFagsakId(Long fagsakId) {
            this.fagsakId = fagsakId;
            return this;
        }

        public Builder medBehandlingId(Long behandlingId) {
            this.behandlingId = behandlingId;
            return this;
        }

        public Builder medXmlClob(String xmlClob) {
            this.xmlClob = xmlClob;
            return this;
        }

        public LagretVedtak build() {
            verifyStateForBuild();
            LagretVedtak lagretVedtak = new LagretVedtak();
            lagretVedtak.fagsakId = fagsakId;
            lagretVedtak.behandlingId = behandlingId;
            lagretVedtak.xmlClob = xmlClob;
            return lagretVedtak;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(fagsakId, "fagsakId");
            Objects.requireNonNull(behandlingId, "behandlingId");
            Objects.requireNonNull(xmlClob, "xmlClob");
        }
    }
}
