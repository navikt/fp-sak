package no.nav.foreldrepenger.mottak.kabal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.kabal.KabalHendelse;
import no.nav.foreldrepenger.behandling.kabal.KabalUtfall;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;

class KabelHendelseTest  {

    @Test
    void kabalJsonParse() throws IOException {
        var payload = """
                {
                    "eventId": "b79cae6f-4afc-4f51-9e0b-623bb53f1805",
                    "kildeReferanse": "23da11e6-8130-4edc-acee-2eb2a5fd4d97",
                    "kilde": "FS36",
                    "type": "KLAGEBEHANDLING_AVSLUTTET",
                    "detaljer": {
                       "klagebehandlingAvsluttet": {
                          "utfall": "MEDHOLD",
                          "journalpostReferanser": [
                             "510857598"
                          ]
                       }
                    },
                    "kabalReferanse": "af4204bb-7f2c-4354-8af9-b279a5492104"
                }
            """;

        var hendelse = StandardJsonConfig.fromJson(payload, KabalHendelse.class);

        assertThat(hendelse.kilde()).isEqualTo(Fagsystem.FPSAK.getOffisiellKode());
        assertThat(hendelse.type()).isEqualTo(KabalHendelse.BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET);
        assertThat(hendelse.detaljer().klagebehandlingAvsluttet().utfall()).isEqualTo(KabalUtfall.MEDHOLD);
        assertThat(hendelse.detaljer().klagebehandlingAvsluttet().journalpostReferanser().get(0)).isEqualTo("510857598");

    }

}
