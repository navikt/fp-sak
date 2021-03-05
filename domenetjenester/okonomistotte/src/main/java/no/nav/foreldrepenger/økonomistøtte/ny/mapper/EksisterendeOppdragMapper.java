package no.nav.foreldrepenger.økonomistøtte.ny.mapper;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeStatusLinje;
import no.nav.foreldrepenger.økonomistøtte.OppdragKvitteringTjeneste;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.DelytelseId;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.OppdragKjede;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.OppdragLinje;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Periode;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Satsen;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.SatsType;
import no.nav.vedtak.util.env.Environment;


public class EksisterendeOppdragMapper {

    private static final Logger LOG = LoggerFactory.getLogger(EksisterendeOppdragMapper.class);

    public static Map<KjedeNøkkel, OppdragKjede> tilKjeder(List<Oppdragskontroll> oppdragskontroll) {
        return oppdragTilKjeder(oppdragskontroll.stream().flatMap(ok -> ok.getOppdrag110Liste().stream()).collect(Collectors.toList()));
    }

    public static Map<KjedeNøkkel, OppdragKjede> oppdragTilKjeder(List<Oppdrag110> tidligereOppdrag) {
        List<Oppdrag110> godkjenteOppdrag = sorterOgVelgKunGyldige(tidligereOppdrag);
        var buildere = lagOppdragskjedeBuildere(godkjenteOppdrag);
        return build(buildere);
    }

    private static Map<KjedeNøkkel, OppdragKjede.Builder> lagOppdragskjedeBuildere(List<Oppdrag110> godkjenteOppdrag) {
        Map<DelytelseId, KjedeNøkkel> nøkkelMap = new HashMap<>();
        Map<DelytelseId, OppdragKjede.Builder> builderMap = new HashMap<>();
        Set<DelytelseId> startpunkt = new TreeSet<>();

        for (Oppdrag110 oppdrag110 : godkjenteOppdrag) {
            for (Oppdragslinje150 linje : sortert(oppdrag110.getOppdragslinje150Liste())) {
                KjedeNøkkel nøkkel = tilNøkkel(linje);
                OppdragLinje oppdragslinje = tilOppdragslinje(linje);

                DelytelseId delytelseId = oppdragslinje.getDelytelseId();
                DelytelseId refDelytelseId = oppdragslinje.getRefDelytelseId();

                OppdragKjede.Builder builder;
                if (refDelytelseId != null) {
                    builder = builderMap.get(refDelytelseId);
                } else if (oppdragslinje.erOpphørslinje()) {
                    builder = builderMap.get(delytelseId);
                } else {
                    builder = OppdragKjede.builder();
                }
                validerNøkkelKonsistentGjennomKjeden(nøkkelMap, nøkkel, delytelseId, refDelytelseId);
                builderMap.put(delytelseId, builder.medOppdragslinje(oppdragslinje));
                nøkkelMap.put(delytelseId, nøkkel);
                if (refDelytelseId == null && !oppdragslinje.erOpphørslinje()) {
                    startpunkt.add(delytelseId);
                }
            }
        }

        Map<KjedeNøkkel, OppdragKjede.Builder> resultat = new HashMap<>();
        for (DelytelseId delytelseId : startpunkt) {
            KjedeNøkkel preferertNøkkel = nøkkelMap.get(delytelseId);
            KjedeNøkkel ledigNøkkel = finnLedigNøkkel(preferertNøkkel, resultat.keySet());
            boolean harEksisterendeOpphørtKjede = resultat.containsKey(preferertNøkkel) && resultat.get(preferertNøkkel).erEffektivtTom();
            OppdragKjede.Builder kjedeBuilder = builderMap.get(delytelseId);
            if (harEksisterendeOpphørtKjede) {
                resultat.put(ledigNøkkel, resultat.get(preferertNøkkel));
                resultat.put(preferertNøkkel, kjedeBuilder);
            } else {
                resultat.put(ledigNøkkel, kjedeBuilder);
            }
        }

        return resultat;
    }

    private static KjedeNøkkel finnLedigNøkkel(KjedeNøkkel preferertNøkkel, Set<KjedeNøkkel> brukteNøkler) {
        KjedeNøkkel nøkkel = preferertNøkkel;
        while (brukteNøkler.contains(nøkkel)) {
            nøkkel = nøkkel.forNesteKnekteKjededel();
        }
        return nøkkel;
    }

