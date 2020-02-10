package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import static no.nav.vedtak.feil.LogLevel.ERROR;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface SøknadDtoFeil extends DeklarerteFeil {
    SøknadDtoFeil FACTORY = FeilFactory.create(SøknadDtoFeil.class);

    @TekniskFeil(feilkode = "FP-113411", feilmelding = "Annen forelder på søknad kan ikke være samme person som søker", logLevel = ERROR)
    Feil kanIkkeVæreBådeFarOgMorTilEtBarn();

    @TekniskFeil(feilkode = "FP-175810", feilmelding = "Ektefelle kan ikke være samme person som søker", logLevel = ERROR)
    Feil kanIkkeVæreSammePersonSomSøker();


}
