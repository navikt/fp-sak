package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.historikk.dto.HistorikkInnslagDokumentLinkDto;

public class HistorikkDtoFellesMapper {

    private static final Logger LOG = LoggerFactory.getLogger(HistorikkDtoFellesMapper.class);
    private static final String TOM_LINJE = "";

    public static HistorikkinnslagDtoV2 tilHistorikkInnslagDto(Historikkinnslag h, UUID behandlingUUID, List<String> tekster) {
        return tilHistorikkInnslagDto(h, behandlingUUID, null, tekster);
    }

    public static HistorikkinnslagDtoV2 tilHistorikkInnslagDto(Historikkinnslag h, UUID behandlingUUID, List<HistorikkInnslagDokumentLinkDto> lenker, List<String> tekster) {
        var skjermlenkeOpt = skjermlenkeFra(h);

        return new HistorikkinnslagDtoV2(
            behandlingUUID,
            HistorikkinnslagDtoV2.HistorikkAktørDto.fra(h.getAktør(), h.getOpprettetAv()),
            skjermlenkeOpt.orElse(null),
            h.getOpprettetTidspunkt(),
            lenker,
            skjermlenkeOpt.isEmpty() ? lagTittel(h) : null,
            fjernTrailingAvsnittFraTekst(tekster)
        );
    }

    private static String lagTittel(Historikkinnslag h) {
        var hendelseFelt = h.getHistorikkinnslagDeler().stream()
            .map(HistorikkinnslagDel::getHendelse)
            .flatMap(Optional::stream)
            .toList();
        if (hendelseFelt.size() > 1) {
            LOG.info("Flere deler med HENDELSE-felt for historikkinnslag {} på sak {}. Er alle like? Er det noe grunn til å ha undertittler? ", h.getId(), h.getFagsakId());
        }

        if (hendelseFelt.isEmpty()) {
            return h.getType().getNavn();
        }
        return fraHendelseFelt(hendelseFelt.getFirst());
    }

    // BEH_VENT har satt tilverdi som brukes i tittelen (Behandling på vent 05.12.2024)
    public static String fraHendelseFelt(HistorikkinnslagFelt felt) {
        var hendelsetekst = HistorikkinnslagType.fraKode(felt.getNavn()).getNavn();
        return felt.getTilVerdi() != null
            ? String.format("%s %s", hendelsetekst, felt.getTilVerdi())
            : hendelsetekst;
    }

    @SafeVarargs
    public static void leggTilAlleTeksterIHovedliste(List<String> hovedListe, List<String>... lister) {
        for (List<String> liste : lister) {
            hovedListe.addAll(liste);
        }
        hovedListe.add(TOM_LINJE);
    }


    private static List<String> fjernTrailingAvsnittFraTekst(List<String> tekster) {
        if (tekster.isEmpty()) {
            return tekster;
        }

        if (tekster.getLast().equals(TOM_LINJE)) {
            tekster.removeLast();
        }
        return tekster.stream().toList();
    }

    private static Optional<SkjermlenkeType> skjermlenkeFra(Historikkinnslag h) {
        var skjermlenker = h.getHistorikkinnslagDeler().stream().flatMap(del -> del.getSkjermlenke().stream()).toList();
        if (skjermlenker.size() > 1) {
            return Optional.empty();
        } else {
            return skjermlenker.stream()
                .map(SkjermlenkeType::fraKode)
                .findFirst();
        }
    }
}
