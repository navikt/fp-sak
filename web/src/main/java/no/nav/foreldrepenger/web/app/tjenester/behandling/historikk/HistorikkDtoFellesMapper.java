package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.historikk.dto.HistorikkInnslagDokumentLinkDto;

public class HistorikkDtoFellesMapper {

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
            skjermlenkeOpt.isEmpty() ? h.getType().getNavn() : null,
            tekster);
    }

    @SafeVarargs
    public static void leggTilAlleTeksterIHovedliste(List<String> hovedListe, List<String>... lister) {
        for (List<String> liste : lister) {
            hovedListe.addAll(liste);
        }
    }

    private static Optional<SkjermlenkeType> skjermlenkeFra(Historikkinnslag h) {
        return h.getHistorikkinnslagDeler().stream()
            .flatMap(del -> del.getSkjermlenke().stream())
            .map(SkjermlenkeType::fraKode)
            .findFirst();
    }


}
