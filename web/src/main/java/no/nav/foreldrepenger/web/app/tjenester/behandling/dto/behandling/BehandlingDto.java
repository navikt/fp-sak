package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.VilkårDto;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BehandlingDto {

    //Fptilbake bruker fortsatt denne intern id'n.
    @JsonProperty("id")
    private Long id;
    @JsonProperty("uuid") @NotNull
    private UUID uuid;
    @JsonProperty("versjon") @NotNull
    private Long versjon;
    @JsonProperty("type") @NotNull
    private BehandlingType type;
    @JsonProperty("status") @NotNull
    private BehandlingStatus status;
    @JsonProperty("fagsakId")
    private Long fagsakId;
    @JsonProperty("opprettet") @NotNull
    private LocalDateTime opprettet;
    @JsonProperty("avsluttet")
    private LocalDateTime avsluttet;
    @JsonProperty("endret")
    private LocalDateTime endret;
    @JsonProperty("endretAvBrukernavn")
    private String endretAvBrukernavn;
    @JsonProperty("behandlendeEnhetId") @NotNull
    private String behandlendeEnhetId;
    @JsonProperty("behandlendeEnhetNavn") @NotNull
    private String behandlendeEnhetNavn;
    @JsonProperty("erAktivPapirsoknad") @NotNull
    private boolean erAktivPapirsoknad = false;
    @JsonProperty("førsteÅrsak")
    private BehandlingÅrsakDto førsteÅrsak;
    @JsonProperty("behandlingsfristTid")
    private LocalDate behandlingsfristTid;
    @JsonProperty("gjeldendeVedtak") @NotNull
    private boolean gjeldendeVedtak;
    @JsonProperty("erPaaVent") // Obsolete? ikke i frontend
    private boolean erPaaVent = false;
    @JsonProperty("originalVedtaksDato")
    private LocalDate originalVedtaksDato;
    @JsonProperty("behandlingHenlagt") @NotNull
    private boolean behandlingHenlagt;
    @JsonProperty("behandlingPåVent") @NotNull
    private boolean behandlingPåVent;
    @JsonProperty("fristBehandlingPåVent")
    private String fristBehandlingPåVent;
    @JsonProperty("venteÅrsakKode")
    private String venteÅrsakKode;
    @JsonProperty("språkkode") @NotNull
    private Språkkode språkkode;
    @JsonProperty("behandlingKøet") @NotNull
    private boolean behandlingKøet;
    @JsonProperty("ansvarligSaksbehandler")
    private String ansvarligSaksbehandler;
    @JsonProperty("toTrinnsBehandling") @NotNull
    private boolean toTrinnsBehandling;
    @JsonProperty("behandlingsresultat") @NotNull
    private BehandlingsresultatDto behandlingsresultat;
    @JsonProperty("behandlingÅrsaker") @NotNull
    private List<BehandlingÅrsakDto> behandlingÅrsaker;
    @JsonProperty("vilkår") @NotNull
    private List<VilkårDto> vilkår;

    /**
     * REST HATEOAS - pekere på data innhold som hentes fra andre url'er, eller handlinger som er tilgjengelig på behandling.
     *
     * @see https://restfulapi.net/hateoas/
     */
    @JsonProperty("links") @NotNull
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

    public List<VilkårDto> getVilkår() {
        return vilkår;
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

    public boolean isBehandlingHenlagt() {
        return behandlingHenlagt;
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

    public String getAnsvarligSaksbehandler() {
        return ansvarligSaksbehandler;
    }

    public boolean getToTrinnsBehandling() {
        return toTrinnsBehandling;
    }

    @JsonProperty("behandlingÅrsaker")
    public List<BehandlingÅrsakDto> getBehandlingÅrsaker() {
        return behandlingÅrsaker;
    }

    void setBehandlingÅrsaker(List<BehandlingÅrsakDto> behandlingÅrsaker) {
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

    public void setBehandlingHenlagt(boolean behandlingHenlagt) {
        this.behandlingHenlagt = behandlingHenlagt;
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

    public void setVilkår(List<VilkårDto> vilkår) {
        this.vilkår = vilkår;
    }
}
