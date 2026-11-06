package no.nav.foreldrepenger.behandlingslager.fagsak;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@Entity(name = "FagsakProsessTask")
@Table(name = "FAGSAK_PROSESS_TASK")
public class FagsakProsessTask extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FAGSAK_PROSESS_TASK")
    private Long id;

    @NotNull
    @Convert(converter = Saksnummer.SaksnummerConverter.class)
    @Column(name = "saksnummer", updatable = false, nullable=false)
    private Saksnummer saksnummer;

    @Column(name = "prosess_task_id", nullable = false, updatable = false)
    private Long prosessTaskId;

    @Column(name = "behandling_id", updatable = false)
    private Long behandlingId;

    @Column(name = "gruppe_sekvensnr", updatable = false)
    private Long gruppeSekvensNr;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    protected FagsakProsessTask() {
        // Hibernate trenger en
    }

    public FagsakProsessTask(Saksnummer saksnummer, Long prosessTaskId, Long behandlingId, Long gruppeSekvensNr) {
        this.saksnummer = saksnummer;
        this.prosessTaskId = prosessTaskId;
        this.behandlingId = behandlingId;
        this.gruppeSekvensNr = gruppeSekvensNr;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public Long getProsessTaskId() {
        return prosessTaskId;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public Long getGruppeSekvensNr() {
        return gruppeSekvensNr;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof FagsakProsessTask other)) {
            return false;
        }
        return Objects.equals(prosessTaskId, other.prosessTaskId)
            && Objects.equals(saksnummer, other.saksnummer)
            && Objects.equals(behandlingId, other.behandlingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prosessTaskId, saksnummer, behandlingId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<"
            + "prosessTask=" + prosessTaskId
            + ", fagsak=" + saksnummer
            + (behandlingId == null ? "" : ", behandling=" + behandlingId)
            + (gruppeSekvensNr == null ? "" : ", gruppeSekvensNr=" + gruppeSekvensNr)
            + ">";
    }
}
