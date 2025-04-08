package no.nav.foreldrepenger.web.app.tjenester.formidling.rest.dto;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.SkjæringstidspunktDto;
import no.nav.foreldrepenger.web.app.tjenester.formidling.rest.kodeverk.KonsekvensForYtelsen;

public class BehandlingsresultatDto {

    private BehandlingResultatType type;
    private Avslagsårsak avslagsarsak;
    private String avslagsarsakFritekst;
    private List<KonsekvensForYtelsen> konsekvenserForYtelsen;
    private String overskrift;
    private String fritekstbrev;
    private SkjæringstidspunktDto skjæringstidspunkt;
    private boolean endretDekningsgrad;
    private LocalDate opphørsdato;

    public BehandlingsresultatDto() {
        // trengs for deserialisering av JSON
    }

    public void setType(BehandlingResultatType type) {
        this.type = type;
    }

    public void setAvslagsarsak(Avslagsårsak avslagsarsak) {
        this.avslagsarsak = avslagsarsak;
    }

    public void setAvslagsarsakFritekst(String avslagsarsakFritekst) {
        this.avslagsarsakFritekst = avslagsarsakFritekst;
    }

    public void setKonsekvenserForYtelsen(List<KonsekvensForYtelsen> konsekvenserForYtelsen) {
        this.konsekvenserForYtelsen = konsekvenserForYtelsen;
    }

    public BehandlingResultatType getType() {
        return type;
    }

    public Avslagsårsak getAvslagsarsak() {
        return avslagsarsak;
    }

    public String getAvslagsarsakFritekst() {
        return avslagsarsakFritekst;
    }

    public List<KonsekvensForYtelsen> getKonsekvenserForYtelsen() {
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
