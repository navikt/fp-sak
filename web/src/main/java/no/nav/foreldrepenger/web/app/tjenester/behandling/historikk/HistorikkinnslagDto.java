package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;

public record HistorikkinnslagDto(UUID behandlingUuid,
                                  @NotNull HistorikkAktørDto aktør,
                                  SkjermlenkeType skjermlenke,
                                  @NotNull LocalDateTime opprettetTidspunkt,
                                  List<HistorikkInnslagDokumentLinkDto> dokumenter,
                                  String tittel,
                                  @NotNull List<Linje> linjer) {

    public record HistorikkAktørDto(@NotNull HistorikkAktør type, String ident) {
        public static HistorikkAktørDto fra(HistorikkAktør aktør, String opprettetAv) {
            if (Set.of(HistorikkAktør.SAKSBEHANDLER, HistorikkAktør.BESLUTTER).contains(aktør)) {
                return new HistorikkAktørDto(aktør, opprettetAv);
            }
            return new HistorikkAktørDto(aktør, null);
        }
    }

    public record Linje(@NotNull Type type, String tekst) {
        public static Linje tekstlinje(String tekst) {
            return new Linje(Type.TEKST, tekst);
        }

        public static Linje linjeskift() {
            return new Linje(Type.LINJESKIFT, null);
        }

        public enum Type {
            TEKST,
            LINJESKIFT
        }
    }
}