    private static void validerNøkkelKonsistentGjennomKjeden(Map<DelytelseId, KjedeNøkkel> nøkkelMap, KjedeNøkkel nøkkel, DelytelseId delytelseId, DelytelseId refDelytelseId) {
        if (refDelytelseId != null && !nøkkel.equals(nøkkelMap.get(refDelytelseId))) {
            if (Environment.current().isProd()) {
                //må kunne takle hvis dette finnes (gjør det i k9-oppdrag i hvert fall)
                LOG.warn("Linje med delytelseId {} peker på kjede med en annen nøkkel", delytelseId);
            } else {
                throw new IllegalArgumentException("Linje med delytelseId " + delytelseId + " peker på kjede med en annen nøkkel");
            }
        }
    }

    private static Map<KjedeNøkkel, OppdragKjede> build(Map<KjedeNøkkel, OppdragKjede.Builder> buildere) {
        Map<KjedeNøkkel, OppdragKjede> kjeder = new HashMap<>();
        for (var entry : buildere.entrySet()) {
            kjeder.put(entry.getKey(), entry.getValue().build());
        }
        return kjeder;
    }

    private static List<Oppdrag110> sorterOgVelgKunGyldige(List<Oppdrag110> tidligereOppdrag) {
        return tidligereOppdrag.stream()
            .filter(ok -> ok.venterKvittering() || OppdragKvitteringTjeneste.harPositivKvittering(ok))
            .sorted(Comparator.comparing(Oppdrag110::getAvstemming))
            .collect(Collectors.toList());
    }

    private static OppdragLinje tilOppdragslinje(Oppdragslinje150 linje) {
        return OppdragLinje.builder()
            .medDelytelseId(linje.getDelytelseId())
            .medRefDelytelseId(linje.getRefDelytelseId())
            .medPeriode(Periode.of(linje.getDatoVedtakFom(), linje.getDatoVedtakTom()))
            .medSats(mapSats(linje))
            .medUtbetalingsgrad(mapUtbetalingsgrad(linje))
            .medOpphørFomDato(mapOpphørsdato(linje))
            .build();
    }

    private static LocalDate mapOpphørsdato(Oppdragslinje150 linje) {
        if (linje.getDatoStatusFom() == null) {
            return null;
        }
        if (KodeStatusLinje.OPPHØR.equals(linje.getKodeStatusLinje())) {
            return linje.getDatoStatusFom();
        }
        throw new IllegalStateException("Fikk ikke-støttet kodeStatus=" + linje.getKodeStatusLinje());
    }

    private static no.nav.foreldrepenger.økonomistøtte.ny.domene.Utbetalingsgrad mapUtbetalingsgrad(Oppdragslinje150 linje) {
        return Optional.ofNullable(linje.getUtbetalingsgrad()).map(Utbetalingsgrad::getVerdi).map(no.nav.foreldrepenger.økonomistøtte.ny.domene.Utbetalingsgrad::new).orElse(null);
    }

    private static Satsen mapSats(Oppdragslinje150 linje) {
        if (linje.getTypeSats().getKode().equals(SatsType.DAG.getKode())) {
            return Satsen.dagsats(linje.getSats().getVerdi().longValue());
        } else if (linje.getTypeSats().getKode().equals(SatsType.ENGANG.getKode())) {
            return Satsen.engang(linje.getSats().getVerdi().longValue());
        } else {
            throw new IllegalArgumentException("Ikke-støttet satstype: " + linje.getTypeSats());
        }
    }

    private static KjedeNøkkel tilNøkkel(Oppdragslinje150 linje) {
        Refusjonsinfo156 refusjonsinfo = linje.getRefusjonsinfo156();
        Betalingsmottaker mottaker = refusjonsinfo == null
            ? Betalingsmottaker.BRUKER
            : Betalingsmottaker.forArbeidsgiver(normaliserOrgnr(refusjonsinfo.getRefunderesId()));
        KjedeNøkkel.Builder builder = KjedeNøkkel.builder(linje.getKodeKlassifik(), mottaker);
        if (linje.getKodeKlassifik().gjelderFeriepenger()) {
            builder.medFeriepengeÅr(linje.getDatoVedtakFom().getYear() - 1);
        }
        return builder.build();
    }

    private static String normaliserOrgnr(String orgnr) {
        if (orgnr.length() == 11 && orgnr.startsWith("00")) {
            return orgnr.substring(2);
        } else if (orgnr.length() == 9) {
            return orgnr;
        } else {
            throw new IllegalArgumentException("orgnr skal være 9 tegn, eller 11 tegn og starte med 00");
        }
    }

    private static List<Oppdragslinje150> sortert(List<Oppdragslinje150> oppdragslinje150Liste) {
        return oppdragslinje150Liste.stream()
            .sorted(Comparator.comparing(Oppdragslinje150::getDelytelseId).thenComparing(Oppdragslinje150::getKodeStatusLinje, Comparator.nullsFirst(Comparator.naturalOrder())))
            .collect(Collectors.toList());
    }

}
