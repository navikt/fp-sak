package no.nav.foreldrepenger.poststed;


import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.IntegrasjonFeil;

public interface PoststedFeil extends DeklarerteFeil {

    PoststedFeil FACTORY = FeilFactory.create(PoststedFeil.class);

    @IntegrasjonFeil(feilkode = "FP-868814", feilmelding = "Kodeverk ikke funnet", logLevel = LogLevel.ERROR)
    Feil hentPoststedKodeverkIkkeFunnet();

    @IntegrasjonFeil(feilkode = "FP-402871", feilmelding = "Kodeverktype ikke støttet: %s", logLevel = LogLevel.ERROR)
    Feil hentPoststedKodeverkTypeIkkeStøttet(String kodeverkType);

    @IntegrasjonFeil(feilkode = "FP-563156", feilmelding = "Synkronisering av kodeverk feilet", logLevel = LogLevel.WARN)
    Feil synkroniseringAvPoststedFeilet(IntegrasjonException e);

}
