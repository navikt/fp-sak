package no.nav.foreldrepenger.web.app.tjenester.dokument.dto;

import jakarta.validation.constraints.Digits;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
public class JournalpostIdDto {

    @JsonProperty("journalpostId")
    @Digits(integer = 18, fraction = 0)
    private String journalpostId;

    public JournalpostIdDto(String journalpostId) {
        this.journalpostId = journalpostId;
    }

    public String getJournalpostId() {
        return journalpostId;
    }
}
