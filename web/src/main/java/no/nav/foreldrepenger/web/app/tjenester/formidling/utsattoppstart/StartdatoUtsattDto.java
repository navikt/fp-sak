package no.nav.foreldrepenger.web.app.tjenester.formidling.utsattoppstart;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.time.LocalDate;
import java.util.Optional;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_ABSENT, content = JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = NONE, getterVisibility = NONE, setterVisibility = NONE, isGetterVisibility = NONE, creatorVisibility = NONE)
public class StartdatoUtsattDto {

    @JsonProperty("utsettelseFraStart")
    @Valid
    private Boolean utsettelseFraStart;

    @JsonProperty(value = "nyStartDato")
    @Valid
    private LocalDate nyStartDato;

    public StartdatoUtsattDto(@Valid Boolean utsettelseFraStart,
                              @Valid LocalDate nyStartDato) {
        this.utsettelseFraStart = utsettelseFraStart;
        this.nyStartDato = nyStartDato;
    }

    public Boolean getUtsettelseFraStart() {
        return utsettelseFraStart;
    }

    public LocalDate getNyStartDato() {
        return nyStartDato;
    }

    public Optional<LocalDate> getNyStartDatoOptional() {
        return Optional.ofNullable(nyStartDato);
    }
}
