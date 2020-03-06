package no.nav.foreldrepenger.behandling;

import java.time.LocalDate;
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
 */
public class BehandlingReferanse {

    private Saksnummer saksnummer;

    private Long fagsakId;

    private Long behandlingId;

    private FagsakYtelseType fagsakYtelseType;

    /**
     * Søkers aktørid.
     */
    private AktørId aktørId;
    /**
     * Søkers rolle ifht. subjekt for ytelsen (eks. barn).
     */
    private RelasjonsRolleType relasjonRolle;

    private BehandlingType behandlingType;

    /**
     * Original behandling id (i tilfelle dette f.eks er en revurdering av en annen behandling.
     */
    private Optional<Long> originalBehandlingId;

    /**
     * Inneholder relevante tidspunkter for en behandling
     */
    private Skjæringstidspunkt skjæringstidspunkt;

    private BehandlingStatus behandlingStatus;

    /** Eksternt refererbar UUID for behandling. */
    private UUID behandlingUuid;

    BehandlingReferanse() {
    }

    private BehandlingReferanse(FagsakYtelseType fagsakYtelseType, BehandlingType behandlingType, RelasjonsRolleType relasjonRolle, AktørId aktørId, // NOSONAR
                                Saksnummer saksnummer, Long fagsakId, Long behandlingId, UUID behandlingUuid, Optional<Long> originalBehandlingId,
                                BehandlingStatus behandlingStatus, Skjæringstidspunkt skjæringstidspunkt) {
        this.fagsakYtelseType = fagsakYtelseType;
        this.behandlingType = behandlingType;
        this.relasjonRolle = relasjonRolle;
        this.aktørId = aktørId;
        this.saksnummer = saksnummer;
        this.fagsakId = fagsakId;
        this.behandlingId = behandlingId;
        this.behandlingUuid = behandlingUuid;
        this.originalBehandlingId = originalBehandlingId;
        this.behandlingStatus = behandlingStatus;
        this.skjæringstidspunkt = skjæringstidspunkt;
    }

    /**
     * Oppretter referanse uten skjæringstidspunkt fra behandling.
     */
    public static BehandlingReferanse fra(Behandling behandling) {
        return fra(behandling, (LocalDate) null);
    }

    /*
     * Unngå bruk av denne.
     */
    public static BehandlingReferanse fra(Behandling behandling, LocalDate utledetSkjæringstidspunkt) {
        return new BehandlingReferanse(behandling.getFagsakYtelseType(),
            behandling.getType(),
            behandling.getRelasjonsRolleType(),
            behandling.getAktørId(),
            behandling.getFagsak().getSaksnummer(),
            behandling.getFagsakId(),
            behandling.getId(),
            behandling.getUuid(),
            behandling.getOriginalBehandling().map(Behandling::getId),
            behandling.getStatus(),
            Skjæringstidspunkt.builder()
                .medUtledetSkjæringstidspunkt(utledetSkjæringstidspunkt)
                .build());
    }

    public static BehandlingReferanse fra(Behandling behandling, Skjæringstidspunkt skjæringstidspunkt) {
        return new BehandlingReferanse(behandling.getFagsakYtelseType(),
            behandling.getType(),
            behandling.getRelasjonsRolleType(),
            behandling.getAktørId(),
            behandling.getFagsak().getSaksnummer(),
            behandling.getFagsakId(),
            behandling.getId(),
            behandling.getUuid(),
            behandling.getOriginalBehandling().map(Behandling::getId),
            behandling.getStatus(),
            skjæringstidspunkt);
    }

