package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.familiehendelse.rest.SøknadType;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SoknadAdopsjonDto.class),
    @JsonSubTypes.Type(value = SoknadFodselDto.class)
})
public abstract class SoknadDto {

    @NotNull private SøknadType soknadType;
    @NotNull private LocalDate mottattDato;
    private String begrunnelseForSenInnsending;
    @NotNull private Integer antallBarn;
    @NotNull private OppgittTilknytningDto oppgittTilknytning;
    @NotNull private List<ManglendeVedleggDto> manglendeVedlegg;
    @NotNull private OppgittFordelingDto oppgittFordeling;
    @NotNull private SøknadsfristDto søknadsfrist;

    protected SoknadDto() {
    }

    public SøknadType getSoknadType() {
        return soknadType;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public boolean erSoknadsType(SøknadType søknadType) {
        return søknadType.equals(this.soknadType);
    }

    public String getBegrunnelseForSenInnsending() {
        return begrunnelseForSenInnsending;
    }

    public Integer getAntallBarn() {
        return antallBarn;
    }

    public OppgittTilknytningDto getOppgittTilknytning() {
        return oppgittTilknytning;
    }

    public OppgittFordelingDto getOppgittFordeling() {
        return oppgittFordeling;
    }

    public void setSoknadType(SøknadType soknadType) {
        this.soknadType = soknadType;
    }

    public void setMottattDato(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
    }

    public void setBegrunnelseForSenInnsending(String begrunnelseForSenInnsending) {
        this.begrunnelseForSenInnsending = begrunnelseForSenInnsending;
    }

    public void setAntallBarn(Integer antallBarn) {
        this.antallBarn = antallBarn;
    }

    public void setOppgittTilknytning(OppgittTilknytningDto oppgittTilknytning) {
        this.oppgittTilknytning = oppgittTilknytning;
    }

    public List<ManglendeVedleggDto> getManglendeVedlegg() {
        return manglendeVedlegg;
    }

    public void setManglendeVedlegg(List<ManglendeVedleggDto> manglendeVedlegg) {
        this.manglendeVedlegg = manglendeVedlegg;
    }

    public void setOppgittFordeling(OppgittFordelingDto oppgittFordeling) {
        this.oppgittFordeling = oppgittFordeling;
    }

    public SøknadsfristDto getSøknadsfrist() {
        return søknadsfrist;
    }

    public void setSøknadsfrist(SøknadsfristDto søknadsfrist) {
        this.søknadsfrist = søknadsfrist;
    }
}
