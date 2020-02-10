package no.nav.foreldrepenger.domene.medlem.impl;

import no.nav.tjeneste.virksomhet.medlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.medlemskap.v2.Sikkerhetsbegrensning;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.IntegrasjonFeil;

public interface MedlemFeil extends DeklarerteFeil {

    MedlemFeil FACTORY = FeilFactory.create(MedlemFeil.class);

    @IntegrasjonFeil(feilkode = "FP-963823", feilmelding = "Fikk sikkerhetsavvik ved kall til medlemskap tjenesten.", logLevel = LogLevel.ERROR)
    Feil fikkSikkerhetsavvikFraMedlem(Sikkerhetsbegrensning e);

    @IntegrasjonFeil(feilkode = "FP-085790", feilmelding = "Feil ved kall til medlemskap tjenesten.", logLevel = LogLevel.ERROR)
    Feil feilVedKallTilMedlem(PersonIkkeFunnet e);
}
