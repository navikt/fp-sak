package no.nav.foreldrepenger.web.app.tjenester.behandling.innsyn.aksjonspunkt;

import jakarta.validation.constraints.Digits;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.domene.typer.JournalpostId;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
public class VurderInnsynDokumentDto {

    @JsonProperty("fikkInnsyn") @NotNull
    private boolean fikkInnsyn;

    @JsonProperty("journalpostId") @NotNull
    @Digits(integer = 18, fraction = 0)
    private String journalpostId;

    @JsonProperty("dokumentId") @NotNull
    @Digits(integer = 18, fraction = 0)
    private String dokumentId;

    public VurderInnsynDokumentDto() {
    }

    public VurderInnsynDokumentDto(boolean fikkInnsyn, String journalpostId, String dokumentId) {
        this.fikkInnsyn = fikkInnsyn;
        this.journalpostId = journalpostId;
        this.dokumentId = dokumentId;
    }

    public boolean isFikkInnsyn() {
        return fikkInnsyn;
    }

    public JournalpostId getJournalpostId() {
        return new JournalpostId(journalpostId);
    }

    public String getDokumentId() {
        return dokumentId;
    }
}
