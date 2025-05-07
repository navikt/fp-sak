package no.nav.foreldrepenger.web.app.tjenester.formidling.rest.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import no.nav.foreldrepenger.web.app.rest.ResourceLink;
import no.nav.foreldrepenger.web.app.tjenester.formidling.rest.kodeverk.*;

public class BrevGrunnlagResponseDto {

    private UUID uuid;
    private BehandlingTypeDto type;
    private BehandlingStatusDto status;
    private LocalDateTime opprettet;
    private LocalDateTime avsluttet;
    private String behandlendeEnhetId;
    private SpråkkodeDto språkkode;
    private boolean toTrinnsBehandling;
    private Boolean harAvklartAnnenForelderRett;
    private UUID originalBehandlingUuid;
    private AvslagÅrsakDto medlemskapOpphørsårsak;
    private LocalDate medlemskapFom;
    private BehandlingsresultatDto behandlingsresultat; //
    private FagsakDto fagsak;
    private VergeDto verge;

    private List<BehandlingÅrsakTypeDto> behandlingÅrsaker; //
    private List<VilkårTypeDto> vilkår;
    private List<ResourceLink> links = new ArrayList<>();

    public BrevGrunnlagResponseDto() {
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

    public void setMedlemskapOpphørsårsak(AvslagÅrsakDto medlemskapOpphørsårsak) {
        this.medlemskapOpphørsårsak = medlemskapOpphørsårsak;
    }

    public AvslagÅrsakDto getMedlemskapOpphørsårsak() {
        return medlemskapOpphørsårsak;
    }

    public LocalDate getMedlemskapFom() {
        return medlemskapFom;
    }

    public void setMedlemskapFom(LocalDate medlemskapFom) {
        this.medlemskapFom = medlemskapFom;
    }

    public FagsakDto getFagsak() {
        return fagsak;
    }

    public UUID getUuid() {
        return uuid;
    }

    public BehandlingTypeDto getType() {
        return type;
    }

    public LocalDateTime getOpprettet() {
        return opprettet;
    }

    public LocalDateTime getAvsluttet() {
        return avsluttet;
    }

    public BehandlingStatusDto getStatus() {
        return status;
    }

    public BehandlingsresultatDto getBehandlingsresultat() {
        return behandlingsresultat;
    }

    public String getBehandlendeEnhetId() {
        return behandlendeEnhetId;
    }

    public List<VilkårTypeDto> getVilkår() {
        return vilkår;
    }

    public List<ResourceLink> getLinks() {
        return Collections.unmodifiableList(links);
    }

    public void leggTil(ResourceLink link) {
        links.add(link);
    }

    public SpråkkodeDto getSpråkkode() {
        return språkkode;
    }

    public boolean getToTrinnsBehandling() {
        return toTrinnsBehandling;
    }

    public List<BehandlingÅrsakTypeDto> getBehandlingÅrsaker() {
        return behandlingÅrsaker;
    }

    public void setBehandlingÅrsaker(List<BehandlingÅrsakTypeDto> behandlingÅrsaker) {
        this.behandlingÅrsaker = behandlingÅrsaker;
    }

    public void setFagsak(FagsakDto fagsak) {
        this.fagsak = fagsak;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setType(BehandlingTypeDto type) {
        this.type = type;
    }

    public void setOpprettet(LocalDateTime opprettet) {
        this.opprettet = opprettet;
    }

    public void setAvsluttet(LocalDateTime avsluttet) {
        this.avsluttet = avsluttet;
    }

    public void setBehandlingsresultat(BehandlingsresultatDto behandlingsresultat) {
        this.behandlingsresultat = behandlingsresultat;
    }

    public void setStatus(BehandlingStatusDto status) {
        this.status = status;
    }

    public void setBehandlendeEnhetId(String behandlendeEnhetId) {
        this.behandlendeEnhetId = behandlendeEnhetId;
    }

    public void setSpråkkode(SpråkkodeDto språkkode) {
        this.språkkode = språkkode;
    }

    public void setToTrinnsBehandling(boolean toTrinnsBehandling) {
        this.toTrinnsBehandling = toTrinnsBehandling;
    }

    public void setVilkår(List<VilkårTypeDto> vilkår) {
        this.vilkår = vilkår;
    }

    public VergeDto getVerge() {
        return verge;
    }

    public void setVerge(VergeDto verge) {
        this.verge = verge;
    }
}
