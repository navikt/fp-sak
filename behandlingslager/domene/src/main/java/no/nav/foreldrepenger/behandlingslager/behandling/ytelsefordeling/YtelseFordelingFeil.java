package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

interface YtelseFordelingFeil extends DeklarerteFeil {

    YtelseFordelingFeil FACTORY = FeilFactory.create(YtelseFordelingFeil.class);

    @TekniskFeil(feilkode = "FP-634781", feilmelding = "Fant ikke forventet YtelseFordeling grunnlag for behandling med id %s", logLevel = LogLevel.WARN)
    Feil fantIkkeForventetGrunnlagPåBehandling(Long behandlingId);

    @TekniskFeil(feilkode = "FP-852328", feilmelding = "Kan ikke overstyre søknadsperioder før det finnes noen søknadsperioder å overstyre.", logLevel = LogLevel.WARN)
    Feil kanIkkeOverstyreDetFinnesIkkeOrginalSøknadsperiode();


}
