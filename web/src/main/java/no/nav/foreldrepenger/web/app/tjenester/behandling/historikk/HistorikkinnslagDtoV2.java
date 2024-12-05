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
                                    List<Linje> linjer) {

    public record HistorikkAktørDto(HistorikkAktør type, String ident) {
        public static HistorikkAktørDto fra(HistorikkAktør aktør, String opprettetAv) {
            if (Set.of(HistorikkAktør.SAKSBEHANDLER, HistorikkAktør.BESLUTTER).contains(aktør)) {
                return new HistorikkAktørDto(aktør, opprettetAv);
            }
            return new HistorikkAktørDto(aktør, null);
        }
    }

    public record Linje(Type type, String tekst) {
        public static Linje tekstlinje(String tekst) {
            return new Linje(Type.TEKST, tekst);
        }

        public static Linje linjeskift() {
            return new Linje(Type.LINJESKIFT, null);
        }

        public boolean erLinjeskift() {
            return Type.LINJESKIFT.equals(type);
        }

        enum Type {
            TEKST,
            LINJESKIFT
        }
    }
}
