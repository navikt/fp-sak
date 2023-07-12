package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "Oppdragskontroll")
@Table(name = "OPPDRAG_KONTROLL")
public class Oppdragskontroll extends BaseEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OPPDRAG_KONTROLL")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    /**
     * Offisielt tildelt saksnummer fra GSAK.
     */
    @Embedded
    @AttributeOverride(name = "saksnummer", column = @Column(name = "saksnummer", unique = true, nullable = false, updatable = false))
    private Saksnummer saksnummer;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "venter_kvittering", nullable = false)
    private Boolean venterKvittering = Boolean.TRUE;

    @Column(name = "prosess_task_id", nullable = false)
    private Long prosessTaskId;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "oppdragskontroll")
    private List<Oppdrag110> oppdrag110Liste = new ArrayList<>();

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "patched")
    private Boolean patched;

    public Oppdragskontroll() {
        // default constructor
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public Boolean getVenterKvittering() {
        return venterKvittering;
    }

    public void setVenterKvittering(Boolean venterKvittering) {
        this.venterKvittering = venterKvittering;
    }

    public void setPatched(final Boolean patched) {
        this.patched = patched;
    }

    public Long getProsessTaskId() {
        return prosessTaskId;
    }

    public void setProsessTaskId(final Long prosessTaskId) {
        this.prosessTaskId = prosessTaskId;
    }

    public List<Oppdrag110> getOppdrag110Liste() {
        return oppdrag110Liste;
    }

    public long getVersjon() {
        return versjon;
    }

    protected void addOppdrag110(Oppdrag110 oppdrag110) {
        Objects.requireNonNull(oppdrag110, "oppdrag110");
        oppdrag110Liste.add(oppdrag110);
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Oppdragskontroll oppdragskontroll)) {
            return false;
        }
        return Objects.equals(behandlingId, oppdragskontroll.getBehandlingId())
            && Objects.equals(saksnummer, oppdragskontroll.getSaksnummer())
            && Objects.equals(venterKvittering, oppdragskontroll.getVenterKvittering())
            && Objects.equals(prosessTaskId, oppdragskontroll.getProsessTaskId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, saksnummer, venterKvittering, prosessTaskId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long behandlingId;
        private Saksnummer saksnummer;
        private Boolean venterKvittering;
        private Long prosessTaskId;

        public Builder medSaksnummer(Saksnummer saksnummer) {
            this.saksnummer = saksnummer;
            return this;
        }

        public Builder medVenterKvittering(Boolean venterKvittering) {
            this.venterKvittering = venterKvittering;
            return this;
        }

        public Builder medProsessTaskId(Long prosessTaskId) {
            this.prosessTaskId = prosessTaskId;
            return this;
        }

        public Builder medBehandlingId(Long behandlingId) {
            this.behandlingId = behandlingId;
            return this;
        }

        public Oppdragskontroll build() {
            verifyStateForBuild();
            var oppdragskontroll = new Oppdragskontroll();
            oppdragskontroll.behandlingId = behandlingId;
            oppdragskontroll.saksnummer = saksnummer;
            oppdragskontroll.venterKvittering = venterKvittering;
            oppdragskontroll.prosessTaskId = prosessTaskId;

            return oppdragskontroll;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(behandlingId, "behandlingId");
            Objects.requireNonNull(saksnummer, "saksnummer");
            Objects.requireNonNull(venterKvittering, "venterKvittering");
            Objects.requireNonNull(prosessTaskId, "prosessTaskId");
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            (id != null ? "id=" + id + ", " : "")
            + "behandlingId=" + behandlingId + ", "
            + "saksnummer=" + saksnummer + ", "
            + "venterKvittering=" + venterKvittering + ", "
            + "prosessTaskId=" + prosessTaskId + ", "
            + "opprettetTs=" + getOpprettetTidspunkt()
            + ">";
    }
}
