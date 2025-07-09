package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.Rettighetstype;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;

/**
 * Dataobjekt som skal brukes som respons ut mot fp-formidling.
 * Skal populere formidlingRessurser med formidling-spesifikke ressurslenker.
 */
public class BehandlingFormidlingDto extends BehandlingDto {

    /**
     * REST HATEOAS - pekere på resttjenester som skal brukes for å populere brev.
     * Skal erstatte links feltet på BehandlingDto for brev.
     *
     * @see https://restfulapi.net/hateoas/
     */
    @JsonProperty("formidlingRessurser")
    private List<ResourceLink> formidlingRessurser = new ArrayList<>();

    @JsonProperty("harAvklartAnnenForelderRett")
    private Boolean harAvklartAnnenForelderRett;

    @JsonProperty("originalBehandlingUuid")
    private UUID originalBehandlingUuid;

    @JsonProperty("medlemskapOpphørsårsak")
    private Avslagsårsak medlemskapOpphørsårsak;

    @JsonProperty("medlemskapFom")
    private LocalDate medlemskapFom;
    
    @JsonProperty("rettigheter")
    private Rettigheter rettigheter;

    public List<ResourceLink> getFormidlingRessurser() {
        return formidlingRessurser;
    }

    public void leggTilFormidlingRessurs(ResourceLink link) {
        this.formidlingRessurser.add(link);
    }

    public void setFormidlingRessurser(List<ResourceLink> formidlingRessurser) {
        this.formidlingRessurser = formidlingRessurser;
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

    public Rettigheter getRettigheter() {
        return rettigheter;
    }

    public void setRettigheter(Rettigheter rettigheter) {
        this.rettigheter = rettigheter;
    }

     public record Rettigheter(Rettighetstype opprinnelig,  //søknad eller forrige vedtak
                               Rettighetstype gjeldende) {
    }
}
