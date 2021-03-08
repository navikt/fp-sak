package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import static no.nav.vedtak.feil.LogLevel.WARN;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.FunksjonellFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface SvangerskapsTjenesteFeil extends DeklarerteFeil {
    SvangerskapsTjenesteFeil FACTORY = FeilFactory.create(SvangerskapsTjenesteFeil.class);

    @TekniskFeil(feilkode = "FP-598421",
        feilmelding = "Finner ikke terminbekreftelse på svangerskapspengerssøknad med behandling: %s", logLevel = WARN)
    Feil kanIkkeFinneTerminbekreftelsePåSvangerskapspengerSøknad(Long behandlingId);

    @TekniskFeil(feilkode = "FP-254831",
        feilmelding = "Finner ikke svangerskapspenger grunnlag for behandling: %s", logLevel = WARN)
    Feil kanIkkeFinneSvangerskapspengerGrunnlagForBehandling(Long behandlingId);

    @TekniskFeil(feilkode = "FP-572361",
        feilmelding = "Finner ikke eksisterende tilrettelegging på svangerskapspengergrunnlag med identifikator: %s", logLevel = WARN)
    Feil kanIkkFinneTilretteleggingForSvangerskapspenger(Long tilretteleggingId);

    @TekniskFeil(feilkode = "FP-564312",
        feilmelding = "Antall overstyrte arbeidsforhold for svangerskapspenger stemmre ikke overens med arbeidsforhold fra søknaden: %s", logLevel = WARN)
    Feil overstyrteArbeidsforholdStemmerIkkeOverensMedSøknadsgrunnlag(Long behandlingId);
}
