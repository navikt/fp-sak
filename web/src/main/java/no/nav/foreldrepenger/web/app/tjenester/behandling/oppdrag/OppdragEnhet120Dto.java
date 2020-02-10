package no.nav.foreldrepenger.web.app.tjenester.behandling.oppdrag;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragsenhet120;

class OppdragEnhet120Dto extends SporingDto {

    private String typeEnhet;
    private String enhet;
    private LocalDate datoEnhetFom;

    public OppdragEnhet120Dto(Oppdragsenhet120 entitet) {
        super(entitet, entitet.getVersjon(), entitet.getId());
    }

    public String getTypeEnhet() {
        return typeEnhet;
    }

    public String getEnhet() {
        return enhet;
    }

    public LocalDate getDatoEnhetFom() {
        return datoEnhetFom;
    }

    public static OppdragEnhet120Dto fraDomene(Oppdragsenhet120 entitet) {
        OppdragEnhet120Dto dto = new OppdragEnhet120Dto(entitet);
        dto.typeEnhet = entitet.getTypeEnhet();
        dto.enhet = entitet.getEnhet();
        dto.datoEnhetFom = entitet.getDatoEnhetFom();
        return dto;
    }
}
