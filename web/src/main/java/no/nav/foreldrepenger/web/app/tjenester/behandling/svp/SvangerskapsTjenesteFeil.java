package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import no.nav.vedtak.exception.TekniskException;

final class SvangerskapsTjenesteFeil {

    private SvangerskapsTjenesteFeil() {
    }

    static TekniskException kanIkkeFinneTerminbekreftelsePåSvangerskapspengerSøknad(Long behandlingId) {
        return new TekniskException("FP-598421", "Finner ikke terminbekreftelse på svangerskapspengerssøknad med behandling: " + behandlingId);
    }

    static TekniskException kanIkkeFinneSvangerskapspengerGrunnlagForBehandling(Long behandlingId) {
        return new TekniskException("FP-254831", "Finner ikke svangerskapspenger grunnlag for behandling: " + behandlingId);
    }

}
