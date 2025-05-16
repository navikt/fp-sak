package no.nav.foreldrepenger.web.app.tjenester.formidling.rest.dto;

import no.nav.foreldrepenger.web.app.tjenester.formidling.rest.kodeverk.AvslagÅrsakDto;
import no.nav.foreldrepenger.web.app.tjenester.formidling.rest.kodeverk.BehandlingResultatTypeDto;
import no.nav.foreldrepenger.web.app.tjenester.formidling.rest.kodeverk.KonsekvensForYtelsenDto;

import java.time.LocalDate;
import java.util.List;

public class BehandlingsresultatDto {

    private BehandlingResultatTypeDto type;
    private AvslagÅrsakDto avslagsarsak;
    private String avslagsarsakFritekst;
    private List<KonsekvensForYtelsenDto> konsekvenserForYtelsen;
    private String overskrift;
    private String fritekstbrev;
    private SkjæringstidspunktDto skjæringstidspunkt;
    private Boolean endretDekningsgrad;
    private LocalDate opphørsdato;

    public BehandlingsresultatDto() {
        // trengs for deserialisering av JSON
    }

    public void setType(BehandlingResultatTypeDto type) {
        this.type = type;
    }

    public void setAvslagsarsak(AvslagÅrsakDto avslagsarsak) {
        this.avslagsarsak = avslagsarsak;
    }

    public void setAvslagsarsakFritekst(String avslagsarsakFritekst) {
        this.avslagsarsakFritekst = avslagsarsakFritekst;
    }

    public void setKonsekvenserForYtelsen(List<KonsekvensForYtelsenDto> konsekvenserForYtelsen) {
        this.konsekvenserForYtelsen = konsekvenserForYtelsen;
    }

    public BehandlingResultatTypeDto getType() {
        return type;
    }

    public AvslagÅrsakDto getAvslagsarsak() {
        return avslagsarsak;
    }

    public String getAvslagsarsakFritekst() {
        return avslagsarsakFritekst;
    }

    public List<KonsekvensForYtelsenDto> getKonsekvenserForYtelsen() {
        return konsekvenserForYtelsen;
    }

    public String getOverskrift() {
        return overskrift;
    }

    public void setOverskrift(String overskrift) {
        this.overskrift = overskrift;
    }

    public String getFritekstbrev() {
        return fritekstbrev;
    }

    public void setFritekstbrev(String fritekstbrev) {
        this.fritekstbrev = fritekstbrev;
    }

    public SkjæringstidspunktDto getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public void setSkjæringstidspunkt(SkjæringstidspunktDto skjæringstidspunkt) {
        this.skjæringstidspunkt = skjæringstidspunkt;
    }

    public void setEndretDekningsgrad(Boolean endretDekningsgrad) {
        this.endretDekningsgrad = endretDekningsgrad;
    }

    public Boolean isEndretDekningsgrad() {
        return endretDekningsgrad;
    }

    public LocalDate getOpphørsdato() {
        return opphørsdato;
    }

    public void setOpphørsdato(LocalDate opphørsdato) {
        this.opphørsdato = opphørsdato;
    }
}
