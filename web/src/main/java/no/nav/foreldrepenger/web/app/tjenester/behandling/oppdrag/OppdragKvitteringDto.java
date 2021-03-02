package no.nav.foreldrepenger.web.app.tjenester.behandling.oppdrag;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragKvittering;

public class OppdragKvitteringDto extends SporingDto {

    private String alvorlighetsgrad;
    private String beskrMelding;

    public OppdragKvitteringDto(OppdragKvittering entitet) {
        super(entitet, 0L, entitet.getId());
    }

    public String getAlvorlighetsgrad() {
        return alvorlighetsgrad;
    }

    public String getBeskrMelding() {
        return beskrMelding;
    }

    public static OppdragKvitteringDto fraDomene(OppdragKvittering oppdragKvittering) {
        OppdragKvitteringDto dto = new OppdragKvitteringDto(oppdragKvittering);
        dto.alvorlighetsgrad = oppdragKvittering.getAlvorlighetsgrad();
        dto.beskrMelding = oppdragKvittering.getBeskrMelding();
        return dto;
    }

}
