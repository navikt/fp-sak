package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;

public class BehandlingDto {

    private Long id;
    private UUID uuid;
    private Long versjon;
    private BehandlingType type;
    private BehandlingStatus status;
    private Long fagsakId;
    private LocalDateTime opprettet;
    private LocalDateTime avsluttet;
    private LocalDateTime endret;
    private String endretAvBrukernavn;
    private String behandlendeEnhetId;
    private String behandlendeEnhetNavn;
    private boolean erAktivPapirsoknad = false;
    private BehandlingÅrsakDto førsteÅrsak;
    private LocalDate behandlingsfristTid;
    private boolean gjeldendeVedtak;
    private boolean erPaaVent = false;
    private LocalDate originalVedtaksDato;
    private boolean behandlingPåVent;
    private String fristBehandlingPåVent;
    private String venteÅrsakKode;
    private Språkkode språkkode;
    private boolean behandlingKøet;
    private String ansvarligSaksbehandler;
    private boolean toTrinnsBehandling;
    private BehandlingsresultatDto behandlingsresultat;
    private List<BehandlingÅrsakDto> behandlingÅrsaker;

    /**
     * REST HATEOAS - pekere på data innhold som hentes fra andre url'er, eller handlinger som er tilgjengelig på behandling.
     *
     * @see https://restfulapi.net/hateoas/
     */
    private List<ResourceLink> links = new ArrayList<>();

    public Long getFagsakId() {
        return fagsakId;
    }

    public Long getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Long getVersjon() {
        return versjon;
    }

    public BehandlingType getType() {
        return type;
    }

    public LocalDateTime getOpprettet() {
        return opprettet;
    }

    public LocalDateTime getAvsluttet() {
        return avsluttet;
    }

    public BehandlingStatus getStatus() {
        return status;
    }

    public LocalDateTime getEndret() {
        return endret;
    }

    public BehandlingsresultatDto getBehandlingsresultat() {
        return behandlingsresultat;
    }

    public String getEndretAvBrukernavn() {
        return endretAvBrukernavn;
    }

    public String getBehandlendeEnhetId() {
        return behandlendeEnhetId;
    }

    public String getBehandlendeEnhetNavn() {
        return behandlendeEnhetNavn;
    }

    public BehandlingÅrsakDto getFørsteÅrsak() {
        return førsteÅrsak;
    }

    public boolean isErAktivPapirsoknad() {
        return erAktivPapirsoknad;
    }

    public List<ResourceLink> getLinks() {
        return Collections.unmodifiableList(links);
    }

    @JsonProperty("behandlingPaaVent")
    public boolean isBehandlingPåVent() {
        return behandlingPåVent;
    }

    @JsonProperty("fristBehandlingPaaVent")
    public String getFristBehandlingPåVent() {
        return fristBehandlingPåVent;
    }

    @JsonProperty("venteArsakKode")
    public String getVenteÅrsakKode() {
        return venteÅrsakKode;
    }

    @JsonProperty("sprakkode")
    public Språkkode getSpråkkode() {
        return språkkode;
    }

    @JsonProperty("behandlingKoet")
    public boolean isBehandlingKoet() {
        return behandlingKøet;
    }

    @JsonProperty("ansvarligSaksbehandler")
    public String getAnsvarligSaksbehandler() {
        return ansvarligSaksbehandler;
    }

    @JsonProperty("toTrinnsBehandling")
    public boolean getToTrinnsBehandling() {
        return toTrinnsBehandling;
    }

    @JsonProperty("behandlingArsaker")
    public List<BehandlingÅrsakDto> getBehandlingÅrsaker() {
        return behandlingÅrsaker;
    }

    void setBehandlingArsaker(List<BehandlingÅrsakDto> behandlingÅrsaker) {
        this.behandlingÅrsaker = behandlingÅrsaker;
    }

    void setFagsakId(Long fagsakId) {
        this.fagsakId = fagsakId;
    }

    void setId(Long id) {
        this.id = id;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    void setVersjon(Long versjon) {
        this.versjon = versjon;
    }

    void setType(BehandlingType type) {
        this.type = type;
    }

    void setOpprettet(LocalDateTime opprettet) {
        this.opprettet = opprettet;
    }

    void setEndret(LocalDateTime endret) {
        this.endret = endret;
    }

    void setEndretAvBrukernavn(String endretAvBrukernavn) {
        this.endretAvBrukernavn = endretAvBrukernavn;
    }

    void setAvsluttet(LocalDateTime avsluttet) {
        this.avsluttet = avsluttet;
    }

    void setBehandlingsresultat(BehandlingsresultatDto behandlingsresultat) {
        this.behandlingsresultat = behandlingsresultat;
    }

    void setStatus(BehandlingStatus status) {
        this.status = status;
    }

    void setBehandlendeEnhetId(String behandlendeEnhetId) {
        this.behandlendeEnhetId = behandlendeEnhetId;
    }

    void setBehandlendeEnhetNavn(String behandlendeEnhetNavn) {
        this.behandlendeEnhetNavn = behandlendeEnhetNavn;
    }

    void setErAktivPapirsøknad(boolean erAktivPapirsoknad) {
        this.erAktivPapirsoknad = erAktivPapirsoknad;
    }

    public void setFørsteÅrsak(BehandlingÅrsakDto førsteÅrsak) {
        this.førsteÅrsak = førsteÅrsak;
    }

    void leggTil(ResourceLink link) {
        links.add(link);
    }

    public LocalDate getBehandlingsfristTid() {
        return behandlingsfristTid;
    }

    public void setBehandlingsfristTid(LocalDate behandlingsfristTid) {
        this.behandlingsfristTid = behandlingsfristTid;
    }

    public boolean isGjeldendeVedtak() {
        return gjeldendeVedtak;
    }

    public void setGjeldendeVedtak(boolean gjeldendeVedtak) {
        this.gjeldendeVedtak = gjeldendeVedtak;
    }

    public boolean isErPaaVent() {
        return erPaaVent;
    }

    public void setErPaaVent(boolean erPaaVent) {
        this.erPaaVent = erPaaVent;
    }

    public LocalDate getOriginalVedtaksDato() {
        return originalVedtaksDato;
    }

    public void setOriginalVedtaksDato(LocalDate originalVedtaksDato) {
        this.originalVedtaksDato = originalVedtaksDato;
    }

    void setBehandlingPåVent(boolean behandlingPåVent) {
        this.behandlingPåVent = behandlingPåVent;
    }

    void setFristBehandlingPåVent(String fristBehandlingPåVent) {
        this.fristBehandlingPåVent = fristBehandlingPåVent;
    }

    void setVenteÅrsakKode(String venteÅrsakKode) {
        this.venteÅrsakKode = venteÅrsakKode;
    }

    void setSpråkkode(Språkkode språkkode) {
        this.språkkode = språkkode;
    }

    void setBehandlingKøet(boolean behandlingKøet) {
        this.behandlingKøet = behandlingKøet;
    }

    void setAnsvarligSaksbehandler(String ansvarligSaksbehandler) {
        this.ansvarligSaksbehandler = ansvarligSaksbehandler;
    }

    void setToTrinnsBehandling(boolean toTrinnsBehandling) {
        this.toTrinnsBehandling = toTrinnsBehandling;
    }
}
