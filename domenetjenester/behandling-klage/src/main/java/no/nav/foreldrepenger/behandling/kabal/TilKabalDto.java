package no.nav.foreldrepenger.behandling.kabal;

import java.time.LocalDate;
import java.util.List;

import javax.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;

/**
 * Tilpasset versjon av V3 https://kabal-api.dev.intern.nav.no/swagger-ui/?urls.primaryName=external#/kabal-api-external/sendInnSakV3UsingPOST
 *
 * Har utelatt dvhReferanse (=kildereferanse=behandlinguuid), frist, innsynUrl, sakMottattKaDato, sakenGjelder
 */
public record TilKabalDto(@NotNull Klager klager,
                          @NotNull String forrigeBehandlendeEnhet, // Førsteinstans
                          @NotNull List<DokumentReferanse> tilknyttedeJournalposter,
                          @NotNull LocalDate brukersHenvendelseMottattNavDato, // Required Mottattdato?
                          @NotNull LocalDate innsendtTilNav, // Innsendingsdato?
                          @NotNull String kilde, //Fagsystem.FPSAK.getOffisiellKode();
                          @NotNull String kildeReferanse, // Typisk BehandlingUUID
                          @NotNull String dvhReferanse,
                          @NotNull KlageAnke type,
                          @NotNull KabalYtelse ytelse,
                          Sak fagsak,
                          List<String> hjemler,
                          String kommentar) {

    public static TilKabalDto klage(Behandling behandling,
                                    @NotNull Klager klager,
                                    @NotNull String forrigeBehandlendeEnhet, // Førsteinstans
                                    @NotNull List<DokumentReferanse> tilknyttedeJournalposter,
                                    @NotNull LocalDate brukersHenvendelseMottattNavDato, // Required Mottattdato?
                                    @NotNull LocalDate innsendtTilNav, // Innsendingsdato?
                                    List<String> hjemler,
                                    String kommentar) {
        return new TilKabalDto(klager, forrigeBehandlendeEnhet, tilknyttedeJournalposter,
            brukersHenvendelseMottattNavDato, innsendtTilNav, Fagsystem.FPSAK.getOffisiellKode(),
            behandling.getUuid().toString(), behandling.getUuid().toString(), KlageAnke.KLAGE, mapYtelseType(behandling),
            new Sak(behandling.getFagsak().getSaksnummer().getVerdi(), Fagsystem.FPSAK.getOffisiellKode()),
            hjemler, kommentar);
    }

    public static TilKabalDto anke(Behandling behandling,
                                   String kildereferanse,
                                   @NotNull Klager klager,
                                   @NotNull String forrigeBehandlendeEnhet, // Førsteinstans
                                   @NotNull List<DokumentReferanse> tilknyttedeJournalposter,
                                   @NotNull LocalDate brukersHenvendelseMottattNavDato, // Required Mottattdato?
                                   @NotNull LocalDate innsendtTilNav, // Innsendingsdato?
                                   List<String> hjemler) {
        return new TilKabalDto(klager, forrigeBehandlendeEnhet, tilknyttedeJournalposter,
            brukersHenvendelseMottattNavDato, innsendtTilNav, Fagsystem.FPSAK.getOffisiellKode(),
            kildereferanse, behandling.getUuid().toString(), KlageAnke.ANKE, mapYtelseType(behandling),
            new Sak(behandling.getFagsak().getSaksnummer().getVerdi(), Fagsystem.FPSAK.getOffisiellKode()),
            hjemler, "");
    }

    public static record Sak(@NotNull String fagsakId, @NotNull String fagsystem) {}

    public static record Part(@NotNull PartsType type, @NotNull String verdi) {}

    public static record Fullmektig(@NotNull Part id, @NotNull boolean skalKlagerMottaKopi) {}

    public static record Klager(@NotNull Part id, Fullmektig klagersProsessfullmektig) {}

    public static record DokumentReferanse(@NotNull String journalpostId, @NotNull DokumentReferanseType type) {}

    public enum DokumentReferanseType {
        ANNET, BRUKERS_KLAGE, BRUKERS_SOEKNAD, KLAGE_VEDTAK, OPPRINNELIG_VEDTAK, OVERSENDELSESBREV
    }

    public enum PartsType {
        PERSON, VIRKSOMHET
    }

    public enum KlageAnke {
        ANKE, KLAGE
    }

    public enum KabalYtelse {
        FOR_ENG, FOR_FOR, FOR_SVA
    }

    private static KabalYtelse mapYtelseType(Behandling behandling) {
        return switch (behandling.getFagsakYtelseType()) {
            case ENGANGSTØNAD -> TilKabalDto.KabalYtelse.FOR_ENG;
            case FORELDREPENGER -> TilKabalDto.KabalYtelse.FOR_FOR;
            case SVANGERSKAPSPENGER -> TilKabalDto.KabalYtelse.FOR_SVA;
            default -> throw new IllegalArgumentException("Mangler ytelsetype");
        };
    }

}
