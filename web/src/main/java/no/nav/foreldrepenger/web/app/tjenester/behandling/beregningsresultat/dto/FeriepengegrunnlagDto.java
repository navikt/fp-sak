package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_ABSENT, content = JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = NONE, getterVisibility = NONE, setterVisibility = NONE, isGetterVisibility = NONE, creatorVisibility = NONE)
public class FeriepengegrunnlagDto {

    @JsonProperty(value = "feriepengeperiodeFom")
    @NotNull
    @Valid
    private LocalDate feriepengeperiodeFom;

    @JsonProperty(value = "feriepengeperiodeTom")
    @NotNull
    @Valid
    private LocalDate feriepengeperiodeTom;

    @JsonProperty(value = "andeler")
    @NotNull
    @Valid
    @Size(max = 100)
    private List<FeriepengegrunnlagAndelDto> andeler = new ArrayList<>();

    private FeriepengegrunnlagDto() {

    }

    public LocalDate getFeriepengeperiodeFom() {
        return feriepengeperiodeFom;
    }

    public LocalDate getFeriepengeperiodeTom() {
        return feriepengeperiodeTom;
    }

    public List<FeriepengegrunnlagAndelDto> getAndeler() {
        return andeler;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private FeriepengegrunnlagDto kladd;

        public Builder() {
            kladd = new FeriepengegrunnlagDto();
        }

        public Builder medFeriepengeperiodeFom(LocalDate feriepengeperiodeFom) {
            kladd.feriepengeperiodeFom = feriepengeperiodeFom;
            return this;
        }

        public Builder medFeriepengeperiodeTom(LocalDate feriepengeperiodeTom) {
            kladd.feriepengeperiodeTom = feriepengeperiodeTom;
            return this;
        }

        public Builder leggTilAndel(FeriepengegrunnlagAndelDto andel) {
            kladd.andeler.add(andel);
            return this;
        }

        public FeriepengegrunnlagDto build() {
            validerTilstand();
            return kladd;
        }

        private void validerTilstand() {
            Objects.requireNonNull(kladd.feriepengeperiodeFom, "feriepengeperiodeFom");
            Objects.requireNonNull(kladd.feriepengeperiodeTom, "feriepengeperiodeTom");
        }

    }

}
