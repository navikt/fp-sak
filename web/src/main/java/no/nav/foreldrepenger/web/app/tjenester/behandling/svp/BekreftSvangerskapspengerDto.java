package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_SVP_TILRETTELEGGING_KODE)
public class BekreftSvangerskapspengerDto extends BekreftetAksjonspunktDto {


    private LocalDate termindato;

    private LocalDate fødselsdato;
    @Size(min = 1, max = 1000)
    private List<@Valid SvpArbeidsforholdDto> bekreftetSvpArbeidsforholdList;

    public BekreftSvangerskapspengerDto() {
        //For Jackson
    }

    public BekreftSvangerskapspengerDto(String begrunnelse) {
        super(begrunnelse);

    }


    public LocalDate getTermindato() {
        return termindato;
    }

    public void setTermindato(LocalDate termindato) {
        this.termindato = termindato;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public void setFødselsdato(LocalDate fødselsdato) {
        this.fødselsdato = fødselsdato;
    }

    public List<SvpArbeidsforholdDto> getBekreftetSvpArbeidsforholdList() {
        if (bekreftetSvpArbeidsforholdList == null) {
            return Collections.emptyList();
        }
        return bekreftetSvpArbeidsforholdList;
    }

    public void setBekreftetSvpArbeidsforholdList(List<SvpArbeidsforholdDto> bekreftetSvpArbeidsforholdList) {
        this.bekreftetSvpArbeidsforholdList = bekreftetSvpArbeidsforholdList;
    }
}
