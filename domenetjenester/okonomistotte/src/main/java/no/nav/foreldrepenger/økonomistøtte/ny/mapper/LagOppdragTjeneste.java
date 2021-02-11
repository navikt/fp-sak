package no.nav.foreldrepenger.økonomistøtte.ny.mapper;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.domene.person.pdl.AktørTjeneste;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Oppdrag;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.OverordnetOppdragKjedeOversikt;
import no.nav.foreldrepenger.økonomistøtte.ny.tjeneste.EndringsdatoTjeneste;
import no.nav.foreldrepenger.økonomistøtte.ny.tjeneste.OppdragFactory;
import no.nav.foreldrepenger.økonomistøtte.ØkonomioppdragRepository;

@Dependent
public class LagOppdragTjeneste {

    public LagOppdragTjeneste() { }

    public Oppdragskontroll lagOppdrag(Input input, boolean brukFellesEndringstidspunkt) {
        GruppertYtelse målbilde = mapTilkjentYtelse(input);
        OverordnetOppdragKjedeOversikt tidligereOppdrag = mapTidligereOppdrag(input.getTidligereOppdrag());

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

    public static List<Oppdrag> lagOppdrag(List<Oppdragskontroll> tidligereOppdragskontroll, Input input) {
        GruppertYtelse målbilde = mapTilkjentYtelse(input);
        OverordnetOppdragKjedeOversikt tidligereOppdrag = mapTidligereOppdrag(tidligereOppdragskontroll);

        OppdragFactory oppdragFactory = new OppdragFactory(ØkonomiFagområdeMapper::tilFagområde, input.getYtelseType(), input.getSaksnummer());
        return oppdragFactory.lagOppdrag(tidligereOppdrag, målbilde);
    }

    private static OverordnetOppdragKjedeOversikt mapTidligereOppdrag(List<Oppdragskontroll> tidligereOppdragskontroll) {
        return new OverordnetOppdragKjedeOversikt(EksisterendeOppdragMapper.tilKjeder(tidligereOppdragskontroll));
    }

    private static GruppertYtelse mapTilkjentYtelse(Input input) {
        TilkjentYtelseMapper tilkjentYtelseMapper = TilkjentYtelseMapper.lagFor(input.getFamilieYtelseType());
        return tilkjentYtelseMapper.fordelPåNøkler(input.getTilkjentYtelse());
    }

}
