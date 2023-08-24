package no.nav.foreldrepenger.web.app.tjenester.behandling.oppdrag;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;

import java.time.LocalDate;

public class RefusjonInfo156Dto extends SporingDto {

    private LocalDate maksDato;
    private String refunderesId;
    private LocalDate datoFom;

    public RefusjonInfo156Dto(Refusjonsinfo156 entitet) {
        super(entitet, 0L, entitet.getId());
    }

    public LocalDate getMaksDato() {
        return maksDato;
    }

    public void setMaksDato(LocalDate maksDato) {
        this.maksDato = maksDato;
    }

    public String getRefunderesId() {
        return refunderesId;
    }

    public void setRefunderesId(String refunderesId) {
        this.refunderesId = refunderesId;
    }

    public LocalDate getDatoFom() {
        return datoFom;
    }

    public void setDatoFom(LocalDate datoFom) {
        this.datoFom = datoFom;
    }

    public static RefusjonInfo156Dto fraDomene(Refusjonsinfo156 refusjonsInfo156) {
        var refusjonInfo156Dto = new RefusjonInfo156Dto(refusjonsInfo156);
        refusjonInfo156Dto.maksDato = refusjonsInfo156.getMaksDato();
        refusjonInfo156Dto.refunderesId = refusjonsInfo156.getRefunderesId();
        refusjonInfo156Dto.datoFom = refusjonsInfo156.getDatoFom();
        return refusjonInfo156Dto;
    }
}
