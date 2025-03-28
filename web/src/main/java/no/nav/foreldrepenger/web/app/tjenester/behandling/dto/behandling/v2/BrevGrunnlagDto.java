package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;

public class BrevGrunnlagDto {

    private UUID uuid;
    private BehandlingType type;
    private BehandlingStatus status;
    private LocalDateTime opprettet;
    private LocalDateTime avsluttet;
    private String behandlendeEnhetId;
    private Språkkode språkkode;
    private boolean toTrinnsBehandling;
    private Boolean harAvklartAnnenForelderRett;
    private UUID originalBehandlingUuid;
    private Avslagsårsak medlemskapOpphørsårsak;
    private LocalDate medlemskapFom;
    private BehandlingsresultatV2Dto behandlingsresultat; //
    private FagsakV2Dto fagsak;
    private VergeV2Dto verge;

    private List<BehandlingÅrsakType> behandlingÅrsaker; //
    private List<VilkårType> vilkår;
    private List<ResourceLink> links = new ArrayList<>();

    public BrevGrunnlagDto() {
        // trengs for deserialisering av JSON
    }

    public Boolean getHarAvklartAnnenForelderRett() {
        return harAvklartAnnenForelderRett;
    }

    public void setHarAvklartAnnenForelderRett(Boolean harAvklartAnnenForelderRett) {
        this.harAvklartAnnenForelderRett = harAvklartAnnenForelderRett;
    }

    public UUID getOriginalBehandlingUuid() {
        return originalBehandlingUuid;
    }

    public void setOriginalBehandlingUuid(UUID originalBehandlingUuid) {
        this.originalBehandlingUuid = originalBehandlingUuid;
    }

    public void setMedlemskapOpphørsårsak(Avslagsårsak medlemskapOpphørsårsak) {
        this.medlemskapOpphørsårsak = medlemskapOpphørsårsak;
    }

    public Avslagsårsak getMedlemskapOpphørsårsak() {
        return medlemskapOpphørsårsak;
    }

    public LocalDate getMedlemskapFom() {
        return medlemskapFom;
    }

    public void setMedlemskapFom(LocalDate medlemskapFom) {
        this.medlemskapFom = medlemskapFom;
    }

    public FagsakV2Dto getFagsak() {
        return fagsak;
    }

    public UUID getUuid() {
        return uuid;
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

    public BehandlingsresultatV2Dto getBehandlingsresultat() {
        return behandlingsresultat;
    }

    public String getBehandlendeEnhetId() {
        return behandlendeEnhetId;
    }

    public List<VilkårType> getVilkår() {
        return vilkår;
    }

    public List<ResourceLink> getLinks() {
        return Collections.unmodifiableList(links);
    }

    void leggTil(ResourceLink link) {
        links.add(link);
    }

    public Språkkode getSpråkkode() {
        return språkkode;
    }

    public boolean getToTrinnsBehandling() {
        return toTrinnsBehandling;
    }

    public List<BehandlingÅrsakType> getBehandlingÅrsaker() {
        return behandlingÅrsaker;
    }

    void setBehandlingÅrsaker(List<BehandlingÅrsakType> behandlingÅrsaker) {
        this.behandlingÅrsaker = behandlingÅrsaker;
    }

    void setFagsak(FagsakV2Dto fagsak) {
        this.fagsak = fagsak;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    void setType(BehandlingType type) {
        this.type = type;
    }

    void setOpprettet(LocalDateTime opprettet) {
        this.opprettet = opprettet;
    }

    void setAvsluttet(LocalDateTime avsluttet) {
        this.avsluttet = avsluttet;
    }

    void setBehandlingsresultat(BehandlingsresultatV2Dto behandlingsresultat) {
        this.behandlingsresultat = behandlingsresultat;
    }

    void setStatus(BehandlingStatus status) {
        this.status = status;
    }

    void setBehandlendeEnhetId(String behandlendeEnhetId) {
        this.behandlendeEnhetId = behandlendeEnhetId;
    }

    void setSpråkkode(Språkkode språkkode) {
        this.språkkode = språkkode;
    }

    void setToTrinnsBehandling(boolean toTrinnsBehandling) {
        this.toTrinnsBehandling = toTrinnsBehandling;
    }

    public void setVilkår(List<VilkårType> vilkår) {
        this.vilkår = vilkår;
    }

    public VergeV2Dto getVerge() {
        return verge;
    }

    public void setVerge(VergeV2Dto verge) {
        this.verge = verge;
    }
}
