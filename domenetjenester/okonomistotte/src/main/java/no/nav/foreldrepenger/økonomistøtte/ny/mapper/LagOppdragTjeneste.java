package no.nav.foreldrepenger.økonomistøtte.ny.mapper;

import java.util.List;

import javax.enterprise.context.Dependent;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Oppdrag;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.OverordnetOppdragKjedeOversikt;
import no.nav.foreldrepenger.økonomistøtte.ny.tjeneste.EndringsdatoTjeneste;
import no.nav.foreldrepenger.økonomistøtte.ny.tjeneste.OppdragFactory;

@Dependent
public class LagOppdragTjeneste {

    public LagOppdragTjeneste() { }

    public Oppdragskontroll lagOppdrag(Input input, boolean brukFellesEndringstidspunkt) {
        GruppertYtelse målbilde = input.getTilkjentYtelse();
        OverordnetOppdragKjedeOversikt tidligereOppdrag = input.getTidligereOppdrag();

        OppdragFactory oppdragFactory = new OppdragFactory(ØkonomiFagområdeMapper::tilFagområde, input.getYtelseType(), input.getSaksnummer());

        if (brukFellesEndringstidspunkt) {
            oppdragFactory.setFellesEndringstidspunkt(EndringsdatoTjeneste.normal().finnTidligsteEndringsdato(målbilde, tidligereOppdrag));
        }
        List<Oppdrag> oppdragene = oppdragFactory.lagOppdrag(tidligereOppdrag, målbilde);
        if (oppdragene.isEmpty()) {
            return null;
        }
        Oppdragskontroll oppdragskontroll = LagOppdragskontrollTjeneste.lagOppdragskontroll(input);
        OppdragMapper oppdragMapper = new OppdragMapper(input.getBrukerFnr(), tidligereOppdrag, input);
        for (Oppdrag oppdrag : oppdragene) {
            oppdragMapper.mapTilOppdrag110(oppdrag, oppdragskontroll);
        }
        return oppdragskontroll;
    }

    public static List<Oppdrag> lagOppdrag(Input input) {
        GruppertYtelse målbilde = input.getTilkjentYtelse();
        OverordnetOppdragKjedeOversikt tidligereOppdrag = input.getTidligereOppdrag();

        OppdragFactory oppdragFactory = new OppdragFactory(ØkonomiFagområdeMapper::tilFagområde, input.getYtelseType(), input.getSaksnummer());
        return oppdragFactory.lagOppdrag(tidligereOppdrag, målbilde);
    }


}
