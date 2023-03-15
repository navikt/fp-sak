package no.nav.foreldrepenger.web.app.tjenester.behandling.oppdrag;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;

public class OppdragDto extends SporingDto {

    private String saksnummer;

    private Long behandlingId;

    private boolean venterKvittering;

    private List<Oppdrag110Dto> oppdrag110;

    public OppdragDto(Oppdragskontroll entitet) {
        super(entitet, entitet.getVersjon(), entitet.getId());
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public List<Oppdrag110Dto> getOppdrag110() {
        return oppdrag110;
    }

    public void setOppdrag110(List<Oppdrag110Dto> oppdrag110) {
        this.oppdrag110 = oppdrag110;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public OppdragDto setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
        return this;
    }

    public boolean getVenterKvittering() {
        return venterKvittering;
    }

    public static OppdragDto fraDomene(Oppdragskontroll oppdragskontroll) {
        var oppdragDto = new OppdragDto(oppdragskontroll);
        oppdragDto.saksnummer = oppdragskontroll.getSaksnummer().getVerdi();
        oppdragDto.behandlingId = oppdragskontroll.getBehandlingId();
        oppdragDto.oppdrag110 = oppdragskontroll.getOppdrag110Liste().stream().map(Oppdrag110Dto::fraDomene).toList();
        oppdragDto.venterKvittering = oppdragskontroll.getVenterKvittering();
        return oppdragDto;
    }
}
