package no.nav.foreldrepenger.web.app.tjenester.registrering.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.es.ManuellRegistreringEngangsstonadDto;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;

public class ManuellRegistreringDtoTest {

    @Test
    public void skal_sende_behandlingId_og_FagsakId_til_abac() throws Exception {
        ManuellRegistreringDto dto = new ManuellRegistreringEngangsstonadDto();

        assertThat(dto.abacAttributter()).isEqualTo(AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.AKSJONSPUNKT_KODE, AksjonspunktKodeDefinisjon.REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD_KODE));
    }
}
