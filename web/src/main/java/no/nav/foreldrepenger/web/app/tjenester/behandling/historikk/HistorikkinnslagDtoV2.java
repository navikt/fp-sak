package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.historikk.dto.HistorikkInnslagDokumentLinkDto;

public record HistorikkinnslagDtoV2(UUID behandlingUuid,
                                    HistorikkAktørDto aktør,
                                    SkjermlenkeType skjermlenke,
                                    LocalDateTime opprettetTidspunkt,
                                    List<HistorikkInnslagDokumentLinkDto> dokumenter,
                                    String tittel,
                                    String body) {

    public record HistorikkAktørDto(HistorikkAktør type, String ident) {
        public static HistorikkAktørDto fra(HistorikkAktør aktør, String opprettetAv) {
            if (Set.of(HistorikkAktør.SAKSBEHANDLER, HistorikkAktør.BESLUTTER).contains(aktør)) {
                return new HistorikkAktørDto(aktør, opprettetAv);
            }
            return new HistorikkAktørDto(aktør, null);
        }
    }

}
