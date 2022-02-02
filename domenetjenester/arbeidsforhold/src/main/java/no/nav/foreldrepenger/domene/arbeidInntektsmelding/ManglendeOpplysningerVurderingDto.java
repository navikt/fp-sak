package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.UUID;

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
    @Pattern(regexp = "\\d{9}|\\d{13}")
    private String arbeidsgiverIdent;
    @JsonProperty("internArbeidsforholdRef")
    @Size(max = 100)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String internArbeidsforholdRef;

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
                                             @Pattern(regexp = "\\d{9}|\\d{13}")
                                             @JsonProperty("arbeidsgiverIdent")
                                                 String arbeidsgiverIdent,
                                             @Size(max = 100)
                                             @Pattern(regexp = InputValideringRegex.FRITEKST)
                                             @JsonProperty("internArbeidsforholdRef")
                                                 String internArbeidsforholdRef) {
        this.behandlingUuid = behandlingUuid;
        this.vurdering = vurdering;
        this.begrunnelse = begrunnelse;
        this.arbeidsgiverIdent = arbeidsgiverIdent;
        this.internArbeidsforholdRef = internArbeidsforholdRef;
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
}
