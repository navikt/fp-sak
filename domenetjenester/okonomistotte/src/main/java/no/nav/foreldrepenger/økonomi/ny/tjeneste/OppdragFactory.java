package no.nav.foreldrepenger.økonomi.ny.tjeneste;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomi.ny.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomi.ny.domene.FagsystemId;
import no.nav.foreldrepenger.økonomi.ny.domene.Oppdrag;
import no.nav.foreldrepenger.økonomi.ny.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomi.ny.domene.samlinger.OverordnetOppdragKjedeOversikt;
import no.nav.foreldrepenger.økonomi.ny.util.SetUtil;

public class OppdragFactory {

    private BiFunction<FagsakYtelseType, Boolean, ØkonomiKodeFagområde> fagområdeMapper;
    private FagsakYtelseType ytelseType;
    private final Saksnummer saksnummer;
    private LocalDate fellesEndringstidspunkt;

    public OppdragFactory(BiFunction<FagsakYtelseType, Boolean, ØkonomiKodeFagområde> fagområdeMapper, FagsakYtelseType ytelseType, Saksnummer saksnummer) {
        this.fagområdeMapper = fagområdeMapper;
        this.ytelseType = ytelseType;
        this.saksnummer = saksnummer;
    }

    public void setFellesEndringstidspunkt(LocalDate fellesEndringstidspunkt) {
        this.fellesEndringstidspunkt = fellesEndringstidspunkt;
    }

    public List<Oppdrag> lagOppdrag(OverordnetOppdragKjedeOversikt tidligereOppdrag, GruppertYtelse målbilde) {
        FagsystemIdUtleder fagsystemIdUtleder = new FagsystemIdUtleder(saksnummer, tidligereOppdrag);
        List<Oppdrag> resultat = new ArrayList<>();
        for (Betalingsmottaker betalingsmottaker : SetUtil.sortertUnion(Betalingsmottaker.COMPARATOR, tidligereOppdrag.getBetalingsmottakere(), målbilde.getBetalingsmottakere())) {
            var tidligereOppdragForMottaker = tidligereOppdrag.filter(betalingsmottaker);
            var målbildeForMottaker = målbilde.finnYtelse(betalingsmottaker);
            ØkonomiKodeFagområde økonomiFagområde = utledØkonomiFagområde(betalingsmottaker);
            OppdragForMottakerTjeneste oppdragForMottakerTjeneste = new OppdragForMottakerTjeneste(økonomiFagområde, fagsystemIdUtleder.getFagsystemId(betalingsmottaker), betalingsmottaker, fellesEndringstidspunkt);
            resultat.add(oppdragForMottakerTjeneste.lagOppdrag(tidligereOppdragForMottaker, målbildeForMottaker));
        }
        return resultat.stream()
            .filter(Oppdrag::harLinjer)
            .collect(Collectors.toList());
    }

    private ØkonomiKodeFagområde utledØkonomiFagområde(Betalingsmottaker betalingsmottaker) {
        boolean erRefusjon = !Betalingsmottaker.BRUKER.equals(betalingsmottaker);
        return fagområdeMapper.apply(ytelseType, erRefusjon);
    }

    static class FagsystemIdUtleder {

        private FagsystemId nesteFagsystemId;
        private OverordnetOppdragKjedeOversikt tidligereOppdrag;

        FagsystemIdUtleder(Saksnummer saksnummer, OverordnetOppdragKjedeOversikt tidligereOppdrag) {
            this.tidligereOppdrag = tidligereOppdrag;
            nesteFagsystemId = finnNesteFagsystemId(saksnummer, tidligereOppdrag);
        }

        private FagsystemId finnNesteFagsystemId(Saksnummer saksnummer, OverordnetOppdragKjedeOversikt tidligereOppdrag) {
            FagsystemId høyesteFagsystemId = tidligereOppdrag.høyesteFagsystemId();
            return høyesteFagsystemId != null ? høyesteFagsystemId.neste() : FagsystemId.førsteForFagsak(saksnummer.getVerdi());
        }

        public FagsystemId getFagsystemId(Betalingsmottaker betalingsmottaker) {
            FagsystemId fagsystemId = tidligereOppdrag.getFagsystemIdPrMottaker().get(betalingsmottaker);
            if (fagsystemId != null) {
                return fagsystemId;
            }
            try {
                return nesteFagsystemId;
            } finally {
                nesteFagsystemId = nesteFagsystemId.neste();
            }
        }
    }

}

