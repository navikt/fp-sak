package no.nav.foreldrepenger.web.app.tjenester.behandling.innsyn.aksjonspunkt;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynResultatType;
import no.nav.foreldrepenger.validering.ValidKodeverk;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_INNSYN_KODE)
public class VurderInnsynDto extends BekreftetAksjonspunktDto {


    @JsonProperty("innsynResultatType")
    @NotNull
    @ValidKodeverk
    private InnsynResultatType innsynResultatType;

    @JsonProperty("mottattDato")
    @NotNull
    private LocalDate mottattDato;

    @JsonProperty("innsynDokumenter")
    @NotNull
    @Size(max = 1000)
    private List<@Valid VurderInnsynDokumentDto> innsynDokumenter;

    @JsonProperty("sattPaVent")
    private boolean sattPaVent;

    @JsonProperty("fristDato")
    private LocalDate fristDato;

    @SuppressWarnings("unused")
    private VurderInnsynDto() {
        super();
        // For Jackson
    }

    public VurderInnsynDto(String begrunnelse, InnsynResultatType innsynResultatType, LocalDate mottattDato,
                           boolean sattPaVent, List<VurderInnsynDokumentDto> innsynDokumenter, LocalDate fristDato) {
        super(begrunnelse);
        this.innsynResultatType = innsynResultatType;
        this.mottattDato = mottattDato;
        this.sattPaVent = sattPaVent;
        this.innsynDokumenter = innsynDokumenter;
        this.fristDato = fristDato;
    }



    public InnsynResultatType getInnsynResultatType() {
        return innsynResultatType;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public List<VurderInnsynDokumentDto> getInnsynDokumenter() {
        return innsynDokumenter;
    }

    public boolean isSattPaVent() {
        return sattPaVent;
    }

    public LocalDate getFristDato() {
        return fristDato;
    }
}
