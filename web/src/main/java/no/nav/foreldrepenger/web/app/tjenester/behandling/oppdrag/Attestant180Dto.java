package no.nav.foreldrepenger.web.app.tjenester.behandling.oppdrag;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Attestant180;

public class Attestant180Dto extends SporingDto {

    private String attestantId;

    public String getAttestantId() {
        return attestantId;
    }

    public Attestant180Dto(Attestant180 entitet) {
        super(entitet, entitet.getVersjon(), entitet.getId());
    }

    public void setAttestantId(String attestantId) {
        this.attestantId = attestantId;
    }

    public static Attestant180Dto fraDomene(Attestant180 attestant180) {
        Attestant180Dto attestant180Dto = new Attestant180Dto(attestant180);
        attestant180Dto.attestantId = attestant180.getAttestantId();
        return attestant180Dto;
    }
}
