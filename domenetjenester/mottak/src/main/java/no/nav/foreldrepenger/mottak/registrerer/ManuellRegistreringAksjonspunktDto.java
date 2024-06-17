package no.nav.foreldrepenger.mottak.registrerer;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;

public class ManuellRegistreringAksjonspunktDto {

    private boolean erFullstendigSøknad;
    private String søknadsXml;
    private DokumentTypeId dokumentTypeId;
    private LocalDate mottattDato;
    private boolean erRegistrertVerge;

    public ManuellRegistreringAksjonspunktDto(boolean erFullstendigSøknad) {
        this.erFullstendigSøknad = erFullstendigSøknad;
    }

    public ManuellRegistreringAksjonspunktDto(boolean erFullstendigSøknad,
                                              String søknadsXml,
                                              DokumentTypeId dokumentTypeId,
                                              LocalDate mottattDato,
                                              boolean erRegistrertVerge) {
        this.erFullstendigSøknad = erFullstendigSøknad;
        this.søknadsXml = søknadsXml;
        this.dokumentTypeId = dokumentTypeId;
        this.mottattDato = mottattDato;
        this.erRegistrertVerge = erRegistrertVerge;
    }

    public boolean getErFullstendigSøknad() {
        return erFullstendigSøknad;
    }

    public String getSøknadsXml() {
        return søknadsXml;
    }

    public DokumentTypeId getDokumentTypeId() {
        return dokumentTypeId != null ? dokumentTypeId : DokumentTypeId.UDEFINERT;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public boolean getErRegistrertVerge() {
        return erRegistrertVerge;
    }
}
