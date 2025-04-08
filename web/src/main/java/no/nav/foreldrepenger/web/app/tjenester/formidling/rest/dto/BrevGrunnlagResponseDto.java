package no.nav.foreldrepenger.web.app.tjenester.formidling.rest.dto;

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

public class BrevGrunnlagResponseDto {

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
    private BehandlingsresultatDto behandlingsresultat; //
    private FagsakDto fagsak;
    private VergeDto verge;

    private List<BehandlingÅrsakType> behandlingÅrsaker; //
    private List<VilkårType> vilkår;
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

    public FagsakDto getFagsak() {
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

    public BehandlingsresultatDto getBehandlingsresultat() {
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

    public void leggTil(ResourceLink link) {
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

    public void setBehandlingÅrsaker(List<BehandlingÅrsakType> behandlingÅrsaker) {
        this.behandlingÅrsaker = behandlingÅrsaker;
    }

    public void setFagsak(FagsakDto fagsak) {
        this.fagsak = fagsak;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setType(BehandlingType type) {
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

    public void setStatus(BehandlingStatus status) {
        this.status = status;
    }

    public void setBehandlendeEnhetId(String behandlendeEnhetId) {
        this.behandlendeEnhetId = behandlendeEnhetId;
    }

    public void setSpråkkode(Språkkode språkkode) {
        this.språkkode = språkkode;
    }

    public void setToTrinnsBehandling(boolean toTrinnsBehandling) {
        this.toTrinnsBehandling = toTrinnsBehandling;
    }

    public void setVilkår(List<VilkårType> vilkår) {
        this.vilkår = vilkår;
    }

    public VergeDto getVerge() {
        return verge;
    }

    public void setVerge(VergeDto verge) {
        this.verge = verge;
    }
}
