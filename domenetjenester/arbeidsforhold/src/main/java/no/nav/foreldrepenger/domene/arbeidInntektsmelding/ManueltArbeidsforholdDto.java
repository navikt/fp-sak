package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import java.time.LocalDate;
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

public class ManueltArbeidsforholdDto {
    @JsonProperty("behandlingUuid")
    @Valid
    @NotNull
    private UUID behandlingUuid;
    @JsonProperty("begrunnelse")
    @Size(max = 2000)
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
    @JsonProperty("arbeidsgiverNavn")
    @Size(max = 200)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String arbeidsgiverNavn;
    @JsonProperty("fom")
    @Valid
    @NotNull
    private LocalDate fom;
    @JsonProperty("tom")
    @Valid
    private LocalDate tom;
    @JsonProperty("stillingsprosent")
    @Min(0)
    @Max(100)
    @Valid
    @NotNull
    private Integer stillingsprosent;
    @JsonProperty("vurdering")
    @ValidKodeverk
    @NotNull
    private ArbeidsforholdKomplettVurderingType vurdering;
    @JsonProperty("behandlingVersjon")
    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingVersjon;

    public ManueltArbeidsforholdDto() {
    }

    public ManueltArbeidsforholdDto(
            @JsonProperty("behandlingUuid")
            UUID behandlingUuid,
            @JsonProperty("begrunnelse")
            String begrunnelse,
            @JsonProperty("arbeidsgiverIdent")
            String arbeidsgiverIdent,
            @JsonProperty("internArbeidsforholdRef")
            String internArbeidsforholdRef,
            @JsonProperty("arbeidsgiverNavn")
            String arbeidsgiverNavn,
            @JsonProperty("fom")
            LocalDate fom,
            @JsonProperty("tom")
            LocalDate tom,
            @JsonProperty("stillingsprosent")
            Integer stillingsprosent,
            @JsonProperty("vurdering")
            ArbeidsforholdKomplettVurderingType vurdering,
            @NotNull @JsonProperty("behandlingVersjon")
            Long behandlingVersjon) {
        this.behandlingUuid = behandlingUuid;
        this.begrunnelse = begrunnelse;
        this.arbeidsgiverIdent = arbeidsgiverIdent;
        this.internArbeidsforholdRef = internArbeidsforholdRef;
        this.arbeidsgiverNavn = arbeidsgiverNavn;
        this.fom = fom;
        this.tom = tom;
        this.stillingsprosent = stillingsprosent;
        this.vurdering = vurdering;
        this.behandlingVersjon = behandlingVersjon;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getArbeidsgiverIdent() {
        return arbeidsgiverIdent;
    }

    public String getArbeidsgiverNavn() {
        return arbeidsgiverNavn;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public Integer getStillingsprosent() {
        return stillingsprosent;
    }

    public ArbeidsforholdKomplettVurderingType getVurdering() {
        return vurdering;
    }

    public String getInternArbeidsforholdRef() {
        return internArbeidsforholdRef;
    }

    public Long getBehandlingVersjon() {
        return behandlingVersjon;
    }
}
