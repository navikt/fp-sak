package no.nav.foreldrepenger.web.app.tjenester.behandling.innsyn;

import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynResultatType;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.InnsynVedtaksdokumentasjonDto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InnsynsbehandlingDto {

	private LocalDate innsynMottattDato;
	private InnsynResultatType innsynResultatType;
	private List<InnsynVedtaksdokumentasjonDto> vedtaksdokumentasjon  = new ArrayList<>();
	private List<InnsynDokumentDto> dokumenter = new ArrayList<>();

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
