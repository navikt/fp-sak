package no.nav.foreldrepenger.behandling;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

/**
 * Minimal metadata for en behandling.
 *
 * @param saksnummer Saksnummer til saken.
 * @param fagsakId Fagsak id til saken.
 * @param fagsakYtelseType fagsak ytelse type
 * @param behandlingId Behandling id til saken.
 * @param behandlingUuid Eksternt refererbar UUID for behandlin.
 * @param behandlingStatus Behandling status.
 * @param behandlingType Behandling type
 * @param aktørId Søkers aktørid.
 * @param relasjonRolle Søkers rolle ifht. subjekt for ytelsen (eks. barn).
 * @param originalBehandlingId Original behandling id (i tilfelle dette f.eks er en revurdering av en annen behandling.
 *
 */
public record BehandlingReferanse(Saksnummer saksnummer,
                                  Long fagsakId,
                                  FagsakYtelseType fagsakYtelseType,
                                  Long behandlingId,
                                  UUID behandlingUuid,
                                  BehandlingStatus behandlingStatus,
                                  BehandlingType behandlingType,
                                  Long originalBehandlingId,
                                  AktørId aktørId,
                                  RelasjonsRolleType relasjonRolle) {

    /**
     * Oppretter referanse uten skjæringstidspunkt fra behandling.
     */
    public static BehandlingReferanse fra(Behandling behandling) {
        return new BehandlingReferanse(
                behandling.getSaksnummer(),
                behandling.getFagsakId(),
                behandling.getFagsakYtelseType(),
                behandling.getId(),
                behandling.getUuid(),
                behandling.getStatus(),
                behandling.getType(),
                behandling.getOriginalBehandlingId().orElse(null),
                behandling.getAktørId(),
                behandling.getRelasjonsRolleType());
    }


    public Optional<Long> getOriginalBehandlingId() {
        return Optional.ofNullable(originalBehandlingId);
    }

    public boolean erRevurdering() {
        return BehandlingType.REVURDERING.equals(behandlingType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, saksnummer, aktørId, fagsakYtelseType, behandlingType, relasjonRolle, originalBehandlingId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        var other = (BehandlingReferanse) obj;
        return Objects.equals(behandlingId, other.behandlingId)
            && Objects.equals(saksnummer, other.saksnummer)
            && Objects.equals(aktørId, other.aktørId)
            && Objects.equals(fagsakYtelseType, other.fagsakYtelseType)
            && Objects.equals(behandlingType, other.behandlingType)
            && Objects.equals(relasjonRolle, other.relasjonRolle)
            && Objects.equals(originalBehandlingId, other.originalBehandlingId)
            // tar ikke med status eller skjæringstidspunkt i equals siden de kan endre seg
            ;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + String.format(
            "<saksnummer=%s, behandlingId=%s, fagsakType=%s, behandlingType=%s, rolle=%s, aktørId=%s, status=%s, originalBehandlingId=%s>",
            saksnummer, behandlingId, fagsakYtelseType, behandlingType, relasjonRolle, aktørId, behandlingStatus, originalBehandlingId);
    }
}
