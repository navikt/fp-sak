package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

public class ManglendeOpplysningerVurderingDto {
    @JsonProperty("behandlingUuid")
    @Valid
    @NotNull
    private UUID behandlingUuid;
    @JsonProperty("vurdering")
    @ValidKodeverk
    private ArbeidsforholdKomplettVurderingType vurdering;
    @JsonProperty("begrunnelse")
    @Size(max = 100000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String begrunnelse;
    @JsonProperty("arbeidsgiverIdent")
    @NotNull
    @Pattern(regexp = InputValideringRegex.ARBEIDSGIVER)
    private String arbeidsgiverIdent;
    @JsonProperty("internArbeidsforholdRef")
    @Size(max = 100)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String internArbeidsforholdRef;
    // Gjør denne @NotNull når frontend sender den med
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingVersjon;

    public ManglendeOpplysningerVurderingDto() {
    }

    public ManglendeOpplysningerVurderingDto(@Valid
                                             @NotNull
                                             @JsonProperty("behandlingUuid")
                                                 UUID behandlingUuid,
                                             @ValidKodeverk
                                             @JsonProperty("vurdering")
                                                 ArbeidsforholdKomplettVurderingType vurdering,
                                             @Size(max = 100000)
                                             @Pattern(regexp = InputValideringRegex.FRITEKST)
                                             @JsonProperty("begrunnelse")
                                                 String begrunnelse,
                                             @NotNull
                                             @Pattern(regexp = InputValideringRegex.ARBEIDSGIVER)
                                             @JsonProperty("arbeidsgiverIdent")
                                                 String arbeidsgiverIdent,
                                             @Size(max = 100)
                                             @Pattern(regexp = InputValideringRegex.FRITEKST)
                                             @JsonProperty("internArbeidsforholdRef")
                                                 String internArbeidsforholdRef,
                                             Long behandlingVersjon) {
        this.behandlingUuid = behandlingUuid;
        this.vurdering = vurdering;
        this.begrunnelse = begrunnelse;
        this.arbeidsgiverIdent = arbeidsgiverIdent;
        this.internArbeidsforholdRef = internArbeidsforholdRef;
        this.behandlingVersjon = behandlingVersjon;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public ArbeidsforholdKomplettVurderingType getVurdering() {
        return vurdering;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getArbeidsgiverIdent() {
        return arbeidsgiverIdent;
    }

    public String getInternArbeidsforholdRef() {
        return internArbeidsforholdRef;
    }

    public Long getBehandlingVersjon() {
        return behandlingVersjon;
    }
}
