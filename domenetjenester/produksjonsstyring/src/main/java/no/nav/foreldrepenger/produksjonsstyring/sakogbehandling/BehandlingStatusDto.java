package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling;

import java.time.LocalDateTime;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class BehandlingStatusDto {

    private Long behandlingId;
    private Saksnummer saksnummer;
    private AktørId aktørId;
    private BehandlingType behandlingType;
    private BehandlingTema behandlingTema;
    private BehandlingStatus behandlingStatus;
    private OrganisasjonsEnhet enhet;
    private Long originalBehandlingId;
    private LocalDateTime hendelsesTidspunkt;
    private UUID behandlingEksternRef;

    public Long getBehandlingId() {
        return behandlingId;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public BehandlingType getBehandlingType() {
        return behandlingType;
    }

    public BehandlingTema getBehandlingTema() {
        return behandlingTema;
    }

    public String getBehandlingStatusKode() {
        return behandlingStatus.getKode();
    }

    public boolean erBehandlingAvsluttet() {
        return BehandlingStatus.AVSLUTTET.equals(behandlingStatus);
    }

    public OrganisasjonsEnhet getEnhet() {
        return enhet;
    }

    public Long getOriginalBehandlingId() {
        return originalBehandlingId;
    }

    public LocalDateTime getHendelsesTidspunkt() {
        return hendelsesTidspunkt;
    }

    public UUID getBehandlingEksternRef() {
        return behandlingEksternRef;
    }

    @Override
    public String toString() {
        return "BehandlingStatusDto{" +
            "behandlingId=" + behandlingId +
            ", saksnummer=" + saksnummer +
            ", hendelsesTidspunkt=" + hendelsesTidspunkt +
            '}';
    }

    public static B getBuilder() {
        return new B();
    }

    public static class B {
        private BehandlingStatusDto status;

        B() {
            this.status = new BehandlingStatusDto();
        }

        public B medBehandlingId(Long behandlingId)  {
            this.status.behandlingId = behandlingId;
            return this;
        }

        public B medSaksnummer(Saksnummer saksnummer) {
            this.status.saksnummer = saksnummer;
            return this;
        }

        public B medAktørId(AktørId aktørId)  {
            this.status.aktørId = aktørId;
            return this;
        }

        public B medBehandlingStatus(BehandlingStatus behandlingStatus) {
            this.status.behandlingStatus = behandlingStatus;
            return this;
        }

        public B medBehandlingType(BehandlingType behandlingType) {
            this.status.behandlingType = behandlingType;
            return this;
        }

        public B medBehandlingTema(BehandlingTema behandlingTema) {
            this.status.behandlingTema = behandlingTema;
            return this;
        }

        public B medEnhet(OrganisasjonsEnhet organisasjonsEnhet) {
            this.status.enhet = organisasjonsEnhet;
            return this;
        }

        public B medOriginalBehandling(Long originalId) {
            this.status.originalBehandlingId = originalId;
            return this;
        }

        public B medHendelsesTidspunkt(LocalDateTime hendelse) {
            this.status.hendelsesTidspunkt = hendelse;
            return this;
        }

        public B medBehandlingEksternRef(UUID eksternRef) {
            this.status.behandlingEksternRef = eksternRef;
            return this;
        }

        public BehandlingStatusDto build() {
            return this.status;
        }

    }
}
