package no.nav.foreldrepenger.økonomistøtte.ny.mapper;

import java.util.List;

import javax.enterprise.context.Dependent;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Oppdrag;
import no.nav.foreldrepenger.økonomistøtte.ny.tjeneste.EndringsdatoTjeneste;
import no.nav.foreldrepenger.økonomistøtte.ny.tjeneste.OppdragFactory;

@Dependent
public class LagOppdragTjeneste {

    public LagOppdragTjeneste() { }

    public Oppdragskontroll lagOppdrag(OppdragInput input, boolean brukFellesEndringstidspunkt) {
        var målbilde = input.getTilkjentYtelse();
        var tidligereOppdrag = input.getTidligereOppdrag();

        var oppdragFactory = new OppdragFactory(FagområdeMapper::tilFagområde, input.getYtelseType(), input.getSaksnummer());

        if (brukFellesEndringstidspunkt) {
            oppdragFactory.setFellesEndringstidspunkt(EndringsdatoTjeneste.normal().finnTidligsteEndringsdato(målbilde, tidligereOppdrag));
        }
        var oppdragene = oppdragFactory.lagOppdrag(tidligereOppdrag, målbilde);
        if (oppdragene.isEmpty()) {
            return null;
        }
        var oppdragskontroll = LagOppdragskontrollTjeneste.lagOppdragskontroll(input);
        var oppdragMapper = new OppdragMapper(input.getBrukerFnr(), tidligereOppdrag, input);
        for (var oppdrag : oppdragene) {
            oppdragMapper.mapTilOppdrag110(oppdrag, oppdragskontroll);
        }
        return oppdragskontroll;
    }

    public static List<Oppdrag> lagOppdrag(OppdragInput input) {
        var målbilde = input.getTilkjentYtelse();
        var tidligereOppdrag = input.getTidligereOppdrag();

        var oppdragFactory = new OppdragFactory(FagområdeMapper::tilFagområde, input.getYtelseType(), input.getSaksnummer());
        return oppdragFactory.lagOppdrag(tidligereOppdrag, målbilde);
    }


}
