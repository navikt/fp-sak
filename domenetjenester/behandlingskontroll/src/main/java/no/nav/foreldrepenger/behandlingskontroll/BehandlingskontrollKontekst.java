package no.nav.foreldrepenger.behandlingskontroll;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

/**
 * Container som holder kontekst under prosessering av {@link BehandlingSteg}.
 */
public class BehandlingskontrollKontekst {

    private final BehandlingLås behandlingLås;
    private final Long fagsakId;
    private final Saksnummer saksnummer;
    private final FagsakYtelseType ytelseType;
    private final BehandlingType behandlingType;

    /**
     * NB: Foretrekk {@link BehandlingskontrollTjeneste#initBehandlingskontroll} i
     * stedet for å opprette her direkte.
     */
    public BehandlingskontrollKontekst(Saksnummer saksnummer, Long fagsakId, BehandlingLås behandlingLås,
                                       FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        Objects.requireNonNull(behandlingLås, "behandlingLås");
        this.saksnummer = saksnummer;
        this.fagsakId = fagsakId;
        this.behandlingLås = behandlingLås;
        this.ytelseType = ytelseType;
        this.behandlingType = behandlingType;
    }

    public BehandlingskontrollKontekst(Behandling behandling, BehandlingLås behandlingLås) {
        Objects.requireNonNull(behandlingLås, "behandlingLås");
        this.saksnummer = behandling.getSaksnummer();
        this.fagsakId = behandling.getFagsakId();
        this.behandlingLås = behandlingLås;
        this.ytelseType = behandling.getFagsakYtelseType();
        this.behandlingType = behandling.getType();
    }

    public BehandlingLås getSkriveLås() {
        return behandlingLås;
    }

    public Long getBehandlingId() {
        return behandlingLås.getBehandlingId();
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

    public BehandlingType getBehandlingType() {
        return behandlingType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BehandlingskontrollKontekst other)) {
            return false;
        }
        return Objects.equals(fagsakId, other.fagsakId)
                && Objects.equals(saksnummer, other.saksnummer)
                && Objects.equals(getBehandlingId(), other.getBehandlingId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(fagsakId, saksnummer, getBehandlingId());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<fagsakId=" + fagsakId + ", saksnummer=" + saksnummer + ", behandlingId=" + getBehandlingId() + ">";
    }
}
