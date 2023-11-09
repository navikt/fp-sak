package no.nav.foreldrepenger.web.app.tjenester.registrering.es;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringDto;

@JsonTypeName(AksjonspunktKodeDefinisjon.REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD_KODE)
public class ManuellRegistreringEngangsstonadDto extends ManuellRegistreringDto {

}
