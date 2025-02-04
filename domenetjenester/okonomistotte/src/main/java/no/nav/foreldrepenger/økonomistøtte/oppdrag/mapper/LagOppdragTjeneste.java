package no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Oppdrag;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.tjeneste.EndringsdatoTjeneste;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.tjeneste.OppdragFactory;

@Dependent
public class LagOppdragTjeneste {

    public static Optional<Oppdragskontroll> lagOppdrag(OppdragInput input, boolean brukFellesEndringstidspunkt, final Oppdragskontroll eksisterendeOppdragskontroll) {
        var målbilde = input.getTilkjentYtelse();
        var tidligereOppdrag = input.getTidligereOppdrag();

        var oppdragFactory = new OppdragFactory(FagområdeMapper::tilFagområde, input.getYtelseType(), input.getSaksnummer());

        if (brukFellesEndringstidspunkt) {
            oppdragFactory.setFellesEndringstidspunkt(EndringsdatoTjeneste.normal().finnTidligsteEndringsdato(målbilde, tidligereOppdrag));
        }
        var oppdragene = oppdragFactory.lagOppdrag(tidligereOppdrag, målbilde);
        if (oppdragene.isEmpty()) {
            return Optional.empty();
        }
        var oppdragskontroll = LagOppdragskontrollTjeneste.hentEllerOpprettOppdragskontroll(input, eksisterendeOppdragskontroll);
        var oppdragMapper = new OppdragMapper(input.getBrukerFnr(), tidligereOppdrag, input);
        for (var oppdrag : oppdragene) {
            oppdragMapper.mapTilOppdrag110(oppdrag, oppdragskontroll);
        }
        return Optional.of(oppdragskontroll);
    }

    static List<Oppdrag> lagOppdrag(OppdragInput input) {
        var målbilde = input.getTilkjentYtelse();
        var tidligereOppdrag = input.getTidligereOppdrag();

        var oppdragFactory = new OppdragFactory(FagområdeMapper::tilFagområde, input.getYtelseType(), input.getSaksnummer());
        return oppdragFactory.lagOppdrag(tidligereOppdrag, målbilde);
    }
}
