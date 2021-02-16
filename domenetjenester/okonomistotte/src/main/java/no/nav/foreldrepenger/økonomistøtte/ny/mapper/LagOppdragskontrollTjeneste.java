package no.nav.foreldrepenger.økonomistøtte.ny.mapper;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;

class LagOppdragskontrollTjeneste {

    static Oppdragskontroll lagOppdragskontroll(Input input) {
        return Oppdragskontroll.builder()
            .medSaksnummer(input.getSaksnummer())
            .medBehandlingId(input.getBehandlingId())
            .medVenterKvittering(Boolean.TRUE)
            .medProsessTaskId(input.getProsessTaskId())
            .build();
    }
}
