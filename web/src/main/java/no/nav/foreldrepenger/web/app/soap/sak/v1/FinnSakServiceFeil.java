package no.nav.foreldrepenger.web.app.soap.sak.v1;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

interface FinnSakServiceFeil extends DeklarerteFeil {
    FinnSakServiceFeil FACTORY = FeilFactory.create(FinnSakServiceFeil.class);

    @TekniskFeil(feilkode = "FP-132949", feilmelding = "Ikke-støttet årsakstype: %s", logLevel = LogLevel.WARN)
    Feil ikkeStøttetÅrsakstype(BehandlingTema behandlingTema);

    @TekniskFeil(feilkode = "FP-861850", feilmelding = "Ikke-støttet ytelsestype: %s", logLevel = LogLevel.WARN)
    Feil ikkeStøttetYtelsestype(FagsakYtelseType fagsakYtelseType);
}
