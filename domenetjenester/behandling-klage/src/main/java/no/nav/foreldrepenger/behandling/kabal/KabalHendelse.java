package no.nav.foreldrepenger.behandling.kabal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record KabalHendelse(UUID eventId,
                            String kildeReferanse,
                            String kilde,
                            String kabalReferanse,
                            BehandlingEventType type,
                            BehandlingDetaljer detaljer) {

    public enum BehandlingEventType {
        BEHANDLING_FEILREGISTRERT, KLAGEBEHANDLING_AVSLUTTET,
        ANKEBEHANDLING_OPPRETTET, ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET, ANKEBEHANDLING_AVSLUTTET,
        BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET_AVSLUTTET
    }

    public record BehandlingDetaljer(KlagebehandlingAvsluttetDetaljer klagebehandlingAvsluttet,
                                     AnkebehandlingOpprettetDetaljer ankebehandlingOpprettet,
                                     AnkeITrygderettenbehandlingOpprettetDetaljer ankeITrygderettenbehandlingOpprettet,
                                     AnkebehandlingAvsluttetDetaljer ankebehandlingAvsluttet,
                                     BehandlingEtterTrygderettenOpphevetAvsluttet behandlingEtterTrygderettenOpphevetAvsluttet,
                                     BehandlingFeilregistrertDetaljer behandlingFeilregistrert) {}

    public record KlagebehandlingAvsluttetDetaljer(LocalDateTime avsluttet, KabalUtfall utfall, List<String> journalpostReferanser) {}

    public record AnkebehandlingOpprettetDetaljer(LocalDateTime mottattKlageinstans) {}

    public record AnkeITrygderettenbehandlingOpprettetDetaljer(LocalDateTime sendtTilTrygderetten, KabalUtfall utfall) {}

    public record AnkebehandlingAvsluttetDetaljer(LocalDateTime avsluttet, KabalUtfall utfall, List<String> journalpostReferanser) {}

    public record BehandlingEtterTrygderettenOpphevetAvsluttet(LocalDateTime avsluttet, KabalUtfall utfall, List<String> journalpostReferanser) {}

    public record BehandlingFeilregistrertDetaljer(LocalDateTime feilregistrert, BehandlingType type, String navIdent, String reason) {}

    public enum BehandlingType {
        KLAGE, ANKE, ANKE_I_TRYGDERETTEN
    }
}


