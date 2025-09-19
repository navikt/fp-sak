package no.nav.foreldrepenger.web.app.tjenester.behandling.innsyn;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynResultatType;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.InnsynVedtaksdokumentasjonDto;

public class InnsynsbehandlingDto {

	@NotNull private LocalDate innsynMottattDato;
	@NotNull private InnsynResultatType innsynResultatType;
	@NotNull private List<InnsynVedtaksdokumentasjonDto> vedtaksdokumentasjon  = new ArrayList<>();
	@NotNull private List<InnsynDokumentDto> dokumenter = new ArrayList<>();

	public LocalDate getInnsynMottattDato() {
		return innsynMottattDato;
	}

	public InnsynResultatType getInnsynResultatType() {
		return innsynResultatType;
	}

	public void setInnsynMottattDato(LocalDate innsynMottattDato) {
		this.innsynMottattDato = innsynMottattDato;
	}

	public void setInnsynResultatType(InnsynResultatType innsynResultatType) {
		this.innsynResultatType = innsynResultatType;
	}

	public void setVedtaksdokumentasjon(List<InnsynVedtaksdokumentasjonDto> vedtaksdokumentasjon) {
		this.vedtaksdokumentasjon = vedtaksdokumentasjon;
	}

	public List<InnsynVedtaksdokumentasjonDto> getVedtaksdokumentasjon() {
		return vedtaksdokumentasjon;
	}

	public void setDokumenter(List<InnsynDokumentDto> dokumenter) {
	    this.dokumenter = dokumenter;
    }

    public List<InnsynDokumentDto> getDokumenter() {
        return dokumenter;
    }
}
