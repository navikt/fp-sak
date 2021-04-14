package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.util.InputValideringRegex;

public class SøknadGrunnlagManglerDto implements AbacDto {

    private static final String DATO_PATTERN = "(\\d{4}-\\d{2}-\\d{2})";

    @NotNull
    @QueryParam("behandlingId")
    @DefaultValue("0")
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingId;

    @NotNull
    @Parameter(description = "Søknadsdato (YYYY-MM-DD)")
    @QueryParam("søknadDato")
    @Pattern(regexp = DATO_PATTERN)
    private String søknadDato;

    @NotNull
    @Parameter(description = "Mottatt dato (YYYY-MM-DD)")
    @QueryParam("mottattDato")
    @Pattern(regexp = DATO_PATTERN)
    private String mottattDato;

    @NotNull
    @Parameter(description = "Rolle (MORA/FARA)")
    @QueryParam("rolle")
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String rolle;

    @Parameter(description = "Tilleggsopplysninger")
    @QueryParam("tillegg")
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String tillegg;

    @Parameter(description = "Begrunnelse for sen innsending")
    @QueryParam("forsent")
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String forsent;

    public SøknadGrunnlagManglerDto(@NotNull Long behandlingId, @NotNull String søknadDato, @NotNull String mottattDato,
                                    @NotNull String rolle, String tillegg, String forsent) {
        this.behandlingId = behandlingId;
        this.søknadDato = søknadDato;
        this.mottattDato = mottattDato;
        this.rolle = rolle;
        this.tillegg = tillegg;
        this.forsent = forsent;
    }

    public SøknadGrunnlagManglerDto() {
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        var abac = AbacDataAttributter.opprett();
        if (behandlingId != null) {
            abac.leggTil(AppAbacAttributtType.BEHANDLING_ID, behandlingId);
        }
        return abac;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public LocalDate getSøknadDato() {
        return getLocalDate(søknadDato);
    }

    public LocalDate getMottattDato() {
        return getLocalDate(mottattDato);
    }

    public String getRolle() {
        return rolle;
    }

    public String getTillegg() {
        return tillegg;
    }

    public String getForsent() {
        return forsent;
    }

    private LocalDate getLocalDate(String datoString) {
        if (datoString != null) {
            return LocalDate.parse(datoString, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return null;
    }
}