    public static BehandlingReferanse fra(FagsakYtelseType fagsakYtelseType, BehandlingType behandlingType, RelasjonsRolleType relasjonRolle, AktørId aktørId, // NOSONAR
                                          Saksnummer saksnummer, Long fagsakId, Long behandlingId, UUID behandlingUuid, Optional<Long> originalBehandlingId,
                                          BehandlingStatus behandlingStatus, Skjæringstidspunkt skjæringstidspunkt) {
        return new BehandlingReferanse(fagsakYtelseType,
            behandlingType,
            relasjonRolle,
            aktørId,
            saksnummer,
            fagsakId,
            behandlingId,
            behandlingUuid,
            originalBehandlingId,
            behandlingStatus,
            skjæringstidspunkt);
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public Long getId() {
        return getBehandlingId();
    }

    public Optional<Long> getOriginalBehandlingId() {
        return originalBehandlingId;
    }

    public FagsakYtelseType getFagsakYtelseType() {
        return fagsakYtelseType;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public LocalDate getUtledetSkjæringstidspunkt() {
        // precondition
        sjekkSkjæringstidspunkt();
        return skjæringstidspunkt.getUtledetSkjæringstidspunkt();
    }

    public Skjæringstidspunkt getSkjæringstidspunkt() {
        sjekkSkjæringstidspunkt();
        return skjæringstidspunkt;
    }

    public LocalDate getSkjæringstidspunktBeregning() {
        // precondition
        sjekkSkjæringstidspunkt();
        return skjæringstidspunkt.getSkjæringstidspunktBeregning();
    }

    public LocalDate getSkjæringstidspunktOpptjening() {
        // precondition
        sjekkSkjæringstidspunkt();
        return skjæringstidspunkt.getSkjæringstidspunktOpptjening();
    }

    public LocalDate getFørsteUttaksdato() {
        // precondition
        sjekkSkjæringstidspunkt();
        return skjæringstidspunkt.getFørsteUttaksdato();
    }

    public RelasjonsRolleType getRelasjonsRolleType() {
        return relasjonRolle;
    }

    public BehandlingType getBehandlingType() {
        return behandlingType;
    }

    public boolean erRevurdering() {
        return BehandlingType.REVURDERING.equals(behandlingType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saksnummer, behandlingId, originalBehandlingId, fagsakYtelseType, behandlingType, aktørId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        BehandlingReferanse other = (BehandlingReferanse) obj;
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

    /**
     * Hvis skjæringstidspunkt ikke er satt, så kastes NPE ved bruk. Utvikler-feil
     */
    private void sjekkSkjæringstidspunkt() {
        Objects.requireNonNull(skjæringstidspunkt,
            "Utvikler-feil: skjæringstidspunkt er ikke satt på BehandlingReferanse. Sørg for at det er satt ifht. anvendelse");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + String.format(
            "<saksnummer=%s, behandlingId=%s, fagsakType=%s, behandlingType=%s, rolle=%s, aktørId=%s, status=%s, skjæringstidspunjkt=%s, originalBehandlingId=%s>",
            saksnummer, behandlingId, fagsakYtelseType, behandlingType, relasjonRolle, aktørId, behandlingStatus, skjæringstidspunkt, originalBehandlingId);
    }

    /**
     * Lag immutable copy av referanse med satt utledet skjæringstidspunkt.
     */
    public BehandlingReferanse medSkjæringstidspunkt(LocalDate utledetSkjæringstidspunkt) {
        return new BehandlingReferanse(getFagsakYtelseType(),
            getBehandlingType(),
            getRelasjonsRolleType(),
            getAktørId(),
            getSaksnummer(),
            getFagsakId(),
            getId(),
            getBehandlingUuid(),
            getOriginalBehandlingId(),
            getBehandlingStatus(),
            Skjæringstidspunkt.builder()
                .medUtledetSkjæringstidspunkt(utledetSkjæringstidspunkt)
                .build());
    }

    /**
     * Lag immutable copy av referanse med mulighet til å legge til skjæringstidspunkt av flere typer
     */
    public BehandlingReferanse medSkjæringstidspunkt(Skjæringstidspunkt skjæringstidspunkt) {
        return new BehandlingReferanse(getFagsakYtelseType(),
            getBehandlingType(),
            getRelasjonsRolleType(),
            getAktørId(),
            getSaksnummer(),
            getFagsakId(),
            getId(),
            getBehandlingUuid(),
            getOriginalBehandlingId(),
            getBehandlingStatus(),
            skjæringstidspunkt);
    }

    public BehandlingStatus getBehandlingStatus() {
        return behandlingStatus;
    }

}
