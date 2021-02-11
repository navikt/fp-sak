package no.nav.foreldrepenger.økonomistøtte.ny.mapper;

import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

class LagOppdragskontrollTjeneste {

    static Oppdragskontroll lagOppdragskontroll(Input input) {
        Saksnummer saksnummer = input.getSaksnummer();
        Long behandlingId = input.getBehandlingId();

        Optional<Oppdragskontroll> oppdragskontrollForBehandlingOpt = input.getTidligereOppdrag().stream()
            .filter(oppdragskontroll -> behandlingId.equals(oppdragskontroll.getBehandlingId()))
            .findFirst();
        if (oppdragskontrollForBehandlingOpt.isPresent()) {
            Oppdragskontroll oppdragskontroll = oppdragskontrollForBehandlingOpt.get();
            oppdragskontroll.setVenterKvittering(Boolean.TRUE);
            return oppdragskontroll;
        }
        return Oppdragskontroll.builder()
            .medSaksnummer(saksnummer)
            .medBehandlingId(behandlingId)
            .medVenterKvittering(Boolean.TRUE)
            .medProsessTaskId(input.getProsessTaskId())
            .build();
    }
}
