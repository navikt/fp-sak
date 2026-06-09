package no.nav.foreldrepenger.mottak.fyllutsendinn.frontend;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(AksjonspunktKodeDefinisjon.REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD_KODE)
public class PapirsoknadEngangsstonadMellomlagreDto extends PapirsoknadMellomlagreDto {

}
