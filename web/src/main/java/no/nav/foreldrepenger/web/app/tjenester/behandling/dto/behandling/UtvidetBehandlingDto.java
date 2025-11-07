package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.AsyncPollingStatus;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.VilkårDto;

@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class UtvidetBehandlingDto {

    @JsonProperty("aksjonspunkt")
    @NotNull private Set<AksjonspunktDto> aksjonspunkt;

    @JsonProperty("harSøknad") @NotNull
    private boolean harSøknad;

    @JsonProperty("harSattEndringsdato") @NotNull
    private boolean harSattEndringsdato;

    @JsonProperty("alleUttaksperioderAvslått")
    private boolean alleUttaksperioderAvslått;

    @JsonProperty("venteÅrsakKode")
    private String venteÅrsakKode;

    /** Eventuelt async status på tasks. */
    @JsonProperty("taskStatus")
    private AsyncPollingStatus taskStatus;
    @JsonProperty("uuid")
    @NotNull
    private UUID uuid;
    @JsonProperty("versjon")
    @NotNull
    private Long versjon;
    @JsonProperty("type")
    @NotNull
    private BehandlingType type;
    @JsonProperty("status")
    @NotNull
    private BehandlingStatus status;
    @JsonProperty("behandlendeEnhetId")
    @NotNull
    private String behandlendeEnhetId;
    @JsonProperty("behandlendeEnhetNavn")
    @NotNull
    private String behandlendeEnhetNavn;
    @JsonProperty("erAktivPapirsoknad")
    @NotNull
    private boolean erAktivPapirsoknad;
    @JsonProperty("aktivPapirsøknad")
    @NotNull
    private boolean aktivPapirsøknad;
    @JsonProperty("behandlingHenlagt")
    @NotNull
    private boolean behandlingHenlagt;
    @JsonProperty("behandlingPåVent")
    @NotNull
    private boolean behandlingPåVent;
    @JsonProperty("språkkode")
    @NotNull
    private Språkkode språkkode;
    @JsonProperty("behandlingsresultat")
    private BehandlingsresultatDto behandlingsresultat;
    @JsonProperty("behandlingÅrsaker")
    @NotNull
    private List<BehandlingÅrsakDto> behandlingÅrsaker;
    @JsonProperty("vilkår")
    @NotNull
    private List<VilkårDto> vilkår;
    @JsonProperty("fristBehandlingPåVent")
    private String fristBehandlingPåVent;
    /**
     * REST HATEOAS - pekere på data innhold som hentes fra andre url'er, eller handlinger som er tilgjengelig på behandling.
     *
     * @see https://restfulapi.net/hateoas/
     */
    @JsonProperty("links")
    @NotNull
    private List<ResourceLink> links = new ArrayList<>();

    //Kun autotest
    @NotNull
    @JsonProperty("id")
    private long id;
    //Kun autotest
    @NotNull
    @JsonProperty("opprettet")
    private LocalDateTime opprettet;

    public AsyncPollingStatus getTaskStatus() {
        return taskStatus;
    }

    public Set<AksjonspunktDto> getAksjonspunkt() {
        return aksjonspunkt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public LocalDateTime getOpprettet() {
        return opprettet;
    }

    public void setOpprettet(LocalDateTime opprettet) {
        this.opprettet = opprettet;
    }

    public boolean getHarSøknad() {
        return harSøknad;
    }

    public boolean getHarSattEndringsdato() {
        return harSattEndringsdato;
    }

    public boolean getAlleUttaksperioderAvslått() {
        return alleUttaksperioderAvslått;
    }

    public void setAsyncStatus(AsyncPollingStatus asyncStatus) {
        this.taskStatus = asyncStatus;
    }

    public void setAksjonspunkt(Set<AksjonspunktDto> aksjonspunkt) {
        this.aksjonspunkt = aksjonspunkt;
    }

    public void setHarSøknad(boolean harSøknad) {
        this.harSøknad = harSøknad;
    }

    public void setHarSattEndringsdato(boolean harSattEndringsdato) {
        this.harSattEndringsdato = harSattEndringsdato;
    }

    public void setAlleUttaksperioderAvslått(boolean alleUttaksperioderAvslått) {
        this.alleUttaksperioderAvslått = alleUttaksperioderAvslått;
    }

    public String getVenteÅrsakKode() {
        return venteÅrsakKode;
    }

    public void setVenteÅrsakKode(String venteÅrsakKode) {
        this.venteÅrsakKode = venteÅrsakKode;
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

    public BehandlingStatus getStatus() {
        return status;
    }

    public BehandlingsresultatDto getBehandlingsresultat() {
        return behandlingsresultat;
    }

    public String getBehandlendeEnhetId() {
        return behandlendeEnhetId;
    }

    public String getBehandlendeEnhetNavn() {
        return behandlendeEnhetNavn;
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

    public boolean isBehandlingPåVent() {
        return behandlingPåVent;
    }

    public boolean isBehandlingHenlagt() {
        return behandlingHenlagt;
    }

    public Språkkode getSpråkkode() {
        return språkkode;
    }

    @JsonProperty("behandlingÅrsaker")
    public List<BehandlingÅrsakDto> getBehandlingÅrsaker() {
        return behandlingÅrsaker;
    }

    void setBehandlingÅrsaker(List<BehandlingÅrsakDto> behandlingÅrsaker) {
        this.behandlingÅrsaker = behandlingÅrsaker;
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
        this.aktivPapirsøknad = erAktivPapirsoknad;
    }

    void leggTil(ResourceLink link) {
        links.add(link);
    }

    public void setBehandlingHenlagt(boolean behandlingHenlagt) {
        this.behandlingHenlagt = behandlingHenlagt;
    }

    void setBehandlingPåVent(boolean behandlingPåVent) {
        this.behandlingPåVent = behandlingPåVent;
    }

    void setSpråkkode(Språkkode språkkode) {
        this.språkkode = språkkode;
    }

    public void setVilkår(List<VilkårDto> vilkår) {
        this.vilkår = vilkår;
    }

    public String getFristBehandlingPåVent() {
        return fristBehandlingPåVent;
    }

    public void setFristBehandlingPåVent(String fristBehandlingPåVent) {
        this.fristBehandlingPåVent = fristBehandlingPåVent;
    }
}
