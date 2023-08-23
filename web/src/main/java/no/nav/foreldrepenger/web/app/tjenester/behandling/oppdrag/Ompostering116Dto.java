package no.nav.foreldrepenger.web.app.tjenester.behandling.oppdrag;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Ompostering116;

import java.time.LocalDate;

public class Ompostering116Dto extends SporingDto {
    private Boolean omPostering;
    private LocalDate datoOmposterFom;
    private String tidspktReg;

    public Ompostering116Dto(Ompostering116 entitet) {
        super(entitet, null, entitet.getId());
    }

    public Boolean getOmPostering() {
        return omPostering;
    }

    public LocalDate getDatoOmposterFom() {
        return datoOmposterFom;
    }

    public String getTidspktReg() {
        return tidspktReg;
    }

    public static Ompostering116Dto fraDomene(Ompostering116 ompostering116) {
        var dto = new Ompostering116Dto(ompostering116);
        dto.omPostering = ompostering116.getOmPostering();
        dto.datoOmposterFom = ompostering116.getDatoOmposterFom();
        dto.tidspktReg = ompostering116.getTidspktReg();
        return dto;
    }

}
