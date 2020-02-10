package no.nav.foreldrepenger.web.app.tjenester.registrering;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.vedtak.felles.xml.soeknad.v3.Soeknad;

public interface SøknadMapper {

    <V extends ManuellRegistreringDto> Soeknad mapSøknad(V registreringDto, NavBruker navBruker);

}
